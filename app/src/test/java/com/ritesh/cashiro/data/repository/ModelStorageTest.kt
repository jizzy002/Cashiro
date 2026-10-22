package com.ritesh.cashiro.data.repository

import com.ritesh.cashiro.core.ChatModelInfo
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File
import java.nio.file.Files

class ModelStorageTest {

    private lateinit var dir: File

    private val smallModel = ChatModelInfo(
        id = "small",
        displayName = "Small",
        tierLabel = "Test",
        url = "https://example.com/small.task",
        fileName = "small.task",
        sizeBytes = 100L
    )

    private val otherModel = smallModel.copy(id = "other", fileName = "other.task")

    @Before
    fun setUp() {
        dir = Files.createTempDirectory("model-storage").toFile()
    }

    @After
    fun tearDown() {
        dir.deleteRecursively()
    }

    @Test
    fun `file for a model uses its catalog file name`() {
        assertEquals(File(dir, "small.task"), ModelStorage.fileFor(dir, smallModel))
    }

    @Test
    fun `a file at 99 percent of the expected size is complete`() {
        val file = File(dir, smallModel.fileName).apply { writeBytes(ByteArray(99)) }

        assertTrue(ModelStorage.isComplete(file, smallModel))
    }

    @Test
    fun `a short file is incomplete`() {
        val file = File(dir, smallModel.fileName).apply { writeBytes(ByteArray(98)) }

        assertFalse(ModelStorage.isComplete(file, smallModel))
    }

    @Test
    fun `a missing file is incomplete`() {
        assertFalse(ModelStorage.isComplete(File(dir, smallModel.fileName), smallModel))
    }

    @Test
    fun `deleting one model leaves the other files untouched`() {
        val small = File(dir, smallModel.fileName).apply { writeBytes(ByteArray(100)) }
        val other = File(dir, otherModel.fileName).apply { writeBytes(ByteArray(100)) }

        assertTrue(ModelStorage.delete(dir, smallModel))
        assertFalse(small.exists())
        assertTrue(other.exists())
    }

    @Test
    fun `deleting a missing model reports false`() {
        assertFalse(ModelStorage.delete(dir, smallModel))
    }
}
