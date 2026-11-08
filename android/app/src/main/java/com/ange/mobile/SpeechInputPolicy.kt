package com.ange.mobile

/** Routing is independent of the answer model. Network use needs session consent. */
internal object SpeechInputPolicy {
    enum class Route { LOCAL, ANDROID_SERVICE, UNAVAILABLE }

    fun languageTag(language: String): String = when (language) {
        "fr" -> "fr-FR"
        "en" -> "en-US"
        "mg" -> "mg-MG"
        else -> language.replace('_', '-')
    }

    fun initialRoute(localAvailable: Boolean, networkAllowed: Boolean): Route = when {
        localAvailable -> Route.LOCAL
        networkAllowed -> Route.ANDROID_SERVICE
        else -> Route.UNAVAILABLE
    }

    // Android error 12: unsupported language; 13: supported but not downloaded.
    // Never retry the default service itself, or unrelated errors, in a loop.
    fun retryWithService(error: Int, wasLocal: Boolean, networkAllowed: Boolean): Boolean =
        wasLocal && networkAllowed && error in setOf(12, 13)

    fun errorMessage(error: Int, language: String, local: Boolean): String {
        val tag = languageTag(language)
        val service = if (local) "Le moteur local" else "Le service vocal Android"
        val help = if (local) "Ouvrez les réglages de reconnaissance pour installer la langue si elle est proposée, ou autorisez le réseau dans Options puis réessayez."
                   else "Vérifiez la langue et le service dans les réglages de reconnaissance du téléphone."
        return when (error) {
            12 -> "$service ne prend pas en charge $tag (code 12). $help Vous pouvez écrire votre demande."
            13 -> "Le modèle vocal $tag n’est pas disponible ou téléchargé (code 13). $help Vous pouvez écrire votre demande."
            9 -> "Microphone refusé. Vérifiez les autorisations."
            6, 7 -> "Je n’ai pas compris. Réessayez ou écrivez votre message."
            1, 2 -> "Connexion du service vocal indisponible (code $error). Vérifiez le réseau ou écrivez votre demande."
            8 -> "Service vocal occupé. Attendez un instant puis appuyez sur Parler."
            else -> "Reconnaissance vocale indisponible (code $error). Réessayez ou écrivez votre demande."
        }
    }
}
