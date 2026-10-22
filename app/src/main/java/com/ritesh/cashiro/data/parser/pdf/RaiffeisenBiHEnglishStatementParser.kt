package com.ritesh.cashiro.data.parser.pdf

import com.ritesh.parser.core.ParsedTransaction
import com.ritesh.parser.core.TransactionType
import java.math.BigDecimal
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatterBuilder
import java.util.Locale

/**
 * Parser for Raiffeisen Bank BiH's English "Account Statement" export.
 *
 * This is the e-banking statement layout, not the Bosnian "IZVOD" PDF. Rows are
 * ordered and carry a closing balance, so the balance delta is authoritative:
 * it tells us both the amount and the direction even when the debit/credit
 * columns are collapsed by PDF text extraction. The parser refuses to emit
 * anything unless every row and the final balance reconcile.
 *
 * Service fees that belong to the next row (foreign purchase fee, servicing
 * fee, refund fee) are netted into that transaction. This keeps the imported
 * totals identical to the statement while avoiding a second "fee" row.
 */
class RaiffeisenBiHEnglishStatementParser : PdfStatementParser {

    override fun canHandle(text: String): Boolean {
        val normalized = normalize(text)
        return normalized.contains("account statement", ignoreCase = true) &&
            normalized.contains("book date", ignoreCase = true) &&
            normalized.contains("balance at period start", ignoreCase = true) &&
            normalized.contains("bam", ignoreCase = true) &&
            extractAccountNumber(normalized) != null
    }

    override fun hasTimeOfDay(): Boolean = false

    override fun parse(text: String): List<ParsedTransaction> {
        val normalized = normalize(text)
        val lines = normalized.lines()
        val accountNumber = extractAccountNumber(normalized)
            ?: throw IllegalStateException("Raiffeisen account number not found in statement")
        val last4 = accountNumber.takeLast(4)

        val opening = findBalance(lines, "Balance at Period Start")
            ?: throw IllegalStateException("Statement opening balance not found")
        val closing = findBalance(lines, "Balance at Period End")
            ?: throw IllegalStateException("Statement closing balance not found")
        val rows = collectRows(lines)
        if (rows.isEmpty()) {
            throw IllegalStateException("No transaction rows found in this statement")
        }

        var runningBalance = opening
        rows.forEachIndexed { rowIndex, row ->
            val delta = row.balance.subtract(runningBalance)
            if (delta.abs().compareTo(row.amount) != 0) {
                throw IllegalStateException(
                    "The Raiffeisen statement row ${rowIndex + 1} could not be reconciled."
                )
            }
            runningBalance = row.balance
        }
        if (runningBalance.compareTo(closing) != 0) {
            throw IllegalStateException("The Raiffeisen statement totals could not be reconciled.")
        }

        val transactions = mutableListOf<ParsedTransaction>()
        var index = 0
        var previousBalance = opening
        while (index < rows.size) {
            val current = rows[index]
            val next = rows.getOrNull(index + 1)
            val mergeKind = mergeKind(current, next)

            if (mergeKind != null) {
                val after = next!!.balance
                val delta = after.subtract(previousBalance)
                if (delta.signum() != 0) {
                    val isRefund = mergeKind == MergeKind.REFUND_FEE
                    val fee = if (isRefund) next.amount else current.amount
                    val date = if (isRefund) current.valueDate else next.valueDate
                    val bookDate = if (isRefund) current.bookDate else next.bookDate
                    val description = if (isRefund) current.description else next.description
                    val merchant = merchantFor(if (isRefund) current else next)
                    transactions += toParsed(
                        amount = delta.abs(),
                        type = if (delta.signum() > 0) TransactionType.INCOME else TransactionType.EXPENSE,
                        merchant = merchant,
                        reference = current.reference,
                        accountLast4 = last4,
                        balance = after,
                        bookDate = bookDate,
                        valueDate = date,
                        description = description,
                        fee = fee,
                        feeNetted = true
                    )
                }
                previousBalance = after
                index += 2
            } else {
                val delta = current.balance.subtract(previousBalance)
                if (delta.signum() != 0) {
                    transactions += toParsed(
                        amount = delta.abs(),
                        type = if (delta.signum() > 0) TransactionType.INCOME else TransactionType.EXPENSE,
                        merchant = merchantFor(current),
                        reference = current.reference,
                        accountLast4 = last4,
                        balance = current.balance,
                        bookDate = current.bookDate,
                        valueDate = current.valueDate,
                        description = current.description,
                        fee = BigDecimal.ZERO,
                        feeNetted = false
                    )
                }
                previousBalance = current.balance
                index += 1
            }
        }

        return transactions
    }

