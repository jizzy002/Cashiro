package com.ritesh.cashiro.data.parser.pdf

import com.ritesh.parser.core.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.time.ZoneId

class RaiffeisenBiHStatementParserTest {

    private val parser = RaiffeisenBiHStatementParser()

    private fun timestamp(year: Int, month: Int, day: Int): Long =
        LocalDate.of(year, month, day)
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()

    // Anonymized excerpt: fake IBAN, merchant names and amounts only.
    private val statement = """
        IZVOD - OBAVIJEST
        period od: 01/09/2026 do: 30/09/2026
        IBAN:
        BA3916130000000001234
        Pregled stanja racuna
        Pocetno stanje: 1000.00
        Datum obrade Datumtransakcije Opis transakcije Valutatransakcije Iznostransakcije uvaluti Iznostransakcije uKM Saldo
        03/09/2026  02/09/2026  SAMPLE MARKET  BAM  0.00  -26.00  974.00
        07/09/2026  05/09/2026  SAMPLE SHOP  BAM  0.00  -0.15  973.85
        07/09/2026  05/09/2026  SAMPLE SHOP  EUR  -7.77  -15.20  958.65
        08/09/2026  07/09/2026  SAMPLE FEE  BAM  0.00  -0.15  958.50
        08/09/2026  07/09/2026  OTHER SHOP  BAM  0.00  -13.50  945.00
        18/09/2026  18/09/2026  SAMPLE PAYER  BAM  0.00  1,200.00  2,145.00
        21/09/2026  18/09/2026  SAMPLE MUSIC  BAM  0.00  -0.01  2,144.99
        21/09/2026  18/09/2026  SAMPLE MUSIC  USD  -0.50  -0.88  2,144.11
        30/09/2026  30/09/2026  Naknada za vodenje paketa  BAM  0.00  -4.99  2,139.12
        Pregled ukupnog iznosa neplacenih naknada i kamata:
        Konacno stanje: 2,139.12
        Hvala sto koristite karticu Raiffeisen BANK d.d. Bosna i Hercegovina.
    """.trimIndent()

    @Test
    fun `detects raiffeisen statements only`() {
        assertTrue(parser.canHandle(statement))
        assertFalse(parser.canHandle("Google Pay UPI Transaction ID 123"))
        assertFalse(parser.canHandle("PhonePe statement"))
    }

    @Test
    fun `statement rows are date-only`() {
        assertFalse(parser.hasTimeOfDay())
    }

    @Test
    fun `parses BAM purchase from the KM column`() {
        val transactions = parser.parse(statement)
        val purchase = transactions.first { it.merchant == "SAMPLE MARKET" }

        assertEquals(BigDecimal("26.00"), purchase.amount)
        assertEquals("BAM", purchase.currency)
        assertEquals(TransactionType.EXPENSE, purchase.type)
        assertEquals(timestamp(2026, 9, 2), purchase.timestamp)
        assertEquals(BigDecimal("974.00"), purchase.balance)
        assertEquals("1234", purchase.accountLast4)
        assertEquals("Raiffeisen Bank BiH", purchase.bankName)
    }

    @Test
    fun `merges a commission row into the following FX transaction`() {
        val transactions = parser.parse(statement)
        val shop = transactions.filter { it.merchant == "SAMPLE SHOP" }

        assertEquals(1, shop.size)
        assertEquals(BigDecimal("15.35"), shop.single().amount)
        assertEquals(TransactionType.EXPENSE, shop.single().type)
        assertTrue(shop.single().smsBody.contains("commission"))
    }

    @Test
    fun `keeps a lone commission as its own transaction`() {
        val transactions = parser.parse(statement)
        val fee = transactions.filter { it.merchant == "SAMPLE FEE" }

        assertEquals(1, fee.size)
        assertEquals(BigDecimal("0.15"), fee.single().amount)
        assertEquals(TransactionType.EXPENSE, fee.single().type)
    }

    @Test
    fun `parses thousands separators and income rows`() {
        val transactions = parser.parse(statement)
        val payer = transactions.first { it.merchant == "SAMPLE PAYER" }

        assertEquals(BigDecimal("1200.00"), payer.amount)
        assertEquals(TransactionType.INCOME, payer.type)
        assertEquals(BigDecimal("2145.00"), payer.balance)
    }

    @Test
    fun `merges USD commission and keeps the foreign amount in the body`() {
        val transactions = parser.parse(statement)
        val music = transactions.filter { it.merchant == "SAMPLE MUSIC" }

        assertEquals(1, music.size)
        assertEquals(BigDecimal("0.89"), music.single().amount)
        assertTrue(music.single().smsBody.contains("0.50 USD"))
    }

    @Test
    fun `parses the monthly account fee`() {
        val transactions = parser.parse(statement)
        val fee = transactions.first { it.merchant == "Naknada za vodenje paketa" }

        assertEquals(BigDecimal("4.99"), fee.amount)
        assertEquals(TransactionType.EXPENSE, fee.type)
    }

    @Test
    fun `headers and footer do not create transactions`() {
        assertEquals(7, parser.parse(statement).size)
    }
}
