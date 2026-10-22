package com.ritesh.cashiro.domain.service

import com.ritesh.cashiro.data.database.dao.MerchantMappingDao
import com.ritesh.cashiro.data.database.entity.MerchantMappingEntity
import com.ritesh.cashiro.data.database.entity.TransactionEntity
import com.ritesh.cashiro.data.database.entity.TransactionType
import com.ritesh.cashiro.data.repository.MerchantMappingRepository
import java.math.BigDecimal
import java.time.LocalDateTime
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Test
import kotlin.test.assertEquals

class AutomaticCategoryResolverTest {

    private val mappingStorage = mutableMapOf<String, String>()

    private val merchantMappingRepository = MerchantMappingRepository(
        merchantMappingDao = RecordingMerchantMappingDao(mappingStorage)
    )

    private class RecordingMerchantMappingDao(
        private val storage: MutableMap<String, String>
    ) : MerchantMappingDao {
        override suspend fun getCategoryForMerchant(merchantName: String): String? =
            storage[merchantName]

        override suspend fun getCategoryForMerchantCaseInsensitive(merchantName: String): String? =
            storage.entries
                .firstOrNull { it.key.lowercase() == merchantName.lowercase() }
                ?.value

        override suspend fun insertOrUpdateMapping(mapping: MerchantMappingEntity) {
            storage[mapping.merchantName] = mapping.category
        }

        override suspend fun insertMapping(mapping: MerchantMappingEntity) {
            storage[mapping.merchantName] = mapping.category
        }

        override suspend fun deleteMapping(merchantName: String) {
            storage.remove(merchantName)
        }

        override fun getAllMappings(): Flow<List<MerchantMappingEntity>> =
            flowOf(storage.entries.sortedBy { it.key }.map { (name, category) ->
                MerchantMappingEntity(merchantName = name, category = category)
            })

        override suspend fun getMappingCount(): Int = storage.size

        override suspend fun deleteAllMappings() {
            storage.clear()
        }
    }

    private fun transaction(
        id: Long,
        merchant: String,
        category: String?,
        type: TransactionType = TransactionType.EXPENSE
    ): TransactionEntity = TransactionEntity(
        id = id,
        amount = BigDecimal("42"),
        merchantName = merchant,
        category = category ?: "",
        transactionType = type,
        dateTime = LocalDateTime.now(),
        transactionHash = "hash-$id"
    )

    @Test
    fun computesChangesForLearnedMerchant(): Unit = runBlocking {
        mappingStorage["pavic doo"] = "Transport"

        val preview = AutomaticCategoryResolver.computePreview(
            listOf(transaction(1, "PAVIC DOO 12", "Groceries")),
            merchantMappingRepository
        )

        assertEquals(1, preview.scannedTransactions)
        assertEquals(1, preview.changes.size)
        assertEquals("Transport", preview.changes.first().newCategory)
    }

    @Test
    fun computesChangesForStaticMerchant(): Unit = runBlocking {
        val preview = AutomaticCategoryResolver.computePreview(
            listOf(transaction(1, "KONZUM 12", "Miscellaneous")),
            merchantMappingRepository
        )

        assertEquals(1, preview.scannedTransactions)
        assertEquals("Groceries", preview.changes.first().newCategory)
    }

    @Test
    fun skipsTransactionsAlreadyCategorized(): Unit = runBlocking {
        val preview = AutomaticCategoryResolver.computePreview(
            listOf(transaction(1, "KONZUM 12", "Groceries")),
            merchantMappingRepository
        )

        assertEquals(1, preview.scannedTransactions)
        assertEquals(0, preview.changes.size)
    }

    @Test
    fun skipsDowngradeToMiscellaneous(): Unit = runBlocking {
        val preview = AutomaticCategoryResolver.computePreview(
            listOf(transaction(1, "IU IT TEAM", "Business")),
            merchantMappingRepository
        )

        assertEquals(1, preview.scannedTransactions)
        assertEquals(0, preview.changes.size)
    }

    @Test
    fun keepsNoChangesForUnknownMerchantWhenCategoryIsMiscellaneous(): Unit = runBlocking {
        val preview = AutomaticCategoryResolver.computePreview(
            listOf(transaction(1, "UNKNOWN MERCHANT", "Miscellaneous")),
            merchantMappingRepository
        )

        assertEquals(1, preview.scannedTransactions)
        assertEquals(0, preview.changes.size)
    }

    @Test
    fun skipsExpenseKeywordProposalsForIncomeTransactions(): Unit = runBlocking {
        val preview = AutomaticCategoryResolver.computePreview(
            listOf(transaction(1, "KONZUM 12", "Miscellaneous", TransactionType.INCOME)),
            merchantMappingRepository
        )

        assertEquals(1, preview.scannedTransactions)
        assertEquals(0, preview.changes.size)
    }

    @Test
    fun proposesSalaryKeywordForIncomeTransactions(): Unit = runBlocking {
        val preview = AutomaticCategoryResolver.computePreview(
            listOf(transaction(1, "PLATA M BH DOO", "Miscellaneous", TransactionType.INCOME)),
            merchantMappingRepository
        )

        assertEquals(1, preview.changes.size)
        assertEquals("Salary", preview.changes.first().newCategory)
    }

    @Test
    fun learnedMappingStillProposesForIncomeTransactions(): Unit = runBlocking {
        mappingStorage["wise"] = "Business"

        val preview = AutomaticCategoryResolver.computePreview(
            listOf(transaction(1, "WISE 1234", "Miscellaneous", TransactionType.INCOME)),
            merchantMappingRepository
        )

        assertEquals(1, preview.changes.size)
        assertEquals("Business", preview.changes.first().newCategory)
    }
}
