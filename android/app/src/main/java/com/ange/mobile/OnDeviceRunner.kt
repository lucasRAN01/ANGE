package com.ange.mobile

import android.app.ActivityManager
import android.content.Context
import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.Conversation
import com.google.ai.edge.litertlm.ConversationConfig
import com.google.ai.edge.litertlm.Contents
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import com.google.ai.edge.litertlm.SamplerConfig
import java.io.File
import java.util.concurrent.CancellationException
import java.util.concurrent.atomic.AtomicBoolean

/** All initialize/generate/unload calls use the controller's single worker, never the UI thread. */
class OnDeviceRunner(private val context: Context) {
    private var engine: Engine? = null
    @Volatile private var conversation: Conversation? = null
    private val conversationLock = Any()
    private val cancelled = AtomicBoolean(false)
    fun cancel() { cancelled.set(true); synchronized(conversationLock) { runCatching { conversation?.cancelProcess() } } }
    fun unload() { engine?.close(); engine = null }

    fun generate(file: File, prompt: String): String {
        cancelled.set(false)
        try {
            if (engine == null) {
                val memory = ActivityManager.MemoryInfo()
                (context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager).getMemoryInfo(memory)
                require(memory.availMem > 1200L * 1024 * 1024) { "Mémoire disponible insuffisante. Fermez quelques applications et réessayez." }
                val candidate = Engine(EngineConfig(modelPath = file.absolutePath, backend = Backend.CPU(threadCount = 2),
                    maxNumTokens = 1024, cacheDir = context.cacheDir.absolutePath))
                try { candidate.initialize(); engine = candidate } catch (failure: Throwable) { runCatching { candidate.close() }; throw failure }
            }
            if (cancelled.get() || Thread.currentThread().isInterrupted) throw CancellationException("Demande arrêtée")
            val config = ConversationConfig(
                systemInstruction = Contents.of("Tu es ANGE, une compagne IA locale et respectueuse. Tu es une IA. Réponds simplement, sans pensées internes. Tu n’as ni accès Internet, ni outils, ni contrôle des applications. Ne prétends pas en avoir. /no_think"),
                samplerConfig = SamplerConfig(topK = 20, topP = 0.9, temperature = 0.5),
                maxOutputToken = 160, automaticToolCalling = false)
            engine!!.createConversation(config).use { current ->
                synchronized(conversationLock) { conversation = current }
                try {
                    if (cancelled.get() || Thread.currentThread().isInterrupted) throw CancellationException("Demande arrêtée")
                    val response = current.sendMessage(prompt).toString()
                    if (cancelled.get()) throw CancellationException("Demande arrêtée")
                    return OnDevicePolicy.visibleReply(response).ifBlank { error("Le petit modèle n’a pas donné de réponse exploitable. Raccourcissez la question.") }
                } finally { synchronized(conversationLock) { conversation = null } }
            }
        } catch (failure: LinkageError) {
            unload()
            throw IllegalStateException("Le moteur local ne peut pas démarrer sur cet appareil. Conservez le mode Simple ou PC local.", failure)
        } catch (failure: OutOfMemoryError) {
            unload()
            throw IllegalStateException("Le téléphone manque de mémoire pour ce modèle. Fermez d’autres applications.", failure)
        }
    }
}
