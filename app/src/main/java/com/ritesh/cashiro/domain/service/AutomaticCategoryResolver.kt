package com.ritesh.cashiro.domain.service

import com.ritesh.cashiro.data.database.entity.TransactionEntity
import com.ritesh.cashiro.data.repository.MerchantMappingRepository
import com.ritesh.cashiro.presentation.common.icons.CategoryMapping

/**
 * A single proposed category change for one transaction, produced by the
 * automatic (non-AI) merchant resolution flow.
 */
data class AutomaticCategoryChange(
    val transaction: TransactionEntity,
    val newCategory: String
)

/**
 * Result of scanning a list of transactions:
 * [scannedTransactions] is the scope size, [changes] the proposed rewrites.
 */
data class AutomaticCategoryPreview(
    val scannedTransactions: Int,
    val changes: List<AutomaticCategoryChange>
)

/**
 * Resolves automatic categories for existing transactions.
 *
 * Resolution order matches the import flow: learned merchant mappings first,
 * then the bundled [CategoryMapping] keyword pack. It never invents mappings
 * or rules and never proposes a downgrade to [MISCELLANEOUS]; manually fixed
 * categories are only touched when the user explicitly accepts the change.
 */
object AutomaticCategoryResolver {

    private const val MISCELLANEOUS = "Miscellaneous"

    suspend fun computePreview(
        transactions: List<TransactionEntity>,
        merchantMappingRepository: MerchantMappingRepository
    ): AutomaticCategoryPreview {
        val changes = transactions.mapNotNull { transaction ->
            val proposed = merchantMappingRepository.getCategoryForMerchant(transaction.merchantName)
                ?: CategoryMapping.getCategory(transaction.merchantName)

            if (proposed == transaction.category || proposed == MISCELLANEOUS) {
                null
            } else {
                AutomaticCategoryChange(transaction, proposed)
            }
        }
        return AutomaticCategoryPreview(scannedTransactions = transactions.size, changes = changes)
    }
}
