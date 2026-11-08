package com.ange.mobile

import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

internal object OnlineConversation {
    const val MODEL="gemini-3.1-flash-lite"
    const val ENDPOINT="https://generativelanguage.googleapis.com/v1beta/models/$MODEL:generateContent"
    fun payload(message: String, history: List<Pair<String,String>>, language: String): JSONObject {
        val contents=JSONArray()
        fun item(role: String,text: String)=JSONObject().put("role",role).put("parts",JSONArray().put(JSONObject().put("text",text)))
        history.filter { it.first in setOf("user","assistant") && it.second.isNotBlank() }.takeLast(12).forEach { (role,text) -> contents.put(item(if(role=="assistant") "model" else "user",text.take(1500))) }
        contents.put(item("user",message.take(4000)))
        val instruction="Tu es ANGE, une compagne IA chaleureuse et honnête. Discute naturellement en $language. Réponds à la demande réelle, adapte la longueur et pose une question utile si nécessaire. Tu ne vois pas le téléphone, ses contacts ou son écran. Les commandes Android sont traitées séparément par l’application. Ne prétends jamais avoir effectué une action. Tu es une IA, pas une personne réelle. Aucun outil n’est fourni."
        return JSONObject().put("contents",contents).put("systemInstruction",JSONObject().put("parts",JSONArray().put(JSONObject().put("text",instruction))))
            .put("generationConfig",JSONObject().put("maxOutputTokens",1024).put("temperature",0.7))
    }
    fun reply(data: JSONObject): String {
        val parts=data.optJSONArray("candidates")?.optJSONObject(0)?.optJSONObject("content")?.optJSONArray("parts")
        val answer=buildString { if(parts!=null) for(i in 0 until parts.length()) {
            val part=parts.optJSONObject(i) ?: continue
            if(!part.optBoolean("thought")) append(part.optString("text"))
        } }.trim().take(12000)
        require(answer.isNotBlank()) { "Le service n’a fourni aucune réponse textuelle. Reformulez la demande." }
        return answer
    }
    fun generate(key: String, payload: JSONObject, track: (HttpURLConnection?) -> Unit): String {
        require(key.trim().length in 20..200 && !key.contains('\n') && !key.contains('\r')) { "Clé Google AI Studio manquante ou invalide." }
        val http=URL(ENDPOINT).openConnection() as HttpURLConnection
        track(http)
        try {
            http.instanceFollowRedirects=false; http.connectTimeout=10000; http.readTimeout=65000
            http.requestMethod="POST"; http.doOutput=true
            http.setRequestProperty("Content-Type","application/json; charset=utf-8"); http.setRequestProperty("x-goog-api-key",key.trim())
            http.outputStream.use { it.write(payload.toString().toByteArray(Charsets.UTF_8)) }
            val code=http.responseCode
            require(code in 200..299) { when(code) {
                429 -> "Quota Google atteint. Réessayez plus tard ou choisissez le mode local. Aucun autre modèle n’est appelé."
                400,401,403 -> "Google a refusé la demande. Vérifiez la clé, la région et l’accès gratuit du projet."
                404 -> "Ce modèle n’est plus disponible pour votre projet. Aucun remplacement automatique."
                else -> "Service de conversation indisponible (HTTP $code)."
            } }
            val raw=http.inputStream.bufferedReader().use { reader ->
                val output=StringBuilder(); val buffer=CharArray(4096)
                while(true) { val n=reader.read(buffer); if(n<0) break; require(output.length+n<=262144) { "Réponse trop volumineuse." }; output.append(buffer,0,n) }
                output.toString()
            }
            return reply(JSONObject(raw))
        } finally { http.disconnect(); track(null) }
    }
}
