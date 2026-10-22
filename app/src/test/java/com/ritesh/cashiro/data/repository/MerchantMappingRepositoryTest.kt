package com.ritesh.cashiro.data.repository

import com.ritesh.cashiro.data.database.dao.MerchantMappingDao
import com.ritesh.cashiro.data.database.entity.MerchantMappingEntity
import kotlinx.coroutines.runBlocking
import org.junit.Test
import kotlin.test.assertEquals

class MerchantMappingRepositoryTest {

    private val storage = mutableMapOf<String, MerchantMappingEntity>()

    private val dao = object : MerchantMappingDao {
        override suspend fun getCategoryForMerchant(merchantName: String): String? =
            storage[merchantName]?.category

        override suspend fun insertOrUpdateMapping(mapping: MerchantMappingEntity) {
            storage[mapping.merchantName] = mapping
        }

        override suspend fun insertMapping(mapping: MerchantMappingEntity) {
            storage[mapping.merchantName] = mapping
        }

        override suspend fun deleteMapping(merchantName: String) {
            storage.remove(merchantName)
        }

        override fun getAllMappings(): kotlinx.coroutines.flow.Flow<List<MerchantMappingEntity>> =
            kotlinx.coroutines.flow.flowOf(storage.values.sortedBy { it.merchantName })

        override suspend fun getMappingCount(): Int = storage.size

        override suspend fun deleteAllMappings() {
            storage.clear()
        }
    }

    private fun repository(): MerchantMappingRepository = MerchantMappingRepository(dao)

    @Test
    fun storesAndLooksUpNormalizedMerchantName(): Unit = runBlocking {
        val repository = repository()

        repository.setMapping("PAVIC DOO BENZ CRPKA", "Transport")

        assertEquals(
            "Transport",
            repository.getCategoryForMerchant("PAVIC DOO BENZ CRPKA")
        )
    }

    @Test
    fun stripsTrailingStoreNumberWhenMatching(): Unit = runBlocking {
        val repository = repository()

        repository.setMapping("PAVIC DOO BENZ CRPKA", "Transport")

        assertEquals(
            "Transport",
            repository.getCategoryForMerchant("PAVIC DOO BENZ CRPKA 12")
        )
    }

    @Test
    fun foldsDiacriticsWhenMatching(): Unit = runBlocking {
        val repository = repository()

        repository.setMapping("Pavić Doo Benzinska", "Transport")

        assertEquals(
            "Transport",
            repository.getCategoryForMerchant("PAVIC DOO BENZINSKA")
        )
    }

    @Test
    fun doesNotReturnMappingForDifferentMerchant(): Unit = runBlocking {
        val repository = repository()

        repository.setMapping("PAVIC DOO BENZ CRPKA", "Transport")

        assertEquals(
            null,
            repository.getCategoryForMerchant("SPAR Doo")
        )
    }
}
