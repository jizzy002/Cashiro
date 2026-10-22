package com.ritesh.cashiro.data.currency

import com.ritesh.parser.core.ParsedTransaction
import com.ritesh.parser.core.bank.FxPolicy
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * A rate expressed as the value of one unit of the source currency in the
 * target currency, together with a human-readable source label.
 */
data class FxRate(
    val ratePerUnit: BigDecimal,
    val sourceLabel: String
)

/**
 * Converts a parsed foreign-currency transaction into the bank's settlement
 * currency, applying the configured percentage fee on top of the converted
 * amount.
 *
 * Rate lookup is injected so the chain (bank list → cached/market rates →
 * nothing) can be exercised without Android or network dependencies.
 */
class FxTransactionApplier(
    private val primaryRate: suspend (currency: String, date: LocalDate) -> FxRate?,
    private val fallbackRate: suspend (currency: String, targetCurrency: String) -> BigDecimal?
) {

    suspend fun apply(transaction: ParsedTransaction, policy: FxPolicy): ParsedTransaction {
        if (transaction.currency.equals(policy.targetCurrency, ignoreCase = true)) {
            return transaction
        }

        val date = Instant.ofEpochMilli(transaction.timestamp)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()

        val rate = primaryRate(transaction.currency, date)
            ?: fallbackRate(transaction.currency, policy.targetCurrency)
                ?.let { FxRate(it, "market rate") }
            ?: return transaction

        val feeFactor = BigDecimal.ONE.add(
            policy.feePercent.divide(BigDecimal(100), FEE_SCALE, RoundingMode.HALF_UP)
        )
        val convertedAmount = transaction.amount
            .multiply(rate.ratePerUnit)
            .multiply(feeFactor)
            .setScale(2, RoundingMode.HALF_UP)

        val note = buildString {
            append("\n[Converted: ")
            append(transaction.amount.stripTrailingZeros().toPlainString())
            append(' ')
            append(transaction.currency)
            append(" → ")
            append(convertedAmount.toPlainString())
            append(' ')
            append(policy.targetCurrency)
            append(" (")
            append(rate.sourceLabel)
            append(" + ")
            append(policy.feePercent.stripTrailingZeros().toPlainString())
            append("% fee)]")
        }

        return transaction.copy(
            amount = convertedAmount,
            currency = policy.targetCurrency,
            smsBody = transaction.smsBody + note
        )
    }

    private companion object {
        const val FEE_SCALE = 6
    }
}
