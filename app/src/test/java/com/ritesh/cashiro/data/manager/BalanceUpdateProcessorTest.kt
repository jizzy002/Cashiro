package com.ritesh.cashiro.data.manager

import android.content.ContextWrapper
import com.ritesh.cashiro.data.database.dao.CardDao
import com.ritesh.cashiro.data.database.entity.CardEntity
import com.ritesh.cashiro.data.database.entity.CardType
import com.ritesh.cashiro.data.database.entity.TransactionEntity
import com.ritesh.cashiro.data.database.entity.TransactionType
import com.ritesh.cashiro.data.repository.AccountBalanceRepository
import com.ritesh.cashiro.data.repository.CardRepository
import com.ritesh.cashiro.testing.FakeAccountBalanceDao
import com.ritesh.cashiro.testing.balance
import com.ritesh.parser.core.ParsedTransaction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDateTime
import kotlin.test.assertEquals

class BalanceUpdateProcessorTest {

    @Test
    fun processUsesResolvedEntityBankNameForBalanceSnapshot() = runTest {
        val timestamp = LocalDateTime.of(2026, 5, 23, 20, 35)
        val dao = FakeAccountBalanceDao()
        dao.seedBalance(
            balance(1, "Raiffeisen", "1234", BigDecimal("1000"), timestamp = timestamp.minusMinutes(5))
                .copy(sourceType = "MANUAL")
        )
        val processor = BalanceUpdateProcessor(
            cardRepository = CardRepository(FakeCardDao()),
            accountBalanceRepository = AccountBalanceRepository(dao, ContextWrapper(null))
        )

        processor.process(
            parsedTransaction = ParsedTransaction(
                amount = BigDecimal("50"),
                type = com.ritesh.parser.core.TransactionType.EXPENSE,
                merchant = "Merchant",
                reference = null,
                accountLast4 = "1234",
                balance = null,
                smsBody = "",
                sender = "RaiffeisenBiH",
                timestamp = 1748020535000L,
                bankName = "Raiffeisen Bank BiH",
                currency = "BAM"
            ),
            entity = TransactionEntity(
                id = 7,
                amount = BigDecimal("50"),
                merchantName = "Merchant",
                category = "Shopping",
                transactionType = TransactionType.EXPENSE,
                dateTime = timestamp,
                transactionHash = "hash",
                currency = "BAM",
                bankName = "Raiffeisen",
                accountNumber = "1234"
            ),
            rowId = 7L
        )

        assertEquals(1, dao.insertedBalances.size)
        val inserted = dao.insertedBalances.single()
        assertEquals("Raiffeisen", inserted.bankName)
        assertEquals("1234", inserted.accountLast4)
        assertEquals(BigDecimal("950"), inserted.balance)
    }

    @Test
    fun processKeepsParserBankNameForCardBalanceSnapshot() = runTest {
        val timestamp = LocalDateTime.of(2026, 5, 23, 20, 35)
        val dao = FakeAccountBalanceDao()
        val cardDao = FakeCardDao()
        val processor = BalanceUpdateProcessor(
            cardRepository = CardRepository(cardDao),
            accountBalanceRepository = AccountBalanceRepository(dao, ContextWrapper(null))
        )

        processor.process(
            parsedTransaction = ParsedTransaction(
                amount = BigDecimal("50"),
                type = com.ritesh.parser.core.TransactionType.CREDIT,
                merchant = "Merchant",
                reference = null,
                accountLast4 = "4321",
                balance = null,
                smsBody = "",
                sender = "RaiffeisenBiH",
                timestamp = 1748020535000L,
                bankName = "Card Bank",
                currency = "BAM",
                isFromCard = true
            ),
            entity = TransactionEntity(
                id = 7,
                amount = BigDecimal("50"),
                merchantName = "Merchant",
                category = "Shopping",
                transactionType = TransactionType.EXPENSE,
                dateTime = timestamp,
                transactionHash = "card-hash",
                currency = "BAM",
                bankName = "Some Other Bank",
                accountNumber = null
            ),
            rowId = 7L
        )

        assertEquals(1, dao.insertedBalances.size)
        assertEquals("Card Bank", dao.insertedBalances.single().bankName)
    }

    private class FakeCardDao : CardDao {
        val cards = mutableListOf<CardEntity>()
        private var nextId = 1L

        override suspend fun insertCard(card: CardEntity): Long {
            val id = nextId++
            cards.add(card.copy(id = id))
            return id
        }

        override suspend fun updateCard(card: CardEntity) = Unit
        override suspend fun deleteCard(card: CardEntity) = Unit
        override suspend fun getCard(bankName: String, cardLast4: String): CardEntity? =
            cards.find { it.bankName == bankName && it.cardLast4 == cardLast4 }
        override suspend fun getCardById(cardId: Long): CardEntity? = null
        override suspend fun getCardsByLast4(cardLast4: String): List<CardEntity> = emptyList()
        override suspend fun getCardsForAccount(accountLast4: String): List<CardEntity> = emptyList()
        override fun getCardsForAccountFlow(accountLast4: String): Flow<List<CardEntity>> = flowOf(emptyList())
        override suspend fun getOrphanedCards(cardType: CardType): List<CardEntity> = emptyList()
        override fun getAllActiveCards(): Flow<List<CardEntity>> = flowOf(emptyList())
        override fun getAllCards(): Flow<List<CardEntity>> = flowOf(emptyList())
        override suspend fun linkCardToAccount(cardId: Long, accountLast4: String?) = Unit
        override suspend fun setCardActive(cardId: Long, isActive: Boolean) = Unit
        override suspend fun getCardCount(): Int = 0
        override suspend fun getCardCountByType(cardType: CardType): Int = 0
        override suspend fun getBankCards(bankName: String, cardType: CardType): List<CardEntity> = emptyList()
        override suspend fun deleteAllCards() = Unit
        override suspend fun deleteSampleCards() = Unit
    }
}
