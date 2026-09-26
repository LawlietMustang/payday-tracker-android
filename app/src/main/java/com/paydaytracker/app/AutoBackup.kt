package com.paydaytracker.app

import com.paydaytracker.app.reminders.BackupReminder

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.DocumentsContract as Docs
import android.util.AtomicFile
import org.json.JSONObject
import java.io.File
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

// One serialized writer: rapid edits replace the pending snapshot, never race file writes.
class AutoBackup private constructor(private val context: Context) {
    private val prefs = context.getSharedPreferences("auto-backup", Context.MODE_PRIVATE)
    private val resolver = context.contentResolver
    private val worker = Executors.newSingleThreadScheduledExecutor()
    private val pending = AtomicFile(File(context.filesDir, "backup-pending.json"))
    private var scheduled: ScheduledFuture<*>? = null
    @Volatile var changed: (() -> Unit)? = null
    private val enabled get() = prefs.getBoolean("enabled", false)
    init { if (enabled && pending.baseFile.exists()) worker.execute { schedule(1000) } }
    private fun notifyChanged() { Handler(Looper.getMainLooper()).post { changed?.invoke() } }
    fun state(): String = JSONObject().apply {
        put("enabled", enabled); put("folder", prefs.getString("folder", ""))
        put("status", prefs.getString("status", if (enabled) "pending" else "off"))
        put("hash", prefs.getString("hash", "")); put("queuedHash", prefs.getString("queuedHash", ""))
        put("savedAt", prefs.getLong("savedAt", 0))
    }.toString()
    fun configure(uri: Uri, flags: Int) { worker.execute {
        try {
            val grants = flags and (Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            require(grants == (Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION))
            resolver.takePersistableUriPermission(uri, grants)
            val root = Docs.buildDocumentUriUsingTree(uri, Docs.getTreeDocumentId(uri))
            val name = resolver.query(root, arrayOf(Docs.Document.COLUMN_DISPLAY_NAME), null, null, null)?.use { if (it.moveToFirst()) it.getString(0) else null } ?: "WageTrack"
            scheduled?.cancel(false); pending.delete()
            prefs.edit().clear().putBoolean("enabled", true).putString("tree", uri.toString()).putString("folder", name).putString("status", "pending").commit()
            notifyChanged()
        } catch (_: Exception) { fail() }
    } }
    fun disable() { worker.execute {
        scheduled?.cancel(false); pending.delete()
        prefs.edit().putBoolean("enabled", false).remove("queuedHash").putString("status", "off").commit()
        notifyChanged()
    } }
    fun enqueue(document: String, hash: String) { worker.execute {
        if (!enabled || !hash.matches(Regex("[a-f0-9]{64}"))) return@execute
        try {
            validate(document.toByteArray(Charsets.UTF_8))
            if (hash == prefs.getString("hash", "")) {
                // An undo can return to the saved snapshot while a newer edit is queued.
                scheduled?.cancel(false); pending.delete()
                prefs.edit().remove("queuedHash").putString("status", "saved").commit(); notifyChanged()
                return@execute
            }
            if (hash == prefs.getString("queuedHash", "")) return@execute
            val bytes = JSONObject().put("hash", hash).put("document", document).toString().toByteArray(Charsets.UTF_8)
            val stream = pending.startWrite()
            try { stream.write(bytes); pending.finishWrite(stream) } catch (e: Exception) { pending.failWrite(stream); throw e }
            prefs.edit().putString("queuedHash", hash).putString("status", "pending").commit()
            notifyChanged(); schedule(3000)
        } catch (_: Exception) {
            // Retain the rejected revision too, so UI refreshes cannot form a retry loop.
            prefs.edit().putString("queuedHash", hash).commit(); fail()
        }
    } }
    fun flush() { worker.execute { if (enabled && pending.baseFile.exists()) schedule(0) } }
    fun retry() { worker.execute {
        if (!enabled) return@execute
        if (pending.baseFile.exists()) schedule(0)
        else { prefs.edit().remove("queuedHash").putString("status", "pending").commit(); notifyChanged() }
    } }
    private fun schedule(delay: Long) { scheduled?.cancel(false); scheduled = worker.schedule({ writePending() }, delay, TimeUnit.MILLISECONDS) }
    private fun fail() { prefs.edit().putString("status", "error").commit(); notifyChanged() }
    private fun writePending() {
        if (!enabled || !pending.baseFile.exists()) return
        try {
            val snapshot = JSONObject(String(pending.readFully(), Charsets.UTF_8))
            val bytes = snapshot.getString("document").toByteArray(Charsets.UTF_8)
            validate(bytes)
            prefs.edit().putString("status", "saving").commit(); notifyChanged()
            val tree = Uri.parse(prefs.getString("tree", ""))
            val root = Docs.buildDocumentUriUsingTree(tree, Docs.getTreeDocumentId(tree))
            val files = mutableMapOf<String, Uri>()
            val children = Docs.buildChildDocumentsUriUsingTree(tree, Docs.getTreeDocumentId(tree))
            resolver.query(children, arrayOf(Docs.Document.COLUMN_DOCUMENT_ID, Docs.Document.COLUMN_DISPLAY_NAME), null, null, null)?.use { c ->
                while (c.moveToNext()) files[c.getString(1)] = Docs.buildDocumentUriUsingTree(tree, c.getString(0))
            } ?: error("Folder unavailable")
            fun file(name: String): Uri = files.getOrPut(name) { requireNotNull(Docs.createDocument(resolver, root, "application/json", name)) }
            // Validate a staging file first. The old current file is untouched on staging failure.
            val staged = file("WageTrack-pending.json")
            writeVerified(staged, bytes)
            val current = files[CURRENT]
            // A damaged current copy must never replace the last good previous copy.
            val old = current?.let { read(it) }
            if (old != null && valid(old) && !old.contentEquals(bytes)) writeVerified(file(PREVIOUS), old)
            writeVerified(current ?: file(CURRENT), bytes)
            try { Docs.deleteDocument(resolver, staged) } catch (_: Exception) { /* reused next time */ }
            prefs.edit().putString("hash", snapshot.getString("hash")).putLong("savedAt", System.currentTimeMillis()).putString("status", "saved").remove("queuedHash").commit()
            pending.delete()
            BackupReminder.update(context, false, context.getSharedPreferences("device",0).getString("language","de") ?: "de")
            notifyChanged()
        } catch (_: Exception) { fail() }
    }
    private fun read(uri: Uri): ByteArray = requireNotNull(resolver.openInputStream(uri)).use { input ->
        val out = java.io.ByteArrayOutputStream(); val buffer = ByteArray(8192)
        while (true) { val n = input.read(buffer); if (n < 0) break; require(out.size()+n <= LIMIT); out.write(buffer,0,n) }; out.toByteArray()
    }
    private fun writeVerified(uri: Uri, bytes: ByteArray) {
        requireNotNull(resolver.openOutputStream(uri, "wt")).use { it.write(bytes) }
        check(read(uri).contentEquals(bytes)) { "Backup verification failed" }
    }
    private fun valid(bytes: ByteArray): Boolean = try { validate(bytes); true } catch (_: Exception) { false }
    private fun validate(bytes: ByteArray) {
        require(bytes.size <= LIMIT)
        val doc = JSONObject(String(bytes, Charsets.UTF_8))
        require(doc.getString("app") == "PaydayTracker" && doc.getInt("version") == 1)
        doc.getJSONObject("data").getJSONArray("shifts"); doc.getJSONObject("data").getJSONObject("settings")
    }
    companion object {
        const val CURRENT = "WageTrack-backup.json"
        const val PREVIOUS = "WageTrack-previous.json"
        private const val LIMIT = 10 * 1024 * 1024
        @Volatile private var instance: AutoBackup? = null
        fun get(context: Context): AutoBackup = instance ?: synchronized(this) { instance ?: AutoBackup(context.applicationContext).also { instance = it } }
    }
}