    private fun collectRows(lines: List<String>): List<RawRow> {
        val rows = mutableListOf<RawRow>()
        var lineIndex = 0
        while (lineIndex < lines.size) {
            val start = matchRowStart(lines[lineIndex].trim())
            if (start == null) {
                lineIndex++
                continue
            }

            val bookDate = parseDate(start.groupValues[1])
                ?: throw IllegalStateException("Invalid book date in transaction row ${rows.size + 1}.")
            val reference = start.groupValues[2]
            val rowText = StringBuilder(start.groupValues[3].trim())
            var endLine = lineIndex
            var endMatch: MatchResult? = null

            while (endLine < lines.size && endLine - lineIndex <= MAX_ROW_WRAP_LINES) {
                val match = ROW_END.find(rowText.toString())
                if (match != null) {
                    endMatch = match
                    break
                }
                endLine++
                if (endLine >= lines.size) break
                val nextLine = lines[endLine].trim()
                if (nextLine.isEmpty() || isNoiseLine(nextLine)) continue
                // A new transaction started before this row found its value date.
                if (matchRowStart(nextLine) != null) break
                rowText.append(' ').append(nextLine)
            }

            if (endMatch == null) {
                // Not a complete transaction row (for example a truncated line).
                // Move on so later complete rows can still be read.
                lineIndex++
                continue
            }

            val valueDate = parseDate(endMatch.groupValues[2])
                ?: throw IllegalStateException("Invalid value date in transaction row ${rows.size + 1}.")
            val numbers = NUMBER_PATTERN.findAll(endMatch.groupValues[3])
                .map { it.value.toAmount() }
                .toList()
            if (numbers.size < 2) {
                throw IllegalStateException(
                    "Could not read amount and balance in transaction row ${rows.size + 1}."
                )
            }
            val balance = numbers.last()
            val nonZeroAmounts = numbers.dropLast(1).filter { it.signum() != 0 }
            if (nonZeroAmounts.size > 1) {
                throw IllegalStateException(
                    "Ambiguous debit/credit columns in transaction row ${rows.size + 1}."
                )
            }
            val amount = nonZeroAmounts.singleOrNull() ?: BigDecimal.ZERO
            rows += RawRow(
                bookDate = bookDate,
                reference = reference,
                description = endMatch.groupValues[1].trim(),
                valueDate = valueDate,
                amount = amount,
                balance = balance,
                lineIndex = lineIndex,
                endLine = endLine
            )
            lineIndex = endLine + 1
        }

        return rows.mapIndexed { index, row ->
            val end = rows.getOrNull(index + 1)?.lineIndex ?: lines.size
            row.copy(
                continuation = cleanContinuation(lines.subList(row.endLine + 1, end))
            )
        }
    }

    private fun cleanContinuation(lines: List<String>): List<String> {
        val result = mutableListOf<String>()
        var skipPageNumber = false
        for (raw in lines) {
            val line = raw.trim()
            if (line.isEmpty()) continue
            if (skipPageNumber) {
                skipPageNumber = false
                if (line.matches(PAGE_NUMBER)) continue
            }
            if (line.startsWith("Page ", ignoreCase = true)) {
                skipPageNumber = true
                continue
            }
            if (!isNoiseLine(line)) result += line
        }
        return result
    }

    private fun mergeKind(current: RawRow, next: RawRow?): MergeKind? {
        if (next == null) return null
        if (current.reference != next.reference) return null
        if (current.valueDate != next.valueDate) return null
        return when {
            current.isProcFee && !next.isProcFee -> MergeKind.FEE_PURCHASE
            current.isServicingFee -> MergeKind.SERVICING_PAYMENT
            current.isRefund && next.isProcFee -> MergeKind.REFUND_FEE
            else -> null
        }
    }

    private fun merchantFor(row: RawRow): String {
        val continuation = row.continuation.joinToString(" ").trim()
        if (continuation.isNotEmpty()) return continuation
        return row.description
            .replace(Regex("""\s*Proc Fee""", RegexOption.IGNORE_CASE), "")
            .trim()
            .ifEmpty { row.description }
    }

    private fun toParsed(
        amount: BigDecimal,
        type: TransactionType,
        merchant: String,
        reference: String,
        accountLast4: String,
        balance: BigDecimal,
        bookDate: LocalDate,
        valueDate: LocalDate,
        description: String,
        fee: BigDecimal,
        feeNetted: Boolean
    ): ParsedTransaction {
        val body = buildString {
            appendLine("Raiffeisen Bank BiH account statement")
            appendLine("Account: ****$accountLast4")
            appendLine("Book date: ${bookDate.format(DISPLAY_DATE)}")
            appendLine("Value date: ${valueDate.format(DISPLAY_DATE)}")
            appendLine("Reference: $reference")
            appendLine("Description: $description")
            appendLine("Merchant: $merchant")
            append("Amount: ${amount.toPlainString()} BAM\n")
            if (fee.signum() > 0) {
                append(if (feeNetted) "Fee netted: " else "Fee included: ")
                append(fee.toPlainString())
                append(" BAM\n")
            }
            append("Balance: ${balance.toPlainString()} BAM")
        }

        return ParsedTransaction(
            amount = amount,
            type = type,
            merchant = merchant,
            reference = reference,
            accountLast4 = accountLast4,
            balance = balance,
            smsBody = body,
            sender = "RaiffeisenBiH PDF",
            timestamp = valueDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli(),
            bankName = "Raiffeisen Bank BiH",
            currency = "BAM"
        )
    }

