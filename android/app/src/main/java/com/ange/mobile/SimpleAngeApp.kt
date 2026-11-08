package com.ange.mobile

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import kotlin.math.roundToInt

private val Space=Color(0xFF080D1B)
private val Glass=Color(0xFF121D32)
private val Mint=Color(0xFF69E8DC)
private val Lilac=Color(0xFFB9ACFF)
private val Muted=Color(0xFF98A9C3)

@Composable
fun SimpleAngeApp(controller: CompanionController, voiceRequest: Int = 0, chatRequest: Int = 0) {
    val context=LocalContext.current
    var page by rememberSaveable { mutableIntStateOf(0) }
    var draft by rememberSaveable { mutableStateOf("") }
    var history by remember { mutableStateOf(false) }
    var call by remember { mutableStateOf(false) }
    var callName by rememberSaveable { mutableStateOf("") }
    var advanced by remember { mutableStateOf(false) }
    val draftFocus=remember { FocusRequester() }
    val keyboard=LocalSoftwareKeyboardController.current
    var pendingChatFocus by remember { mutableStateOf(false) }
    LaunchedEffect(chatRequest) { if(chatRequest>0) { page=0; advanced=false; pendingChatFocus=true } }
    val discussionScroll=rememberLazyListState()
    LaunchedEffect(controller.lines.size) {
        if(controller.lines.isNotEmpty()) discussionScroll.animateScrollToItem(2+(if(controller.mode=="ondevice" && !controller.modelReady) 1 else 0)+controller.lines.takeLast(4).size)
    }
    var savedPermissionAction by remember { mutableStateOf<DeviceAction?>(null) }
    var voiceConfirmation by remember { mutableStateOf(false) }
    val microphone=rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted -> if(granted) controller.startListening(voiceConfirmation) else controller.permissionDenied(); voiceConfirmation=false }
    val actionPermissions=rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        controller.resumePermissionAction(savedPermissionAction); savedPermissionAction=null
    }
    LaunchedEffect(controller.permissionAction) { controller.permissionAction?.let { action ->
        savedPermissionAction=action; actionPermissions.launch(controller.missingPermissions(action).toTypedArray())
    } }
    LaunchedEffect(voiceRequest) { if(voiceRequest>0) { page=0; microphone.launch(Manifest.permission.RECORD_AUDIO) } }
    BackHandler(page!=0 || advanced) { advanced=false; page=0 }
    if(advanced) { AngeApp(controller); return }
    fun send() { val text=draft.trim(); if(text.isNotEmpty()) { controller.send(text); draft="" } }
    MaterialTheme(colorScheme=darkColorScheme(primary=Mint,secondary=Lilac,background=Space,surface=Glass,onSurface=Color(0xFFF0F4FC),onPrimary=Space)) {
        Scaffold(containerColor=Space,bottomBar={
            Column(Modifier.imePadding().background(Space)) {
                if(page==0) {
                    LaunchedEffect(pendingChatFocus) { if(pendingChatFocus) { draftFocus.requestFocus(); keyboard?.show(); pendingChatFocus=false } }
                    Text(controller.status,modifier=Modifier.padding(horizontal=22.dp,vertical=6.dp),color=if(controller.mood==CompanionMood.ERROR) Color(0xFFFFB4B4) else Muted,fontSize=11.sp,maxLines=5)
                    Row(Modifier.fillMaxWidth().padding(horizontal=16.dp,vertical=7.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(9.dp)) {
                        OutlinedTextField(value=draft,onValueChange={ draft=it.take(4000) },placeholder={ Text("Parlez-moi, ou demandez une action…",fontSize=12.sp) },modifier=Modifier.weight(1f).focusRequester(draftFocus),shape=RoundedCornerShape(22.dp),maxLines=3,
                            keyboardOptions=KeyboardOptions(imeAction=ImeAction.Send),keyboardActions=KeyboardActions(onSend={ send() }))
                        FilledIconButton(onClick={ if(draft.isNotBlank()) send() else microphone.launch(Manifest.permission.RECORD_AUDIO) },modifier=Modifier.size(50.dp),colors=IconButtonDefaults.filledIconButtonColors(containerColor=Mint)) {
                            if(draft.isBlank()) NativeIcon("voice",Modifier.size(25.dp)) else Text("↑",color=Space,fontSize=27.sp)
                        }
                    }
                }
                NavigationBar(containerColor=Space,tonalElevation=0.dp) {
                    listOf("ANGE" to "home","Actions" to "control","Mémoire" to "memory","Réglages" to "settings").forEachIndexed { index,(label,icon) ->
                        NavigationBarItem(selected=page==index,onClick={ page=index },icon={ NativeIcon(icon,Modifier.size(23.dp)) },label={ Text(label,fontSize=10.sp) },
                            colors=NavigationBarItemDefaults.colors(indicatorColor=Mint.copy(alpha=.12f),selectedTextColor=Mint,unselectedTextColor=Muted))
                    }
                }
            }
        }) { padding ->
            Column(Modifier.fillMaxSize().padding(padding).padding(horizontal=22.dp)) {
                Row(Modifier.fillMaxWidth().padding(top=8.dp,bottom=12.dp),verticalAlignment=Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(10.dp)) {
                            Image(painterResource(R.drawable.ange_logo),null,Modifier.size(35.dp))
                            Column { Text("ANGE",fontSize=23.sp,fontWeight=FontWeight.SemiBold,letterSpacing=4.sp); Text("Votre compagne personnelle",fontSize=10.sp,color=Muted) }
                        }
                        AssistChip(onClick={ page=3 },label={ Text(when(controller.mode) { "online" -> "En ligne"; "local" -> "PC local"; "offline" -> "Simple"; else -> "Sur téléphone" },fontSize=10.sp) })
                    }
                    CompanionAvatar(controller,Modifier.width(96.dp))
                }
                when(page) {
                    0 -> LazyColumn(Modifier.fillMaxSize(),state=discussionScroll,verticalArrangement=Arrangement.spacedBy(15.dp),contentPadding=PaddingValues(bottom=18.dp)) {
                        item {
                            Text("À vos côtés.",fontSize=30.sp,fontWeight=FontWeight.SemiBold)
                            Text("Une conversation. Une action. Tout commence ici.",fontSize=12.sp,color=Muted,modifier=Modifier.padding(top=5.dp))
                            if(!Settings.canDrawOverlays(context)) OutlinedButton(onClick={ context.startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,Uri.parse("package:${context.packageName}"))) }) { Text("Garder mini-ANGE visible hors de l’application",fontSize=12.sp) }
                            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.Center) {
                                FilledTonalButton(onClick={ microphone.launch(Manifest.permission.RECORD_AUDIO) }) { NativeIcon("voice",Modifier.size(19.dp)); Spacer(Modifier.width(7.dp)); Text("Parler à ANGE") }
                                TextButton(onClick={ controller.stop() }) { Text("Arrêter",color=Muted) }
                            }
                        }
                        item {
                            Row(horizontalArrangement=Arrangement.spacedBy(9.dp)) {
                                QuickAction("Appeler", "call",Modifier.weight(1f)) { call=true }
                                QuickAction("Caméra", "camera",Modifier.weight(1f)) { controller.send("Ouvre appareil photo") }
                                QuickAction("L’heure", "history",Modifier.weight(1f)) { controller.send("Quelle heure est-il ?") }
                                QuickAction("Batterie", "battery",Modifier.weight(1f)) { controller.send("Quel est le niveau de batterie ?") }
                            }
                        }
                        if(controller.mode=="ondevice" && !controller.modelReady) item {
                            TextButton(onClick={ page=3 }) { Text("Installer le modèle pour les conversations hors ligne →",fontSize=12.sp,color=Lilac) }
                        }
                        if(controller.lines.isNotEmpty()) {
                            item { Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically) {
                                Text("Notre discussion",fontSize=16.sp,fontWeight=FontWeight.Medium)
                                TextButton(onClick={ history=true }) { Text("Tout voir",fontSize=11.sp,color=Mint) }
                            } }
                            items(controller.lines.takeLast(4)) { DialogueCard(it) }
                        }
                    }
                    1 -> SimpleActions(controller)
                    2 -> Column(Modifier.verticalScroll(rememberScrollState())) { Text("Ce que vous me confiez",fontSize=25.sp); Spacer(Modifier.height(16.dp)); JarvisMemoryScreen(controller) }
                    3 -> SimpleSettings(controller,onAdvanced={ advanced=true })
                }
            }
        }
        if(call) AlertDialog(onDismissRequest={ call=false },title={ Text("Qui souhaitez-vous appeler ?") },text={ Column { Text("Le contact sera recherché uniquement sur ce téléphone.",fontSize=12.sp); OutlinedTextField(callName,{ callName=it.take(100) },label={ Text("Nom du contact, par exemple Babe") },singleLine=true) } },confirmButton={ TextButton(onClick={ call=false; controller.send("Appelle $callName") },enabled=callName.isNotBlank()) { Text("Chercher") } },dismissButton={ TextButton(onClick={ call=false }) { Text("Annuler") } })
        controller.pendingAction?.let { action -> AlertDialog(onDismissRequest={ controller.cancelAction() },title={ Text(action.title) },text={ Column { Text(action.detail); if(action is DeviceAction.CallNumber) TextButton(onClick={ voiceConfirmation=true; microphone.launch(Manifest.permission.RECORD_AUDIO) }) { Text("Confirmer à la voix : « Confirme l’appel »") } } },confirmButton={ TextButton(onClick={ controller.confirmAction() }) { Text(if(action is DeviceAction.CallNumber) "Appeler" else "Continuer") } },dismissButton={ TextButton(onClick={ controller.cancelAction() }) { Text("Annuler") } }) }
        if(controller.contactChoices.isNotEmpty()) AlertDialog(onDismissRequest={ controller.cancelContactChoice() },title={ Text("Choisir le numéro") },text={ LazyColumn(Modifier.heightIn(max=350.dp)) { items(controller.contactChoices) { contact -> TextButton(onClick={ controller.chooseContact(contact) }) { Text("${contact.name}\n${contact.number}") } } } },confirmButton={ TextButton(onClick={ controller.cancelContactChoice() }) { Text("Annuler") } })
        if(controller.appChoices.isNotEmpty()) AlertDialog(onDismissRequest={ controller.cancelAppChoice() },title={ Text("Quelle application ouvrir ?") },text={ LazyColumn(Modifier.heightIn(max=350.dp)) { items(controller.appChoices,key={ it.packageName }) { app -> TextButton(onClick={ controller.chooseApp(app) },modifier=Modifier.fillMaxWidth()) { Column { Text(app.label); Text(app.packageName,fontSize=10.sp,color=Muted) } } } } },confirmButton={ TextButton(onClick={ controller.cancelAppChoice() }) { Text("Annuler") } })
        if(history) AlertDialog(onDismissRequest={ history=false },title={ Text("Notre discussion") },text={ LazyColumn(Modifier.heightIn(max=500.dp),verticalArrangement=Arrangement.spacedBy(10.dp)) { items(controller.lines.toList()) { DialogueCard(it) } } },confirmButton={ TextButton(onClick={ history=false }) { Text("Fermer") } },dismissButton={ TextButton(onClick={ controller.clearHistory(); history=false }) { Text("Effacer la session") } })
    }
}

