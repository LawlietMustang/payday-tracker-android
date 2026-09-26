package com.paydaytracker.app.auth

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.KeyGenerator
import javax.crypto.Mac
import javax.crypto.SecretKey

// Only a device-bound verifier is persisted. PINs never cross the WebView bridge.
class AppPin(context: Context) {
    private val prefs = context.getSharedPreferences("app-pin", Context.MODE_PRIVATE)
    val configured get() = prefs.contains("verifier")
    val waitSeconds get() = ((prefs.getLong("until", 0) - System.currentTimeMillis() + 999) / 1000).coerceAtLeast(0)
    private fun encode(bytes: ByteArray) = Base64.encodeToString(bytes, Base64.NO_WRAP)
    private fun digest(pin: String, salt: ByteArray): ByteArray {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        val key = (store.getKey("wagetrack-app-pin", null) as? SecretKey) ?: KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_HMAC_SHA256, "AndroidKeyStore").run {
            init(KeyGenParameterSpec.Builder("wagetrack-app-pin", KeyProperties.PURPOSE_SIGN or KeyProperties.PURPOSE_VERIFY).build()); generateKey()
        }
        return Mac.getInstance("HmacSHA256").run { init(key); update(salt); doFinal(pin.toByteArray(Charsets.UTF_8)) }
    }
    fun create(pin: String) {
        require(pin.matches(Regex("[0-9]{6}")))
        val salt = ByteArray(32).also { SecureRandom().nextBytes(it) }
        check(prefs.edit().putString("salt", encode(salt)).putString("verifier", encode(digest(pin, salt))).putInt("attempts", 0).putLong("until", 0).commit())
    }
    fun verify(pin: String): Boolean {
        if (!configured || waitSeconds > 0) return false
        val valid = try { MessageDigest.isEqual(Base64.decode(prefs.getString("verifier", ""), Base64.NO_WRAP), digest(pin, Base64.decode(prefs.getString("salt", ""), Base64.NO_WRAP))) } catch (_: Exception) { false }
        if (valid) prefs.edit().putInt("attempts", 0).putLong("until", 0).commit()
        else {
            val attempts = prefs.getInt("attempts", 0) + 1
            val delay = if (attempts < 5) 0L else (30000L * (1L shl (attempts - 5).coerceAtMost(4))).coerceAtMost(300000)
            prefs.edit().putInt("attempts", attempts).putLong("until", System.currentTimeMillis() + delay).commit()
        }
        return valid
    }
}
