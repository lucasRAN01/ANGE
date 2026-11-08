package com.ange.mobile

import android.provider.Settings
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.math.min
import kotlin.math.sin

@Composable
fun CompanionAvatar(controller: CompanionController, modifier: Modifier = Modifier) {
    if(controller.avatar3D) { RobotCompanion3D(controller); return }
    val atlas = ImageBitmap.imageResource(R.drawable.companion_atlas)
    val context = LocalContext.current
    val systemMotion = remember { Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) > 0f }
    val animate = controller.animations && systemMotion && controller.active
    var frame by remember { mutableIntStateOf(0) }
    var tick by remember { mutableIntStateOf(0) }
    var waveUntil by remember { mutableLongStateOf(0L) }
    LaunchedEffect(controller.mood, animate) {
        tick = 0
        if (!animate) frame = 0
        while (animate) {
            frame = CompanionPolicy.frame(controller.mood.name, tick, controller.mouthBeat, animate, System.currentTimeMillis() < waveUntil)
            tick++
            delay(55)
        }
    }
    val breath = if (animate) sin(tick * .035).toFloat() else 0f
    Box(modifier.fillMaxWidth().height(350.dp)
        .background(Brush.radialGradient(listOf(Color(0x6631B5F4), Color.Transparent)), RoundedCornerShape(30.dp))
        .semantics { contentDescription = "ANGE, compagne 2D animée. Touchez-la pour une salutation vocale." }
        .clickable { waveUntil = System.currentTimeMillis() + 950; controller.greet() }, contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize().graphicsLayer {
            translationY = breath * 2f; scaleX = 1f + breath * .004f; scaleY = 1f + breath * .004f
            rotationZ = if (controller.mood == CompanionMood.LISTENING && animate) -.6f else breath * .15f
        }) {
            val w = atlas.width / 4; val h = atlas.height / 2
            val height = min(size.height, size.width * h / w).toInt()
            val width = (height.toFloat() * w / h).toInt()
            drawImage(atlas, srcOffset = IntOffset(frame % 4 * w, frame / 4 * h), srcSize = IntSize(w,h),
                dstOffset = IntOffset((size.width.toInt() - width) / 2, size.height.toInt() - height), dstSize = IntSize(width,height))
        }
    }
}

