package com.vatsalya.founderpocket.data.security

import android.content.Context
import android.net.Uri
import androidx.security.crypto.EncryptedFile
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.InputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Spike E — Encrypted document storage using Jetpack Security EncryptedFile.
 *
 * Documents (Aadhaar, PAN, passport, resume, etc.) are stored as AES-256-GCM
 * encrypted blobs under filesDir/docs/. Only the filename is stored in Room —
 * raw bytes never touch the database. Never log the filename or the URI.
 *
 * Usage:
 *   val ref = store.save(pickedUri, "aadhaar")  // returns "doc_<ts>_aadhaar.enc"
 *   val stream = store.openInput(ref)            // returns decrypted InputStream
 *   store.delete(ref)                            // secure delete
 */
@Singleton
class EncryptedDocStore @Inject constructor(@ApplicationContext private val context: Context) {

    private val masterKey: MasterKey by lazy {
        MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
    }

    private val docsDir: File get() = context.filesDir.resolve("docs").also { it.mkdirs() }

    /**
     * Encrypts the content at [uri] and writes it to an encrypted file.
     * @return filename (the encrypted reference to store in Room payload)
     */
    fun save(uri: Uri, docType: String): String {
        val filename = "doc_${System.currentTimeMillis()}_$docType.enc"
        val encFile = buildEncryptedFile(filename)
        context.contentResolver.openInputStream(uri)?.use { src ->
            encFile.openFileOutput().use { dst -> src.copyTo(dst) }
        } ?: error("Could not open input stream for $uri")
        return filename
    }

    /**
     * Returns a decrypted [InputStream] for the stored document, or null if not found.
     * Caller is responsible for closing the stream.
     */
    fun openInput(filename: String): InputStream? {
        val file = docsDir.resolve(filename)
        if (!file.exists()) return null
        return runCatching { buildEncryptedFile(filename).openFileInput() }
            .getOrNull()
    }

    fun exists(filename: String): Boolean = docsDir.resolve(filename).exists()

    fun delete(filename: String): Boolean = docsDir.resolve(filename).let {
        it.writeBytes(ByteArray(it.length().toInt())) // overwrite before delete
        it.delete()
    }

    private fun buildEncryptedFile(filename: String) = EncryptedFile.Builder(
        context,
        docsDir.resolve(filename),
        masterKey,
        EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB
    ).build()
}
