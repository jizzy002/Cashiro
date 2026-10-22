package com.ritesh.cashiro.data.currency

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.math.BigDecimal
import java.math.RoundingMode
import java.net.HttpURLConnection
import java.net.URL
import java.time.Clock
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * One middle-rate quote from Raiffeisen Bank BiH's publicly published
 * "kursna lista" (account exchange list).
 *
 * @param middleRate BAM value of one unit of [currency] (parity already applied).
 */
data class FxQuote(
    val currency: String,
    val middleRate: BigDecimal,
    val parity: Int,
    val effectiveDate: LocalDate,
    val listNumber: Int?
)

/**
 * Fetches the Raiffeisen Bank BiH middle rate ("srednji kurs") for a currency.
 *
 * The bank publishes a daily JSON list; when today's list is not available
 * (weekend/holiday) the provider walks back up to [MAX_BACKTRACK_DAYS].
 * Results are cached in memory per currency and list date.
 */
@Singleton
class RaiffeisenRateProvider @Inject constructor() {

    internal var clock: Clock = Clock.systemDefaultZone()

    internal var fetchText: suspend (String) -> String? = { url -> defaultFetchText(url) }

    private val cache = ConcurrentHashMap<Pair<String, LocalDate>, FxQuote>()

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    /**
     * Returns the middle-rate quote for [currency] on or before [date].
     * BAM is the settlement currency and needs no quote.
     */
    suspend fun getMiddleRate(
        currency: String,
        date: LocalDate = LocalDate.now(clock)
    ): FxQuote? {
        val code = currency.uppercase()
        if (code == "BAM") {
            return FxQuote(code, BigDecimal.ONE, 1, date, null)
        }

        repeat(MAX_BACKTRACK_DAYS + 1) { attempt ->
            val probeDate = date.minusDays(attempt.toLong())
            cache[code to probeDate]?.let { return it }

            val body = fetchText(urlFor(probeDate)) ?: return@repeat
            val quote = parseKursnaLista(body, code, probeDate) ?: return@repeat
            cache[code to probeDate] = quote
            return quote
        }

        return null
    }

    internal fun parseKursnaLista(
        body: String,
        currency: String,
        effectiveDate: LocalDate
    ): FxQuote? = runCatching {
        val rates = json.parseToJsonElement(body).jsonObject["rates"]?.jsonArray
            ?: return null

        for (rate in rates) {
            val entries = rate.jsonObject["currencyList"]?.jsonArray ?: continue
            for (entry in entries) {
                val obj = entry.jsonObject
                val right = obj["currencyPair"]?.jsonObject
                    ?.get("right")?.jsonObject
                    ?.get("code")?.jsonPrimitive?.content
                    ?: continue
                if (!right.equals(currency, ignoreCase = true)) continue

                val middle = obj["middleRate"]?.jsonObject
                    ?.get("value")?.jsonPrimitive
                    ?.content?.toBigDecimalOrNull()
                    ?: continue
                val parity = obj["parity"]?.jsonPrimitive
                    ?.content?.toBigDecimalOrNull()
                    ?.toInt() ?: 1
                if (parity <= 0) continue

                val listNumber = obj["listNumber"]?.jsonPrimitive
                    ?.content?.toIntOrNull()
                val perUnit = middle.divide(
                    BigDecimal(parity),
                    RATE_SCALE,
                    RoundingMode.HALF_UP
                )

                return FxQuote(
                    currency = currency.uppercase(),
                    middleRate = perUnit,
                    parity = parity,
                    effectiveDate = effectiveDate,
                    listNumber = listNumber
                )
            }
        }
        null
    }.getOrNull()

    private fun urlFor(date: LocalDate): String =
        URL_TEMPLATE.format(date.format(URL_DATE_FORMAT))

    private suspend fun defaultFetchText(url: String): String? = withContext(Dispatchers.IO) {
        var connection: HttpURLConnection? = null
        try {
            connection = URL(url).openConnection() as HttpURLConnection
            connection.connectTimeout = TIMEOUT_MS
            connection.readTimeout = TIMEOUT_MS
            connection.instanceFollowRedirects = true
            connection.requestMethod = "GET"
            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                return@withContext null
            }
            connection.inputStream.bufferedReader().use { it.readText() }
        } catch (e: Exception) {
            null
        } finally {
            connection?.disconnect()
        }
    }

    private companion object {
        const val MAX_BACKTRACK_DAYS = 5
        const val TIMEOUT_MS = 8_000
        const val RATE_SCALE = 10
        const val URL_TEMPLATE =
            "https://www.raiffeisenbank.ba/bs.exchangerates.%s.ACCOUNT.AUD-CAD-CNY-CZK-DKK-HUF-JPY-NOK-SEK-CHF-GBP-USD-RSD-TRY-EUR.BAM.SOURCE.json"
        val URL_DATE_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMdd")
    }
}
