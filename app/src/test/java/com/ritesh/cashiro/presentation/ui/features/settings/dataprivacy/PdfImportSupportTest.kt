package com.ritesh.cashiro.presentation.ui.features.settings.dataprivacy

import com.ritesh.cashiro.data.database.entity.TransactionEntity
import com.ritesh.cashiro.data.database.entity.TransactionType
import com.ritesh.parser.core.ParsedTransaction
import com.ritesh.parser.core.TransactionType as ParsedTransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal
import java.time.Duration
import java.time.LocalDateTime

class PdfImportSupportTest {

    private val statementTimestamp = 1_789_000_000_000L

    private fun parsed(
        amount: String = "26.00",
        type: ParsedTransactionType = ParsedTransactionType.EXPENSE,
        merchant: String? = "SAMPLE MARKET",
        accountLast4: String? = "1234",
        reference: String? = null,
        balance: String? = null,
        timestamp: Long = statementTimestamp
    ) = ParsedTransaction(
        amount = BigDecimal(amount),
        type = type,
        merchant = merchant,
        reference = reference,
        accountLast4 = accountLast4,
        balance = balance?.let(::BigDecimal),
        smsBody = "statement row",
        sender = "RaiffeisenBiH",
        timestamp = timestamp,
        bankName = "Raiffeisen Bank BiH",
        currency = "BAM"
    )

    private fun existing(
        amount: String = "26.00",
        type: TransactionType = TransactionType.EXPENSE,
        merchant: String = "SAMPLE MARKET",
        accountNumber: String? = "****1234",
        reference: String? = null,
        smsBody: String? = null
    ) = TransactionEntity(
        amount = BigDecimal(amount),
        merchantName = merchant,
        category = "Miscellaneous",
        transactionType = type,
        dateTime = LocalDateTime.of(2026, 9, 3, 12, 0),
        accountNumber = accountNumber,
        reference = reference,
        smsBody = smsBody,
        transactionHash = "existing-hash"
    )

    @Test
    fun `date only statements get a 48 hour duplicate window`() {
        assertEquals(
            Duration.ofHours(48),
            PdfImportSupport.duplicateWindow(hasTimeOfDay = false)
        )
    }

    @Test
    fun `statements with time of day keep the 15 minute window`() {
        assertEquals(
            Duration.ofMinutes(15),
            PdfImportSupport.duplicateWindow(hasTimeOfDay = true)
        )
    }

    @Test
    fun `same amount merchant direction and account is a duplicate`() {
        assertTrue(PdfImportSupport.isLikelyDuplicate(parsed(), existing()))
    }

    @Test
    fun `same amount and account with a different merchant is not a duplicate`() {
        assertFalse(
            PdfImportSupport.isLikelyDuplicate(
                parsed(),
                existing(merchant = "OTHER SHOP")
            )
        )
    }

    @Test
    fun `same amount and merchant in the opposite direction is not a duplicate`() {
        assertFalse(
            PdfImportSupport.isLikelyDuplicate(
                parsed(type = ParsedTransactionType.INCOME),
                existing(type = TransactionType.EXPENSE)
            )
        )
    }

    @Test
    fun `same amount and merchant on a different account is not a duplicate`() {
        assertFalse(
            PdfImportSupport.isLikelyDuplicate(
                parsed(),
                existing(accountNumber = "****9999")
            )
        )
    }

    @Test
    fun `merchant containment matches a longer stored name`() {
        assertTrue(
            PdfImportSupport.isLikelyDuplicate(
                parsed(merchant = "KONZUM"),
                existing(merchant = "KONZUM DOO SARAJEVO")
            )
        )
    }

    @Test
    fun `a matching reference wins even when merchant names differ`() {
        assertTrue(
            PdfImportSupport.isLikelyDuplicate(
                parsed(merchant = "DIFFERENT SHOP", reference = "UTR No. 123 456"),
                existing(merchant = "SAMPLE MARKET", smsBody = "UTR No. 123 456")
            )
        )
    }

    @Test
    fun `amount alone is never treated as a duplicate`() {
        assertFalse(
            PdfImportSupport.isLikelyDuplicate(
                parsed(merchant = null, reference = null),
                existing(merchant = "SAMPLE MARKET")
            )
        )
    }

    @Test
    fun `blank merchant names do not match`() {
        assertFalse(
            PdfImportSupport.isLikelyDuplicate(
                parsed(merchant = "   "),
                existing(merchant = "")
            )
        )
    }

    @Test
    fun `closing balance uses the last statement row on the same day`() {
        val rows = listOf(
            parsed(balance = "974.00", timestamp = 1_000L),
            parsed(balance = "958.65", timestamp = 2_000L),
            parsed(balance = "945.00", timestamp = 2_000L)
        )

        val closing = PdfImportSupport.closingBalancesByAccount(rows)

        assertEquals(BigDecimal("945.00"), closing["1234"]?.balance)
    }

    @Test
    fun `closing balances are tracked per account`() {
        val rows = listOf(
            parsed(accountLast4 = "1234", balance = "100.00", timestamp = 1_000L),
            parsed(accountLast4 = "9999", balance = "200.00", timestamp = 2_000L),
            parsed(accountLast4 = "1234", balance = "150.00", timestamp = 3_000L)
        )

        val closing = PdfImportSupport.closingBalancesByAccount(rows)

        assertEquals(BigDecimal("150.00"), closing["1234"]?.balance)
        assertEquals(BigDecimal("200.00"), closing["9999"]?.balance)
    }
}
