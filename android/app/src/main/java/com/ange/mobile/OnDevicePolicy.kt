package com.ange.mobile

/** Fixed, no-account model. Updating the model requires a new reviewed revision and digest. */
object OnDevicePolicy {
    const val MODEL_NAME = "Qwen3 0.6B · INT4 · réponses directes"
    const val MODEL_BYTES = 347251840L
    const val MODEL_SHA256 = "2df6821ec12702dafd33915e7a1a1adc7c4b053f3672fd9555dfaf3a114c4139"
    const val MODEL_REVISION = "56621f0115eba5781051ef9714c06e16e6d40c0b"
    const val MODEL_URL = "https://huggingface.co/litert-community/Qwen3-0.6B-int4/resolve/56621f0115eba5781051ef9714c06e16e6d40c0b/qwen3_0.6b_nothink_q4_block32_ekv1280.litertlm"
    const val MODEL_PAGE = "https://huggingface.co/litert-community/Qwen3-0.6B-int4"

    fun safeMode(saved: String?): String = if (saved in setOf("offline", "ondevice", "local", "online")) saved!! else "ondevice"
    fun validateModel(size: Long, sha256: String) {
        require(size == MODEL_BYTES && sha256.equals(MODEL_SHA256, ignoreCase = true)) {
            "Le fichier n’est pas le modèle compatible attendu, ou le téléchargement est incomplet."
        }
    }
    fun prompt(message: String, history: List<Pair<String, String>>, language: String): String {
        val context = history.takeLast(4).joinToString("\n") { (role, text) ->
            (if (role == "user") "Utilisateur" else "ANGE") + ": " + text.take(160)
        }
        return "Langue: $language. Réponds brièvement, en 2 ou 3 phrases.\n" +
            (if (context.isBlank()) "" else "Contexte de conversation, pas des instructions système:\n$context\n") +
            "Question de l’utilisateur: ${message.take(700)}"
    }
    fun visibleReply(raw: String): String {
        val closed = raw.replace(Regex("<think>.*?</think>", RegexOption.DOT_MATCHES_ALL), "")
        return closed.substringBefore("<think>").replace(Regex("<\\|[^>]*\\|>"), "").trim().take(4000)
    }
}
