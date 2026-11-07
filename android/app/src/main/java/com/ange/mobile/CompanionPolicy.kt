package com.ange.mobile

import java.net.URI
import java.text.DateFormat
import java.util.Date
import java.util.Locale

object CompanionPolicy {
    fun validateServer(value: String, debug: Boolean): String {
        val base = URI(value.trim().trimEnd('/'))
        require(base.host != null && base.userInfo == null && base.rawQuery == null && base.rawFragment == null && (base.path.isNullOrEmpty() || base.path == "/")) { "Adresse du PC invalide, utilisez https://nom-du-pc:8765." }
        require(base.scheme == "https" || (debug && base.scheme == "http" && base.host == "10.0.2.2")) { "HTTPS requis. HTTP est limité à l’émulateur en version debug." }
        return base.toString().trimEnd('/')
    }
    fun frame(mood: String, tick: Int, beat: Int, animated: Boolean, greeting: Boolean): Int {
        if (!animated) return 0
        if (greeting) return 6
        return when {
            mood == "SPEAKING" -> intArrayOf(0,2,3,2,4,2,0,2)[Math.floorMod(tick / 2 + beat, 8)]
            mood == "LISTENING" -> 5
            mood == "THINKING" -> 7
            tick % 100 in 87..89 -> 1
            tick % 260 in 205..218 -> 7
            else -> 0
        }
    }
    fun offline(text: String, language: String, now: Date = Date()): String {
        val lower = text.lowercase(Locale.ROOT)
        val time = DateFormat.getTimeInstance(DateFormat.SHORT, Locale.forLanguageTag(language)).format(now)
        val date = DateFormat.getDateInstance(DateFormat.LONG, Locale.forLanguageTag(language)).format(now)
        if (language == "en") return when {
            "time" in lower -> "It is $time."
            "date" in lower || "day" in lower -> "Today is $date."
            "hello" in lower || "hi" == lower -> "Hello! I'm ANGE. In limited mobile mode I can tell you the time and date, and read text aloud."
            else -> "Limited mobile mode is active, without a generative AI. Choose Local PC or Cloud for an intelligent conversation."
        }
        if (language == "mg") return when {
            "salama" in lower -> "Salama! Izaho no ANGE. Voafetra ny fomba tsy misy IA."
            else -> "Tsy misy IA generativa eto amin’ity fomba ity. Safidio ny Local PC na Cloud."
        }
        return when {
            "heure" in lower -> "Il est $time."
            "date" in lower || "quel jour" in lower -> "Nous sommes le $date."
            "bonjour" in lower || "salut" in lower -> "Bonjour ! Je suis ANGE. En mode mobile limité, je peux donner l’heure, la date et lire les réponses."
            else -> "Le mode mobile limité est actif, sans IA générative. Choisissez Local PC ou Cloud pour une conversation intelligente."
        }
    }
}
