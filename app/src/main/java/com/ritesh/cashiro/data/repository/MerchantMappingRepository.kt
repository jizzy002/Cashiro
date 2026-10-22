package com.ritesh.cashiro.data.repository

import com.ritesh.cashiro.data.database.dao.MerchantMappingDao
import com.ritesh.cashiro.data.database.entity.MerchantMappingEntity
import kotlinx.coroutines.flow.Flow
import java.time.LocalDateTime
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MerchantMappingRepository @Inject constructor(
    private val merchantMappingDao: MerchantMappingDao
) {
    
    suspend fun getCategoryForMerchant(merchantName: String): String? {
        val patternKey = normalizePatternKey(merchantName)
        merchantMappingDao.getCategoryForMerchant(patternKey)?.let { return it }
        
        // Fallback for legacy raw keys that are still stored in the same column.
        merchantMappingDao.getCategoryForMerchant(merchantName.trim())?.let { return it }
        
        return null
    }
    
    suspend fun setMapping(merchantName: String, category: String) {
        merchantMappingDao.insertOrUpdateMapping(
            MerchantMappingEntity(
                merchantName = normalizePatternKey(merchantName),
                category = category,
                updatedAt = LocalDateTime.now()
            )
        )
    }
    
    suspend fun removeMapping(merchantName: String) {
        merchantMappingDao.deleteMapping(merchantName)
    }
    
    fun getAllMappings(): Flow<List<MerchantMappingEntity>> {
        return merchantMappingDao.getAllMappings()
    }
    
    suspend fun getMappingCount(): Int {
        return merchantMappingDao.getMappingCount()
    }

    suspend fun deleteAllMappings() {
        merchantMappingDao.deleteAllMappings()
    }

    /**
     * Normalizes a raw merchant name into a stable, reusable pattern key.
     *
     *merchant_mappings keeps the pattern in its existing `merchant_name` column,
     * so no schema migration is needed.
     */
    private fun normalizePatternKey(merchantName: String): String {
        var cleaned = merchantName.trim().lowercase(Locale.ROOT)
        cleaned = cleaned
            .replace('č', 'c')
            .replace('ć', 'c')
            .replace('š', 's')
            .replace('ž', 'z')
            .replace('đ', 'd')
        cleaned = cleaned.replace(Regex("[^a-z0-9&]+"), " ").trim()

        // Trailing store/terminal numbers are not part of the merchant identity.
        // Repeat until stripping one token no longer changes the value.
        while (cleaned.contains(" ")) {
            val lastToken = cleaned.substringAfterLast(" ")
            if (lastToken.isEmpty()) {
                cleaned = cleaned.substringBeforeLast(" ").trim()
            } else if (lastToken.all { it.isDigit() }) {
                cleaned = cleaned.substringBeforeLast(" ").trim()
            } else {
                break
            }
        }
        return cleaned
    }
}