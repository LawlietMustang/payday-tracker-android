package com.paydaytracker.app.auth

import android.app.Activity
import android.os.CancellationSignal
import androidx.credentials.*
import androidx.credentials.exceptions.*
import com.google.android.libraries.identity.googleid.*
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import org.json.JSONObject
import java.util.concurrent.Executor

// Firebase validates the Google credential. No token is exposed to the WebView or backups.
class GoogleAccount(private val activity: Activity, private val changed: (String) -> Unit) {
    private val executor = Executor { activity.runOnUiThread(it) }
    private val manager by lazy { CredentialManager.create(activity) }
    private var signal: CancellationSignal? = null
    private var busy = false
    private var clientId = ""
    private val auth: FirebaseAuth? by lazy {
        try {
            val config = JSONObject(activity.assets.open("google-services.json").bufferedReader().use { it.readText() })
            val clients = config.getJSONArray("client")
            val client = (0 until clients.length()).map { clients.getJSONObject(it) }.first {
                it.getJSONObject("client_info").getJSONObject("android_client_info").getString("package_name") == activity.packageName
            }
            val oauth = client.getJSONArray("oauth_client")
            clientId = (0 until oauth.length()).map { oauth.getJSONObject(it) }.first { it.getInt("client_type") == 3 }.getString("client_id")
            val options = FirebaseOptions.Builder().setApplicationId(client.getJSONObject("client_info").getString("mobilesdk_app_id"))
                .setApiKey(client.getJSONArray("api_key").getJSONObject(0).getString("current_key"))
                .setProjectId(config.getJSONObject("project_info").getString("project_id")).build()
            val app = FirebaseApp.getApps(activity).firstOrNull { it.name == "payday-auth" } ?: FirebaseApp.initializeApp(activity, options, "payday-auth")
            FirebaseAuth.getInstance(app)
        } catch (_: Exception) { null }
    }
    fun state(): String {
        val firebase = auth
        return JSONObject().put("configured", firebase != null && clientId.isNotBlank()).put("busy", busy)
            .put("email", firebase?.currentUser?.email ?: "").put("name", firebase?.currentUser?.displayName ?: "").toString()
    }
    fun signIn() {
        if (busy) return
        val firebase = auth
        if (firebase == null || clientId.isBlank()) { changed("setup"); return }
        busy = true; signal = CancellationSignal(); changed("busy")
        try {
            val option = GetSignInWithGoogleOption.Builder(clientId).build()
            val request = GetCredentialRequest.Builder().addCredentialOption(option).build()
            manager.getCredentialAsync(activity, request, signal, executor, object : CredentialManagerCallback<GetCredentialResponse, GetCredentialException> {
                override fun onResult(result: GetCredentialResponse) {
                    if (activity.isDestroyed) return
                    try {
                        val credential = result.credential
                        require(credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL)
                        val google = GoogleIdTokenCredential.createFrom(credential.data)
                        firebase.signInWithCredential(GoogleAuthProvider.getCredential(google.idToken, null)).addOnCompleteListener(executor) { task ->
                            busy = false; if (!activity.isDestroyed) changed(if (task.isSuccessful) "signedIn" else "verifyError")
                        }
                    } catch (_: Exception) { busy = false; changed("error") }
                }
                override fun onError(e: GetCredentialException) {
                    busy = false
                    if (!activity.isDestroyed) changed(if (e is GetCredentialCancellationException) "cancelled" else "error")
                }
            })
        } catch (_: Exception) { busy = false; changed("error") }
    }
    fun signOut() {
        if (busy) return
        auth?.signOut(); busy = true
        manager.clearCredentialStateAsync(ClearCredentialStateRequest(), null, executor, object : CredentialManagerCallback<Void?, ClearCredentialException> {
            override fun onResult(result: Void?) { busy = false; if (!activity.isDestroyed) changed("signedOut") }
            override fun onError(e: ClearCredentialException) { busy = false; if (!activity.isDestroyed) changed("signedOut") }
        })
    }
    fun destroy() { signal?.cancel() }
}