@Composable private fun QuickAction(title: String,icon: String,modifier: Modifier,onClick: () -> Unit) {
    Surface(onClick=onClick,modifier=modifier,shape=RoundedCornerShape(19.dp),color=Glass) {
        Column(Modifier.padding(vertical=12.dp,horizontal=4.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(7.dp)) {
            NativeIcon(icon,Modifier.size(23.dp)); Text(title,fontSize=11.sp,fontWeight=FontWeight.Medium,maxLines=1)
        }
    }
}
@Composable private fun DialogueCard(line: DialogueLine) {
    Surface(shape=RoundedCornerShape(18.dp),color=if(line.role=="user") Color(0xFF1C2941) else Glass,modifier=Modifier.fillMaxWidth()) {
        Column(Modifier.padding(15.dp),verticalArrangement=Arrangement.spacedBy(7.dp)) {
            Text(if(line.role=="user") "Vous" else "ANGE · ${line.engine}",fontSize=10.sp,color=if(line.role=="user") Muted else Mint)
            Text(line.content,fontSize=14.sp,lineHeight=21.sp)
        }
    }
}

@Composable private fun SimpleActions(controller: CompanionController) {
    var phrase by rememberSaveable { mutableStateOf("") }; var command by rememberSaveable { mutableStateOf("") }
    var appQuery by rememberSaveable { mutableStateOf("") }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(14.dp)) {
        Text("Je m’en occupe.",fontSize=28.sp,fontWeight=FontWeight.SemiBold)
        Text("Des commandes locales. Le modèle n’exécute aucune action.",color=Muted,fontSize=12.sp)
        listOf("Ouvre appareil photo","Allume la lampe","Éteins la lampe","Capture d’écran","Verrouille le téléphone","Active le Wi-Fi","Active le Bluetooth","Active les données mobiles","Quelle heure est-il ?","Quel jour sommes-nous ?","Combien de batterie me reste-t-il ?","Ouvre paramètres","Volume à 40 %","Va à l’accueil").forEach { sample ->
            OutlinedButton(onClick={ controller.send(sample) },modifier=Modifier.fillMaxWidth(),shape=RoundedCornerShape(14.dp)) { Text(sample) }
        }
        Text("Applications",fontSize=20.sp)
        TextButton(onClick={ controller.refreshApps() }) { Text("Actualiser les applications") }
        OutlinedTextField(appQuery,{ appQuery=it.take(80) },label={ Text("Rechercher une application installée") },modifier=Modifier.fillMaxWidth())
        controller.availableApps.filter { DeviceActionPolicy.normalized(it.label).contains(DeviceActionPolicy.normalized(appQuery)) }.take(12).forEach { app ->
            TextButton(onClick={ controller.send("Ouvre ${app.label}") }) { Text(app.label) }
        }
        HorizontalDivider(color=Muted.copy(alpha=.2f)); Text("Apprenez-moi un raccourci",fontSize=20.sp)
        Text("Associez vos mots à une action connue. Je retiens ce raccourci sur votre téléphone ; cela n’entraîne pas le modèle IA.",fontSize=12.sp,color=Muted)
        OutlinedTextField(phrase,{ phrase=it.take(80) },label={ Text("Quand je dis…") },placeholder={ Text("Mode discret") },modifier=Modifier.fillMaxWidth())
        OutlinedTextField(command,{ command=it.take(1200) },label={ Text("La commande à effectuer") },placeholder={ Text("Volume à 20 %") },modifier=Modifier.fillMaxWidth())
        if(controller.lastActionCommand.isNotBlank()) TextButton(onClick={ command=controller.lastActionCommand }) { Text("Utiliser ma dernière action") }
        Button(onClick={ controller.teachRoutine(phrase,command) },enabled=phrase.isNotBlank() && command.isNotBlank()) { Text("Apprendre ce raccourci") }
        if(controller.routineStatus.isNotBlank()) Text(controller.routineStatus,fontSize=12.sp,color=Mint)
        controller.routines.forEach { routine -> Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) { Text(routine.phrase); Text(routine.command,fontSize=11.sp,color=Muted) }
            TextButton(onClick={ controller.removeRoutine(routine.phrase) }) { Text("Oublier") }
        } }
        Spacer(Modifier.height(20.dp))
    }
}

