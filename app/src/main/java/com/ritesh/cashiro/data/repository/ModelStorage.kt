package com.ritesh.cashiro.data.repository

import com.ritesh.cashiro.core.ChatModelInfo
import java.io.File

/**
 * Pure file-layout and completeness rules for downloaded chat models.
 *
 * Kept free of Android APIs so the download rules can be unit tested.
 */
internal object ModelStorage {

    /** A file at 99 % of the published artifact size counts as complete. */
    private const val COMPLETE_FRACTION = 0.99

    fun fileFor(directory: File, model: ChatModelInfo): File =
        File(directory, model.fileName)

    fun isComplete(file: File, model: ChatModelInfo): Boolean {
        val minBytes = (model.sizeBytes * COMPLETE_FRACTION).toLong()
        return file.isFile && file.length() >= minBytes
    }

    fun delete(directory: File, model: ChatModelInfo): Boolean =
        fileFor(directory, model).delete()
}
