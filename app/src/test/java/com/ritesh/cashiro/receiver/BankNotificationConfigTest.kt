package com.ritesh.cashiro.receiver

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BankNotificationConfigTest {

    @Test
    fun `maps the RMB package to the Raiffeisen alias`() {
        assertEquals(true, BankNotificationConfig.isAllowed("ba.raiffeisenbank.mobile.prod"))
        assertEquals("RaiffeisenBiH", BankNotificationConfig.senderAlias("ba.raiffeisenbank.mobile.prod"))
    }

    @Test
    fun `keeps existing notification banks`() {
        assertEquals("Enpara", BankNotificationConfig.senderAlias("finansbank.enpara"))
        assertEquals("FaysalBank", BankNotificationConfig.senderAlias("com.avanza.ambitwizfbl"))
    }

    @Test
    fun `rejects unknown packages`() {
        assertEquals(false, BankNotificationConfig.isAllowed("com.example.unknown"))
        assertNull(BankNotificationConfig.senderAlias("com.example.unknown"))
    }

    @Test
    fun `prefers the longer body so Opis survives collapsed notifications`() {
        val selected = NotificationMessagePicker.pick(
            text = "Odlazna transakcija",
            bigText = "Odlazna transakcija\nSa Vašeg računa 1613000088114273 izvršena je isplata u iznosu od 310.00 BAM.\nOpis:KONZUM",
            title = "RMB",
            subText = null,
            infoText = null
        )

        assertEquals(true, selected!!.contains("Opis:KONZUM"))
    }

    @Test
    fun `falls back to the title when text extras are blank`() {
        val selected = NotificationMessagePicker.pick(
            text = "   ",
            bigText = "",
            title = "Dolazna transakcija",
            subText = null,
            infoText = null
        )

        assertEquals("Dolazna transakcija", selected)
    }

    @Test
    fun `returns null when every extra is blank`() {
        assertNull(
            NotificationMessagePicker.pick(
                text = " ",
                bigText = null,
                title = "",
                subText = null,
                infoText = "\n"
            )
        )
    }
}