@Composable
fun CompanionScreen(controller: CompanionController, onMicrophone: () -> Unit) {
    var draft by rememberSaveable { mutableStateOf("") }
    var configuration by rememberSaveable { mutableStateOf(false) }
    var showHistory by rememberSaveable { mutableStateOf(false) }
    var clearConfirm by remember { mutableStateOf(false) }
    var downloadConfirm by remember { mutableStateOf(false) }
    var removeModelConfirm by remember { mutableStateOf(false) }
    var actions by remember { mutableStateOf(false) }
    var jarvisLicense by remember { mutableStateOf(false) }
    val localContext = LocalContext.current
    val importModel = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> if (uri != null) controller.importModel(uri) }
    val cyan = Color(0xFF25C9FF)
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Column(Modifier.weight(1f)) { Text("Votre compagne ANGE", fontSize = 22.sp, fontWeight = FontWeight.SemiBold); Text("Voix Android · bot 3D ou portrait animé", fontSize = 11.sp, color = cyan) }
            TextButton(onClick = { configuration = !configuration }, modifier = Modifier.width(80.dp)) { Text("Options", maxLines = 1) }
        }
        Row(horizontalArrangement=Arrangement.spacedBy(7.dp),verticalAlignment=Alignment.CenterVertically) {
            FilterChip(selected=controller.avatar3D,onClick={ controller.avatar3D=true; controller.saveSettings() },label={ Text("Bot 3D",fontSize=11.sp) })
            FilterChip(selected=!controller.avatar3D,onClick={ controller.avatar3D=false; controller.saveSettings() },label={ Text("Portrait 2D",fontSize=11.sp) })
            TextButton(onClick={ actions=true; controller.refreshApps() }) { Text("Actions") }
            Button(onClick=onMicrophone,enabled=!controller.modelBusy && controller.mood!=CompanionMood.THINKING,
                contentPadding=PaddingValues(horizontal=10.dp,vertical=4.dp)) { Text("Parler",fontSize=11.sp) }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf("offline" to "Simple", "ondevice" to "IA téléphone", "local" to "PC local").forEach { (id, label) ->
                FilterChip(selected = controller.mode == id, onClick = { controller.stop(); controller.mode = id; controller.saveSettings() }, label = { Text(label, fontSize = 11.sp) })
            }
        }
        if (controller.mode == "ondevice" && !controller.modelReady) {
            Text("Pour discuter sans PC ni API payante, installez le petit modèle local une seule fois.", fontSize = 12.sp, color = cyan)
            Text(controller.modelStatus, fontSize = 11.sp)
            if (controller.modelBusy) {
                LinearProgressIndicator(progress = { controller.modelProgress }, modifier = Modifier.fillMaxWidth())
                Text((controller.modelProgress * 100).toInt().toString() + " %", fontSize = 11.sp)
                OutlinedButton(onClick = { controller.stop() }) { Text("Annuler") }
            } else Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Button(onClick = { downloadConfirm = true }) { Text("Télécharger · 347 Mo", fontSize = 11.sp) }
                OutlinedButton(onClick = { importModel.launch(arrayOf("*/*")) }) { Text("Importer", fontSize = 11.sp) }
            }
        }
        CompanionAvatar(controller)
        Text(when(controller.mood) {
            CompanionMood.IDLE -> "À vos côtés, sur demande"
            CompanionMood.LISTENING -> "Je vous écoute…"
            CompanionMood.THINKING -> "Un instant…"
            CompanionMood.SPEAKING -> "ANGE vous parle"
            CompanionMood.ERROR -> "Une vérification est nécessaire"
        }, color = cyan, fontSize = 20.sp, modifier = Modifier.align(Alignment.CenterHorizontally))
        Text(controller.status, fontSize = 12.sp, color = Color(0xFFB9D8F3))
        if (controller.mood == CompanionMood.LISTENING) LinearProgressIndicator(progress = { controller.microphoneLevel }, modifier = Modifier.fillMaxWidth())
        if (controller.lastReply.isNotBlank()) {
            Text(controller.lastReply, Modifier.fillMaxWidth().border(1.dp, cyan.copy(alpha=.5f), RoundedCornerShape(14.dp))
                .background(Color(0xAA09254B), RoundedCornerShape(14.dp)).padding(16.dp), fontSize = 15.sp)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onMicrophone, enabled = !controller.modelBusy && controller.mood != CompanionMood.THINKING) { NativeIcon("voice"); Spacer(Modifier.width(7.dp)); Text("Parler") }
            OutlinedButton(onClick = { controller.stop() }) { Text("Arrêter") }
            TextButton(onClick = { controller.speak() }, enabled = controller.lastReply.isNotBlank()) { Text("Relire") }
        }
        OutlinedButton(onClick={ if(controller.handsFree) controller.stop() else if(controller.enableHandsFree()) onMicrophone() },enabled=!controller.modelBusy && controller.mood!=CompanionMood.THINKING,modifier=Modifier.fillMaxWidth()) {
            Text(if(controller.handsFree) "Discussion active · arrêter" else "Discussion vocale · démarrer",fontSize=12.sp)
        }
        if(controller.handsFree) Text("Le micro se rouvre après la réponse, uniquement tant qu’ANGE reste au premier plan. Arrêter termine la discussion.",fontSize=11.sp,color=cyan)
        OutlinedTextField(value = draft, onValueChange = { draft = it.take(4000) }, label = { Text("Écrivez à ANGE") }, modifier = Modifier.fillMaxWidth(), minLines = 2, maxLines = 6)
        Button(onClick = {
            controller.send(draft); draft = ""
        }, enabled = !controller.modelBusy && draft.isNotBlank() && controller.mood != CompanionMood.THINKING, modifier = Modifier.fillMaxWidth()) { Text("Envoyer") }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(onClick = { showHistory = !showHistory }) { Text("Historique de session (${controller.lines.size})") }
            TextButton(onClick = { clearConfirm = true }) { Text("Effacer") }
        }
        if (showHistory) controller.lines.forEach { line ->
            Text((if (line.role == "user") "Vous" else "ANGE · ${line.engine}") + "\n" + line.content,
                Modifier.fillMaxWidth().background(Color(0xAA0B2342), RoundedCornerShape(10.dp)).padding(12.dp), fontSize = 13.sp)
        }
        if (configuration) {
            HorizontalDivider()
            Text("IA sur le téléphone", color = cyan, fontSize = 18.sp)
            Text(OnDevicePolicy.MODEL_NAME + "\n" + controller.modelStatus, fontSize = 12.sp)
            Text("Modèle libre Apache 2.0 · aucune clé ni serveur. CPU limité à deux threads, réponses courtes. Les performances sur Galaxy A53 restent à mesurer.", fontSize = 11.sp)
            if (controller.modelBusy) LinearProgressIndicator(progress = { controller.modelProgress }, modifier = Modifier.fillMaxWidth())
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedButton(onClick = { downloadConfirm = true }, enabled = !controller.modelBusy) { Text("Télécharger", fontSize = 11.sp) }
                OutlinedButton(onClick = { importModel.launch(arrayOf("*/*")) }, enabled = !controller.modelBusy) { Text("Importer", fontSize = 11.sp) }
                TextButton(onClick = { removeModelConfirm = true }, enabled = !controller.modelBusy && controller.modelReady) { Text("Supprimer", fontSize = 11.sp) }
            }
            if (controller.modelBusy) OutlinedButton(onClick = { controller.stop() }) { Text("Arrêter le transfert") }
            Text("Voix et mouvements", color = cyan, fontSize = 18.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("fr" to "FR", "en" to "EN", "mg" to "MG").forEach { (code, label) ->
                    FilterChip(selected = controller.language == code, onClick = { controller.stop(); controller.language = code; controller.selectedVoice = ""; controller.saveSettings() }, label = { Text(label) })
                }
            }
            SettingToggle("Lire les réponses à voix haute", controller.autoSpeak) { controller.autoSpeak = it; controller.saveSettings() }
            SettingToggle("Animations du personnage", controller.animations) { controller.animations = it; controller.saveSettings() }
            SettingToggle("Masquer les infos sensibles à la lecture",controller.privateVoice) { controller.privateVoice=it; controller.saveSettings() }
            Text("Filtre vocal dérivé de Jarvis : mails, numéros et jetons courants. Le texte affiché est conservé. Ce filtre reste imparfait.",fontSize=11.sp)
            TextButton(onClick={ jarvisLicense=true }) { Text("Base Jarvis · origine et licence MIT") }
            SettingToggle("Autoriser une voix réseau", controller.allowNetworkVoice) { controller.allowNetworkVoice = it }
            Text("Une voix réseau peut transmettre le texte au moteur vocal installé. Autorisation valable pour cette session.", fontSize = 11.sp)
            val voices = controller.availableVoices.filter { it.locale.language == controller.language && (!it.isNetworkConnectionRequired || controller.allowNetworkVoice) }
            var voicesExpanded by remember { mutableStateOf(false) }
            Box { OutlinedButton(onClick = { voicesExpanded = true }) { Text("Voix : " + (controller.selectedVoice.ifBlank { "automatique" }), maxLines = 2) }
                DropdownMenu(expanded = voicesExpanded, onDismissRequest = { voicesExpanded = false }) {
                    voices.forEach { voice -> DropdownMenuItem(text = { Text(voice.name + if(voice.isNetworkConnectionRequired) " · réseau" else " · locale") }, onClick = { controller.selectedVoice = voice.name; controller.saveSettings(); voicesExpanded = false }) }
                }
            }
            OutlinedButton(onClick = { controller.greet() }) { Text("Tester la salutation et la voix") }
            TextButton(onClick = { try { localContext.startActivity(Intent("com.android.settings.TTS_SETTINGS")) } catch (_: Exception) { localContext.startActivity(Intent(Settings.ACTION_SETTINGS)) } }) { Text("Réglages vocaux du téléphone") }
            SettingToggle("Reconnaissance vocale réseau autorisée", controller.allowNetworkRecognition) { controller.allowNetworkRecognition = it }
            Text("Si la reconnaissance locale est indisponible, le service vocal Android peut envoyer votre audio à son fournisseur. Aucune écoute continue ni en arrière-plan.", fontSize = 11.sp)
            if (controller.mode == "local") {
            Text("Connexion facultative au PC", color = cyan, fontSize = 18.sp)
            OutlinedTextField(value = controller.serverUrl, onValueChange = { controller.serverUrl = it.take(300) }, label = { Text("https://nom-du-pc:8765") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            OutlinedTextField(value = controller.accessToken, onValueChange = { controller.accessToken = it.take(256) }, label = { Text("Jeton ANGE Core, pas une clé de fournisseur IA") }, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth(), singleLine = true)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { controller.saveSettings(); controller.checkConnection() }) { Text("Enregistrer et tester") }
                TextButton(onClick = { controller.accessToken = ""; controller.saveSettings() }) { Text("Oublier le jeton") }
            }
            Text(controller.backendStatus, fontSize = 12.sp)
            Text("Le jeton est protégé par Android Keystore. Le mode IA téléphone n’utilise pas cette connexion.", fontSize = 11.sp)
            }
        }
        Text("Bot en vraie 3D procédurale ou portrait en huit poses 2D. Synchronisation vocale approximative. Les commandes Android explicites utilisent les fonctions des applications ; les réponses du modèle ne déclenchent aucune action.", fontSize = 10.sp, color = Color(0xFF93B4D0))
    }
    if(actions) DeviceActionsDialog(controller,onDismiss={ actions=false })
    if(jarvisLicense) {
        val license=remember { localContext.assets.open("licenses/jarvis-MIT.txt").bufferedReader().use { it.readText() } }
        AlertDialog(onDismissRequest={ jarvisLicense=false },title={ Text("Base Jarvis · MIT") },
            text={ Column(Modifier.heightIn(max=400.dp).verticalScroll(rememberScrollState())) {
                Text("sosoj92/jarvis-assistant-vocal\nRévision 15f3f87e0bfd3fe68ec38deafbfeab22636c3993\nPorts Android : mémoire, nettoyage et confidentialité vocale. Le moteur Python complet n’est pas exécuté dans cet APK.\n\n"+license,fontSize=12.sp)
            } },confirmButton={ TextButton(onClick={ jarvisLicense=false }) { Text("Fermer") } })
    }
    controller.pendingAction?.let { action -> AlertDialog(onDismissRequest={ controller.cancelAction() },title={ Text(action.title) },text={ Text(action.detail) },
        confirmButton={ TextButton(onClick={ controller.confirmAction() }) { Text("Continuer") } },dismissButton={ TextButton(onClick={ controller.cancelAction() }) { Text("Annuler") } }) }
    if (downloadConfirm) AlertDialog(onDismissRequest = { downloadConfirm = false }, title = { Text("Télécharger le modèle gratuit ?") },
        text = { Text("Environ 347 Mo seront téléchargés depuis Hugging Face, puis vérifiés. Ce modèle est sous licence Apache 2.0. Aucune conversation n’est envoyée. Le téléchargement utilise votre connexion Internet ; privilégiez le Wi-Fi. Gardez l’application ouverte. Source : " + OnDevicePolicy.MODEL_PAGE) },
        confirmButton = { TextButton(onClick = { downloadConfirm = false; controller.downloadModel() }) { Text("Télécharger") } }, dismissButton = { TextButton(onClick = { downloadConfirm = false }) { Text("Annuler") } })
    if (removeModelConfirm) AlertDialog(onDismissRequest = { removeModelConfirm = false }, title = { Text("Supprimer le modèle local ?") }, text = { Text("Il faudra le télécharger ou l’importer à nouveau pour utiliser IA téléphone.") },
        confirmButton = { TextButton(onClick = { removeModelConfirm = false; controller.removeModel() }) { Text("Supprimer") } }, dismissButton = { TextButton(onClick = { removeModelConfirm = false }) { Text("Annuler") } })
    if (clearConfirm) AlertDialog(onDismissRequest = { clearConfirm = false }, title = { Text("Effacer la session ?") }, text = { Text("Cela supprime les messages en mémoire sur ce téléphone. Une requête déjà transmise au fournisseur ne peut pas être retirée.") },
        confirmButton = { TextButton(onClick = { controller.clearHistory(); clearConfirm = false }) { Text("Effacer") } }, dismissButton = { TextButton(onClick = { clearConfirm = false }) { Text("Annuler") } })
}

