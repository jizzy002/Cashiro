package com.ritesh.cashiro.data.repository

/**
 * Decides whether the LLM engine must be (re)initialized for the selected model.
 */
internal object ModelLoadPolicy {

    fun needsInitialization(
        isInitialized: Boolean,
        loadedPath: String?,
        selectedPath: String
    ): Boolean = !isInitialized || loadedPath != selectedPath
}