    private fun findBalance(lines: List<String>, label: String): BigDecimal? {
        val index = lines.indexOfFirst { it.contains(label, ignoreCase = true) }
        if (index < 0) return null
        val line = lines[index]
        val labelIndex = line.indexOf(label, ignoreCase = true)
        val sameLine = if (labelIndex >= 0) {
            NUMBER_PATTERN.find(line.substring(labelIndex + label.length))?.value
        } else {
            null
        }
        val raw = sameLine ?: lines.drop(index + 1)
            .firstOrNull { it.isNotBlank() }
            ?.let { NUMBER_PATTERN.find(it)?.value }
        return raw?.toAmount()
    }

    private fun extractAccountNumber(text: String): String? {
        val raw = ACCOUNT_HEADER.find(text)?.groupValues?.get(1) ?: return null
        return BBAN_PATTERN.find(raw.filter(Char::isDigit))?.value
    }

    private fun matchRowStart(line: String): MatchResult? {
        val match = ROW_START.matchEntire(line) ?: return null
        // A value-date line also starts with a date, but its second token is an
        // amount (for example "02 OCT 26  1,200.00  2,189.90"), not a reference.
        return match.takeUnless { AMOUNT_TOKEN.matches(it.groupValues[2]) }
    }

    private fun parseDate(raw: String): LocalDate? =
        runCatching { LocalDate.parse(raw, DATE_FORMAT) }.getOrNull()

    private fun String.toAmount(): BigDecimal =
        replace(",", "").toBigDecimal()

    private fun isNoiseLine(line: String): Boolean {
        if (line.startsWith("AC-", ignoreCase = true)) return true
        if (line.startsWith("AC ", ignoreCase = true)) return true
        if (line.startsWith("Balance at Period", ignoreCase = true)) return true
        if (line.startsWith("Page ", ignoreCase = true)) return true
        if (line.startsWith("Account Statement", ignoreCase = true)) return true
        if (line.startsWith("Book Date", ignoreCase = true)) return true
        if (line.startsWith("Currency", ignoreCase = true)) return true
        if (line.startsWith("Customer", ignoreCase = true)) return true
        if (ACCOUNT_LABEL.matches(line)) return true
        return false
    }

    private fun normalize(text: String): String =
        text.replace("\r\n", "\n")
            .replace('\r', '\n')
            .replace('\u000C', '\n')
            .replace('\u00A0', ' ')

    private data class RawRow(
        val bookDate: LocalDate,
        val reference: String,
        val description: String,
        val valueDate: LocalDate,
        val amount: BigDecimal,
        val balance: BigDecimal,
        val lineIndex: Int,
        val endLine: Int,
        val continuation: List<String> = emptyList()
    ) {
        val isProcFee: Boolean
            get() = description.contains("Proc Fee", ignoreCase = true) &&
                !description.contains("Servicing Fee", ignoreCase = true)

        val isServicingFee: Boolean
            get() = description.contains("Servicing Fee", ignoreCase = true)

        val isRefund: Boolean
            get() = description.startsWith("Refund", ignoreCase = true)
    }

    private enum class MergeKind {
        FEE_PURCHASE,
        SERVICING_PAYMENT,
        REFUND_FEE
    }

    private companion object {
        val DATE_FORMAT: DateTimeFormatter = DateTimeFormatterBuilder()
            .parseCaseInsensitive()
            .appendPattern("dd MMM yy")
            .toFormatter(Locale.ENGLISH)

        val DISPLAY_DATE: DateTimeFormatter = DateTimeFormatter.ofPattern("dd MMM yy", Locale.ENGLISH)

        val ACCOUNT_HEADER = Regex(
            """Account\s*:\s*((?:BA\s*\d{2}\s*)?[\d\s-]{10,})""",
            RegexOption.IGNORE_CASE
        )
        val BBAN_PATTERN = Regex("""161\d{13}""")
        val ACCOUNT_LABEL = Regex("""Account\s*:.*""", RegexOption.IGNORE_CASE)

        const val MAX_ROW_WRAP_LINES = 4

        val ROW_START = Regex(
            """^(\d{2} [A-Z]{3} \d{2})\s+(\S+)\s+(.+)$""",
            RegexOption.IGNORE_CASE
        )
        val ROW_END = Regex(
            """^(.*)\s+(\d{2} [A-Z]{3} \d{2})\s+(-?[\d,]+\.\d{2}(?:\s+-?[\d,]+\.\d{2})*)\s*$""",
            RegexOption.IGNORE_CASE
        )
        val NUMBER_PATTERN = Regex("""-?[\d,]+\.\d{2}""")
        val AMOUNT_TOKEN = Regex("""-?[\d,]+\.\d{2}""")
        val PAGE_NUMBER = Regex("""\d+""")
    }
}
