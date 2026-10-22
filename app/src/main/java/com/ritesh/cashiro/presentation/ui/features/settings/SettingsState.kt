package com.ritesh.cashiro.presentation.ui.features.settings

import com.ritesh.cashiro.core.ChatModelInfo
import com.ritesh.cashiro.core.Constants

data class SettingsUiState(
    val downloadStatus: DownloadState = DownloadState.NOT_DOWNLOADED,
    val downloadProgress: Int = 0,
    val downloadedMB: Long = 0L,
    val totalMB: Long = 0L,
    val selectedModelId: String = Constants.ModelDownload.defaultModelId,
    val availableModels: List<ChatModelInfo> = Constants.ModelDownload.models,
    val downloadedModelIds: Set<String> = emptySet(),
    val importExportMessage: String? = null,
    val exportedBackupFile: java.io.File? = null,
    val isSeeding: Boolean = false,
    val seedMessage: String? = null
)

enum class DownloadState {
    NOT_DOWNLOADED,
    DOWNLOADING,
    PAUSED,
    COMPLETED,
    FAILED,
    ERROR_INSUFFICIENT_SPACE
}
