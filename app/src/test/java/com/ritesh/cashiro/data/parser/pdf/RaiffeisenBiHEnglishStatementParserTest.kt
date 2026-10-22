package com.ritesh.cashiro.data.parser.pdf

import com.ritesh.parser.core.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.time.ZoneId

class RaiffeisenBiHEnglishStatementParserTest {

    private val parser = RaiffeisenBiHEnglishStatementParser()

    private fun timestamp(year: Int, month: Int, day: Int): Long =
        LocalDate.of(year, month, day)
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()

    // Anonymized layout: fake account, customer, merchants and amounts only.
    private val statement = """
        Account Statement

        Account :
        1610000000009999

        Customer :
        9999999999
        SAMPLE CUSTOMER

        Currency :
        BAM

        Book Date    Reference    Description    Value Date    Debit    Credit    Closing Balance

        Balance at Period Start    1000.00

        01 OCT 26    FT0001    Goods n Services AbrdProc Fee    01 OCT 26    0.10    999.90
                                                         AC-BAM126250002

        01 OCT 26    FT0001    Goods and ServicesAbroad    01 OCT 26    10.00    989.90
                                                         SAMPLE SHOP

        02 OCT 26    FT0002    Salary
                                                         02 OCT 26    1,200.00    2,189.90
                                                         SAMPLE PAYER

        03 OCT 26    FT0003    Goods n Services AbrdProc Fee    03 OCT 26    0.05    2,189.85
                                                         AC-BAM126250002

        03 OCT 26    FT0003    Goods and Services
                                                         Abroad    03 OCT 26    5.00    2,184.85
                                                         WRAPPED
                                                         MERCHANT
                                                         5

        04 OCT 26    FT0004    Servicing Fee    04 OCT 26    0.60    2,184.25
                                                         AC-BAM126300001
                                                         SAMPLE HOTEL

        04 OCT 26    FT0004    Payment    04 OCT 26    13.00    2,171.25
                                                         SAMPLE HOTEL
                                                         SAMPLE CITY

        05 OCT 26    FT0005    Refund Goods andServices Abrd    05 OCT 26    3.28    2,174.53
                                                         PP 4524CODE

        05 OCT 26    FT0005    Goods n Services AbrdProc Fee    05 OCT 26    0.03    2,174.50
                                                         AC-1610000000009999

        06 OCT 26    FT0006    Cash ATM RBBH    06 OCT 26    70.00    2,104.50
                                                         Page 1 of 1
                                                         1
                                                         ATM SAMPLE

        08 OCT 26    FT0008    Goods n Services AbrdProc Fee    08 OCT 26    0.02    2,104.48
                                                         AC-BAM126250002

        Balance at Period End    2104.48

        Page 1 of 1
    """.trimIndent()

    @Test
    fun `detects the English Raiffeisen account statement only`() {
        assertTrue(parser.canHandle(statement))
        assertFalse(parser.canHandle(statement.replace("1610000000009999", "1234567890123456")))
        assertFalse(parser.canHandle("Google Pay UPI Transaction ID 123"))
        assertFalse(parser.canHandle("PhonePe statement"))
        assertFalse(parser.canHandle("IZVOD Raiffeisen Bank BiH"))
    }

    @Test
    fun `accepts a different Raiffeisen branch and grouped IBAN`() {
        val grouped = statement.replace(
            "1610000000009999",
            "BA39 1610 0000 0000 9999"
        )

        assertTrue(parser.canHandle(grouped))
        val salary = parser.parse(grouped).first { it.reference == "FT0002" }
        assertEquals("9999", salary.accountLast4)
    }

    @Test
    fun `statement rows are date-only`() {
        assertFalse(parser.hasTimeOfDay())
    }

    @Test
    fun `parses salary with account and bank metadata`() {
        val salary = parser.parse(statement).first { it.reference == "FT0002" }

        assertEquals("SAMPLE PAYER", salary.merchant)
        assertEquals(BigDecimal("1200.00"), salary.amount)
        assertEquals(TransactionType.INCOME, salary.type)
        assertEquals(BigDecimal("2189.90"), salary.balance)
        assertEquals("9999", salary.accountLast4)
        assertEquals("Raiffeisen Bank BiH", salary.bankName)
        assertEquals("BAM", salary.currency)
        assertEquals(timestamp(2026, 10, 2), salary.timestamp)
    }

    @Test
    fun `merges the foreign purchase fee into one expense`() {
        val shop = parser.parse(statement).first { it.reference == "FT0001" }

        assertEquals("SAMPLE SHOP", shop.merchant)
        assertEquals(BigDecimal("10.10"), shop.amount)
        assertEquals(TransactionType.EXPENSE, shop.type)
        assertEquals(BigDecimal("989.90"), shop.balance)
        assertTrue(shop.smsBody.contains("0.10"))
        assertTrue(shop.smsBody.contains("fee", ignoreCase = true))
    }

