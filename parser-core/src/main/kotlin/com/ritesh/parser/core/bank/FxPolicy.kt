package com.ritesh.parser.core.bank

import java.math.BigDecimal

/**
 * Describes how a bank converts foreign-currency transactions into its
 * settlement currency.
 *
 * @param targetCurrency ISO code the transaction is converted into (e.g. "BAM").
 * @param feePercent percentage fee applied on top of the converted amount (e.g. 1 for 1 %).
 */
data class FxPolicy(
    val targetCurrency: String,
    val feePercent: BigDecimal
)
