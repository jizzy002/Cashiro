package com.ritesh.cashiro.presentation.widget

import com.ritesh.cashiro.data.database.entity.AccountBalanceEntity
import com.ritesh.cashiro.utils.CurrencyFormatter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDateTime
import java.time.ZoneOffset

class WidgetStateBuilderTest {

    private fun bal(bank: String, last4: String, amount: String, currency: String, ts: Long) =
        AccountBalanceEntity(
            bankName = bank,
            accountLast4 = last4,
            balance = BigDecimal(amount),
            timestamp = LocalDateTime.ofEpochSecond(ts, 0, ZoneOffset.UTC),
            currency = currency,
        )

    @Test
    fun `mixed currencies total is em dash`() {
        val s = WidgetStateBuilder.build(
            listOf(
                bal("Raiffeisen", "1234", "2340.50", "BAM", 100),
                bal("UniCredit", "5678", "1890.67", "EUR", 90),
            ),
            emptySet(), false, LocalDateTime.now(),
        )
        assertEquals("—", s.totalText)
    }

    @Test
    fun `single currency total sums`() {
        val expected = CurrencyFormatter.formatCurrency(java.math.BigDecimal("3591.50"), "BAM")
        val s = WidgetStateBuilder.build(
            listOf(
                bal("Raiffeisen", "1234", "2340.50", "BAM", 100),
                bal("Intesa", "9012", "1251.00", "BAM", 90),
            ),
            emptySet(), false, LocalDateTime.now(),
        )
        assertEquals(expected, s.totalText)
    }

    @Test
    fun `hidden account excluded`() {
        val s = WidgetStateBuilder.build(
            listOf(
                bal("Raiffeisen", "1234", "2340.50", "BAM", 100),
                bal("UniCredit", "5678", "1890.67", "BAM", 90),
            ),
            setOf("Raiffeisen_1234"), false, LocalDateTime.now(),
        )
        assertEquals(1, s.accounts.size)
        assertEquals("UniCredit", s.accounts.first().bankName)
    }

    @Test
    fun `privacy flag carried`() {
        val s = WidgetStateBuilder.build(emptyList(), emptySet(), true, LocalDateTime.now())
        assertTrue(s.privacyHidden)
    }

    @Test
    fun `picks three most recent`() {
        val s = WidgetStateBuilder.build(
            listOf(
                bal("A", "1", "1.00", "BAM", 10),
                bal("B", "2", "2.00", "BAM", 30),
                bal("C", "3", "3.00", "BAM", 20),
                bal("D", "4", "4.00", "BAM", 40),
            ),
            emptySet(), false, LocalDateTime.now(),
        )
        assertEquals(3, s.accounts.size)
        assertEquals(listOf("D", "B", "C"), s.accounts.map { it.bankName })
    }

    @Test
    fun `total sums all accounts even when rows capped at three`() {
        val s = WidgetStateBuilder.build(
            listOf(
                bal("A", "1", "1.00", "BAM", 10),
                bal("B", "2", "2.00", "BAM", 30),
                bal("C", "3", "3.00", "BAM", 20),
                bal("D", "4", "4.00", "BAM", 40),
                bal("E", "5", "5.00", "BAM", 50),
            ),
            emptySet(), false, LocalDateTime.now(),
        )
        val expected = CurrencyFormatter.formatCurrency(java.math.BigDecimal("15.00"), "BAM")
        assertEquals(expected, s.totalText)
        assertEquals(3, s.accounts.size)
    }

    @Test
    fun `empty list total is em dash`() {
        val s = WidgetStateBuilder.build(emptyList(), emptySet(), false, LocalDateTime.now())
        assertEquals("—", s.totalText)
        assertTrue(s.accounts.isEmpty())
    }

    @Test
    fun `unmixed rows show bare amount, mixed rows keep inline currency`() {
        val mixed = WidgetStateBuilder.build(
            listOf(
                bal("Raiffeisen", "1234", "2340.50", "BAM", 100),
                bal("Intesa", "9012", "1251.00", "EUR", 90),
            ),
            emptySet(), false, LocalDateTime.now(),
        )
        assertTrue(mixed.mixedCurrency)
        assertTrue(mixed.accounts.first().amountText.contains("KM"))

        val unmixed = WidgetStateBuilder.build(
            listOf(
                bal("Raiffeisen", "1234", "2340.50", "BAM", 100),
                bal("Intesa", "9012", "1251.00", "BAM", 90),
            ),
            emptySet(), false, LocalDateTime.now(),
        )
        org.junit.Assert.assertFalse(unmixed.mixedCurrency)
        org.junit.Assert.assertFalse(unmixed.accounts.first().amountText.contains("KM"))
    }
}
