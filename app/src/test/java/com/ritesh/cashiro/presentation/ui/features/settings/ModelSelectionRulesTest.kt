package com.ritesh.cashiro.presentation.ui.features.settings

import com.ritesh.cashiro.core.Constants
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ModelSelectionRulesTest {

    @Test
    fun `selecting the already selected model is a no-op`() {
        assertFalse(
            ModelSelectionRules.canSelect(
                currentId = "qwen2.5-1.5b",
                targetId = "qwen2.5-1.5b",
                isDownloading = false
            )
        )
    }

    @Test
    fun `a different model can be selected when idle`() {
        assertTrue(
            ModelSelectionRules.canSelect(
                currentId = "qwen2.5-1.5b",
                targetId = "qwen3-0.6b",
                isDownloading = false
            )
        )
    }

    @Test
    fun `selection is blocked while a download is running`() {
        assertFalse(
            ModelSelectionRules.canSelect(
                currentId = "qwen2.5-1.5b",
                targetId = "qwen3-0.6b",
                isDownloading = true
            )
        )
    }

    @Test
    fun `downloaded ids only include catalog entries that report complete`() {
        val complete = setOf("qwen3-0.6b")

        val downloaded = ModelSelectionRules.downloadedIds(
            models = Constants.ModelDownload.models,
            isDownloaded = { it.id in complete }
        )

        assertEquals(setOf("qwen3-0.6b"), downloaded)
    }
}
