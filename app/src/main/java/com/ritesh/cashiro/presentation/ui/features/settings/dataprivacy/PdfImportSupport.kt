package com.ritesh.cashiro.presentation.ui.features.settings.dataprivacy

import com.ritesh.cashiro.data.database.entity.TransactionEntity
import com.ritesh.cashiro.data.database.entity.TransactionType as StoredTransactionType
import com.ritesh.parser.core.ParsedTransaction
import com.ritesh.parser.core.TransactionType as ParsedTransactionType
import java.time.Duration
import java.util.Locale

/**
 * Small, testable decisions shared by the PDF import flow.
 */
internal object PdfImportSupport {

    private val TIMED_WINDOW: Duration = Duration.ofMinutes(15)
    private val DATE_ONLY_WINDOW: Duration = Duration.ofHours(48)
    private val REFERENCE_PATTERN = Regex(
        """(?:UPI[:\s]*|UTR\s+No\.?[:\s]*|Ref\s+No\.?[:\s]*|ID[:\s]*)([\d\s]+)""",
        RegexOption.IGNORE_CASE
    )
    private val DIGITS_ONLY = Regex("""[\d\s]+""")
    private val NON_ALPHANUMERIC = Regex("""[^a-z0-9]+""")

    /**
     * Statements that only carry a calendar date need a wider window because
     * the parsed timestamp is clamped to midnight.
     */
    fun duplicateWindow(hasTimeOfDay: Boolean): Duration =
        if (hasTimeOfDay) TIMED_WINDOW else DATE_ONLY_WINDOW

    /**
     * A duplicate needs more than an amount inside the date window: the money
     * must flow in the same direction, the merchant must agree, and a known
     * account must not contradict. References are the strongest evidence.
     *
     * Deliberately returns false when only the amount matches. Defaulting those
     * rows to "skip" silently dropped legitimate small repeated purchases such
     * as 0.15 or 4.99.
     */
    fun isLikelyDuplicate(
        parsed: ParsedTransaction,
        existing: TransactionEntity
    ): Boolean {
        if (existing.isDeleted) return false
        if (existing.amount.compareTo(parsed.amount) != 0) return false
        if (existing.transactionType == StoredTransactionType.BALANCE_UPDATE) return false

        val parsedIsIncome = parsed.type == ParsedTransactionType.INCOME
        val existingIsIncome = existing.transactionType == StoredTransactionType.INCOME
        if (parsedIsIncome != existingIsIncome) return false

        val parsedReference = extractReferenceNumber(parsed.reference)
        if (!parsedReference.isNullOrEmpty()) {
            val existingReference = extractReferenceNumber(existing.reference)
                ?: extractReferenceNumber(existing.smsBody)
                ?: extractReferenceNumber(existing.description)
            if (parsedReference == existingReference) return true
        }

        if (!merchantsMatch(parsed.merchant, existing.merchantName)) return false

        val parsedLast4 = last4(parsed.accountLast4)
        val existingLast4 = last4(existing.accountNumber)
        return parsedLast4 == null || existingLast4 == null || parsedLast4 == existingLast4
    }

    /**
     * Statement order is authoritative for the closing balance. On a date-only
     * statement many rows share midnight, so sorting or `maxBy(timestamp)` picks
     * an arbitrary row of the final day. `toMap()` intentionally keeps the last
     * row seen for each account.
     */
    fun closingBalancesByAccount(
        transactions: List<ParsedTransaction>
    ): Map<String, ParsedTransaction> =
        transactions.mapNotNull { transaction ->
            val account = transaction.accountLast4?.takeIf { it.isNotBlank() }
                ?: return@mapNotNull null
            if (transaction.balance == null) return@mapNotNull null
            account to transaction
        }.toMap()

    fun extractReferenceNumber(text: String?): String? {
        if (text == null) return null
        if (text.trim().matches(DIGITS_ONLY)) {
            return text.filter(Char::isDigit).ifEmpty { null }
        }
        val match = REFERENCE_PATTERN.find(text) ?: return null
        return match.groupValues[1].filter(Char::isDigit).ifEmpty { null }
    }

    private fun merchantsMatch(first: String?, second: String?): Boolean {
        val left = normalizeMerchant(first)
        val right = normalizeMerchant(second)
        if (left.isEmpty() || right.isEmpty()) return false
        if (left == right) return true
        if (left.length < 5 || right.length < 5) return false
        return left.contains(right) || right.contains(left)
    }

    private fun normalizeMerchant(value: String?): String =
        value.orEmpty()
            .lowercase(Locale.ROOT)
            .replace(NON_ALPHANUMERIC, " ")
            .trim()

    private fun last4(value: String?): String? =
        value?.filter(Char::isDigit)?.takeLast(4)?.takeIf { it.isNotEmpty() }
}
