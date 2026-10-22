package com.ritesh.cashiro.utils

import com.ritesh.cashiro.data.currency.model.CurrencySymbols
import org.junit.Assert.assertEquals
import org.junit.Test
import java.math.BigDecimal

class CurrencyBamTest {

    @Test
    fun `BAM uses the KM symbol`() {
        assertEquals("KM", CurrencySymbols.getSymbol("BAM"))
        assertEquals("KM", CurrencyFormatter.getCurrencySymbol("BAM"))
    }

    @Test
    fun `BAM is formatted with Bosnian separators`() {
        val formatted = CurrencyFormatter
            .formatCurrency(BigDecimal("1234.56"), "BAM")
            .replace('\u00A0', ' ')
            .replace('\u202F', ' ')
            .trim()

        assertEquals("1.234,56 KM", formatted)
    }
}
