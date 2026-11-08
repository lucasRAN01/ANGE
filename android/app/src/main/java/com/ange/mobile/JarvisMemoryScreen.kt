package com.ange.mobile

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun JarvisMemoryScreen(controller: CompanionController) {
    var category by remember { mutableStateOf("facts") }
    var key by remember { mutableStateOf("") }
    var text by remember { mutableStateOf("") }
    var query by remember { mutableStateOf("") }
    var forget by remember { mutableStateOf("") }
    var confirmation by remember { mutableStateOf<String?>(null) }
    val labels=mapOf("facts" to "Faits","preferences" to "Préférences","people" to "Proches","projects" to "Projets")
    LaunchedEffect(Unit) { controller.refreshMemory() }
    Column(verticalArrangement=Arrangement.spacedBy(10.dp)) {
        Text("Mémoire persistante issue de Jarvis. Ajouts volontaires uniquement ; aucun souvenir n’est envoyé automatiquement à l’IA, au PC ou au cloud.",fontSize=12.sp)
        Text(controller.memoryStatus,fontSize=12.sp)
        Row(horizontalArrangement=Arrangement.spacedBy(4.dp)) {
            labels.forEach { (id,label) -> FilterChip(selected=category==id,onClick={ category=id },label={ Text(label,fontSize=10.sp) }) }
        }
        if(category!="facts") OutlinedTextField(value=key,onValueChange={ key=it.take(80) },label={ Text("Étiquette : projet, sujet ou prénom") },modifier=Modifier.fillMaxWidth())
        OutlinedTextField(value=text,onValueChange={ text=it.take(4000) },label={ Text("Souvenir à conserver") },modifier=Modifier.fillMaxWidth(),maxLines=5)
        Button(onClick={ controller.rememberMemory(category,text,key) },enabled=text.isNotBlank()) { Text("Mémoriser localement") }
        OutlinedTextField(value=query,onValueChange={ query=it.take(200) },label={ Text("Rechercher dans les souvenirs") },modifier=Modifier.fillMaxWidth())
        val entries=controller.memoryEntries.filter { JarvisVoice.withoutAccents(it.key+" "+it.text).contains(JarvisVoice.withoutAccents(query)) }
        Text("${entries.size} souvenir(s)",fontSize=12.sp)
        entries.forEach { item ->
            Text((labels[item.category] ?: item.category)+(if(item.key.isBlank()) "" else " · "+item.key)+"\n"+item.text,fontSize=13.sp)
        }
        HorizontalDivider()
        OutlinedTextField(value=forget,onValueChange={ forget=it.take(200) },label={ Text("Sujet à oublier") },modifier=Modifier.fillMaxWidth())
        OutlinedButton(onClick={ confirmation="forget" },enabled=forget.isNotBlank()) { Text("Oublier ce sujet…") }
        TextButton(onClick={ confirmation="erase" }) { Text("Effacer toute la mémoire et sa copie…") }
        Text("Stockage privé Android, sans chiffrement supplémentaire. Un oubli conserve une copie de récupération ; l’effacement total retire aussi cette copie. La mémoire du PC reste indépendante. Ne stockez pas de mots de passe ou de clés.",fontSize=11.sp)
    }
    confirmation?.let { action -> AlertDialog(onDismissRequest={ confirmation=null },title={ Text(if(action=="erase") "Effacer la mémoire locale ?" else "Oublier ce sujet ?") },
        text={ Text(if(action=="erase") "Tous les souvenirs et leur copie de récupération seront supprimés sur ce téléphone." else "Les souvenirs contenant « $forget » seront retirés. Une copie locale de récupération sera conservée.") },
        confirmButton={ TextButton(onClick={ if(action=="erase") controller.eraseMemory() else controller.forgetMemory(forget); confirmation=null }) { Text("Confirmer") } },
        dismissButton={ TextButton(onClick={ confirmation=null }) { Text("Annuler") } }) }
}
