package com.ange.mobile

import java.text.Normalizer
import java.util.Locale

data class LaunchableApp(val packageName: String, val label: String)

sealed class DeviceAction(val title: String, val detail: String, val confirmation: Boolean = false) {
    data class OpenApp(val app: LaunchableApp) : DeviceAction("Ouvrir ${app.label}", app.packageName)
    data class Dial(val number: String) : DeviceAction("Préparer un appel", "Numéro : $number. Vous appuyez ensuite sur Appeler dans Téléphone.", true)
    data class CallContact(val name: String) : DeviceAction("Chercher $name", "Recherche locale dans vos contacts.")
    data class CallNumber(val number: String, val label: String = number) : DeviceAction("Appeler $label", "Appel téléphonique vers $label\n$number\nL’appel sera lancé après votre confirmation.", true)
    data class Navigate(val direction: String) : DeviceAction("Naviguer : $direction", "La version standard peut ouvrir l’accueil Android sans accès à l’écran des autres applications.")
    data class Sms(val number: String, val body: String) : DeviceAction("Préparer un SMS", "À : $number\n\n$body\n\nANGE ouvre le brouillon ; vous seul appuyez sur Envoyer.", true)
    data class Map(val query: String) : DeviceAction("Afficher un lieu", query)
    data class Search(val query: String) : DeviceAction("Rechercher sur le Web", query)
    data class Website(val url: String) : DeviceAction("Ouvrir le navigateur", url)
    data class Alarm(val hour: Int, val minute: Int) : DeviceAction("Demander une alarme", "%02d:%02d · Vérifiez le résultat dans l’Horloge.".format(hour,minute), true)
    data class Timer(val seconds: Int) : DeviceAction("Démarrer un minuteur", "$seconds secondes · Vérifiez le résultat dans l’Horloge.", true)
    data class Calendar(val event: String) : DeviceAction("Préparer un événement", "$event\nVous choisissez la date et enregistrez dans l’Agenda.", true)
    data class Settings(val kind: String) : DeviceAction("Ouvrir les réglages", kind)
    data class Volume(val percent: Int) : DeviceAction("Régler le volume multimédia", "$percent % · Le volume des appels et des alarmes reste distinct.")
    data object Camera : DeviceAction("Ouvrir l’appareil photo", "Vous prenez vous-même la photo.")
}

sealed interface ActionParse {
    data object None : ActionParse
    data class Found(val action: DeviceAction) : ActionParse
    data class Problem(val message: String) : ActionParse
}

