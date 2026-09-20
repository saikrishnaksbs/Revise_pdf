package com.revisepdf.core.hash

import java.io.InputStream
import java.io.OutputStream
import java.security.MessageDigest

object ContentHash {
    fun sha256(bytes: ByteArray): String = MessageDigest.getInstance("SHA-256").digest(bytes).toHex()

    // Copies and hashes in a single pass so a large PDF never has to sit in memory as one array.
    fun copyAndHash(input: InputStream, output: OutputStream, bufferSize: Int = 64 * 1024): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val buffer = ByteArray(bufferSize)
        while (true) {
            val read = input.read(buffer)
            if (read < 0) break
            digest.update(buffer, 0, read)
            output.write(buffer, 0, read)
        }
        output.flush()
        return digest.digest().toHex()
    }

    private fun ByteArray.toHex(): String = joinToString("") { "%02x".format(it) }
}
