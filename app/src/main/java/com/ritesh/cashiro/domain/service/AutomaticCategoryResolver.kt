package com.ritesh.cashiro.domain.service

import com.ritesh.cashiro.data.database.entity.TransactionEntity
import com.ritesh.cashiro.data.database.entity.TransactionType
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
            val proposed = resolve(transaction, merchantMappingRepository) ?: return@mapNotNull null

            if (proposed == transaction.category || proposed == MISCELLANEOUS) {
                null
            } else {
                AutomaticCategoryChange(transaction, proposed)
            }
        }
        return AutomaticCategoryPreview(scannedTransactions = transactions.size, changes = changes)
    }

    /**
     * Mirrors import parity: learned mappings apply to every transaction type,
     * while keyword-pack proposals follow the mapper's income behavior.
     */
    private suspend fun resolve(
        transaction: TransactionEntity,
        merchantMappingRepository: MerchantMappingRepository
    ): String? {
        val learned = merchantMappingRepository.getCategoryForMerchant(transaction.merchantName)
        if (learned != null) return learned

        val keywordCategory = CategoryMapping.getCategory(transaction.merchantName)

        return when (transaction.transactionType) {
            TransactionType.EXPENSE -> keywordCategory
            // Income keep the mapper's Salary outcome; other income keywords are not proposed.
            TransactionType.INCOME -> keywordCategory.takeIf { it == "Salary" }
            else -> null
        }.takeUnless { it == MISCELLANEOUS }
    }
}
