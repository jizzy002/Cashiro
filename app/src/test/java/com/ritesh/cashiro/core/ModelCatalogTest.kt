package com.ritesh.cashiro.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ModelCatalogTest {

    @Test
    fun `catalog ids and file names are unique`() {
        val models = Constants.ModelDownload.models
        assertTrue(models.isNotEmpty())
        assertEquals(models.size, models.map { it.id }.distinct().size)
        assertEquals(models.size, models.map { it.fileName }.distinct().size)
    }

    @Test
    fun `default model is present and remains Qwen 2_5 1_5B`() {
        val defaultId = Constants.ModelDownload.defaultModelId
        val default = Constants.ModelDownload.model(defaultId)

        assertEquals(defaultId, default.id)
        assertEquals(Constants.ModelDownload.MODEL_FILE_NAME, default.fileName)
        assertEquals("Qwen2.5 1.5B", default.displayName)
    }

    @Test
    fun `every model has a secure url and a plausible size`() {
        Constants.ModelDownload.models.forEach { model ->
            assertTrue("${model.id} url", model.url.startsWith("https://"))
            assertTrue("${model.id} size", model.sizeBytes > 0)
            assertTrue("${model.id} file", model.fileName.isNotBlank())
            assertTrue("${model.id} name", model.displayName.isNotBlank())
        }
    }

    @Test
    fun `unknown model ids resolve to the default`() {
        val default = Constants.ModelDownload.model(Constants.ModelDownload.defaultModelId)
        assertEquals(default, Constants.ModelDownload.model("does-not-exist"))
        assertEquals(default, Constants.ModelDownload.model(null))
    }

    @Test
    fun `legacy constants point at the default entry`() {
        val default = Constants.ModelDownload.model(Constants.ModelDownload.defaultModelId)

        assertEquals(default.url, Constants.ModelDownload.MODEL_URL)
        assertEquals(default.fileName, Constants.ModelDownload.MODEL_FILE_NAME)
        assertEquals(default.sizeBytes, Constants.ModelDownload.MODEL_SIZE_BYTES)
        assertEquals(default.sizeMb, Constants.ModelDownload.MODEL_SIZE_MB)
        assertEquals((default.sizeBytes * 1.2).toLong(), Constants.ModelDownload.REQUIRED_SPACE_BYTES)
    }
}
