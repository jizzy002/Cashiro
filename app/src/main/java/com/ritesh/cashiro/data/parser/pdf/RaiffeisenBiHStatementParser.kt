package com.ritesh.cashiro.data.parser.pdf

import com.ritesh.parser.core.ParsedTransaction
import com.ritesh.parser.core.TransactionType
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Parser for Raiffeisen Bank BiH account statements ("IZVOD").
 *
 * Every row has: processing date, transaction date, description, currency,
 * amount in currency, amount in BAM (KM) and running balance. The KM column is
 * authoritative because the bank has already applied its middle rate. Foreign
 * spends are followed/preceded by a separate 1 % commission row in BAM; the
 * two are merged into one transaction.
 */
class RaiffeisenBiHStatementParser : PdfStatementParser {

    override fun canHandle(text: String): Boolean =
        text.contains("Raiffeisen", ignoreCase = true) &&
            text.contains("IZVOD", ignoreCase = true)

    override fun hasTimeOfDay(): Boolean = false

    override fun parse(text: String): List<ParsedTransaction> {
        val last4 = extractAccountLast4(text)
        val rows = ROW_PATTERN.findAll(text)
            .mapNotNull { parseRow(it.groupValues) }
            .filter { it.kmAmount.signum() != 0 }
            .toList()

        val transactions = mutableListOf<ParsedTransaction>()
        var index = 0
        while (index < rows.size) {
            val row = rows[index]
            val next = rows.getOrNull(index + 1)
            if (isCommissionFor(row, next)) {
                val merged = next!!.copy(
                    kmAmount = next.kmAmount.add(row.kmAmount),
                    commissionKm = row.kmAmount.abs()
                )
                transactions += toTransaction(merged, last4)
                index += 2
            } else {
                transactions += toTransaction(row, last4)
                index += 1
            }
        }
        return transactions
    }

    private fun parseRow(groups: List<String>): StatementRow? = runCatching {
        StatementRow(
            transactionDate = LocalDate.parse(groups[2], DATE_FORMAT),
            description = groups[3].trim(),
            currency = groups[4].uppercase(),
            foreignAmount = parseAmount(groups[5]) ?: return null,
            kmAmount = parseAmount(groups[6]) ?: return null,
            balance = parseAmount(groups[7]) ?: return null
        )
    }.getOrNull()

    private fun parseAmount(raw: String): BigDecimal? =
        raw.replace(",", "").trim().toBigDecimalOrNull()

    private fun isCommissionFor(row: StatementRow, next: StatementRow?): Boolean {
        if (next == null) return false
        if (row.currency != "BAM" || row.foreignAmount.signum() != 0) return false
        if (next.currency == "BAM") return false
        if (row.transactionDate != next.transactionDate) return false
        if (!row.description.equals(next.description, ignoreCase = true)) return false

        val commission = row.kmAmount.abs()
        val converted = next.kmAmount.abs()
        if (commission.signum() == 0 || converted.signum() == 0) return false

        return commission <= converted.multiply(COMMISSION_LIMIT)
            .setScale(4, RoundingMode.HALF_UP)
    }

    private fun toTransaction(row: StatementRow, last4: String?): ParsedTransaction {
        val type = if (row.kmAmount.signum() < 0) TransactionType.EXPENSE else TransactionType.INCOME
        val date = row.transactionDate.format(DISPLAY_DATE_FORMAT)

        val body = buildString {
            append("Raiffeisen BiH statement\n")
            append(date)
            append(' ')
            append(row.description)
            if (row.currency == "BAM") {
                append(' ')
                append(row.kmAmount.toPlainString())
                append(" BAM")
            } else {
                append(' ')
                append(row.foreignAmount.toPlainString())
                append(' ')
                append(row.currency)
                append("\nConverted: ")
                append(row.kmAmount.toPlainString())
                append(" BAM")
                if (row.commissionKm.signum() != 0) {
                    append(" (incl. 1% commission ")
                    append(row.commissionKm.toPlainString())
                    append(" BAM)")
                }
            }
        }

        return ParsedTransaction(
            amount = row.kmAmount.abs(),
            type = type,
            merchant = row.description,
            reference = null,
            accountLast4 = last4,
            balance = row.balance,
            smsBody = body,
            sender = "RaiffeisenBiH PDF",
            timestamp = row.transactionDate
                .atStartOfDay(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli(),
            bankName = "Raiffeisen Bank BiH",
            currency = "BAM"
        )
    }

    private fun extractAccountLast4(text: String): String? {
        val raw = IBAN_PATTERN.find(text)?.groupValues?.get(1)
            ?: ACCOUNT_PATTERN.find(text)?.groupValues?.get(1)
            ?: return null
        return raw.filter { it.isDigit() }.takeLast(4).takeIf { it.length == 4 }
    }

    private data class StatementRow(
        val transactionDate: LocalDate,
        val description: String,
        val currency: String,
        val foreignAmount: BigDecimal,
        val kmAmount: BigDecimal,
        val balance: BigDecimal,
        val commissionKm: BigDecimal = BigDecimal.ZERO
    )

    private companion object {
        val DATE_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
        val DISPLAY_DATE_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
        val COMMISSION_LIMIT: BigDecimal = BigDecimal("0.02")

        val ROW_PATTERN = Regex(
            """^[ \t]*(\d{2}/\d{2}/\d{4})\s+(\d{2}/\d{2}/\d{4})\s+(.+?)\s+([A-Z]{3})\s+(-?[\d.,]+)\s+(-?[\d.,]+)\s+(-?[\d.,]+)\s*$""",
            RegexOption.MULTILINE
        )
        val IBAN_PATTERN = Regex("""IBAN:\s*([A-Z]{2}\d{2}[A-Z0-9]{10,30})""")
        val ACCOUNT_PATTERN = Regex("""Racun:\s*(\d{6,})""", RegexOption.IGNORE_CASE)
    }
}
