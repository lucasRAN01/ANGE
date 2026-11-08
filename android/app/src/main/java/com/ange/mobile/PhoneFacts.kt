package com.ange.mobile

import java.text.DateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

internal enum class PhoneFact { TIME, DATE, DATE_TIME, BATTERY }

internal data class PhoneSnapshot(
    val now: Date,
    val timeZone: TimeZone,
    val batteryPercent: Int? = null,
    val charging: Boolean? = null
)

/** Only explicit requests for current device facts bypass the language model. */
internal object PhoneFacts {
    fun parse(input: String): PhoneFact? {
        var text = JarvisVoice.withoutAccents(input)
            .replace(Regex("[^\\p{L}\\p{N} ]"), " ")
            .replace(Regex("\\s+"), " ").trim()
            .replace("aujourd hui", "aujourdhui")
        text = text.replace(Regex("^(?:(?:hey|ok) )?ange +"), "")
            .replace(Regex("^(?:bonjour|salut|hello) +(?:ange +)?"), "")
            .replace(Regex("^(?:(?:est ce que )?(?:peux tu|pourrais tu|tu peux|pouvez vous|pourriez vous) (?:me )?(?:dire|donner|indiquer|rappeler) |(?:dis|donne|indique|rappelle) (?:moi )?|please (?:tell me )?)"), "")
        // Courtesy and local qualifiers may be in either order. No foreign city/date arithmetic.
        repeat(3) {
            text = text.replace(Regex(" +(?:s il te plait|s il vous plait|stp|svp|please|maintenant|actuellement|ici|sur mon telephone|sur ce telephone|du telephone|locale|local|now)$"), "")
        }
        return when (text) {
            "quelle heure est il", "quelle heure il est", "il est quelle heure", "quelle heure",
            "quelle est l heure", "l heure", "heure", "tu as l heure", "as tu l heure",
            "what time is it", "what is the time", "what s the time", "what is the current time",
            "tell me the time", "the time", "time", "amin ny firy izao" -> PhoneFact.TIME
            "quelle est la date", "quelle est la date d aujourdhui", "quelle est la date aujourdhui",
            "quelle date sommes nous", "quelle date est on", "quelle date", "la date", "date",
            "quel jour sommes nous", "quel jour est on", "on est quel jour", "nous sommes quel jour",
            "on est le combien", "nous sommes le combien", "quel jour", "quel jour est ce",
            "what is the date", "what s the date", "what is today s date", "today s date",
            "what day is it", "what day is it today", "inona ny daty androany" -> PhoneFact.DATE
            "quelle heure et quelle date", "l heure et la date", "heure et date", "date et heure",
            "donne l heure et la date", "time and date", "date and time" -> PhoneFact.DATE_TIME
            "combien de batterie me reste t il", "combien de batterie me reste", "combien de batterie reste t il",
            "combien de batterie", "combien il me reste de batterie", "quel est le niveau de batterie",
            "quel est mon niveau de batterie", "niveau de batterie", "la batterie", "batterie",
            "pourcentage de batterie", "quel est le pourcentage de batterie", "battery", "battery level",
            "how much battery is left", "what is my battery level", "what is the battery level" -> PhoneFact.BATTERY
            else -> null
        }
    }

    fun reply(fact: PhoneFact, snapshot: PhoneSnapshot, language: String): String {
        val locale = Locale.forLanguageTag(language)
        val time = DateFormat.getTimeInstance(DateFormat.SHORT, locale).apply { timeZone = snapshot.timeZone }.format(snapshot.now)
        val date = DateFormat.getDateInstance(DateFormat.FULL, locale).apply { timeZone = snapshot.timeZone }.format(snapshot.now)
        val timeReply = when (language) { "en" -> "It is $time."; "mg" -> "Amin’ny $time izao."; else -> "Il est $time." }
        val dateReply = when (language) { "en" -> "Today is $date."; "mg" -> "Androany dia $date."; else -> "Nous sommes le $date." }
        return when (fact) {
            PhoneFact.TIME -> timeReply
            PhoneFact.DATE -> dateReply
            PhoneFact.DATE_TIME -> "$dateReply $timeReply"
            PhoneFact.BATTERY -> {
                val level = snapshot.batteryPercent?.takeIf { it in 0..100 }
                    ?: return if (language == "en") "Android could not provide the battery level." else "Android ne permet pas de lire le niveau de batterie actuellement."
                if (language == "en") "The phone battery is at $level%." + when (snapshot.charging) { true -> " It is charging."; false -> " It is not charging."; null -> "" }
                else "La batterie du téléphone est à $level %." + when (snapshot.charging) { true -> " Elle est en charge."; false -> " Elle n’est pas en charge."; null -> "" }
            }
        }
    }
}