/** Only explicit user commands enter this parser. Model output is never executed. */
object DeviceActionPolicy {
    fun normalized(value: String): String = JarvisVoice.withoutAccents(value).trim().replace(Regex("\\s+"), " ")
    fun parse(input: String, apps: List<LaunchableApp>): ActionParse {
        val original = input.trim().replace(Regex("^(?:hey\\s+)?ange[, :]+",RegexOption.IGNORE_CASE), "")
            .replace(Regex("^(?:peux[- ]tu|pourrais[- ]tu|pouvez[- ]vous|pourriez[- ]vous|tu peux|est[- ]ce que tu peux)\\s+",RegexOption.IGNORE_CASE), "")
        val text = normalized(original).trimEnd('.', '!', '?').trim()
        fun found(action: DeviceAction) = ActionParse.Found(action)
        fun problem(message: String) = ActionParse.Problem(message)
        val navigation = mapOf("retour" to "retour", "reviens en arriere" to "retour", "va a l accueil" to "accueil", "ecran d accueil" to "accueil", "applications recentes" to "récentes", "descends" to "descendre", "fais defiler vers le bas" to "descendre", "monte" to "monter", "fais defiler vers le haut" to "monter")
        val navigationText=text.replace('’',' ').replace('\'',' ').replace(Regex("\\s+")," ")
        navigation[navigationText]?.let { return found(DeviceAction.Navigate(it)) }
        Regex("^(?:prepare|prépare|envoie|envoyer|ecris|écris)\\s+(?:un\\s+)?(?:sms|message)\\s+(?:a|à|au)\\s+([+\\d][\\d ()+.-]{2,24}?)\\s*:\\s*(.+)$",setOf(RegexOption.IGNORE_CASE,RegexOption.DOT_MATCHES_ALL)).matchEntire(original)?.let {
            val number = number(it.groupValues[1]) ?: return problem("Indiquez un numéro de téléphone valide, sans nom de contact.")
            return found(DeviceAction.Sms(number,it.groupValues[2].take(1000)))
        }
        if (Regex("^(?:prepare|envoie|envoyer|ecris) (?:un )?(?:sms|message)\\b").containsMatchIn(text))
            return problem("Exemple : Prépare un SMS au +261340000000 : Bonjour. Aucun SMS n’est envoyé automatiquement.")
        Regex("^(?:appelle|appeler|compose|call) (.+)$").matchEntire(text)?.let {
            val target = it.groupValues[1]
            number(target)?.let { n -> return found(if (text.startsWith("compose ")) DeviceAction.Dial(n) else DeviceAction.CallNumber(n)) }
            if (target.length in 1..100 && Regex("^[\\p{L}][\\p{L} .'-]*$").matches(target)) return found(DeviceAction.CallContact(target))
            return problem("Indiquez un nom de contact ou un numéro valide. Aucun numéro n’est deviné.")
        }
        Regex("^(?:mets? |regle |regler )?(?:le )?volume(?: multimedia)?(?: a)? (.+?)(?:\\s*%| pour cent)?$").matchEntire(text)?.let {
            val percent=spokenNumber(it.groupValues[1])
            return if(percent!=null && percent in 0..100) found(DeviceAction.Volume(percent)) else problem("Le volume doit être compris entre 0 et 100 %.")
        }
        Regex("^(?:(?:mets?|cree|regle) (?:une? )?)?(?:alarme|reveil)(?: a| pour)? (.+)$").matchEntire(text)?.let {
            val clock=it.groupValues[1].replace(" heures", "h").replace(" heure", "h")
            val fields=clock.split(Regex("h|:"),limit=2)
            val hour=spokenNumber(fields[0]); val minute=if(fields.size==1 || fields[1].isBlank()) 0 else spokenNumber(fields[1])
            return if(hour!=null && minute!=null && hour in 0..23 && minute in 0..59) found(DeviceAction.Alarm(hour,minute)) else problem("Utilisez une heure entre 00:00 et 23:59, par exemple : Alarme à 7h30.")
        }
        Regex("^(?:(?:mets?|lance|demarre) (?:un )?)?minuteur(?: de| pour)? ([a-z0-9 -]+?)\\s*(secondes?|minutes?|heures?)$").matchEntire(text)?.let {
            val multiplier=when { it.groupValues[2].startsWith("heure") -> 3600; it.groupValues[2].startsWith("minute") -> 60; else -> 1 }
            val amount=spokenNumber(it.groupValues[1]) ?: return problem("Indiquez une durée précise, par exemple : Minuteur de 5 minutes.")
            val seconds=amount.toLong()*multiplier
            return if(seconds in 1..86400) found(DeviceAction.Timer(seconds.toInt())) else problem("Choisissez une durée entre une seconde et 24 heures.")
        }
        Regex("^(?:ajoute|ajouter|cree|creer|prepare|prépare) (?:un )?(?:evenement|événement|rendez-vous)\\s*:\\s*(.+)$",RegexOption.IGNORE_CASE).matchEntire(original)?.let {
            return found(DeviceAction.Calendar(it.groupValues[1].take(200)))
        }
        Regex("^(?:itineraire|va|navigue)(?: vers| a)? (.+)$").matchEntire(text)?.let { return found(DeviceAction.Map(it.groupValues[1].take(250))) }
        Regex("^(?:cherche|recherche|rechercher)(?: sur (?:le )?web)? (.+)$").matchEntire(text)?.let { return found(DeviceAction.Search(it.groupValues[1].take(500))) }
        Regex("^(?:ouvre|ouvrir|lance|lancer|open)(?: (?:l’application|l'application|application|appli))? (.+)$").matchEntire(text)?.let {
            val name=it.groupValues[1]
            val kind=when(name) { "reglages", "parametres", "settings" -> "général"; "wifi", "wi-fi", "reglages wifi" -> "Wi-Fi"; "bluetooth", "reglages bluetooth" -> "Bluetooth"; "son", "reglages son" -> "son"; else -> null }
            if(kind!=null) return found(DeviceAction.Settings(kind))
            if(name in setOf("camera","appareil photo","la camera","l’appareil photo","l'appareil photo")) return found(DeviceAction.Camera)
            if(name.startsWith("https://") || name.startsWith("http://")) {
                val url=original.substringAfter(' ').trim()
                val uri=runCatching { java.net.URI(url) }.getOrNull()
                return if(uri?.scheme in setOf("https","http") && !uri?.host.isNullOrBlank() && uri?.userInfo==null) found(DeviceAction.Website(url)) else problem("Adresse Web invalide.")
            }
            val matches=apps.filter { normalized(it.label)==name || it.packageName.lowercase(Locale.ROOT)==name }
            return when(matches.size) { 1 -> found(DeviceAction.OpenApp(matches.single())); 0 -> problem("Application « $name » introuvable. Choisissez son nom exact dans Actions → Applications."); else -> problem("Plusieurs applications portent ce nom. Choisissez-en une dans Actions → Applications.") }
        }
        if(text.startsWith("alarme ") || text.startsWith("reveil ")) return problem("Exemple : Alarme à 7h30, avec une heure de 00:00 à 23:59.")
        if(text.startsWith("minuteur ")) return problem("Exemple : Minuteur de 5 minutes.")
        return ActionParse.None
    }
    private fun spokenNumber(raw: String): Int? {
        val value=raw.trim().replace('-', ' ')
        if(value.all { it.isDigit() }) return value.toIntOrNull()
        val numbers=mapOf("zero" to 0,"un" to 1,"une" to 1,"deux" to 2,"trois" to 3,"quatre" to 4,"cinq" to 5,"six" to 6,
            "sept" to 7,"huit" to 8,"neuf" to 9,"dix" to 10,"onze" to 11,"douze" to 12,"treize" to 13,"quatorze" to 14,"quinze" to 15,
            "seize" to 16,"dix sept" to 17,"dix huit" to 18,"dix neuf" to 19,"vingt" to 20,"trente" to 30,"quarante" to 40,"cinquante" to 50,"soixante" to 60,"cent" to 100)
        numbers[value]?.let { return it }
        for(tens in listOf("vingt","trente","quarante","cinquante")) if(value.startsWith("$tens ")) {
            val ones=value.removePrefix("$tens ").removePrefix("et ")
            val n=numbers[ones]
            if(n!=null && n in 1..9) return numbers[tens]!!+n
        }
        return null
    }
    private fun number(value: String): String? {
        if (!Regex("\\+?[0-9 ()..-]+").matches(value)) return null
        val cleaned=value.replace(Regex("[ ()..-]"), "")
        return cleaned.takeIf { Regex("\\+?[0-9]{3,15}").matches(it) }
    }
}
