package com.revisepdf.core.hash

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test

class ContentHashTest {

    @Test
    fun `streaming hash matches the in-memory hash and copies the bytes through`() {
        val payload = ByteArray(200_000) { (it % 251).toByte() }
        val output = ByteArrayOutputStream()

        val streamed = ContentHash.copyAndHash(ByteArrayInputStream(payload), output, bufferSize = 4_096)

        assertEquals(ContentHash.sha256(payload), streamed)
        assertArrayEquals(payload, output.toByteArray())
    }

    @Test
    fun `hashes empty input without copying anything`() {
        val output = ByteArrayOutputStream()
        val streamed = ContentHash.copyAndHash(ByteArrayInputStream(ByteArray(0)), output)

        assertEquals(ContentHash.sha256(ByteArray(0)), streamed)
        assertEquals(0, output.size())
    }

    @Test
    fun `hash is stable and differs for different content`() {
        assertEquals(ContentHash.sha256("abc".toByteArray()), ContentHash.sha256("abc".toByteArray()))
        assert(ContentHash.sha256("abc".toByteArray()) != ContentHash.sha256("abd".toByteArray()))
    }
}
