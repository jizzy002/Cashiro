package com.ritesh.cashiro.presentation.widget

import com.ritesh.cashiro.data.database.entity.AccountBalanceEntity
import com.ritesh.cashiro.utils.CurrencyFormatter
import java.math.BigDecimal
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

data class WidgetAccountRow(
    val bankName: String,
    val last4: String,
    val amountText: String,
    val dotColorArgb: Int,
)

data class WidgetState(
    val totalText: String,
    val mixedCurrency: Boolean,
    val updatedText: String,
    val privacyHidden: Boolean,
    val accounts: List<WidgetAccountRow>,
)

object WidgetStateBuilder {
    fun build(
        balances: List<AccountBalanceEntity>,
        hiddenAccounts: Set<String>,
        privacyHidden: Boolean,
        now: LocalDateTime,
    ): WidgetState {
        val visible = balances
            .filter { "${it.bankName}_${it.accountLast4}" !in hiddenAccounts }
            .sortedByDescending { it.timestamp }

        // Total covers every visible account, not just the rows we display
        val currencies = visible.map { it.currency }.distinct()
        val mixed = currencies.size > 1
        val totalText = when {
            visible.isEmpty() -> "—"
            mixed -> "—"
            else -> CurrencyFormatter.formatCurrency(visible.sumOf { it.balance }, currencies.first())
        }

        val rows = visible
            .take(3)
            .map { b ->
            WidgetAccountRow(
                bankName = b.bankName,
                last4 = b.accountLast4,
                // unmixed: bare number (currency lives in the total); mixed: keep the inline currency
                amountText = if (mixed) CurrencyFormatter.formatCurrency(b.balance, b.currency)
                else CurrencyFormatter.formatAmount(b.balance, b.currency),
                dotColorArgb = parseColor(b.color),
            )
        }

        val updatedText = "Updated " + now.format(DateTimeFormatter.ofPattern("HH:mm"))
        return WidgetState(totalText, mixed, updatedText, privacyHidden, rows)
    }

    private fun parseColor(hex: String): Int = try {
        ColorUtil.fromHex(hex)
    } catch (_: Exception) {
        0xFF33B5E5.toInt()
    }
}

/** Hex helpers kept out of [WidgetStateBuilder] so tests import one object only. */
object ColorUtil {
    fun fromHex(hex: String): Int {
        var h = hex.removePrefix("#")
        if (h.length == 6) h = "FF$h"
        require(h.length == 8) { "bad color: $hex" }
        return h.toLong(16).toInt()
    }
}
