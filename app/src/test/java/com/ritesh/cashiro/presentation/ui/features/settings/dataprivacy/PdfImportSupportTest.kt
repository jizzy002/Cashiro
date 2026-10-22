package com.ritesh.cashiro.presentation.ui.features.settings.dataprivacy

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Duration

class PdfImportSupportTest {

    @Test
    fun `date only statements get a 48 hour duplicate window`() {
        assertEquals(
            Duration.ofHours(48),
            PdfImportSupport.duplicateWindow(hasTimeOfDay = false)
        )
    }

    @Test
    fun `statements with time of day keep the 15 minute window`() {
        assertEquals(
            Duration.ofMinutes(15),
            PdfImportSupport.duplicateWindow(hasTimeOfDay = true)
        )
    }
}
