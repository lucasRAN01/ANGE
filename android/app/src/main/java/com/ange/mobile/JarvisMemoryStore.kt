/* Port of the structured memory/schema and remember/recall/forget operations in
 * core/memoire.py + tools/memoire.py, sosoj92/jarvis-assistant-vocal, revision 15f3f87.
 * Copyright (c) 2026 sosoj92, MIT; full license in assets/licenses/jarvis-MIT.txt.
 * ANGE adaptation: Android private files, AtomicFile, explicit-only edits, 500-entry bound.
 */
package com.ange.mobile

import android.content.Context
import android.util.AtomicFile
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

data class JarvisMemoryEntry(val category: String,val key: String,val text: String)

class JarvisMemoryStore(context: Context) {
    private val file=AtomicFile(File(context.filesDir,"jarvis-memory.json"))
    private val recovery=File(context.filesDir,"jarvis-memory-recovery.json")
    private val categories=listOf("preferences","people","projects","facts")
    private fun empty()=JSONObject().put("preferences",JSONObject()).put("people",JSONObject()).put("projects",JSONObject()).put("facts",JSONArray())
    private fun read(): JSONObject {
        if(!file.baseFile.exists() && !File(file.baseFile.path+".bak").exists()) return empty()
        require(file.baseFile.length()<=2500000) { "Fichier mémoire trop volumineux. Aucun remplacement effectué." }
        return try { JSONObject(file.readFully().toString(Charsets.UTF_8)) } catch(error: Exception) {
            throw IllegalStateException("Mémoire illisible conservée. Effacez-la explicitement dans Mémoire pour repartir.",error)
        }
    }
    private fun entries(data: JSONObject): List<JarvisMemoryEntry> = buildList {
        for(category in categories.dropLast(1)) {
            val group=data.optJSONObject(category) ?: continue
            for(key in group.keys()) (group.opt(key) as? String)?.let { add(JarvisMemoryEntry(category,key,it)) }
        }
        val facts=data.optJSONArray("facts") ?: JSONArray()
        for(i in 0 until facts.length()) (facts.opt(i) as? String)?.let { add(JarvisMemoryEntry("facts","",it)) }
    }
    private fun write(data: JSONObject) {
        require(entries(data).size<=500) { "Limite de 500 souvenirs atteinte." }
        val encoded=data.toString(2).toByteArray(Charsets.UTF_8)
        require(encoded.size<=2500000) { "La mémoire dépasse la limite de taille locale." }
        val stream=file.startWrite()
        try { stream.write(encoded); file.finishWrite(stream) }
        catch(error: Exception) { file.failWrite(stream); throw error }
    }
    @Synchronized fun recall(query: String=""): List<JarvisMemoryEntry> {
        val need=JarvisVoice.withoutAccents(query).trim()
        return entries(read()).filter { need.isEmpty() || JarvisVoice.withoutAccents(it.key+" "+it.text).contains(need) }
    }
    @Synchronized fun remember(category: String,text: String,key: String="") {
        require(category in categories && text.isNotBlank() && text.trim().length<=4000 && key.length<=80) { "Souvenir ou catégorie invalide." }
        val data=read(); val value=text.trim()
        if(category=="facts") {
            val facts=data.optJSONArray("facts") ?: JSONArray().also { data.put("facts",it) }
            if((0 until facts.length()).none { facts.optString(it)==value }) facts.put(value)
        } else {
            val group=data.optJSONObject(category) ?: JSONObject().also { data.put(category,it) }
            group.put(key.trim().ifBlank { value.take(40) },value)
        }
        write(data)
    }
    @Synchronized fun forget(subject: String): Int {
        require(subject.isNotBlank()) { "Sujet requis pour oublier." }
        val data=read(); val before=entries(data)
        val need=JarvisVoice.withoutAccents(subject).trim()
        val kept=before.filterNot { JarvisVoice.withoutAccents(it.key+" "+it.text).contains(need) }
        val removed=before.size-kept.size
        if(removed==0) return 0
        recovery.writeBytes(file.readFully())
        val next=empty()
        for(item in kept) if(item.category=="facts") next.getJSONArray("facts").put(item.text) else next.getJSONObject(item.category).put(item.key,item.text)
        write(next); return removed
    }
    @Synchronized fun erase() { file.delete(); recovery.delete() }
}