    @Test
    fun `joins a wrapped merchant name`() {
        val wrapped = parser.parse(statement).first { it.reference == "FT0003" }

        assertEquals("WRAPPED MERCHANT 5", wrapped.merchant)
        assertEquals(BigDecimal("5.05"), wrapped.amount)
        assertEquals(BigDecimal("2184.85"), wrapped.balance)
    }

    @Test
    fun `merges the servicing fee into the payment`() {
        val hotel = parser.parse(statement).first { it.reference == "FT0004" }

        assertEquals("SAMPLE HOTEL SAMPLE CITY", hotel.merchant)
        assertEquals(BigDecimal("13.60"), hotel.amount)
        assertEquals(TransactionType.EXPENSE, hotel.type)
        assertEquals(BigDecimal("2171.25"), hotel.balance)
        assertTrue(hotel.smsBody.contains("0.60"))
    }

    @Test
    fun `nets the refund fee into one income`() {
        val refund = parser.parse(statement).first { it.reference == "FT0005" }

        assertEquals("PP 4524CODE", refund.merchant)
        assertEquals(BigDecimal("3.25"), refund.amount)
        assertEquals(TransactionType.INCOME, refund.type)
        assertEquals(BigDecimal("2174.50"), refund.balance)
        assertTrue(refund.smsBody.contains("0.03"))
    }

    @Test
    fun `page footer does not contaminate the merchant`() {
        val atm = parser.parse(statement).first { it.reference == "FT0006" }

        assertEquals("ATM SAMPLE", atm.merchant)
        assertEquals(BigDecimal("70.00"), atm.amount)
    }

    @Test
    fun `keeps a lone fee as its own expense`() {
        val loneFee = parser.parse(statement).first { it.reference == "FT0008" }

        assertEquals(BigDecimal("0.02"), loneFee.amount)
        assertEquals(TransactionType.EXPENSE, loneFee.type)
        assertEquals(BigDecimal("2104.48"), loneFee.balance)
    }

    @Test
    fun `headers and footer do not create transactions`() {
        assertEquals(7, parser.parse(statement).size)
    }

    @Test
    fun `parses a negative closing balance`() {
        val negativeStatement = """
            Account Statement

            Account :
            1610000000009999

            Currency :
            BAM

            Book Date    Reference    Description    Value Date    Debit    Credit    Closing Balance

            Balance at Period Start    10.00

            01 OCT 26    FTNEG    Goods and Services    01 OCT 26    10.75    -0.75
                                                             SAMPLE SHOP

            Balance at Period End    -0.75

            Page 1 of 1
        """.trimIndent()

        val transaction = parser.parse(negativeStatement).single()

        assertEquals(BigDecimal("10.75"), transaction.amount)
        assertEquals(BigDecimal("-0.75"), transaction.balance)
        assertEquals(TransactionType.EXPENSE, transaction.type)
    }

    @Test
    fun `rejects a statement whose rows do not reconcile`() {
        val broken = statement.replaceFirst("999.90", "999.00")

        assertThrows(IllegalStateException::class.java) {
            parser.parse(broken)
        }
    }

    @Test
    fun `reads both period balances from a shared summary line`() {
        val sideBySide = """
            Account Statement

            Account :
            1610000000009999

            Currency :
            BAM

            Book Date    Reference    Description    Value Date    Debit    Credit    Closing Balance

            Balance at Period Start 100.00 Balance at Period End 90.00

            01 OCT 26    FTSIDE    Goods and Services    01 OCT 26    10.00    90.00
                                                                 SAMPLE SHOP

            Page 1 of 1
        """.trimIndent()

        val transaction = parser.parse(sideBySide).single()

        assertEquals(BigDecimal("10.00"), transaction.amount)
        assertEquals(BigDecimal("90.00"), transaction.balance)
    }

    @Test
    fun `rejects a row with an amount but no balance movement`() {
        val duplicateBalance = """
            Account Statement

            Account :
            1610000000009999

            Currency :
            BAM

            Book Date    Reference    Description    Value Date    Debit    Credit    Closing Balance

            Balance at Period Start    10.00

            01 OCT 26    FTDUP    Goods and Services    01 OCT 26    10.00    10.00
                                                                 SAMPLE SHOP

            Balance at Period End    10.00

            Page 1 of 1
        """.trimIndent()

        assertThrows(IllegalStateException::class.java) {
            parser.parse(duplicateBalance)
        }
    }

    @Test
    fun `does not expose raw row data in errors`() {
        val malformed = """
            Account Statement

            Account :
            1613000000009999

            Currency :
            BAM

            Book Date    Reference    Description    Value Date    Debit    Credit    Closing Balance

            Balance at Period Start    10.00

            01 OCT 26    FTDUP    Secret Merchant    01 OCT 26    10.00

            Balance at Period End    0.00

            Page 1 of 1
        """.trimIndent()

        val error = assertThrows(IllegalStateException::class.java) {
            parser.parse(malformed)
        }

        assertFalse(error.message.orEmpty().contains("FTDUP"))
        assertFalse(error.message.orEmpty().contains("Secret Merchant"))
        assertFalse(error.message.orEmpty().contains("10.00"))
    }
}
