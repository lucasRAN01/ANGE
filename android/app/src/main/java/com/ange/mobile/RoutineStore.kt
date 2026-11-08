package com.ange.mobile

import android.content.Context
import android.util.AtomicFile
import org.json.JSONObject
import java.io.File

data class LearnedRoutine(val phrase: String, val command: String)

/** Explicitly taught aliases, not model training or passive recording. */
internal class RoutineStore(context: Context) {
    private val file=AtomicFile(File(context.filesDir,"ange-routines.json"))
    @Synchronized fun all(): List<LearnedRoutine> {
        if(!file.baseFile.exists() && !File(file.baseFile.path+".bak").exists()) return emptyList()
        require(file.baseFile.length()<150000) { "Fichier de raccourcis trop volumineux." }
        val data=JSONObject(file.readFully().toString(Charsets.UTF_8))
        return data.keys().asSequence().map { LearnedRoutine(it,data.getString(it)) }.toList()
    }
    @Synchronized fun resolve(phrase: String) = all().find { DeviceActionPolicy.normalized(it.phrase)==DeviceActionPolicy.normalized(phrase).trimEnd('.','!','?') }?.command
    @Synchronized fun save(phrase: String, command: String) {
        val name=phrase.trim(); val value=command.trim()
        require(name.length in 2..80 && value.length in 1..1200) { "Phrase ou commande trop longue." }
        val items=all().filterNot { DeviceActionPolicy.normalized(it.phrase)==DeviceActionPolicy.normalized(name) }+LearnedRoutine(name,value)
        require(items.size<=30) { "Limite de 30 raccourcis atteinte." }
        write(items)
    }
    @Synchronized fun remove(phrase: String) = write(all().filterNot { it.phrase==phrase })
    private fun write(items: List<LearnedRoutine>) {
        val json=JSONObject(); items.forEach { json.put(it.phrase,it.command) }
        val stream=file.startWrite()
        try { stream.write(json.toString().toByteArray(Charsets.UTF_8)); file.finishWrite(stream) }
        catch(error: Exception) { file.failWrite(stream); throw error }
    }
}
