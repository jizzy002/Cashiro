package com.ritesh.cashiro.core

/**
 * One downloadable on-device chat model.
 *
 * Sizes are the exact Hugging Face artifact sizes in bytes; completion checks
 * allow a 1 % shortfall for download-manager metadata.
 */
data class ChatModelInfo(
    val id: String,
    val displayName: String,
    val tierLabel: String,
    val url: String,
    val fileName: String,
    val sizeBytes: Long
) {
    /** Decimal megabytes, as shown in download UIs. */
    val sizeMb: Long get() = sizeBytes / 1_000_000

    val requiredSpaceBytes: Long get() = (sizeBytes * 1.2).toLong()
}

/**
 * Application-wide constants to avoid hardcoded values
 */
object Constants {
    
    /**
     * SMS Processing Configuration
     */
    object SmsProcessing {
        const val DEFAULT_BATCH_SIZE = 100
        const val SMS_PREVIEW_LENGTH = 200
        const val QUERY_LIMIT = 100
        const val INITIAL_SCAN_MONTHS = 3
        const val SCANNING_DELAY_MS = 3000L
    }
    
    /**
     * UI Configuration - Moved to ui/theme/Dimensions.kt for better organization
     * Keeping only non-dimension constants here
     */
    object UI {
        const val BUTTON_WIDTH_RATIO = 0.8f
        const val PROGRESS_STROKE_WIDTH = 2f
    }
    
    /**
     * Database Configuration
     */
    object Database {
        const val DATABASE_NAME = "cashiro_database"
        const val CURRENT_VERSION = 2
        const val TRANSACTION_HASH_DEFAULT = ""
    }
    
    /**
     * WorkManager Configuration
     */
    object WorkManager {
        const val SMS_READER_WORK_NAME = "sms_reader_work"
        const val PERIODIC_SCAN_INTERVAL_HOURS = 24L
        const val INITIAL_DELAY_MINUTES = 15L
    }
    
    /**
     * Parsing Configuration
     */
    object Parsing {
        const val MIN_MERCHANT_NAME_LENGTH = 2
        const val AMOUNT_SCALE = 2
        const val CONFIDENCE_PATTERN_BASED = 0.7f
        const val CONFIDENCE_AI_BASED = 0.9f
    }
    
    /**
     * Navigation Routes
     */
    object Routes {
        const val HOME = "home"
        const val TRANSACTIONS = "transactions"
        const val ANALYTICS = "analytics"
        const val CHAT = "chat"
        const val SETTINGS = "settings"
    }
    
    /**
     * LLM Model Configuration
     */
    object ModelDownload {
        const val DEFAULT_MODEL_ID = "qwen2.5-1.5b"

        /** Lower-case alias used by UI/repository code. */
        val defaultModelId: String get() = DEFAULT_MODEL_ID

        val models: List<ChatModelInfo> = listOf(
            ChatModelInfo(
                id = DEFAULT_MODEL_ID,
                displayName = "Qwen2.5 1.5B",
                tierLabel = "Best quality",
                url = "https://huggingface.co/litert-community/Qwen2.5-1.5B-Instruct/resolve/main/Qwen2.5-1.5B-Instruct_multi-prefill-seq_q8_ekv4096.task?download=true",
                fileName = "qwen2.5-1.5b-instruct.task",
                sizeBytes = 1_598_556_720L
            ),
            ChatModelInfo(
                id = "qwen3-0.6b",
                displayName = "Qwen3 0.6B",
                tierLabel = "Fast · smaller",
                url = "https://huggingface.co/litert-community/Qwen3-0.6B/resolve/main/qwen3_0_6b_mixed_int4.litertlm?download=true",
                fileName = "qwen3-0.6b-mixed-int4.litertlm",
                sizeBytes = 497_516_544L
            ),
            ChatModelInfo(
                id = "smollm2-360m",
                displayName = "SmolLM2 360M",
                tierLabel = "Tiny · fastest",
                url = "https://huggingface.co/litert-community/SmolLM2-360M-Instruct/resolve/main/SmolLM2_360M_instruct.litertlm?download=true",
                fileName = "smollm2-360m-instruct.litertlm",
                sizeBytes = 373_719_040L
            )
        )

        /** Unknown ids fall back to the default, so a stale preference is harmless. */
        fun model(id: String?): ChatModelInfo =
            models.firstOrNull { it.id == id } ?: models.first { it.id == DEFAULT_MODEL_ID }

        // Legacy single-model constants kept for existing call sites.
        val MODEL_URL: String = model(DEFAULT_MODEL_ID).url
        val MODEL_FILE_NAME: String = model(DEFAULT_MODEL_ID).fileName
        val MODEL_SIZE_MB: Long = model(DEFAULT_MODEL_ID).sizeMb
        val MODEL_SIZE_BYTES: Long = model(DEFAULT_MODEL_ID).sizeBytes
        val REQUIRED_SPACE_BYTES: Long = model(DEFAULT_MODEL_ID).requiredSpaceBytes
    }

    /**
     * External Links
     */
    object Links {
        const val DISCORD_URL = "https://discord.gg/6qaYgpJTg"
        const val GITHUB_URL = "https://github.com/ritesh-kanwar/Cashiro"
        const val WEBSITE_URL = "https://ritesh-kanwar.github.io/cashiro.showcase"
        const val PRIVACY_POLICY_URL = "https://ritesh-kanwar.github.io/cashiro.showcase/privacy"
        const val TERMS_OF_SERVICE_URL = "https://ritesh-kanwar.github.io/cashiro.showcase/terms"
        const val FAQ_URL = "https://ritesh-kanwar.github.io/cashiro.showcase/faq"
        const val GUIDE_URL = "https://ritesh-kanwar.github.io/cashiro.showcase/guides"
        const val REPORT_BUG_URL = "https://github.com/ritesh-kanwar/Cashiro/issues/new/choose"
    }
    object Device {
        val is64Bit = android.os.Build.SUPPORTED_ABIS.firstOrNull()
            ?.let { it == "arm64-v8a" || it == "x86_64" } == true
    }
}