@Composable private fun Toggle(label: String,checked: Boolean,change: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) { Text(label,Modifier.weight(1f),fontSize=13.sp); Switch(checked,change) }
}

@Composable private fun SimpleSettings(controller: CompanionController,onAdvanced: () -> Unit) {
    val context=LocalContext.current
    var download by remember { mutableStateOf(false) }; var remove by remember { mutableStateOf(false) }
    var roam by rememberSaveable { mutableStateOf(false) }; var floatingStatus by remember { mutableStateOf("") }
    val importer=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { it?.let(controller::importModel) }
    fun startFloat() { runCatching { controller.saveSettings(); FloatingCompanionService.start(context,controller,roam) }.onFailure { floatingStatus=it.message ?: "Mini-compagne indisponible." } }
    val floatingPermissions=rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { startFloat() }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(14.dp)) {
        Text("À votre façon.",fontSize=28.sp,fontWeight=FontWeight.SemiBold)
        Text("DISCUSSION",fontSize=10.sp,color=Mint,letterSpacing=2.sp)
        listOf("ondevice" to "Sur téléphone · hors ligne","online" to "En ligne · Google Gemini","offline" to "Simple · commandes locales","local" to "Modèle du PC · facultatif").forEach { (mode,title) ->
            FilterChip(selected=controller.mode==mode,onClick={ controller.stop(); controller.mode=mode; controller.saveSettings() },label={ Text(title) },modifier=Modifier.fillMaxWidth())
        }
        if(controller.mode=="online") {
            Text("Gemini 3.1 Flash-Lite. Niveau gratuit soumis à quotas. Utilisez un projet Google AI Studio sans facturation ; une clé liée à un projet payant peut entraîner des frais. Les messages envoyés au niveau gratuit peuvent servir à améliorer les produits Google.",fontSize=12.sp,color=Muted)
            OutlinedTextField(controller.onlineKey,{ controller.onlineKey=it.take(200) },label={ Text("Votre clé Google AI Studio") },visualTransformation=PasswordVisualTransformation(),singleLine=true,modifier=Modifier.fillMaxWidth())
            Row { TextButton(onClick={ context.startActivity(Intent(Intent.ACTION_VIEW,Uri.parse("https://aistudio.google.com/api-keys"))) }) { Text("Créer ma clé") }; TextButton(onClick={ controller.saveSettings() }) { Text("Enregistrer") }; TextButton(onClick={ controller.onlineKey=""; controller.onlineConsent=false; controller.saveSettings() }) { Text("Oublier") } }
            Toggle("Mon projet est sans facturation ; j’autorise l’envoi de la question et de 12 messages précédents à Google pour cette session",controller.onlineConsent) { controller.stop(); controller.onlineConsent=it }
            Text("Ni contacts, ni écran, ni mémoire Jarvis ou raccourcis ajoutés aux requêtes. Les données que vous écrivez vous-même dans le chat peuvent être transmises.",fontSize=11.sp,color=Muted)
        }
        if(controller.mode=="ondevice") {
            Text(controller.modelStatus,fontSize=12.sp,color=Muted)
            Text("Le modèle local est petit : réponses limitées et erreurs possibles. L’heure, les contacts et les actions utilisent directement Android.",fontSize=11.sp,color=Muted)
            Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) { Button(onClick={ download=true },enabled=!controller.modelBusy) { Text("Télécharger") }; OutlinedButton(onClick={ importer.launch(arrayOf("*/*")) },enabled=!controller.modelBusy) { Text("Importer") }; TextButton(onClick={ remove=true },enabled=controller.modelReady) { Text("Supprimer") } }
            if(controller.modelBusy) { LinearProgressIndicator(progress={ controller.modelProgress },modifier=Modifier.fillMaxWidth()); TextButton(onClick={ controller.stop() }) { Text("Arrêter le transfert") } }
        }
        if(controller.mode=="local") {
            OutlinedTextField(controller.serverUrl,{ controller.serverUrl=it.take(300) },label={ Text("Adresse HTTPS du PC") },modifier=Modifier.fillMaxWidth())
            OutlinedTextField(controller.accessToken,{ controller.accessToken=it.take(256) },label={ Text("Jeton ANGE Core") },visualTransformation=PasswordVisualTransformation(),modifier=Modifier.fillMaxWidth())
            Button(onClick={ controller.saveSettings(); controller.checkConnection() }) { Text("Enregistrer et tester") }; Text(controller.backendStatus,fontSize=12.sp)
        }
        HorizontalDivider(color=Muted.copy(alpha=.2f)); Text("MINI-COMPAGNE",fontSize=10.sp,color=Mint,letterSpacing=2.sp)
        Text("Personnage inspiré de votre photo, animé en douze poses. Touchez-le pour une salutation ; déplacez-le du doigt.",fontSize=12.sp,color=Muted)
        Toggle("Animations du personnage",controller.animations) { controller.animations=it; controller.saveSettings() }
        Toggle("Petites balades du personnage flottant",roam) { roam=it }
        Text("La mini-compagne flotte au-dessus des applications. Crayon : ouvrir ANGE. Bulle : écrire un message. Onde audio : parler. Flèche : masquer les commandes ou fermer la mini-compagne. Touchez le personnage pour retrouver les commandes masquées. Arrêt également depuis la notification.",fontSize=12.sp,color=Muted)
        OutlinedButton(onClick={ context.startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,Uri.parse("package:${context.packageName}"))) },modifier=Modifier.fillMaxWidth()) { Text("Autoriser l’affichage flottant") }
        Button(onClick={
            if(!Settings.canDrawOverlays(context)) floatingStatus="Autorisez d’abord l’affichage flottant avec le bouton ci-dessus."
            else floatingPermissions.launch(if(Build.VERSION.SDK_INT>=33) arrayOf(Manifest.permission.RECORD_AUDIO,Manifest.permission.POST_NOTIFICATIONS) else arrayOf(Manifest.permission.RECORD_AUDIO))
        },modifier=Modifier.fillMaxWidth()) { Text(if(FloatingCompanionService.running) "Actualiser la mini-compagne" else "Activer la mini-compagne") }
        if(FloatingCompanionService.running) TextButton(onClick={ FloatingCompanionService.stop(context) }) { Text("Fermer la mini-compagne flottante") }
        if(floatingStatus.isNotBlank()) Text(floatingStatus,fontSize=12.sp,color=Lilac)
        Text("Version standard : aucun service d’accessibilité, aucune lecture de l’écran des autres applications. Retour, Récentes et leur défilement restent manuels. L’ouverture de l’accueil et des applications utilise les fonctions publiques Android. Contacts et Téléphone sont demandés seulement lors d’un appel.",fontSize=11.sp,color=Muted)
        Text("Verrouillage : droit d’administrateur Android facultatif, limité au verrouillage. Aucun effacement ni changement de mot de passe. Capture : Android vous demande un accord à chaque fois ; l’image reste locale. Wi-Fi/Bluetooth/données : validation dans le panneau Android.",fontSize=11.sp,color=Muted)
        OutlinedButton(onClick={ context.startActivity(AngeLockAdmin.request(context)) }) { Text("Autorisation Android de verrouillage") }
        TextButton(onClick={ AngeLockAdmin.manager(context).removeActiveAdmin(AngeLockAdmin.component(context)) }) { Text("Retirer le droit de verrouillage d’ANGE") }
        HorizontalDivider(color=Muted.copy(alpha=.2f)); Text("VOIX",fontSize=10.sp,color=Mint,letterSpacing=2.sp)
        Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) { listOf("fr","en","mg").forEach { lang -> FilterChip(selected=controller.language==lang,onClick={ controller.stop(); controller.language=lang; controller.selectedVoice=""; controller.saveSettings() },label={ Text(lang.uppercase()) }) } }
        Toggle("Lire les réponses",controller.autoSpeak) { controller.autoSpeak=it; controller.saveSettings() }
        Toggle("Enchaîner les tours après le prochain appui sur Parler",controller.handsFree) { if(it) controller.enableHandsFree() else controller.stop() }
        Toggle("Masquer les infos sensibles à la lecture",controller.privateVoice) { controller.privateVoice=it; controller.saveSettings() }
        Toggle("Reconnaissance réseau autorisée pour cette session",controller.allowNetworkRecognition) { controller.setNetworkRecognitionAllowed(it) }
        Toggle("Voix réseau autorisée pour cette session",controller.allowNetworkVoice) { controller.allowNetworkVoice=it }
        Text("Le service vocal peut envoyer l’audio ou le texte à son fournisseur si vous autorisez le réseau. FR local dépend des langues réellement installées.",fontSize=11.sp,color=Muted)
        var voices by remember { mutableStateOf(false) }
        OutlinedButton(onClick={ voices=true }) { Text("Choisir la voix Android") }
        if(voices) AlertDialog(onDismissRequest={ voices=false },title={ Text("Voix disponibles") },text={ LazyColumn(Modifier.heightIn(max=300.dp)) { items(controller.availableVoices.filter { it.locale.language==controller.language && (!it.isNetworkConnectionRequired || controller.allowNetworkVoice) }) { voice -> TextButton(onClick={ controller.selectedVoice=voice.name; controller.saveSettings(); voices=false }) { Text(voice.name,fontSize=12.sp) } } } },confirmButton={ TextButton(onClick={ voices=false }) { Text("Fermer") } })
        OutlinedButton(onClick={ controller.greet() }) { Text("Tester la voix") }
        OutlinedButton(onClick={ runCatching { context.startActivity(SpeechInputIntents.settings()) }.onFailure { context.startActivity(Intent(Settings.ACTION_SETTINGS)) } }) { Text("Réglages de reconnaissance") }
        TextButton(onClick={ runCatching { context.startActivity(Intent("com.android.settings.TTS_SETTINGS")) }.onFailure { context.startActivity(Intent(Settings.ACTION_SETTINGS)) } }) { Text("Réglages de lecture vocale") }
        TextButton(onClick=onAdvanced) { Text("Anciennes vues et provenance Jarvis") }
        Text("ANGE 2.3.2 mini · sans service d’accessibilité. Mini-ANGE unique, animée par poses. Pas de contrôle universel des applications, de clonage vocal ou de réentraînement automatique.",fontSize=10.sp,color=Muted)
        Spacer(Modifier.height(20.dp))
    }
    if(download) AlertDialog(onDismissRequest={ download=false },title={ Text("Installer le modèle local ?") },text={ Text("347 Mo depuis Hugging Face, vérifiés par SHA-256. Gardez ANGE ouverte pendant le transfert. Ce modèle ne garantit pas une conversation du niveau d’un grand modèle en ligne.") },confirmButton={ TextButton(onClick={ download=false; controller.downloadModel() }) { Text("Télécharger") } },dismissButton={ TextButton(onClick={ download=false }) { Text("Annuler") } })
    if(remove) AlertDialog(onDismissRequest={ remove=false },title={ Text("Supprimer le modèle ?") },text={ Text("Les commandes locales et la mini-compagne resteront disponibles. Le modèle devra être réinstallé pour discuter hors ligne.") },confirmButton={ TextButton(onClick={ remove=false; controller.removeModel() }) { Text("Supprimer") } },dismissButton={ TextButton(onClick={ remove=false }) { Text("Annuler") } })
}
