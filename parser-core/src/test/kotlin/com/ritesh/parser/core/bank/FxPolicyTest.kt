package com.ritesh.parser.core.bank

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.math.BigDecimal

class FxPolicyTest {

    @Test
    fun `parsers default to no fx policy`() {
        assertNull(EnparaBankParser().getFxPolicy())
    }

    @Test
    fun `fx policy carries target currency and fee`() {
        val policy = FxPolicy(targetCurrency = "BAM", feePercent = BigDecimal("1"))

        assertEquals("BAM", policy.targetCurrency)
        assertEquals(BigDecimal("1"), policy.feePercent)
    }
}
