package com.ange.mobile

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.net.Uri
import android.speech.RecognitionListener
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL
import java.util.Locale
import java.util.UUID
import java.util.concurrent.Executors
import java.util.concurrent.Future

enum class CompanionMood { IDLE, LISTENING, THINKING, SPEAKING, ERROR }
data class DialogueLine(val role: String, val content: String, val engine: String)

class CompanionController internal constructor(context: Context,
    private val onlineGenerate: (String,JSONObject,(HttpURLConnection?) -> Unit) -> String = OnlineConversation::generate,
    private val lookupContacts: (Context,String) -> List<ContactNumber> = ContactDirectory::find) {
    private val app = context.applicationContext
    private val prefs = app.getSharedPreferences("ange-companion", Context.MODE_PRIVATE)
    private val main = Handler(Looper.getMainLooper())
    private val executor = Executors.newSingleThreadExecutor()
    private val modelStore = OnDeviceModelStore(app)
    private val phoneRunner = OnDeviceRunner(app)
    private val deviceActions = AndroidDeviceActions(context)
    private val actionEngine = "Commandes Android"
    private val factsEngine = "Téléphone · données système"
    private val jarvisMemory=JarvisMemoryStore(app)
    private val routineStore=RoutineStore(app)
    var routines by mutableStateOf(runCatching { routineStore.all() }.getOrDefault(emptyList())); private set
    var routineStatus by mutableStateOf(""); private set
    var lastActionCommand by mutableStateOf(""); private set
    private var actionInstruction = ""
    var permissionAction by mutableStateOf<DeviceAction?>(null); private set
    var contactChoices by mutableStateOf<List<ContactNumber>>(emptyList()); private set
    var avatarStyle by mutableStateOf(prefs.getString("avatarStyle", "photo") ?: "photo")
    var memoryEntries by mutableStateOf<List<JarvisMemoryEntry>>(emptyList()); private set
    var memoryStatus by mutableStateOf("Chargement de la mémoire locale…"); private set
    var privateVoice by mutableStateOf(prefs.getBoolean("privateVoice",true))
    var availableApps by mutableStateOf<List<LaunchableApp>>(emptyList()); private set
    var pendingAction by mutableStateOf<DeviceAction?>(null); private set
    var handsFree by mutableStateOf(false); private set
    var avatar3D by mutableStateOf(prefs.getBoolean("avatar3D",true))
    private var pending: Future<*>? = null
    @Volatile private var connection: HttpURLConnection? = null
    private var generation = 0L
    private var foreground = true
    private var closed = false
    var active by mutableStateOf(true); private set
    private var tts: TextToSpeech? = null
    private var recognition: SpeechRecognizer? = null
    private var recognitionId: String? = null
    private var utteranceId: String? = null
    var mood by mutableStateOf(CompanionMood.IDLE); private set
    var status by mutableStateOf("Je suis ANGE. Écrivez-moi ou appuyez sur Parler."); private set
    var lastReply by mutableStateOf(""); private set
    var speechReady by mutableStateOf(false); private set
    var availableVoices by mutableStateOf<List<Voice>>(emptyList()); private set
    var mouthBeat by mutableStateOf(0); private set
    var microphoneLevel by mutableStateOf(0f); private set
    var backendStatus by mutableStateOf("PC non vérifié"); private set
    var mode by mutableStateOf(OnDevicePolicy.safeMode(prefs.getString("mode", "ondevice")))
    var modelReady by mutableStateOf(modelStore.ready()); private set
    var modelBusy by mutableStateOf(false); private set
    var modelProgress by mutableStateOf(0f); private set
    var modelStatus by mutableStateOf(if (modelStore.ready()) "Modèle présent sur le téléphone" else "Modèle à télécharger ou importer une seule fois"); private set
    var language by mutableStateOf(prefs.getString("language", "fr") ?: "fr")
    var serverUrl by mutableStateOf(prefs.getString("server", "") ?: "")
    var accessToken by mutableStateOf(SecureToken.read(app))
    var onlineKey by mutableStateOf(SecureToken.read(app,"gemini"))
    var onlineConsent by mutableStateOf(false)
    var selectedVoice by mutableStateOf(prefs.getString("voice", "") ?: "")
    var animations by mutableStateOf(prefs.getBoolean("animations", true))
    var autoSpeak by mutableStateOf(prefs.getBoolean("autoSpeak", true))
    var allowNetworkRecognition by mutableStateOf(false); private set
    var allowNetworkVoice by mutableStateOf(false)
    val lines = mutableStateListOf<DialogueLine>()

    init {
        refreshApps()
        refreshMemory()
        tts = TextToSpeech(app) { code -> main.post {
            if (!closed) {
                speechReady = code == TextToSpeech.SUCCESS
                availableVoices = if (speechReady) tts?.voices?.sortedBy { it.name } ?: emptyList() else emptyList()
                if (!speechReady) status = "Synthèse vocale indisponible. Le texte reste utilisable."
            }
        } }
        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(id: String?) { main.post {
                if (validUtterance(id)) { mood = CompanionMood.SPEAKING; status = "ANGE vous parle" }
            } }
            override fun onDone(id: String?) { main.post {
                if (validUtterance(id)) {
                    utteranceId = null; mood = CompanionMood.IDLE; status = "À votre écoute, sur demande."
                    val turn=generation
                    if(handsFree) main.postDelayed({ if(turn==generation && handsFree && foreground && !closed && pendingAction==null) startListening() },650)
                }
            } }
            @Deprecated("Android callback") override fun onError(id: String?) { speechError(id) }
            override fun onError(id: String?, errorCode: Int) { speechError(id) }
            override fun onStop(id: String?, interrupted: Boolean) { main.post {
                if (validUtterance(id)) { utteranceId = null; mood = CompanionMood.IDLE }
            } }
            override fun onRangeStart(id: String?, start: Int, end: Int, frame: Int) { main.post {
                if (validUtterance(id)) mouthBeat = start
            } }
        })
    }

    private fun validUtterance(id: String?) = !closed && foreground && id != null && id == utteranceId
    private fun speechError(id: String?) { main.post {
        if (validUtterance(id)) { handsFree=false; utteranceId = null; mood = CompanionMood.ERROR; status = "Lecture vocale interrompue par le moteur Android." }
    } }

    fun saveSettings() {
        prefs.edit().putString("mode", mode).putString("language", language).putString("server", serverUrl)
            .putString("voice", selectedVoice).putBoolean("animations", animations).putBoolean("autoSpeak", autoSpeak).putBoolean("avatar3D",avatar3D).putBoolean("privateVoice",privateVoice).putString("avatarStyle",avatarStyle).apply()
        try { SecureToken.write(app, accessToken.trim()); SecureToken.write(app,onlineKey.trim(),"gemini") } catch (_: Exception) {
            status = "Clés utilisables en mémoire, mais sauvegarde chiffrée indisponible."
        }
    }

    fun send(text: String) {
        val clean = text.trim().take(4000)
        if (clean.isEmpty() || closed || !foreground) return
        val confirmation=ContactDirectory.normalizedName(clean)
        if(pendingAction!=null && confirmation in setOf("annule","annuler","non annule")) { cancelAction(); return }
        if(pendingAction is DeviceAction.CallNumber && confirmation in setOf("confirme l appel","confirme cet appel","appelle maintenant")) { confirmAction(); return }
        if (mode !in setOf("offline", "ondevice", "local", "online")) { fail("Choisissez un mode disponible dans Réglages."); return }
        stop(false)
        val requestId = generation
        val expanded = try { routineStore.resolve(clean) ?: clean } catch (_: Exception) { fail("Raccourcis locaux illisibles. Vérifiez l’onglet Actions."); return }
        val command=DeviceActionPolicy.parse(expanded,availableApps)
        actionInstruction = expanded
        val fact = if (command == ActionParse.None) PhoneFacts.parse(clean) else null
        val history = conversationHistory().takeLast(20).map { JSONObject().put("role", it.role).put("content", it.content.take(4000)) }
        lines.add(DialogueLine("user", clean, when { command != ActionParse.None -> actionEngine; fact != null -> factsEngine; else -> mode }))
        when(command) {
            is ActionParse.Found -> { handsFree=false; dispatchAction(command.action); return }
            is ActionParse.Problem -> { handsFree=false; complete(command.message,actionEngine); return }
            ActionParse.None -> Unit
        }
        if (fact != null) {
            complete(PhoneFacts.reply(fact, AndroidPhoneFacts.read(app), language), factsEngine)
            return
        }
        if (mode == "offline") {
            complete(CompanionPolicy.offline(clean, language), "Mobile limité · sans IA générative")
            return
        }
        if (mode == "online") {
            if(!onlineConsent || onlineKey.isBlank()) { fail("Dans Réglages, configurez une clé Google AI Studio d’un projet sans facturation et autorisez les conversations en ligne pour cette session."); return }
            mood=CompanionMood.THINKING; status="Discussion en ligne · Google Gemini"
            val payload=OnlineConversation.payload(clean, conversationHistory().dropLast(1).map { it.role to it.content }, language)
            val key=onlineKey
            pending=executor.submit {
                try {
                    val reply=onlineGenerate(key,payload) { connection=it }
                    main.post { if(requestId==generation && foreground && !closed) complete(reply,"En ligne · Gemini 3.1 Flash-Lite") }
                } catch(error: Exception) { main.post { if(requestId==generation && foreground && !closed) fail(error.message ?: "Conversation indisponible.") } }
            }
            return
        }
        if (mode == "ondevice") {
            if (!modelStore.ready()) { fail("Téléchargez ou importez d’abord le modèle dans Réglages → IA sur le téléphone."); return }
            mood = CompanionMood.THINKING
            status = "ANGE réfléchit sur le téléphone · première ouverture du modèle parfois lente"
            val prompt = OnDevicePolicy.prompt(clean, conversationHistory().dropLast(1).map { it.role to it.content }, language)
            pending = executor.submit {
                try {
                    val reply = phoneRunner.generate(modelStore.file, prompt)
                    main.post { if (requestId == generation && foreground && !closed) {
                        complete(reply, "IA téléphone · Qwen3 0.6B · CPU · sans API")
                        main.postDelayed({ if (requestId == generation && !closed && mood != CompanionMood.THINKING) executor.execute { phoneRunner.unload() } },60000)
                    } }
                } catch (error: Exception) {
                    main.post { if (requestId == generation && foreground && !closed) fail(error.message ?: "Impossible d’exécuter le modèle sur le téléphone.") }
                }
            }
            return
        }
        mood = CompanionMood.THINKING
        status = "ANGE réfléchit · modèle local du PC"
        val body = JSONObject().put("message", clean).put("mode", mode).put("language", language)
            .put("cloud_consent", false).put("history", JSONArray(history))
        pending = executor.submit {
            try {
                val response = request("/v1/chat", "POST", body)
                val reply = response.getString("reply")
                val engine = response.getString("engine") + " / " + response.getString("model")
                main.post { if (requestId == generation && foreground && !closed) complete(reply, engine) }
            } catch (error: Exception) {
                main.post { if (requestId == generation && foreground && !closed) fail(error.message ?: "Connexion impossible") }
            }
        }
    }

    // Device facts become stale. Keep them visible, but never present old values to the model.
    private fun conversationHistory() = lines.filter { it.engine != actionEngine && it.engine != factsEngine }

    private fun complete(reply: String, engine: String) {
        val clean=JarvisVoice.clean(reply).ifBlank { reply }
        lastReply = clean
        lines.add(DialogueLine("assistant", clean, engine))
        while (lines.size > 40) lines.removeAt(0)
        mood = CompanionMood.IDLE
        status = engine
        if (autoSpeak || handsFree) speak(clean)
    }

    fun greet() {
        val text = when (language) {
            "en" -> "Hello! I'm ANGE, your AI companion. I'm here to help."
            "mg" -> "Salama! Izaho no ANGE, mpanampy anao."
            else -> "Bonjour ! Je suis ANGE, votre compagne intelligente. Je suis là pour vous aider."
        }
        lastReply = text
        speak(text)
    }

    fun speak(text: String = lastReply) {
        if (text.isBlank() || !foreground || closed) return
        if (!speechReady) { fail("Voix Android non prête. Installez ou activez un moteur de synthèse vocale."); return }
        cancelRecognition()
        val locale = Locale.forLanguageTag(language)
        val voices = availableVoices.filter { it.locale.language == locale.language && (!it.isNetworkConnectionRequired || allowNetworkVoice) }
        val voice = voices.find { it.name == selectedVoice } ?: voices.maxByOrNull { it.quality }
        if (voice == null) { fail("Aucune voix autorisée pour cette langue. Installez une voix locale, ou autorisez une voix réseau."); return }
        tts?.stop()
        tts?.voice = voice
        tts?.setSpeechRate(0.94f)
        utteranceId = UUID.randomUUID().toString()
        val spoken=JarvisVoice.spoken(text,privateVoice)
        val code = tts?.speak(spoken.take(TextToSpeech.getMaxSpeechInputLength()), TextToSpeech.QUEUE_FLUSH, Bundle(), utteranceId)
        if (code == TextToSpeech.ERROR) { utteranceId = null; fail("La voix Android a refusé la lecture.") }
    }

    fun startListening(keepCallConfirmation: Boolean = false) {
        if (closed || !foreground) return
        if (app.checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            fail("Autorisation microphone requise."); return
        }
        val call=if(keepCallConfirmation) pendingAction as? DeviceAction.CallNumber else null
        stop(false)
        if(call!=null) pendingAction=call
        val localAvailable = Build.VERSION.SDK_INT >= 31 && SpeechRecognizer.isOnDeviceRecognitionAvailable(app)
        val route = SpeechInputPolicy.initialRoute(localAvailable, allowNetworkRecognition)
        if (route == SpeechInputPolicy.Route.UNAVAILABLE) {
            fail("Reconnaissance locale indisponible. La saisie texte fonctionne ; la reconnaissance réseau reste désactivée."); return
        }
        startRecognition(route == SpeechInputPolicy.Route.LOCAL)
    }

    fun setNetworkRecognitionAllowed(allowed: Boolean) {
        // Revoking consent also cancels an active recognizer and any queued retry.
        stop(false); handsFree = false
        allowNetworkRecognition = allowed
        status = if (allowed) "Service vocal Android autorisé en secours pour cette session. Il peut transmettre votre audio."
                 else "Reconnaissance réseau désactivée. Seul le moteur local sera utilisé."
    }

    private fun startRecognition(local: Boolean) {
        if (closed || !foreground || (!local && !allowNetworkRecognition)) return
        if (!local && !SpeechRecognizer.isRecognitionAvailable(app)) { fail("Aucun service de reconnaissance installé."); return }
        try {
            val id = UUID.randomUUID().toString()
            recognitionId = id
            recognition = if (local && Build.VERSION.SDK_INT >= 31) SpeechRecognizer.createOnDeviceSpeechRecognizer(app)
                          else SpeechRecognizer.createSpeechRecognizer(app)
            recognition?.setRecognitionListener(object : RecognitionListener {
                private fun valid() = recognitionId == id && !closed && foreground
                override fun onReadyForSpeech(params: Bundle?) { if (valid()) { mood = CompanionMood.LISTENING; status = if (local) "Je vous écoute · sur l’appareil" else "Je vous écoute · service réseau autorisé" } }
                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(rmsdB: Float) { if (valid()) microphoneLevel = (rmsdB / 12f).coerceIn(0f, 1f) }
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() { if (valid()) { mood = CompanionMood.THINKING; status = "Transcription en cours" } }
                override fun onError(error: Int) { if (valid()) {
                    cancelRecognition()
                    if (SpeechInputPolicy.retryWithService(error, local, allowNetworkRecognition)) {
                        val turn = generation
                        status = "Langue indisponible en local · essai du service vocal Android autorisé"
                        main.post { if (turn == generation && foreground && !closed && allowNetworkRecognition) startRecognition(false) }
                    } else fail(SpeechInputPolicy.errorMessage(error, language, local))
                } }
                override fun onResults(results: Bundle?) { if (valid()) {
                    val text = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()
                    cancelRecognition()
                    if (text.isNullOrBlank()) fail("Aucun texte reconnu.") else {
                        send(text)
                    }
                } }
                override fun onPartialResults(partialResults: Bundle?) {}
                override fun onEvent(eventType: Int, params: Bundle?) {}
            })
            mood = CompanionMood.LISTENING
            recognition?.startListening(SpeechInputIntents.recognize(language, local))
            main.postDelayed({ if (recognitionId == id) { cancelRecognition(); fail("Écoute arrêtée après 25 secondes. Appuyez pour recommencer.") } }, 25000)
        } catch (_: Exception) { cancelRecognition(); fail("Impossible de démarrer la reconnaissance vocale.") }
    }

    fun permissionDenied() { fail("Le microphone est refusé. Vous pouvez continuer par écrit.") }
    fun enableHandsFree(): Boolean {
        if(!speechReady) { fail("Activez une voix Android avant la discussion vocale."); return false }
        handsFree=true; return true
    }
    private fun cancelRecognition() {
        recognitionId = null
        val previous = recognition
        recognition = null
        runCatching { previous?.cancel() }; runCatching { previous?.destroy() }
        microphoneLevel = 0f
    }
    fun stop(announce: Boolean = true) {
        generation++
        contactChoices=emptyList(); permissionAction=null
        pendingAction=null
        if(announce) handsFree=false
        if (modelBusy) modelStatus = "Transfert arrêté. Vous pouvez recommencer ou importer le fichier."
        phoneRunner.cancel(); modelStore.cancel(); modelBusy = false
        pending?.cancel(true); pending = null
        connection?.disconnect(); connection = null
        utteranceId = null
        tts?.stop()
        cancelRecognition()
        mood = CompanionMood.IDLE
        val idleId = generation
        main.postDelayed({ if (idleId == generation && !closed && !modelBusy && mood != CompanionMood.THINKING) executor.execute { phoneRunner.unload() } },60000)
        if (announce) status = "Interaction arrêtée."
    }
    fun clearHistory() { stop(); lines.clear(); lastReply = "" }
    fun onBackground() { foreground = false; active = false; handsFree=false; stop(false); executor.execute { phoneRunner.unload() }; status = "En pause. Aucun microphone en arrière-plan." }
    fun onForeground() {
        val returning=!foreground
        foreground = true; active = true
        if(returning) status="Prête. Appuyez sur Parler ou écrivez votre demande."
    }
    fun close() { closed = true; stop(false); tts?.shutdown(); tts = null; executor.execute { phoneRunner.unload() }; executor.shutdown() }
    private fun fail(message: String) { handsFree=false; mood = CompanionMood.ERROR; status = message }

    fun refreshApps() { executor.execute {
        val apps=runCatching { deviceActions.apps() }.getOrDefault(emptyList())
        main.post { if(!closed) availableApps=apps }
    } }
    fun requestAction(action: DeviceAction) {
        if(closed || !foreground) return
        stop(false); handsFree=false
        actionInstruction=when(action) {
            is DeviceAction.CallContact -> "Appelle ${action.name}"
            is DeviceAction.CallNumber -> "Appelle ${action.label}"
            is DeviceAction.OpenApp -> "Ouvre ${action.app.label}"
            is DeviceAction.Volume -> "Volume à ${action.percent} %"
            DeviceAction.Camera -> "Ouvre appareil photo"
            else -> ""
        }
        lines.add(DialogueLine("user",action.title,actionEngine))
        dispatchAction(action)
    }
    private fun dispatchAction(action: DeviceAction) {
        if (missingPermissions(action).isNotEmpty()) { permissionAction=action; status="Autorisation Android nécessaire pour cette action."; mood=CompanionMood.IDLE; return }
        if (action is DeviceAction.CallContact) { findContact(action.name); return }
        if(action.confirmation) { pendingAction=action; status="Vérifiez la demande avant de continuer."; mood=CompanionMood.IDLE }
        else executeAction(action)
    }
    fun missingPermissions(action: DeviceAction): List<String> = when(action) {
        is DeviceAction.CallContact -> listOf(Manifest.permission.READ_CONTACTS)
        is DeviceAction.CallNumber -> listOf(Manifest.permission.CALL_PHONE)
        else -> emptyList()
    }.filter { app.checkSelfPermission(it) != PackageManager.PERMISSION_GRANTED }
    fun resumePermissionAction(saved: DeviceAction? = permissionAction) {
        val action=saved ?: return
        permissionAction=null
        if(missingPermissions(action).isNotEmpty()) { fail("Autorisation refusée. Aucun appel n’a été lancé."); return }
        if(foreground && !closed) dispatchAction(action)
    }
    private fun findContact(name: String) {
        val turn=generation
        mood=CompanionMood.THINKING; status="Recherche locale du contact…"
        pending=executor.submit {
            try {
                val matches=lookupContacts(app,name)
                main.post { if(turn==generation && foreground && !closed) {
                    mood=CompanionMood.IDLE
                    when(matches.size) {
                        0 -> complete("Aucun numéro trouvé pour « $name » dans vos contacts. Vérifiez son nom exact.",actionEngine)
                        1 -> dispatchAction(DeviceAction.CallNumber(matches[0].number,matches[0].name))
                        else -> { contactChoices=matches; status="Choisissez le contact et le numéro à appeler." }
                    }
                } }
            } catch (_: Exception) { main.post { if(turn==generation && !closed) fail("Impossible de lire les contacts. Vérifiez l’autorisation Contacts.") } }
        }
    }
    fun chooseContact(contact: ContactNumber) {
        if(contact !in contactChoices) return
        contactChoices=emptyList(); dispatchAction(DeviceAction.CallNumber(contact.number,contact.name))
    }
    fun cancelContactChoice() { contactChoices=emptyList(); status="Appel annulé." }
    internal fun acceptContactChoices(contacts: List<ContactNumber>) { contactChoices=contacts; status="Choisissez le numéro à appeler." }
    fun teachRoutine(phrase: String, command: String) {
        try {
            require(PhoneFacts.parse(phrase)==null && DeviceActionPolicy.parse(phrase,availableApps)==ActionParse.None) { "Cette phrase est déjà une commande intégrée." }
            require(DeviceActionPolicy.parse(command,availableApps) is ActionParse.Found) { "Choisissez une commande Android prise en charge, pas une réponse du modèle." }
            routineStore.save(phrase,command); routines=routineStore.all(); routineStatus="Raccourci appris localement. Les confirmations des appels restent actives."
        } catch(error: Exception) { routineStatus=error.message ?: "Raccourci non enregistré." }
    }
    fun removeRoutine(phrase: String) { runCatching { routineStore.remove(phrase); routines=routineStore.all() }.onFailure { routineStatus="Suppression impossible." } }
    fun confirmAction() {
        val action=pendingAction ?: return
        pendingAction=null
        if(!closed && foreground) executeAction(action)
    }
    fun cancelAction() { pendingAction=null; status="Action annulée." }
    fun rendererFailed(message: String) {
        if(closed) return
        avatar3D=false; saveSettings(); status="Affichage 3D indisponible : ${message.take(120)}. Portrait 2D activé."
    }
    fun refreshMemory() = memoryTask { "Mémoire locale Jarvis · ${jarvisMemory.recall().size} souvenir(s)" }
    fun rememberMemory(category: String,text: String,key: String) = memoryTask {
        jarvisMemory.remember(category,text,key); "Souvenir enregistré uniquement sur ce téléphone."
    }
    fun forgetMemory(subject: String) = memoryTask {
        "${jarvisMemory.forget(subject)} souvenir(s) oublié(s). Une copie de récupération locale est conservée."
    }
    fun eraseMemory() = memoryTask { jarvisMemory.erase(); "Mémoire et copie de récupération effacées sur ce téléphone." }
    private fun memoryTask(operation: () -> String) {
        if(closed) return
        executor.execute {
            try {
                val result=operation(); val entries=jarvisMemory.recall()
                main.post { if(!closed) { memoryEntries=entries; memoryStatus=result } }
            } catch(error: Exception) { main.post { if(!closed) memoryStatus=error.message ?: "Mémoire inaccessible" } }
        }
    }
    private fun executeAction(action: DeviceAction) {
        try { complete(deviceActions.execute(action),actionEngine); lastActionCommand=actionInstruction }
        catch(error: Exception) { fail(error.message ?: "Android a refusé cette action.") }
    }

    fun downloadModel() = prepareModel(null)
    fun importModel(uri: Uri) = prepareModel(uri)
    private fun prepareModel(uri: Uri?) {
        if (closed || !foreground) return
        stop(false)
        val id = generation
        modelBusy = true; modelProgress = 0f
        modelStatus = if (uri == null) "Téléchargement du modèle · environ 347 Mo" else "Import et vérification du modèle"
        pending = executor.submit {
            try {
                phoneRunner.unload()
                val progress: (Long) -> Unit = { bytes -> main.post { if (id == generation && !closed) modelProgress = bytes.toFloat()/OnDevicePolicy.MODEL_BYTES } }
                if (uri == null) modelStore.download(progress) else modelStore.import(uri,progress)
                main.post { if (id == generation && !closed) { modelReady = true; modelBusy = false; modelStatus = "Modèle prêt · vérification SHA-256 réussie"; status = "Sélectionnez IA téléphone puis envoyez une question courte." } }
            } catch (error: Exception) {
                main.post { if (id == generation && !closed) { modelBusy = false; modelReady = modelStore.ready(); modelStatus = error.message ?: "Import impossible" } }
            }
        }
    }
    fun removeModel() {
        stop(false)
        val id = generation
        pending = executor.submit {
            phoneRunner.unload(); modelStore.remove()
            main.post { if (id == generation && !closed) { modelReady = false; modelStatus = "Modèle supprimé du téléphone." } }
        }
    }

    fun checkConnection() {
        stop(false)
        val id = generation
        backendStatus = "Vérification en cours…"
        pending = executor.submit { try {
            val data = request("/v1/status", "GET")
            val message = "PC joignable · local ${if(data.optBoolean("local_configured")) "configuré" else "à configurer"} · cloud ${if(data.optBoolean("cloud_configured")) "configuré" else "à configurer"}"
            main.post { if (id == generation && !closed) backendStatus = message }
        } catch (e: Exception) { main.post { if (id == generation && !closed) backendStatus = e.message ?: "PC indisponible" } } }
    }

    private fun request(path: String, method: String, body: JSONObject? = null): JSONObject {
        val base = CompanionPolicy.validateServer(serverUrl, BuildConfig.DEBUG)
        require(accessToken.trim().length >= 32 && !accessToken.contains('\n') && !accessToken.contains('\r')) { "Jeton de connexion manquant ou invalide." }
        val http = URL(base + path).openConnection() as HttpURLConnection
        connection = http
        http.instanceFollowRedirects = false
        http.connectTimeout = 6000; http.readTimeout = 95000
        http.requestMethod = method
        http.setRequestProperty("Authorization", "Bearer " + accessToken.trim())
        http.setRequestProperty("Content-Type", "application/json; charset=utf-8")
        try {
            if (body != null) { http.doOutput = true; http.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) } }
            val code = http.responseCode
            val stream = if (code in 200..299) http.inputStream else http.errorStream
            val raw = stream?.bufferedReader()?.use { it.readText().take(200000) } ?: "{}"
            val result = try { JSONObject(raw) } catch (_: Exception) { JSONObject() }
            if (code !in 200..299) throw IllegalStateException(if (code == 401) "Jeton refusé par le PC." else result.optString("detail", "Erreur serveur $code").take(400))
            return result
        } finally { http.disconnect(); if (connection === http) connection = null }
    }

}
