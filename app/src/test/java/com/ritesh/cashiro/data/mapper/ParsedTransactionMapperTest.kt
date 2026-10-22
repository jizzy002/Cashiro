package com.ritesh.cashiro.data.mapper

import com.ritesh.parser.core.ParsedTransaction
import com.ritesh.parser.core.TransactionType
import java.math.BigDecimal
import org.junit.Test
import kotlin.test.assertEquals

class ParsedTransactionMapperTest {

    private fun transaction(
        type: TransactionType,
        merchant: String?
    ): ParsedTransaction = ParsedTransaction(
        amount = BigDecimal("42.00"),
        type = type,
        merchant = merchant,
        reference = "ref",
        accountLast4 = "0700",
        balance = BigDecimal("100.00"),
        smsBody = "body",
        sender = "sender",
        timestamp = 0L,
        bankName = "Raiffeisen Bank"
    )

    @Test
    fun mapsExpenseMerchantToKeywordCategory() {
        val entity = transaction(TransactionType.EXPENSE, "KONZUM P1002 LIVNO").toEntity()

        assertEquals("Groceries", entity.category)
    }

    @Test
    fun mapsIncomeMerchantToSalaryCategory() {
        val entity = transaction(TransactionType.INCOME, "UPLATA MANPOWER DOO SARAJEVO").toEntity()

        assertEquals("Salary", entity.category)
    }

    @Test
    fun fallsBackToMiscellaneousForUnknownMerchant() {
        val entity = transaction(TransactionType.EXPENSE, "IU IT TEAM").toEntity()

        assertEquals("Miscellaneous", entity.category)
    }
}
