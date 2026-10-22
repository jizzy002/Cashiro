package com.ritesh.parser.core.bank

import com.ritesh.parser.core.ParsedTransaction
import com.ritesh.parser.core.TransactionType
import java.math.BigDecimal

/**
 * Parser for Raiffeisen Bank Bosnia and Herzegovina (RMB app) push notifications.
 *
 * Notifications are not SMS and always arrive in the form:
 * ```
 * Odlazna transakcija
 * Sa Vašeg računa <account> izvršena je isplata u iznosu od 310.00 BAM.
 * Opis:<merchant>
 * Vaša Raiffeisen banka
 * ```
 * Incoming payments use "uplata"/"Dolazna transakcija" instead.
 * Foreign-currency transactions are later converted to BAM under [getFxPolicy].
 */
class RaiffeisenBiHParser : BankParser() {

    override fun getBankName() = "Raiffeisen Bank BiH"

    override fun getCurrency() = "BAM"

    override fun canHandle(sender: String): Boolean =
        sender.uppercase().contains("RAIFFEISEN")

    override fun getFxPolicy(): FxPolicy =
        FxPolicy(targetCurrency = "BAM", feePercent = BigDecimal("1"))

    override fun isTransactionMessage(message: String): Boolean {
        val lower = message.lowercase()
        return lower.contains("isplata") || lower.contains("uplata")
    }

    override fun extractAmount(message: String): BigDecimal? {
        val raw = AMOUNT_PATTERN.find(message)?.groupValues?.get(1) ?: return null
        return normalizeAmount(raw)
    }

    override fun extractCurrency(message: String): String? =
        CURRENCY_PATTERN.find(message)?.groupValues?.get(1)?.uppercase()

    override fun extractTransactionType(message: String): TransactionType? {
        val lower = message.lowercase()
        return when {
            lower.contains("isplata") -> TransactionType.EXPENSE
            lower.contains("uplata") -> TransactionType.INCOME
            else -> null
        }
    }

    override fun extractMerchant(message: String, sender: String): String? {
        val raw = MERCHANT_PATTERN.find(message)?.groupValues?.get(1) ?: return null
        return raw
            .replace(LOCATION_SUFFIX, "")
            .trim()
            .takeIf { it.isNotEmpty() }
    }

    override fun extractAccountLast4(message: String): String? {
        val raw = ACCOUNT_PATTERN.find(message)?.groupValues?.get(1) ?: return null
        return extractLast4Digits(raw)
    }

    override fun parse(smsBody: String, sender: String, timestamp: Long): ParsedTransaction? {
        val parsed = super.parse(smsBody, sender, timestamp) ?: return null
        return parsed.copy(currency = extractCurrency(smsBody) ?: getCurrency())
    }

    /**
     * Handles the dot-decimal format used by the RMB push (e.g. 1752.66) and
     * defensively covers comma decimals and grouped values.
     */
    private fun normalizeAmount(raw: String): BigDecimal? {
        val lastComma = raw.lastIndexOf(',')
        val lastDot = raw.lastIndexOf('.')
        val normalized = when {
            lastComma > lastDot -> {
                val digitsAfterComma = raw.length - lastComma - 1
                if (lastDot == -1 && digitsAfterComma == 3) raw.replace(",", "")
                else raw.replace(".", "").replace(',', '.')
            }
            lastDot > lastComma -> raw.replace(",", "")
            else -> raw
        }
        return runCatching { BigDecimal(normalized) }.getOrNull()
    }

    private companion object {
        val AMOUNT_PATTERN = Regex("""iznosu\s+od\s+([0-9][0-9.,]*)""", RegexOption.IGNORE_CASE)
        val CURRENCY_PATTERN =
            Regex("""iznosu\s+od\s+[0-9][0-9.,]*\s*([A-Z]{3})""", RegexOption.IGNORE_CASE)
        val MERCHANT_PATTERN =
            Regex("""Opis:\s*(.+?)(?:\s+Vaša\s+Raiffeisen\s+banka|[\r\n]|$)""", RegexOption.IGNORE_CASE)
        val LOCATION_SUFFIX = Regex("""\s*>.*$""")
        val ACCOUNT_PATTERN = Regex("""ra[čc]un[a]?\s+(\d{6,})""", RegexOption.IGNORE_CASE)
    }
}
