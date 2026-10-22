package com.ritesh.cashiro.data.currency

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

class RaiffeisenRateProviderTest {

    private val fixture = requireNotNull(javaClass.classLoader)
        .getResource("rates/raiffeisen_kursna_lista.json")
        .readText()

    private val date = LocalDate.of(2026, 10, 22)

    private fun provider(fetcher: suspend (String) -> String?): RaiffeisenRateProvider =
        RaiffeisenRateProvider().apply {
            fetchText = fetcher
            clock = Clock.fixed(Instant.parse("2026-10-22T10:00:00Z"), ZoneOffset.UTC)
        }

    private fun assertDecimalEquals(expected: String, actual: BigDecimal) {
        assertEquals(0, BigDecimal(expected).compareTo(actual))
    }

    @Test
    fun `parses USD middle rate and list metadata`() = runTest {
        val quote = provider { fixture }.getMiddleRate("USD", date)

        assertNotNull(quote)
        assertDecimalEquals("1.740992", quote!!.middleRate)
        assertEquals(1, quote.parity)
        assertEquals(295, quote.listNumber)
        assertEquals(date, quote.effectiveDate)
    }

    @Test
    fun `handles parity for JPY`() = runTest {
        val quote = provider { fixture }.getMiddleRate("JPY", date)

        assertNotNull(quote)
        assertEquals(100, quote!!.parity)
        assertDecimalEquals("0.01102746", quote.middleRate)
    }

    @Test
    fun `backtracks up to five days when the list is missing`() = runTest {
        val requested = LocalDate.of(2026, 10, 24)
        val fetchedUrls = mutableListOf<String>()
        val provider = provider { url ->
            fetchedUrls += url
            when {
                url.contains("20261024") || url.contains("20261023") -> null
                else -> fixture
            }
        }

        val quote = provider.getMiddleRate("USD", requested)

        assertNotNull(quote)
        assertEquals(LocalDate.of(2026, 10, 22), quote!!.effectiveDate)
        assertTrue(fetchedUrls.any { it.contains("20261024") })
        assertTrue(fetchedUrls.any { it.contains("20261022") })
    }

    @Test
    fun `caches rates per currency and date`() = runTest {
        var calls = 0
        val provider = provider {
            calls++
            fixture
        }

        provider.getMiddleRate("USD", date)
        provider.getMiddleRate("USD", date)

        assertEquals(1, calls)
    }

    @Test
    fun `returns null for malformed responses`() = runTest {
        assertNull(provider { "{not json" }.getMiddleRate("USD", date))
        assertNull(provider { null }.getMiddleRate("USD", date))
    }

    @Test
    fun `BAM needs no rate`() = runTest {
        val quote = provider { error("must not fetch") }.getMiddleRate("BAM", date)

        assertNotNull(quote)
        assertDecimalEquals("1", quote!!.middleRate)
    }
}
