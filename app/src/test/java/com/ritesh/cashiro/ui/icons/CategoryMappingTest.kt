package com.ritesh.cashiro.ui.icons

import com.ritesh.cashiro.presentation.common.icons.CategoryMapping
import org.junit.Test
import kotlin.test.assertEquals

class CategoryMappingTest {

    @Test
    fun testCategoryMapping() {
        // Test some key merchants from each category
        assertEquals("Food & Drinks", getCategory("Piri Piri Flaming Grill"))
        assertEquals("Food & Drinks", getCategory("Naixue"))
        assertEquals("Food & Drinks", getCategory("BN-Icon Siam"))
        assertEquals("Food & Drinks", getCategory("Amici"))

        assertEquals("Food & Drinks", getCategory("PF Changs"))
        assertEquals("Food & Drinks", getCategory("Bombay Bungalow"))
        assertEquals("Food & Drinks", getCategory("Heytea"))
        assertEquals("Food & Drinks", getCategory("Google Nomadtable"))
        assertEquals("Food & Drinks", getCategory("GHL*JHAROKA BY INDUS BANGKOK  11 TH"))
        assertEquals("Food & Drinks", getCategory("GHLJHAROKA BY INDUS BANGKOK  11 TH"))
        assertEquals("Food & Drinks", getCategory("Ksher  *AKARASKYROOFTOBangkok TH"))


        assertEquals("Groceries", getCategory("7-11"))

        assertEquals("Transport", getCategory("Airports of Thailand"))
        assertEquals("Transport", getCategory("Expressway"))
        assertEquals("Transport", getCategory("Grab A-123"))
        assertEquals("Transport", getCategory("SATS T1"))
        assertEquals("Transport", getCategory("Pyxbolt Services"))

        assertEquals("Shopping", getCategory("Uniqlo TRX"))
        assertEquals("Shopping", getCategory("Lyn"))
        assertEquals("Shopping", getCategory("Apple Central World"))
        assertEquals("Shopping", getCategory("OpenAI ChatGPT"))
        assertEquals("Shopping", getCategory("Sukhumvit City Mall"))
        assertEquals("Shopping", getCategory("The Emsphere"))
        assertEquals("Shopping", getCategory("Central World"))
        assertEquals("Shopping", getCategory("The Empire Tower"))

        assertEquals("Entertainment", getCategory("Major Cineplex"))
        assertEquals("Entertainment", getCategory("Ticketmelon"))
        assertEquals("Entertainment", getCategory("2C2P Major Cineplex"))

        assertEquals("Travel", getCategory("Four Points by Sheraton"))
        assertEquals("Travel", getCategory("Crowne Plaza KLCC"))
        assertEquals("Travel", getCategory("Dusit Thani Bangkok"))
        assertEquals("Travel", getCategory("Marina Bay Sands"))
        assertEquals("Travel", getCategory("Hilton Garden Inn"))
        assertEquals("Travel", getCategory("WESTIN KL-FRONT OFFICE KUALA LUMPUR MY MY"))
        assertEquals("Travel", getCategory("WWW.MAGNOLIASSERVICEDRBANGKOK TH"))
        assertEquals("Travel", getCategory("FOUR POINTS BY SHERATOBANGKOK 11 TH"))


        assertEquals("Medical", getCategory("Life Pharm"))
        assertEquals("Medical", getCategory("Bumrungrad"))
        assertEquals("Medical", getCategory("Medex"))
        assertEquals("Medical", getCategory("BOOTS_4287 C.WORLD 3 FBANGKOK TH"))

        assertEquals("Personal", getCategory("Sultans of Shave"))
        assertEquals("Personal", getCategory("Mandarin Oriental Spa"))
        assertEquals("Personal", getCategory("Truefitt and Hill"))
        assertEquals("Personal", getCategory("Phetsathorn Co.,Ltd."))

        assertEquals("Tax", getCategory("Abu Dhabi Judicial Dep"))
        assertEquals("Tax", getCategory("Sharjah Finance Depart"))

        assertEquals("Bill", getCategory("Tamdeed Projects"))
        assertEquals("Bill", getCategory("WWW.PAYSOLUT*WWW.PAYSOBANGKOK  TH"))

        assertEquals("Business", getCategory("My Fatoorah"))
        assertEquals("Business", getCategory("Transfer: 002 → 001"))
        assertEquals("Business", getCategory("Transfer from 001 to 002"))
        assertEquals("Business", getCategory("Transfer to 0001"))

        // Test some should remain as Miscellaneous (too generic)
        assertEquals("Miscellaneous", getCategory("Twin Made"))
        findDuplicateKeywords()
    }

