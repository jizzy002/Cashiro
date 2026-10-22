package com.ritesh.parser.core.bank

import com.ritesh.parser.core.TransactionType
import com.ritesh.parser.core.test.ExpectedTransaction
import com.ritesh.parser.core.test.ParserTestCase
import com.ritesh.parser.core.test.ParserTestUtils
import com.ritesh.parser.core.test.SimpleTestCase
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import java.math.BigDecimal

class RaiffeisenBiHParserTest {

    private val parser = RaiffeisenBiHParser()

    // All fixtures are anonymized: no real names, payers or full account numbers.
    private val account = "1613000088199999"

    private val outgoingBam = """
        Odlazna transakcija
        Sa Vašeg računa $account izvršena je isplata u iznosu od 310.00 BAM.
        Opis:KONZUM
        Vaša Raiffeisen banka
    """.trimIndent()

    private val incomingBam = """
        Dolazna transakcija
        Na Vaš račun $account izvršena je uplata u iznosu od 1752.66 BAM.
        Opis:SAMPLE EMPLOYER DOO
        Vaša Raiffeisen banka
    """.trimIndent()

    @TestFactory
    fun `parses RMB push notifications`(): List<DynamicTest> {
        val cases = listOf(
            ParserTestCase(
                name = "Outgoing BAM payment",
                message = outgoingBam,
                sender = "RaiffeisenBiH",
                expected = ExpectedTransaction(
                    amount = BigDecimal("310.00"),
                    currency = "BAM",
                    type = TransactionType.EXPENSE,
                    merchant = "KONZUM",
                    accountLast4 = "9999"
                )
            ),
            ParserTestCase(
                name = "Incoming BAM payment",
                message = incomingBam,
                sender = "RaiffeisenBiH",
                expected = ExpectedTransaction(
                    amount = BigDecimal("1752.66"),
                    currency = "BAM",
                    type = TransactionType.INCOME,
                    merchant = "SAMPLE EMPLOYER DOO",
                    accountLast4 = "9999"
                )
            ),
            ParserTestCase(
                name = "Outgoing USD card payment",
                message = """
                    Odlazna transakcija
                    Sa Vašeg računa $account izvršena je isplata u iznosu od 10.49 USD.
                    Opis:SPOTIFY
                    Vaša Raiffeisen banka
                """.trimIndent(),
                sender = "RaiffeisenBiH",
                expected = ExpectedTransaction(
                    amount = BigDecimal("10.49"),
                    currency = "USD",
                    type = TransactionType.EXPENSE,
                    merchant = "SPOTIFY",
                    accountLast4 = "9999"
                )
            ),
            ParserTestCase(
                name = "Outgoing EUR card payment",
                message = """
                    Odlazna transakcija
                    Sa Vašeg računa $account izvršena je isplata u iznosu od 12.00 EUR.
                    Opis:NETFLIX
                    Vaša Raiffeisen banka
                """.trimIndent(),
                sender = "RaiffeisenBiH",
                expected = ExpectedTransaction(
                    amount = BigDecimal("12.00"),
                    currency = "EUR",
                    type = TransactionType.EXPENSE,
                    merchant = "NETFLIX",
                    accountLast4 = "9999"
                )
            ),
            ParserTestCase(
                name = "Collapsed body keeps merchant and strips location",
                message = "Odlazna transakcija Sa Vašeg računa $account izvršena je isplata u iznosu od 45.90 BAM. Opis:APOTEKA >SARAJEVO BA Vaša Raiffeisen banka",
                sender = "RaiffeisenBiH",
                expected = ExpectedTransaction(
                    amount = BigDecimal("45.90"),
                    currency = "BAM",
                    type = TransactionType.EXPENSE,
                    merchant = "APOTEKA",
                    accountLast4 = "9999"
                )
            ),
            ParserTestCase(
                name = "OTP message is not a transaction",
                message = "Raiffeisen Bank: Vaš jednokratni kod je 123456. Ne dijelite ga ni s kim.",
                sender = "RaiffeisenBiH",
                shouldParse = false
            ),
            ParserTestCase(
                name = "Promotional message is not a transaction",
                message = "Raiffeisen banka: Ostvarite 20% popusta uz novu karticu.",
                sender = "RaiffeisenBiH",
                shouldParse = false
            ),
            ParserTestCase(
                name = "Balance-only message is not a transaction",
                message = "Na Vašem računu $account stanje je 250.00 BAM.",
                sender = "RaiffeisenBiH",
                shouldParse = false
            )
        )

        return ParserTestUtils.runTestSuite(
            parser = parser,
            testCases = cases,
            handleCases = listOf(
                "RaiffeisenBiH" to true,
                "RAIFFEISEN" to true,
                "HDFC" to false
            )
        )
    }

    @Test
    fun `declares BAM fx policy with one percent fee`() {
        val policy = parser.getFxPolicy()

        assertEquals(FxPolicy("BAM", BigDecimal("1")), policy)
    }

    @TestFactory
    fun `factory resolves the raiffeisen notification alias`(): List<DynamicTest> {
        val cases = listOf(
            SimpleTestCase(
                bankName = "Raiffeisen Bank BiH",
                sender = "RaiffeisenBiH",
                currency = "BAM",
                message = outgoingBam,
                expected = ExpectedTransaction(
                    amount = BigDecimal("310.00"),
                    currency = "BAM",
                    type = TransactionType.EXPENSE
                ),
                shouldHandle = true
            )
        )

        return ParserTestUtils.runFactoryTestSuite(cases, "Factory resolves Raiffeisen BiH")
    }
}
