package com.ritesh.cashiro.presentation.ui.features.settings.dataprivacy

import java.time.Duration

/**
 * Small, testable decisions shared by the PDF import flow.
 */
internal object PdfImportSupport {

    private val TIMED_WINDOW: Duration = Duration.ofMinutes(15)
    private val DATE_ONLY_WINDOW: Duration = Duration.ofHours(48)

    /**
     * Statements that only carry a calendar date need a wider window because
     * the parsed timestamp is clamped to midnight.
     */
    fun duplicateWindow(hasTimeOfDay: Boolean): Duration =
        if (hasTimeOfDay) TIMED_WINDOW else DATE_ONLY_WINDOW
}
