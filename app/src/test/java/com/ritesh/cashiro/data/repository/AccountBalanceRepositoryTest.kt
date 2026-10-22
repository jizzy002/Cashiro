package com.ritesh.cashiro.data.repository

import android.content.ContextWrapper
import com.ritesh.cashiro.data.database.dao.AccountBalanceTransactionInfo
import com.ritesh.cashiro.data.database.entity.AccountBalanceEntity
import com.ritesh.cashiro.data.database.entity.TransactionEntity
import com.ritesh.cashiro.data.database.entity.TransactionType
import com.ritesh.cashiro.testing.FakeAccountBalanceDao
import com.ritesh.cashiro.testing.accountKey
import com.ritesh.cashiro.testing.balance
import com.ritesh.parser.core.ParsedTransaction
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDateTime
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class AccountBalanceRepositoryTest {

    @Test
    fun insertTransactionBalanceCalculatesFromBalanceAtTransactionTimeAndRecalculatesLaterCalculatedRows() = runTest {
        val transactionTime = LocalDateTime.of(2026, 5, 23, 20, 35)
        val dao = FakeAccountBalanceDao(
            latestBalances = mutableMapOf(
                accountKey("Test Bank", "1234") to balance(
                    id = 1,
                    bankName = "Test Bank",
                    accountLast4 = "1234",
                    balance = BigDecimal("100.00"),
                    timestamp = transactionTime.minusMinutes(5)
                )
            ),
            balanceAtOrBefore = balance(
                id = 1,
                bankName = "Test Bank",
                accountLast4 = "1234",
                balance = BigDecimal("100.00"),
                timestamp = transactionTime.minusMinutes(5)
            ),
            balancesAfter = listOf(
                AccountBalanceTransactionInfo(
                    id = 2,
                    balance = BigDecimal("90.00"),
                    sourceType = "TRANSACTION_CALCULATED",
                    isCreditCard = false,
                    transactionId = 22,
                    transactionAmount = BigDecimal("10.00"),
                    transactionType = TransactionType.EXPENSE.name,
                    transactionBalanceAfter = null,
                    isDeleted = false
                )
            )
        )
        dao.seedBalance(
            AccountBalanceEntity(
                id = 2,
                bankName = "Test Bank",
                accountLast4 = "1234",
                balance = BigDecimal("90.00"),
                timestamp = transactionTime.plusMinutes(5),
                sourceType = "TRANSACTION_CALCULATED",
                isCreditCard = false,
                transactionId = 22
            )
        )
        val repository = AccountBalanceRepository(dao, ContextWrapper(null))

        repository.insertTransactionBalance(
            bankName = "Test Bank",
            accountLast4 = "1234",
            amount = BigDecimal("50.00"),
            transactionType = TransactionType.INCOME,
            explicitBalance = null,
            timestamp = transactionTime,
            transactionId = 11,
            creditLimit = null,
            isCreditCard = false,
            smsSource = "credited alert",
            currency = "INR"
        )

        assertEquals(BigDecimal("150.00"), dao.insertedBalances.single().balance)
        assertEquals(BigDecimal("140.00"), dao.updatedBalances[2])
    }

    @Test
    fun insertTransactionBalanceDoesNotRecalculateAfterExplicitBalanceBoundary() = runTest {
        val transactionTime = LocalDateTime.of(2026, 5, 23, 20, 35)
        val dao = FakeAccountBalanceDao(
            latestBalances = mutableMapOf(
                accountKey("Test Bank", "1234") to balance(
                    id = 1,
                    bankName = "Test Bank",
                    accountLast4 = "1234",
                    balance = BigDecimal("100.00"),
                    timestamp = transactionTime.minusMinutes(5)
                )
            ),
            balanceAtOrBefore = balance(
                id = 1,
                bankName = "Test Bank",
                accountLast4 = "1234",
                balance = BigDecimal("100.00"),
                timestamp = transactionTime.minusMinutes(5)
            ),
            balancesAfter = listOf(
                AccountBalanceTransactionInfo(
                    id = 2,
                    balance = BigDecimal("200.00"),
                    sourceType = "TRANSACTION_SMS_BALANCE",
                    isCreditCard = false,
                    transactionId = 22,
                    transactionAmount = BigDecimal("10.00"),
                    transactionType = TransactionType.EXPENSE.name,
                    transactionBalanceAfter = BigDecimal("200.00"),
                    isDeleted = false
                )
            )
        )
        val repository = AccountBalanceRepository(dao, ContextWrapper(null))

        repository.insertTransactionBalance(
            bankName = "Test Bank",
            accountLast4 = "1234",
            amount = BigDecimal("50.00"),
            transactionType = TransactionType.INCOME,
            explicitBalance = null,
            timestamp = transactionTime,
            transactionId = 11,
            creditLimit = null,
            isCreditCard = false,
            smsSource = "credited alert",
            currency = "INR"
        )

        assertEquals(emptyMap(), dao.updatedBalances)
    }

    @Test
    fun insertTransactionBalanceDoesNotRecalculateAfterManualBalanceBoundary() = runTest {
        val transactionTime = LocalDateTime.of(2026, 5, 23, 20, 35)
        val dao = FakeAccountBalanceDao(
            latestBalances = mutableMapOf(
                accountKey("Test Bank", "1234") to balance(
                    id = 1,
                    bankName = "Test Bank",
                    accountLast4 = "1234",
                    balance = BigDecimal("100.00"),
                    timestamp = transactionTime.minusMinutes(5)
                )
            ),
            balanceAtOrBefore = balance(
                id = 1,
                bankName = "Test Bank",
                accountLast4 = "1234",
                balance = BigDecimal("100.00"),
                timestamp = transactionTime.minusMinutes(5)
            ),
            balancesAfter = listOf(
                AccountBalanceTransactionInfo(
                    id = 2,
                    balance = BigDecimal("200.00"),
                    sourceType = "MANUAL",
                    isCreditCard = false,
                    transactionId = 22,
                    transactionAmount = BigDecimal("10.00"),
                    transactionType = TransactionType.EXPENSE.name,
                    transactionBalanceAfter = null,
                    isDeleted = false
                )
            )
        )
        val repository = AccountBalanceRepository(dao, ContextWrapper(null))

        repository.insertTransactionBalance(
            bankName = "Test Bank",
            accountLast4 = "1234",
            amount = BigDecimal("50.00"),
            transactionType = TransactionType.INCOME,
            explicitBalance = null,
            timestamp = transactionTime,
            transactionId = 11,
            creditLimit = null,
            isCreditCard = false,
            smsSource = "credited alert",
            currency = "INR"
        )

        assertEquals(emptyMap(), dao.updatedBalances)
    }

    @Test
    fun resolveAccountLast4ExpandsUniqueSameBankSuffix() = runTest {
        val dao = FakeAccountBalanceDao(
            suffixMatches = mapOf("Indian Overseas Bank" to mapOf("99" to listOf("1999")))
        )
        val repository = AccountBalanceRepository(dao, ContextWrapper(null))

        assertEquals("1999", repository.resolveAccountLast4("Indian Overseas Bank", "99"))
    }

    @Test
    fun resolveAccountLast4DoesNotGuessAmbiguousSuffix() = runTest {
        val dao = FakeAccountBalanceDao(
            suffixMatches = mapOf("Test Bank" to mapOf("99" to listOf("1999", "2099")))
        )
        val repository = AccountBalanceRepository(dao, ContextWrapper(null))

        assertEquals("99", repository.resolveAccountLast4("Test Bank", "99"))
    }

    @Test
    fun resolveAccountLast4DoesNotQueryForNonDigitSuffix() = runTest {
        val dao = FakeAccountBalanceDao()
        val repository = AccountBalanceRepository(dao, ContextWrapper(null))

        assertEquals("%", repository.resolveAccountLast4("Test Bank", "%"))
        assertEquals(0, dao.suffixLookupCount)
    }

    @Test
    fun resolveAccountLast4TrimsToLast4ForLongInputs() = runTest {
        val dao = FakeAccountBalanceDao()
        val repository = AccountBalanceRepository(dao, ContextWrapper(null))

        assertEquals("6789", repository.resolveAccountLast4("Test Bank", "123456789"))
    }

    @Test
    fun resolveAccountLast4DoesNotQueryForBlankSuffix() = runTest {
        val dao = FakeAccountBalanceDao()
        val repository = AccountBalanceRepository(dao, ContextWrapper(null))

        assertEquals("", repository.resolveAccountLast4("Test Bank", ""))
        assertEquals(0, dao.suffixLookupCount)
    }

    @Test
    fun insertTransactionBalanceReducesCreditCardBalanceForIncomePayment() = runTest {
        val transactionTime = LocalDateTime.of(2026, 5, 23, 20, 35)
        val dao = FakeAccountBalanceDao(
            latestBalances = mutableMapOf(
                accountKey("Test Bank", "1234") to balance(
                    id = 1,
                    bankName = "Test Bank",
                    accountLast4 = "1234",
                    balance = BigDecimal("100.00"),
                    timestamp = transactionTime.minusMinutes(5),
                    isCreditCard = true
                )
            ),
            balanceAtOrBefore = balance(
                id = 1,
                bankName = "Test Bank",
                accountLast4 = "1234",
                balance = BigDecimal("100.00"),
                timestamp = transactionTime.minusMinutes(5),
                isCreditCard = true
            )
        )
        val repository = AccountBalanceRepository(dao, ContextWrapper(null))

        repository.insertTransactionBalance(
            bankName = "Test Bank",
            accountLast4 = "1234",
            amount = BigDecimal("30.00"),
            transactionType = TransactionType.INCOME,
            explicitBalance = null,
            timestamp = transactionTime,
            transactionId = 11,
            creditLimit = null,
            isCreditCard = true,
            smsSource = "payment alert",
            currency = "INR"
        )

        assertEquals(BigDecimal("70.00"), dao.insertedBalances.single().balance)
    }

    @Test
    fun backdatedInsertRecalculatesCalculatedRowsForward() = runTest {
        val dao = FakeAccountBalanceDao()
        val repository = AccountBalanceRepository(dao, ContextWrapper(null))

        // Seed initial balance at T1 (1000)
        val t1 = LocalDateTime.of(2025, 1, 1, 10, 0)
        dao.seedBalance(AccountBalanceEntity(bankName = "Test Bank", accountLast4 = "1234", balance = BigDecimal("1000"), timestamp = t1, sourceType = "CALCULATED"))

        // Seed subsequent CALCULATED balance at T3 (1200, representing income of 200)
        val t3 = LocalDateTime.of(2025, 1, 3, 10, 0)
        val balanceEntityT3 = AccountBalanceEntity(bankName = "Test Bank", accountLast4 = "1234", balance = BigDecimal("1200"), timestamp = t3, sourceType = "CALCULATED", transactionId = 100L)
        dao.seedBalance(balanceEntityT3)
        // Associated transaction for T3 is INCOME of 200
        dao.transactionMap[100L] = Pair(BigDecimal("200"), "INCOME")

        // Now, insert a backdated transaction at T2 (between T1 and T3) of EXPENSE of 300
        val t2 = LocalDateTime.of(2025, 1, 2, 10, 0)
        repository.insertTransactionBalance(
            bankName = "Test Bank",
            accountLast4 = "1234",
            amount = BigDecimal("300"),
            transactionType = TransactionType.EXPENSE,
            explicitBalance = null,
            smsSource = "EXPENSE of 300",
            currency = "INR",
            timestamp = t2,
            transactionId = 200L,
            creditLimit = null,
            isCreditCard = false
        )

        // The backdated row at T2 should be: T1 balance (1000) - T2 expense (300) = 700
        val balanceT2 = dao.balances.find { it.timestamp == t2 }
        assertNotNull(balanceT2)
        assertEquals(BigDecimal("700"), balanceT2.balance)

        // The subsequent row at T3 should be recalculated: T2 balance (700) + T3 income (200) = 900
        val balanceT3 = dao.balances.find { it.timestamp == t3 }
        assertNotNull(balanceT3)
        assertEquals(BigDecimal("900"), balanceT3.balance)
    }

    @Test
    fun explicitSmsBalanceStopsRecalculation() = runTest {
        val dao = FakeAccountBalanceDao()
        val repository = AccountBalanceRepository(dao, ContextWrapper(null))

        val t1 = LocalDateTime.of(2025, 1, 1, 10, 0)
        dao.seedBalance(AccountBalanceEntity(bankName = "Test Bank", accountLast4 = "1234", balance = BigDecimal("1000"), timestamp = t1, sourceType = "CALCULATED"))

        // Seed subsequent explicit SMS balance row at T3 (explicit balance of 1500)
        val t3 = LocalDateTime.of(2025, 1, 3, 10, 0)
        dao.seedBalance(AccountBalanceEntity(
            bankName = "Test Bank",
            accountLast4 = "1234",
            balance = BigDecimal("1500"),
            timestamp = t3,
            sourceType = "SMS_BALANCE", // boundary
            transactionId = 100L
        ))
        dao.transactionMap[100L] = Pair(BigDecimal("200"), "INCOME")

        // Insert backdated transaction at T2 (EXPENSE of 300)
        val t2 = LocalDateTime.of(2025, 1, 2, 10, 0)
        repository.insertTransactionBalance(
            bankName = "Test Bank",
            accountLast4 = "1234",
            amount = BigDecimal("300"),
            transactionType = TransactionType.EXPENSE,
            explicitBalance = null,
            smsSource = "EXPENSE of 300",
            currency = "INR",
            timestamp = t2,
            transactionId = 200L,
            creditLimit = null,
            isCreditCard = false
        )

        // The backdated row at T2 should be 700
        val balanceT2 = dao.balances.find { it.timestamp == t2 }
        assertNotNull(balanceT2)
        assertEquals(BigDecimal("700"), balanceT2.balance)

        // The subsequent row at T3 (explicit boundary) should NOT be changed
        val balanceT3 = dao.balances.find { it.timestamp == t3 }
        assertNotNull(balanceT3)
        assertEquals(BigDecimal("1500"), balanceT3.balance)
    }

    @Test
    fun manualBoundaryCarriesDeltaAndStopsCascade() = runTest {
        val dao = FakeAccountBalanceDao()
        val repository = AccountBalanceRepository(dao, ContextWrapper(null))

        val t1 = LocalDateTime.of(2025, 1, 1, 10, 0)
        dao.seedBalance(AccountBalanceEntity(bankName = "Test Bank", accountLast4 = "1234", balance = BigDecimal("1000"), timestamp = t1, sourceType = "CALCULATED"))

        // Seed subsequent manual balance row at T3
        val t3 = LocalDateTime.of(2025, 1, 3, 10, 0)
        dao.seedBalance(AccountBalanceEntity(
            bankName = "Test Bank",
            accountLast4 = "1234",
            balance = BigDecimal("2000"),
            timestamp = t3,
            sourceType = "MANUAL", // boundary
            transactionId = 100L
        ))
        dao.transactionMap[100L] = Pair(BigDecimal("200"), "INCOME")

        // Insert backdated transaction at T2 (EXPENSE of 300)
        val t2 = LocalDateTime.of(2025, 1, 2, 10, 0)
        repository.insertTransactionBalance(
            bankName = "Test Bank",
            accountLast4 = "1234",
            amount = BigDecimal("300"),
            transactionType = TransactionType.EXPENSE,
            explicitBalance = null,
            smsSource = "EXPENSE of 300",
            currency = "INR",
            timestamp = t2,
            transactionId = 200L,
            creditLimit = null,
            isCreditCard = false
        )

        // The backdated row at T2 should be 700
        val balanceT2 = dao.balances.find { it.timestamp == t2 }
        assertNotNull(balanceT2)
        assertEquals(BigDecimal("700"), balanceT2.balance)

        // MANUAL entries are not authoritative hard stops: the cascade continues
        // through them and the manual row is updated to carry the running balance.
        val balanceT3 = dao.balances.find { it.timestamp == t3 }
        assertNotNull(balanceT3)
        assertEquals(BigDecimal("700"), balanceT3.balance)
    }

    @Test
    fun creditCardIncomePaymentReducesOutstandingBalance() = runTest {
        val dao = FakeAccountBalanceDao()
        val repository = AccountBalanceRepository(dao, ContextWrapper(null))

        val t1 = LocalDateTime.of(2025, 1, 1, 10, 0)
        dao.seedBalance(AccountBalanceEntity(
            bankName = "Test Bank",
            accountLast4 = "1234",
            balance = BigDecimal("2000"),
            timestamp = t1,
            sourceType = "CALCULATED",
            isCreditCard = true
        ))

        // Insert INCOME transaction at T2 (payment to credit card of 500)
        val t2 = LocalDateTime.of(2025, 1, 2, 10, 0)
        repository.insertTransactionBalance(
            bankName = "Test Bank",
            accountLast4 = "1234",
            amount = BigDecimal("500"),
            transactionType = TransactionType.INCOME,
            explicitBalance = null,
            smsSource = "Payment of 500",
            currency = "INR",
            timestamp = t2,
            transactionId = 200L,
            creditLimit = null,
            isCreditCard = true
        )

        // Outstanding balance should decrease: 2000 - 500 = 1500
        val balanceT2 = dao.balances.find { it.timestamp == t2 }
        assertNotNull(balanceT2)
        assertEquals(BigDecimal("1500"), balanceT2.balance)
    }

    @Test
    fun resolveEntityAccountKeepsExactMatchUnchanged() = runTest {
        val dao = FakeAccountBalanceDao()
        dao.seedBalance(balance(1, "Raiffeisen Bank BiH", "1234", BigDecimal("1000"), LocalDateTime.of(2026, 5, 1, 10, 0)))
        val repository = AccountBalanceRepository(dao, ContextWrapper(null))

        val resolved = repository.resolveEntityAccount(
            tranEntity("Raiffeisen Bank BiH", "1234561234"),
            parsed("Raiffeisen Bank BiH")
        )

        assertEquals("Raiffeisen Bank BiH", resolved.bankName)
        assertEquals("1234", resolved.accountNumber)
        assertEquals(0, dao.last4CandidatesLookups)
    }

    @Test
    fun resolveEntityAccountRewritesUniqueLast4Match() = runTest {
        val dao = FakeAccountBalanceDao()
        dao.seedBalance(balance(1, "Raiffeisen", "1234", BigDecimal("1000"), LocalDateTime.of(2026, 5, 1, 10, 0)))
        val repository = AccountBalanceRepository(dao, ContextWrapper(null))

        val resolved = repository.resolveEntityAccount(
            tranEntity("Raiffeisen Bank BiH", "1234561234"),
            parsed("Raiffeisen Bank BiH")
        )

        assertEquals("Raiffeisen", resolved.bankName)
        assertEquals("1234", resolved.accountNumber)
    }

    @Test
    fun resolveEntityAccountPicksAliasMatchAmongSameLast4() = runTest {
        val dao = FakeAccountBalanceDao()
        dao.seedBalance(balance(1, "Raiffeisen banka", "1234", BigDecimal("1000"), LocalDateTime.of(2026, 5, 1, 10, 0)))
        dao.seedBalance(balance(2, "Nordea Bank", "1234", BigDecimal("1000"), LocalDateTime.of(2026, 5, 1, 10, 0)))
        val repository = AccountBalanceRepository(dao, ContextWrapper(null))

        val resolved = repository.resolveEntityAccount(
            tranEntity("Raiffeisen Bank BiH", "1234"),
            parsed("Raiffeisen Bank BiH")
        )

        assertEquals("Raiffeisen banka", resolved.bankName)
        assertEquals("1234", resolved.accountNumber)
    }

    @Test
    fun resolveEntityAccountDoesNotGuessAmbiguousLast4() = runTest {
        val dao = FakeAccountBalanceDao()
        dao.seedBalance(balance(1, "Nordea Bank", "1234", BigDecimal("1000"), LocalDateTime.of(2026, 5, 1, 10, 0)))
        dao.seedBalance(balance(2, "Swedbank BiH", "1234", BigDecimal("1000"), LocalDateTime.of(2026, 5, 1, 10, 0)))
        val repository = AccountBalanceRepository(dao, ContextWrapper(null))

        val resolved = repository.resolveEntityAccount(
            tranEntity("Raiffeisen", "1234"),
            parsed("Raiffeisen")
        )

        assertEquals("Raiffeisen", resolved.bankName)
        assertEquals("1234", resolved.accountNumber)
    }

    @Test
    fun resolveEntityAccountIgnoresSampleAccounts() = runTest {
        val dao = FakeAccountBalanceDao()
        dao.seedBalance(
            AccountBalanceEntity(
                id = 1,
                bankName = "Raiffeisen",
                accountLast4 = "1234",
                balance = BigDecimal("1000"),
                timestamp = LocalDateTime.of(2026, 5, 1, 10, 0),
                isSample = true
            )
        )
        val repository = AccountBalanceRepository(dao, ContextWrapper(null))

        val resolved = repository.resolveEntityAccount(
            tranEntity("Raiffeisen Bank BiH", "1234"),
            parsed("Raiffeisen Bank BiH")
        )

        assertEquals("Raiffeisen Bank BiH", resolved.bankName)
        assertEquals("1234", resolved.accountNumber)
    }

    @Test
    fun resolveEntityAccountSkipsMatchingForNonDigitLast4() = runTest {
        val dao = FakeAccountBalanceDao()
        val repository = AccountBalanceRepository(dao, ContextWrapper(null))

        val resolved = repository.resolveEntityAccount(
            tranEntity("Cash", "wallet"),
            parsed("Cash")
        )

        assertEquals("Cash", resolved.bankName)
        assertEquals("wallet", resolved.accountNumber)
        assertEquals(0, dao.last4CandidatesLookups)
    }

    @Test
    fun resolveEntityAccountKeepsCardTransactionsUnresolved() = runTest {
        val dao = FakeAccountBalanceDao()
        val repository = AccountBalanceRepository(dao, ContextWrapper(null))

        val resolved = repository.resolveEntityAccount(
            tranEntity("Some Bank", "4321"),
            parsed("Some Bank", isFromCard = true)
        )

        assertEquals("Some Bank", resolved.bankName)
        assertEquals("4321", resolved.accountNumber)
        assertEquals(0, dao.last4CandidatesLookups)
    }

    private companion object {
        fun tranEntity(bankName: String?, accountNumber: String?): TransactionEntity =
            TransactionEntity(
                amount = BigDecimal.TEN,
                merchantName = "Merchant",
                category = "Shopping",
                transactionType = TransactionType.EXPENSE,
                dateTime = LocalDateTime.of(2026, 5, 23, 20, 0),
                transactionHash = "hash"
            ).copy(bankName = bankName, accountNumber = accountNumber)

        fun parsed(bankName: String, isFromCard: Boolean = false): ParsedTransaction =
            ParsedTransaction(
                amount = BigDecimal.TEN,
                type = com.ritesh.parser.core.TransactionType.EXPENSE,
                merchant = "Merchant",
                reference = null,
                accountLast4 = null,
                balance = null,
                smsBody = "",
                sender = "RAIFFEISEN",
                timestamp = 0L,
                bankName = bankName,
                isFromCard = isFromCard
            )
    }
}
