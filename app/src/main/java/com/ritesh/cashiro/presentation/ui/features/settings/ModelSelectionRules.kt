package com.ritesh.cashiro.presentation.ui.features.settings

import com.ritesh.cashiro.core.ChatModelInfo

/**
 * Pure selection rules for the model picker, kept out of the ViewModel so they
 * can be unit tested without Android dependencies.
 */
internal object ModelSelectionRules {

    fun canSelect(currentId: String, targetId: String, isDownloading: Boolean): Boolean =
        targetId != currentId && !isDownloading

    fun downloadedIds(
        models: List<ChatModelInfo>,
        isDownloaded: (ChatModelInfo) -> Boolean
    ): Set<String> = models.filter(isDownloaded).map { it.id }.toSet()
}
