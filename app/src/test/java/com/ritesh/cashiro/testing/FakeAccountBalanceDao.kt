package com.ritesh.cashiro.testing

import com.ritesh.cashiro.data.database.dao.AccountBalanceDao
import com.ritesh.cashiro.data.database.dao.AccountBalanceTransactionInfo
import com.ritesh.cashiro.data.database.entity.AccountBalanceEntity
import com.ritesh.cashiro.data.database.entity.TransactionType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import java.math.BigDecimal
import java.time.LocalDateTime

internal fun accountKey(bankName: String, accountLast4: String) = "$bankName:$accountLast4"

internal fun balance(
    id: Long,
    bankName: String,
    accountLast4: String,
    balance: BigDecimal,
    timestamp: LocalDateTime,
    isCreditCard: Boolean = false
) = AccountBalanceEntity(
    id = id,
    bankName = bankName,
    accountLast4 = accountLast4,
    balance = balance,
    timestamp = timestamp,
    isCreditCard = isCreditCard
)

internal class FakeAccountBalanceDao(
    private val latestBalances: MutableMap<String, AccountBalanceEntity> = mutableMapOf(),
    private val balanceAtOrBefore: AccountBalanceEntity? = null,
    private val balancesAfter: List<AccountBalanceTransactionInfo> = emptyList(),
    private val suffixMatches: Map<String, Map<String, List<String>>> = emptyMap()
) : AccountBalanceDao() {
    val insertedBalances = mutableListOf<AccountBalanceEntity>()
    val updatedBalances = mutableMapOf<Long, BigDecimal>()
    var suffixLookupCount = 0
    var last4CandidatesLookups = 0
    val balances = mutableListOf<AccountBalanceEntity>()
    val transactionMap = mutableMapOf<Long, Pair<BigDecimal, String>>()
    private var nextId = 1L

    init {
        // Seed constructor values to dynamic balances as fallback
        latestBalances.values.forEach { seedBalance(it) }
        balanceAtOrBefore?.let { seedBalance(it) }
    }

    fun seedBalance(entity: AccountBalanceEntity) {
        if (balances.none { it.id == entity.id && entity.id != 0L }) {
            balances.add(entity.copy(id = entity.id.takeIf { it != 0L } ?: nextId++))
        }
    }

    override suspend fun getBalanceByTransactionId(transactionId: Long): AccountBalanceEntity? {
        return balances.find { it.transactionId == transactionId }
    }

    override suspend fun getBalanceById(id: Long): AccountBalanceEntity? {
        return balances.find { it.id == id }
    }

    override suspend fun insertBalance(balance: AccountBalanceEntity): Long {
        val id = balance.id.takeIf { it != 0L } ?: nextId++
        val saved = balance.copy(id = id)
        balances.add(saved)
        insertedBalances.add(saved)
        latestBalances[accountKey(balance.bankName, balance.accountLast4)] = saved
        return id
    }

    override suspend fun getLatestBalance(bankName: String, accountLast4: String): AccountBalanceEntity? {
        return latestBalances[accountKey(bankName, accountLast4)]
            ?: balances.filter { it.bankName == bankName && it.accountLast4 == accountLast4 }
                .maxWithOrNull(compareBy<AccountBalanceEntity>({ it.timestamp }, { it.id }))
    }

    override suspend fun getLatestBalanceOnOrBefore(
        bankName: String,
        accountLast4: String,
        timestamp: LocalDateTime
    ): AccountBalanceEntity? {
        return balanceAtOrBefore
            ?: balances.filter {
                it.bankName == bankName && it.accountLast4 == accountLast4 && !it.timestamp.isAfter(timestamp)
            }.maxWithOrNull(compareBy<AccountBalanceEntity>({ it.timestamp }, { it.id }))
    }

    override suspend fun getBalancesAfterWithTransactions(
        bankName: String,
        accountLast4: String,
        timestamp: LocalDateTime
    ): List<AccountBalanceTransactionInfo> {
        if (balancesAfter.isNotEmpty()) {
            return balancesAfter
        }
        return balances.filter {
            it.bankName == bankName && it.accountLast4 == accountLast4 && it.timestamp.isAfter(timestamp)
        }.sortedBy { it.timestamp }
         .map { bal ->
             val tx = bal.transactionId?.let { transactionMap[it] }
             AccountBalanceTransactionInfo(
                 id = bal.id,
                 balance = bal.balance,
                 sourceType = bal.sourceType,
                 isCreditCard = bal.isCreditCard,
                 transactionId = bal.transactionId,
                 transactionAmount = tx?.first,
                 transactionType = tx?.second,
                 transactionBalanceAfter = null,
                 isDeleted = false
             )
         }
    }

    override suspend fun getAccountLast4sEndingWith(bankName: String, suffix: String): List<String> {
        suffixLookupCount++
        return suffixMatches[bankName]?.get(suffix).orEmpty()
    }

    override suspend fun getLatestBalancePerAccountForLast4(accountLast4: String): List<AccountBalanceEntity> {
        last4CandidatesLookups++
        return balances.filter { it.accountLast4 == accountLast4 }
            .groupBy { it.bankName to it.accountLast4 }
            .map { (_, rows) ->
                rows.maxWithOrNull(
                    compareBy<AccountBalanceEntity>({ it.timestamp }, { it.id })
                )!!
            }
    }

    override fun getLatestBalanceFlow(
        bankName: String,
        accountLast4: String
    ): Flow<AccountBalanceEntity?> = flowOf(latestBalances[accountKey(bankName, accountLast4)])

    override fun getAllLatestBalances(): Flow<List<AccountBalanceEntity>> = flowOf(latestBalances.values.toList())

    override fun getAllBalances(): Flow<List<AccountBalanceEntity>> = flowOf(latestBalances.values.toList())

    override suspend fun deleteAllBalances() = Unit

    override suspend fun deleteSampleBalances() = Unit

    override fun getCurrentMonthLatestBalances(): Flow<List<AccountBalanceEntity>> = flowOf(emptyList())

    override fun getTotalBalance(): Flow<BigDecimal?> = flowOf(BigDecimal.ZERO)

    override fun getBalanceHistory(
        bankName: String,
        accountLast4: String,
        startDate: LocalDateTime,
        endDate: LocalDateTime
    ): Flow<List<AccountBalanceEntity>> = flowOf(emptyList())

    override fun getAccountCount(): Flow<Int> = flowOf(0)

    override suspend fun deleteOldBalances(beforeDate: LocalDateTime): Int = 0

    override suspend fun updateBalance(balance: AccountBalanceEntity) {
        updatedBalances[balance.id] = balance.balance
        val index = balances.indexOfFirst { it.id == balance.id }
        if (index != -1) {
            balances[index] = balance
        }
    }

    override suspend fun deleteBalance(balance: AccountBalanceEntity) = Unit

    override suspend fun getBalanceHistoryForAccount(
        bankName: String,
        accountLast4: String
    ): List<AccountBalanceEntity> = emptyList()

    override suspend fun deleteBalanceById(id: Long) = Unit

    override suspend fun updateBalanceById(id: Long, newBalance: BigDecimal) {
        updatedBalances[id] = newBalance
        val index = balances.indexOfFirst { it.id == id }
        if (index != -1) {
            balances[index] = balances[index].copy(balance = newBalance)
        }
    }

    override suspend fun getBalanceCountForAccount(bankName: String, accountLast4: String): Int = balances.filter { it.bankName == bankName && it.accountLast4 == accountLast4 }.size

    override suspend fun deleteAccount(bankName: String, accountLast4: String): Int {
        val beforeSize = balances.size
        balances.removeAll { it.bankName == bankName && it.accountLast4 == accountLast4 }
        return beforeSize - balances.size
    }

    override suspend fun updateAccountBankName(
        oldBankName: String,
        accountLast4: String,
        newBankName: String
    ): Int = 0

    override suspend fun getAccountByLast4(accountLast4: String): AccountBalanceEntity? = null

    override suspend fun getEarliestBalance(bankName: String, accountLast4: String): AccountBalanceEntity? {
        return balances.filter { it.bankName == bankName && it.accountLast4 == accountLast4 }
            .minByOrNull { it.timestamp }
    }
}
