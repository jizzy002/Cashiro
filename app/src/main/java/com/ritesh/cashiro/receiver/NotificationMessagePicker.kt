package com.ritesh.cashiro.receiver

/**
 * Chooses the richest available body from a notification's extras.
 *
 * Messengers and banking apps put the short preview in `android.text` and the
 * full body in `android.bigText`; picking the longer one keeps details such as
 * the `Opis:` line that the parser needs.
 */
internal object NotificationMessagePicker {

    fun pick(
        text: String?,
        bigText: String?,
        title: String?,
        subText: String?,
        infoText: String?
    ): String? {
        val bodies = listOfNotNull(text, bigText)
            .map { it.trim() }
            .filter { it.isNotEmpty() }
        bodies.maxByOrNull { it.length }?.let { return it }

        return listOfNotNull(title, subText, infoText)
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .maxByOrNull { it.length }
    }
}