    @Test
    fun testBosnianMerchants() {
        // Groceries
        assertEquals("Groceries", getCategory("KONZUM P1002 LIVNO"))
        assertEquals("Groceries", getCategory("SPAR BiH"))
        assertEquals("Groceries", getCategory("LIDL HRVATSKA 1234"))
        assertEquals("Groceries", getCategory("STUDENAC 4"))
        assertEquals("Groceries", getCategory("VOLI Rijeka"))
        assertEquals("Groceries", getCategory("TOMMY 7"))
        assertEquals("Groceries", getCategory("INTERSPAR"))
        assertEquals("Groceries", getCategory("FIS 6"))
        assertEquals("Groceries", getCategory("BINGO"))
        assertEquals("Groceries", getCategory("IVANOVIC DOO MARKET"))
        assertEquals("Groceries", getCategory("MARKET A"))
        assertEquals("Groceries", getCategory("ZIF DOO PJ MARKET 3"))
        assertEquals("Groceries", getCategory("POS PJ TROPIC 12"))
        assertEquals("Groceries", getCategory("ZABRISCE B&M DOO PJ 5"))

        // Food
        assertEquals("Food & Drinks", getCategory("MLINAR 12"))
        assertEquals("Food & Drinks", getCategory("Restoran El Rey"))
        assertEquals("Food & Drinks", getCategory("PIVNICA STARI JADRAN"))
        assertEquals("Food & Drinks", getCategory("Animal Caffe"))
        assertEquals("Food & Drinks", getCategory("Yellow Submarine"))
        assertEquals("Food & Drinks", getCategory("Khala Rest. Bar"))
        assertEquals("Food & Drinks", getCategory("MONACO FAST FOOD"))
        assertEquals("Food & Drinks", getCategory("MCDONALDS KIOSK 1122"))

        // Transport
        assertEquals("Transport", getCategory("PAVIC DOO BENZ CRPKA"))
        assertEquals("Transport", getCategory("PAPIC DOO PJ 5"))
        assertEquals("Transport", getCategory("TIOIL D.O.O. STARA BIL"))
        assertEquals("Transport", getCategory("NESTRO PETROL"))
        assertEquals("Transport", getCategory("Coral Energy"))
        assertEquals("Transport", getCategory("HIFA OIL"))
        assertEquals("Transport", getCategory("INA SPLITSMOKOVIK"))
        assertEquals("Transport", getCategory("BP LIVNO"))
        assertEquals("Transport", getCategory("BP NESKOVIC"))
        assertEquals("Transport", getCategory("ALMY DOO PJ BP I ZAVID"))
        assertEquals("Transport", getCategory("JPK BS PETROVAC BULJA"))
        assertEquals("Transport", getCategory("BS BUGOJNO"))
        assertEquals("Transport", getCategory("ZOVKO AUTO"))
        assertEquals("Transport", getCategory("JP AUTOCESTE FBIH BNM"))
        assertEquals("Transport", getCategory("NAPLATNA STANICA"))

        // Bills
        assertEquals("Bill", getCategory("HRVATSKI TELEKOM"))
        assertEquals("Bill", getCategory("STARLINK Internet Serv"))
        assertEquals("Bill", getCategory("CLOUDFLARE"))
        assertEquals("Bill", getCategory("NEXTDNS"))
        assertEquals("Bill", getCategory("GOOGLE Google One"))
        assertEquals("Bill", getCategory("Trajni nalog - 123456"))

        // Shopping
        assertEquals("Shopping", getCategory("TISAK P4210"))
        assertEquals("Shopping", getCategory("Box Now d.o.o."))
        assertEquals("Shopping", getCategory("TEMU"))
        assertEquals("Shopping", getCategory("aliexpress"))
        assertEquals("Shopping", getCategory("PAYPAL VTECH 3371"))
        assertEquals("Shopping", getCategory("Kotorska Suvenirnica"))

        // Personal
        assertEquals("Personal", getCategory("MUELLER 1293 MA"))
        assertEquals("Personal", getCategory("DM 2145"))

        // Business
        assertEquals("Business", getCategory("Wise"))
        assertEquals("Business", getCategory("Aircash AC"))
        assertEquals("Business", getCategory("Euro Kontakt"))

        // Hidden charges
        assertEquals("Hidden Charges", getCategory("Naknada za vodenje paketa"))
        assertEquals("Hidden Charges", getCategory("Provizija za odrzavanje"))

        // Unknown merchants stay unclassified
        assertEquals("Miscellaneous", getCategory("IU IT TEAM"))
        assertEquals("Miscellaneous", getCategory("Formula"))
    }

    @Test
    fun testCashWithdrawalRule() {
        assertEquals("Cash Withdrawal", getCategory("ATM FILIJALA LIVNO"))
        assertEquals("Cash Withdrawal", getCategory("ATM LIVNO KONZUM"))
        assertEquals("Cash Withdrawal", getCategory("BANKOMAT LIVNO"))
        assertEquals("Cash Withdrawal", getCategory("Cash ATM RBBH"))
        assertEquals("Cash Withdrawal", getCategory("Cash withdrawal"))
    }

    @Test
    fun testDiacriticFolding() {
        assertEquals("Groceries", getCategory("Ivanović Market"))
        assertEquals("Groceries", getCategory("Zabrišće B&M"))
        assertEquals("Transport", getCategory("Pavić Doo Benzinska"))
    }

    @Test
    fun testPaypalNextdnsNotShopping() {
        assertEquals("Bill", getCategory("paypal nextdns"))
    }

    @Test
    fun testIncomeKeywords() {
        assertEquals("Salary", getCategory("UPLATA MANPOWER DOO SARAJEVO"))
        assertEquals("Salary", getCategory("PLATA M BH DOO SARAJEVO"))
        assertEquals("Salary", getCategory("ZARADA"))
        assertEquals("Salary", getCategory("Honorar"))
        assertEquals("Salary", getCategory("Mirovina"))
        assertEquals("Salary", getCategory("Penzija"))
    }

    @Test
    fun testSpanishPlataMerchantsAreNotSalary() {
        assertEquals("Miscellaneous", getCategory("LA PLATA"))
        assertEquals("Miscellaneous", getCategory("PLATA JEWELRY"))
    }

    @Test
    fun testTommyHilfigerIsShopping() {
        assertEquals("Shopping", getCategory("TOMMY HILFIGER PARIS"))
        assertEquals("Groceries", getCategory("TOMMY DOO 7"))
    }

    @Test
    fun testPlacaKeywordAfterFolding() {
        assertEquals("Salary", getCategory("Plaća Novembar"))
    }
}

// Helper function to test categorization
private fun getCategory(merchantName: String): String {
    return CategoryMapping.getCategory(merchantName)
}

private fun findDuplicateKeywords(): Set<String> {
    return CategoryMapping.findDuplicateKeywords()
}