@Composable
private fun DeviceActionsDialog(controller: CompanionController,onDismiss: () -> Unit) {
    var appsTab by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    var command by remember { mutableStateOf("") }
    AlertDialog(onDismissRequest=onDismiss,title={ Text("Actions Android") },text={
        Column(verticalArrangement=Arrangement.spacedBy(8.dp)) {
            Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                FilterChip(selected=!appsTab,onClick={ appsTab=false },label={ Text("Commandes") })
                FilterChip(selected=appsTab,onClick={ appsTab=true },label={ Text("Applications") })
            }
            if(appsTab) {
                OutlinedTextField(value=query,onValueChange={ query=it },label={ Text("Nom de l’application") },singleLine=true,modifier=Modifier.fillMaxWidth())
                val apps=controller.availableApps.filter { DeviceActionPolicy.normalized(it.label).contains(DeviceActionPolicy.normalized(query)) }
                if(apps.isEmpty()) Text("Aucune application visible correspondante. Les applis masquées ou du profil professionnel peuvent être absentes.",fontSize=12.sp)
                LazyColumn(Modifier.heightIn(max=300.dp)) { items(apps,key={ it.packageName }) { app ->
                    TextButton(onClick={ onDismiss(); controller.requestAction(DeviceAction.OpenApp(app)) },modifier=Modifier.fillMaxWidth()) { Text(app.label) }
                } }
            } else Column(Modifier.heightIn(max=400.dp).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(8.dp)) {
                Text("Dites ou écrivez une commande précise. Ces commandes fonctionnent sans télécharger le modèle IA.",fontSize=12.sp)
                listOf("Ouvre appareil photo","Ouvre paramètres","Ouvre Wi-Fi","Volume à 40 %").forEach { sample ->
                    OutlinedButton(onClick={ onDismiss(); controller.send(sample) },modifier=Modifier.fillMaxWidth()) { Text(sample,fontSize=12.sp) }
                }
                OutlinedTextField(value=command,onValueChange={ command=it.take(1200) },label={ Text("Votre commande") },placeholder={ Text("Alarme à 7h30") },modifier=Modifier.fillMaxWidth(),maxLines=4)
                Button(onClick={ onDismiss(); controller.send(command) },enabled=command.isNotBlank(),modifier=Modifier.fillMaxWidth()) { Text("Demander l’action") }
                Text("Exemples :\nMinuteur de 5 minutes\nAppelle +261340000000\nPrépare un SMS au +261340000000 : Bonjour\nItinéraire vers Antananarivo\nCherche météo Antananarivo\nAjoute un événement : Réunion équipe",fontSize=12.sp)
                Text("Appels et SMS : validation dans leur application. Agenda : vous enregistrez le brouillon. Wi-Fi/Bluetooth : ouverture des réglages. Aucun contrôle automatique des boutons internes d’autres applications.",fontSize=11.sp)
            }
        }
    },confirmButton={ TextButton(onClick=onDismiss) { Text("Fermer") } })
}

@Composable
private fun SettingToggle(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f), fontSize = 13.sp)
        Switch(checked = checked, onCheckedChange = onChange)
    }
}
