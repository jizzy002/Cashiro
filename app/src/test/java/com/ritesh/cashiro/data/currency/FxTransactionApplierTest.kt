package com.ritesh.cashiro.data.currency

import com.ritesh.parser.core.ParsedTransaction
import com.ritesh.parser.core.TransactionType
import com.ritesh.parser.core.bank.FxPolicy
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDate

class FxTransactionApplierTest {

    private val policy = FxPolicy(targetCurrency = "BAM", feePercent = BigDecimal("1"))

    private fun transaction(
        amount: String,
        currency: String,
        timestamp: Long = 1_790_000_000_000L
    ) = ParsedTransaction(
        amount = BigDecimal(amount),
        type = TransactionType.EXPENSE,
        merchant = "SAMPLE",
        reference = null,
        accountLast4 = "4273",
        balance = null,
        smsBody = "sample body",
        sender = "RaiffeisenBiH",
        timestamp = timestamp,
        bankName = "Raiffeisen Bank BiH",
        currency = currency
    )

    @Test
    fun `converts foreign amount with middle rate and one percent fee`() = runTest {
        val applier = FxTransactionApplier(
            primaryRate = { _, _ -> FxRate(BigDecimal("1.7669"), "Raiffeisen srednji kurs #295") },
            fallbackRate = { _, _ -> null }
        )

        val result = applier.apply(transaction("10.49", "USD"), policy)

        assertEquals(BigDecimal("18.72"), result.amount)
        assertEquals("BAM", result.currency)
        assertTrue(result.smsBody.contains("18.72 BAM"))
        assertTrue(result.smsBody.contains("Raiffeisen srednji kurs #295 + 1% fee"))
    }

    @Test
    fun `parity adjusted rate is applied per unit`() = runTest {
        val applier = FxTransactionApplier(
            primaryRate = { _, _ -> FxRate(BigDecimal("0.0117"), "Raiffeisen srednji kurs #295") },
            fallbackRate = { _, _ -> null }
        )

        val result = applier.apply(transaction("100.00", "JPY"), policy)

        assertEquals(BigDecimal("1.18"), result.amount)
        assertEquals("BAM", result.currency)
    }

    @Test
    fun `BAM transactions pass through untouched`() = runTest {
        val applier = FxTransactionApplier(
            primaryRate = { _, _ -> error("must not fetch a rate") },
            fallbackRate = { _, _ -> error("must not fetch a rate") }
        )
        val original = transaction("310.00", "BAM")

        val result = applier.apply(original, policy)

        assertEquals(original, result)
        assertFalse(result.smsBody.contains("Converted"))
    }

    @Test
    fun `falls back to the conversion service when the bank list is unavailable`() = runTest {
        val applier = FxTransactionApplier(
            primaryRate = { _, _ -> null },
            fallbackRate = { _, _ -> BigDecimal("1.7669") }
        )

        val result = applier.apply(transaction("10.49", "USD"), policy)

        assertEquals(BigDecimal("18.72"), result.amount)
        assertEquals("BAM", result.currency)
        assertTrue(result.smsBody.contains("market rate + 1% fee"))
    }

    @Test
    fun `keeps the original transaction when no rate is available`() = runTest {
        val applier = FxTransactionApplier(
            primaryRate = { _, _ -> null },
            fallbackRate = { _, _ -> null }
        )
        val original = transaction("10.49", "USD")

        val result = applier.apply(original, policy)

        assertEquals(original, result)
        assertEquals("USD", result.currency)
    }

    @Test
    fun `asks the primary source for the transaction date`() = runTest {
        var requestedDate: LocalDate? = null
        val applier = FxTransactionApplier(
            primaryRate = { _, date ->
                requestedDate = date
                FxRate(BigDecimal("1.7669"), "Raiffeisen srednji kurs #295")
            },
            fallbackRate = { _, _ -> null }
        )

        applier.apply(transaction("10.49", "USD"), policy)

        assertTrue(requestedDate != null)
    }
}
