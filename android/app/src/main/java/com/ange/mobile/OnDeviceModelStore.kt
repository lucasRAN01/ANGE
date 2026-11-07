package com.ange.mobile

import android.content.Context
import android.net.Uri
import java.io.File
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.util.concurrent.CancellationException

/** Download/import is explicit. No conversation or key is sent to the model host. */
class OnDeviceModelStore(private val context: Context) {
    val file = File(context.filesDir, "qwen3-phone.litertlm")
    private val stamp = File(context.filesDir, "qwen3-phone.sha256")
    @Volatile private var cancelled = false
    @Volatile private var connection: HttpURLConnection? = null
    fun ready(): Boolean = runCatching { file.length() == OnDevicePolicy.MODEL_BYTES && stamp.readText().trim() == OnDevicePolicy.MODEL_SHA256 }.getOrDefault(false)
    fun cancel() { cancelled = true; connection?.disconnect() }
    fun remove() { file.delete(); stamp.delete() }
    fun download(progress: (Long) -> Unit) {
        cancelled = false
        val http = URL(OnDevicePolicy.MODEL_URL).openConnection() as HttpURLConnection
        connection = http
        http.connectTimeout = 20000; http.readTimeout = 45000
        http.instanceFollowRedirects = true
        try {
            require(http.responseCode == 200) { "Téléchargement indisponible (${http.responseCode}). Vous pouvez importer le modèle depuis un fichier." }
            http.inputStream.use { receive(it, progress) }
        } finally { http.disconnect(); connection = null }
    }
    fun import(uri: Uri, progress: (Long) -> Unit) {
        cancelled = false
        val stream = context.contentResolver.openInputStream(uri) ?: error("Fichier inaccessible.")
        stream.use { receive(it, progress) }
    }
    private fun receive(input: InputStream, progress: (Long) -> Unit) {
        require(context.filesDir.usableSpace > OnDevicePolicy.MODEL_BYTES + 64L * 1024 * 1024) { "Libérez environ 500 Mo avant de télécharger le modèle." }
        val partial = File(context.filesDir, "qwen3-phone.part")
        val digest = MessageDigest.getInstance("SHA-256")
        var count = 0L
        var lastProgress = 0L
        try {
            partial.outputStream().use { output ->
                val buffer = ByteArray(128 * 1024)
                while (true) {
                    if (cancelled || Thread.currentThread().isInterrupted) throw CancellationException("Import arrêté")
                    val read = input.read(buffer)
                    if (read < 0) break
                    count += read
                    require(count <= OnDevicePolicy.MODEL_BYTES) { "Fichier trop volumineux ou incompatible." }
                    output.write(buffer, 0, read); digest.update(buffer, 0, read)
                    if (count - lastProgress > 1024 * 1024) { progress(count); lastProgress = count }
                }
            }
            val hash = digest.digest().joinToString("") { "%02x".format(it) }
            OnDevicePolicy.validateModel(count, hash)
            if (cancelled || Thread.currentThread().isInterrupted) throw CancellationException("Import arrêté")
            check(partial.renameTo(file)) { "Impossible de finaliser le modèle. Réessayez après avoir supprimé l’ancien modèle." }
            stamp.writeText(hash)
            progress(count)
        } finally { partial.delete() }
    }
}
