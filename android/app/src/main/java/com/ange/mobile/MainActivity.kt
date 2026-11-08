package com.ange.mobile

import android.os.Bundle
import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val Midnight = Color(0xFF050B16)
private val Navy = Color(0xFF0B1C32)
private val Cyan = Color(0xFF25C9FF)
private val Ice = Color(0xFF8BEAFF)
private val Labels = listOf("Démarrage", "Accueil", "Mobile", "Voix", "Chat IA", "Cerveau IA", "Mémoire", "Appareils", "Contrôles", "Paramètres", "Compagne")
class MainActivity : ComponentActivity() {
    private lateinit var companion: CompanionController
    private var voiceRequest by mutableIntStateOf(0)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        companion = CompanionController(this)
        setContent { SimpleAngeApp(companion,voiceRequest) }
    }
    override fun onStart() { super.onStart(); visible=true; if (::companion.isInitialized) { companion.onForeground(); FloatingCompanionService.pauseConversation(); consumeHandoff() } }
    override fun onStop() { visible=false; if (::companion.isInitialized) companion.onBackground(); super.onStop() }
    override fun onNewIntent(intent: android.content.Intent) { super.onNewIntent(intent); consumeHandoff() }
    private fun consumeHandoff() {
        val action=CompanionHandoff.action; val contacts=CompanionHandoff.contacts; val voice=CompanionHandoff.startVoice
        CompanionHandoff.clear()
        if(action!=null) companion.requestAction(action) else if(contacts.isNotEmpty()) companion.acceptContactChoices(contacts)
        if(voice) voiceRequest++
    }
    override fun onDestroy() { if (::companion.isInitialized) companion.close(); super.onDestroy() }
    companion object { var visible=false; private set }
}
@Composable
fun NativeIcon(name: String, modifier: Modifier = Modifier) {
    val asset = when(name) {
        "call" -> R.drawable.ui_call
        "camera" -> R.drawable.ui_camera
        "battery" -> R.drawable.ui_battery
        "home" -> R.drawable.ui_home
        "chat" -> R.drawable.ui_chat
        "memory" -> R.drawable.ui_memory
        "settings" -> R.drawable.ui_settings
        "control" -> R.drawable.ui_control
        "voice" -> R.drawable.ui_voice
        "link" -> R.drawable.ui_link
        "vision" -> R.drawable.ui_vision
        "history" -> R.drawable.ui_history
        else -> R.drawable.ui_grid
    }
    Image(painterResource(asset), contentDescription = null, modifier = modifier.size(26.dp))
}
@Composable
fun Orb() {
    Box(Modifier.size(240.dp).semantics { contentDescription = "Logo A orbital et noyau cyan ANGE" }, contentAlignment = Alignment.Center) {
        Image(painterResource(R.drawable.core_halo), contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
        Image(painterResource(R.drawable.ange_logo), contentDescription = null, modifier = Modifier.size(118.dp).offset(y = (-13).dp), contentScale = ContentScale.Fit)
    }
}
@Composable
private fun HomeTile(label: String, name: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    OutlinedButton(onClick = onClick, modifier = modifier.height(78.dp), shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.outlinedButtonColors(containerColor = Color(0xFF08254B)),
        contentPadding = PaddingValues(5.dp)) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(7.dp)) {
            NativeIcon(name)
            Text(label, fontSize = 11.sp, color = Color.White, maxLines = 1)
        }
    }
}
@Composable
private fun Panel(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxWidth().border(1.dp, Cyan.copy(alpha = .3f), RoundedCornerShape(16.dp))
        .background(Brush.linearGradient(listOf(Color(0xFF102B46), Navy)), RoundedCornerShape(16.dp)).padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(title, color = Ice, fontSize = 19.sp)
        content()
    }
}
@Composable
fun AngeApp(companion: CompanionController) {
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) companion.startListening() else companion.permissionDenied()
    }
    val requestMicrophone: () -> Unit = { permission.launch(Manifest.permission.RECORD_AUDIO) }
    var page by rememberSaveable { mutableIntStateOf(10) }
    var menu by remember { mutableStateOf(false) }
    var voice by rememberSaveable { mutableStateOf("Standby") }
    var message by rememberSaveable { mutableStateOf("") }
    val messages = rememberSaveable { mutableStateOf(arrayListOf<String>()) }
    val notes = rememberSaveable { mutableStateOf(arrayListOf<String>()) }
    var note by rememberSaveable { mutableStateOf("") }
    var editIndex by rememberSaveable { mutableIntStateOf(-1) }
    var language by rememberSaveable { mutableStateOf("Français") }
    var engine by rememberSaveable { mutableStateOf("Automatique") }
    var status by rememberSaveable { mutableStateOf("Aucune opération en cours.") }
    var pending by remember { mutableStateOf<String?>(null) }
    var notifications by rememberSaveable { mutableStateOf(false) }
    BackHandler(page != 1) { page = 1 }
    MaterialTheme(colorScheme = darkColorScheme(primary = Cyan, background = Midnight, surface = Navy, onSurface = Color(0xFFF4FAFF))) {
        Scaffold(containerColor = Midnight, bottomBar = {
            NavigationBar(containerColor = Navy) {
                listOf(1, 4, 6, 7, 9).forEach { index ->
                    NavigationBarItem(selected = page == index, onClick = { page = index },
                        icon = { NativeIcon(listOf("home", "chat", "memory", "link", "settings")[listOf(1,4,6,7,9).indexOf(index)]) },
                        label = { Text(if (index == 9) "Réglages" else if (index == 4) "Chat" else if (index == 7) "PC" else Labels[index], fontSize = 10.sp, maxLines = 1) })
                }
            }
        }) { padding ->
            Box(Modifier.fillMaxSize().padding(padding)) {
                Image(painterResource(R.drawable.earth_backdrop), contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop, alpha = 0.5f)
            Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal))
                .verticalScroll(rememberScrollState()).padding(18.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Column { Text("A.N.G.E 2.0", color = Ice, fontSize = 24.sp); Text("Your Intelligent Personal Companion", fontSize = 11.sp, color = Cyan) }
                    Box {
                        TextButton(onClick = { menu = true }) { Text("Menu") }
                        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                            Labels.forEachIndexed { i, title -> DropdownMenuItem(text = { Text(title) }, onClick = { page = i; menu = false }) }
                        }
                    }
                }
                Text("ANGE COMPANION · " + when(companion.mode) { "local" -> "IA LOCALE SUR PC"; "ondevice" -> "IA SUR LE TÉLÉPHONE"; else -> "MODE SIMPLE" }, color = Cyan, fontSize = 10.sp)
                when (page) {
                    0 -> Panel("Démarrage") {
                        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { Orb() }
                        Text("ANGE Mobile est prêt. Initialisation de l’interface uniquement.")
                        LinearProgressIndicator(progress = { 1f }, modifier = Modifier.fillMaxWidth())
                        Button(onClick = { page = 1 }) { Text("Entrer dans ANGE") }
                    }
                    1, 2 -> {
                        CompanionAvatar(companion)
                        Button(onClick = { page = 10 }) { Text("Converser avec ANGE") }
                        Text("Bonjour ! 👋", fontSize = 27.sp, fontWeight = FontWeight.Bold, color = Color.White, modifier = Modifier.align(Alignment.CenterHorizontally))
                        Text("Je suis ANGE, comment\npuis-je vous aider ?", fontSize = 16.sp, color = Color.White, modifier = Modifier.align(Alignment.CenterHorizontally))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                            Text("▁▃▆▂▅▇▃▁", color = Cyan, fontSize = 22.sp)
                            Button(onClick = { page = 3 }, modifier = Modifier.padding(horizontal = 10.dp).size(68.dp), shape = RoundedCornerShape(50), contentPadding = PaddingValues(12.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF007FEF))) {
                                NativeIcon("voice", Modifier.size(36.dp))
                            }
                            Text("▁▃▇▅▂▆▃▁", color = Cyan, fontSize = 22.sp)
                        }
                        listOf(Triple(4,"Chat IA","chat"),Triple(6,"Mémoire","memory"),Triple(8,"Contrôles","settings"),Triple(8,"Vision","vision"),Triple(4,"Historique","history"),Triple(5,"Plus","grid")).chunked(3).forEach { group ->
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                group.forEach { item -> HomeTile(item.second,item.third,Modifier.weight(1f)) { page = item.first } }
                            }
                        }
                        Text("PC non appairé · Prototype UI", color = Ice, fontSize = 11.sp, modifier = Modifier.align(Alignment.CenterHorizontally))
                    }
                    3, 4, 10 -> CompanionScreen(companion, requestMicrophone)
                    5 -> Panel("ANGE Brain") {
                        Orb()
                        Text("Mode choisi : ${companion.mode}\n${companion.backendStatus}\nRAM / CPU / latence : non mesurés")
                        Text("États techniques uniquement. Aucune pensée interne du modèle.")
                    }
                    6 -> Panel("Mémoire locale · base Jarvis") { JarvisMemoryScreen(companion) }
                    7 -> Panel("ANGE Link") {
                        Orb()
                        Text("PC : non connecté\nAndroid : non appairé\nWi-Fi : non vérifié")
                        Button(onClick = { status = "Code fictif ANGE-DEMO. Aucun appareil appairé." }) { Text("Simuler l’appairage") }
                        Text(status)
                    }
                    8 -> Panel("Contrôles et automatisation") {
                        Text("Aucune action système exécutée. Parcours d’autorisation simulés.")
                        listOf("Application Windows", "Rappel Android", "Lumière domotique", "Script préapprouvé").forEach { label ->
                            OutlinedButton(onClick = { pending = label }) { Text(label) }
                        }
                        Text(status)
                    }
                    9 -> Panel("Paramètres") {
                        Text("Langue préférée : $language")
                        listOf("Français", "English", "Malagasy").forEach { l -> TextButton(onClick = { language = l }) { Text(l) } }
                        Text("La sélection est une préférence UI. Traductions complètes et voix non implémentées.")
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Switch(checked = notifications, onCheckedChange = { notifications = it })
                            Text("Notifications simulées")
                        }
                        Text("Aucune permission sensible, clé API ou connexion réseau. Données conservées dans l’état UI sauvegardé, sans base persistante.")
                    }
                }
                Text("${Labels[page]} · Design 2.0 / Midnight + Neon Cyan", fontSize = 10.sp, color = Ice)
            }
            }
        }
        pending?.let { action ->
            AlertDialog(onDismissRequest = { pending = null }, title = { Text("Confirmer") },
                text = { Text(if (action.startsWith("delete:")) "Supprimer cette note de session ?" else "Autoriser la simulation « $action » ? Aucune commande réelle.") },
                confirmButton = { TextButton(onClick = {
                    if (action.startsWith("delete:")) {
                        val i = action.substringAfter(":").toInt()
                        notes.value = ArrayList(notes.value.filterIndexed { index, _ -> index != i })
                        editIndex = -1; note = ""
                    } else status = "Simulation autorisée : $action. Aucune action exécutée."
                    pending = null
                }) { Text("Confirmer") } }, dismissButton = { TextButton(onClick = { pending = null; status = "Demande annulée." }) { Text("Annuler") } })
        }
    }
}
