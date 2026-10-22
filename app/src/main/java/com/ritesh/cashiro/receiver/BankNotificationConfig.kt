package com.ritesh.cashiro.receiver

object BankNotificationConfig {
    private val packageToAlias = mapOf(
        "com.avanza.ambitwizfbl" to "FaysalBank",
        "finansbank.enpara" to "Enpara",
        "com.enparabank.retail" to "Enpara",
        // RMB Raiffeisen Mobile Banking (Bosnia and Herzegovina)
        "ba.raiffeisenbank.mobile.prod" to "RaiffeisenBiH"
    )

    private val allowedPackages: Set<String> = packageToAlias.keys

    fun isAllowed(packageName: String): Boolean = allowedPackages.contains(packageName)

    fun senderAlias(packageName: String): String? = packageToAlias[packageName]

    fun extractMessage(notification: android.app.Notification): String? {
        val extras = notification.extras ?: return null
        return NotificationMessagePicker.pick(
            text = extras.getCharSequence("android.text")?.toString(),
            bigText = extras.getCharSequence("android.bigText")?.toString(),
            title = extras.getCharSequence("android.title")?.toString(),
            subText = extras.getCharSequence("android.subText")?.toString(),
            infoText = extras.getCharSequence("android.infoText")?.toString()
        )
    }
}
