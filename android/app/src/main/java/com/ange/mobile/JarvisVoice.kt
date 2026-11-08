/* Derived from sosoj92/jarvis-assistant-vocal core/util.py and core/confidentialite.py.
 * Copyright (c) 2026 sosoj92. MIT license: assets/licenses/jarvis-MIT.txt.
 * ANGE adaptation: Kotlin/Android, explicit privacy switch; upstream 15f3f87e0bfd3fe68ec38deafbfeab22636c3993.
 */
package com.ange.mobile

import java.text.Normalizer
import java.util.Locale

object JarvisVoice {
    fun withoutAccents(text: String): String = Normalizer.normalize(text.lowercase(Locale.ROOT),Normalizer.Form.NFKD).replace(Regex("\\p{M}+"),"")
    private val starts=listOf(
        "^(?:attends?|patiente(?: un peu)?|un instant|une seconde|deux secondes)\\s*[,.:;!…-]*\\s*",
        "^(?:laisse(?:z)?[- ]moi\\s+(?:regarder|v[ée]rifier|chercher|r[ée]fl[ée]chir)(?:\\s+(?:ça|cela))?)\\s*[,.:;!…-]*\\s*",
        "^(?:je\\s+(?:regarde|v[ée]rifie|cherche)(?:\\s+(?:ça|cela|ta demande|pour toi))?)\\s*[.!…,:;-]+\\s*")
        .map { Regex(it,RegexOption.IGNORE_CASE) }
    private val privatePatterns=listOf(
        // Explicit Unicode classes work on both JVM and Android ICU; ICU rejects (?U).
        Regex("\\b[\\p{L}\\p{N}_.+-]+@[\\p{L}\\p{N}_-]+\\.[\\p{L}\\p{N}_.-]+\\b") to "[adresse mail]",
        Regex("\\b(?:sk|pk|ghp|gho|ghs|xox[baprs]|AKIA|AIza)[-_A-Za-z0-9]{10,}\\b") to "[cle]",
        Regex("\\b(?:\\+?\\p{Nd}[ .\\-]?){9,}\\p{Nd}\\b") to "[numero]",
        Regex("\\b[A-Za-z0-9_\\-]{32,}\\b") to "[jeton]")
    fun clean(text: String): String {
        var value=text.trim()
        repeat(3) { for(pattern in starts) value=pattern.replaceFirst(value,"").trim() }
        return if(value.isNotEmpty() && value[0].isLowerCase()) value.substring(0,1).uppercase(Locale.ROOT)+value.substring(1) else value
    }
    fun redact(text: String): String {
        var result=text
        for((pattern,replacement) in privatePatterns) result=pattern.replace(result,replacement)
        return result
    }
    fun spoken(text: String,privateVoice: Boolean): String = if(privateVoice) redact(clean(text)) else clean(text)
}
