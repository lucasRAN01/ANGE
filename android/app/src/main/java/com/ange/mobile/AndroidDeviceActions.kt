package com.ange.mobile

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.net.Uri
import android.provider.AlarmClock
import android.provider.CalendarContract
import android.provider.MediaStore
import android.provider.Settings

class AndroidDeviceActions(private val context: Context) {
    @Suppress("DEPRECATION")
    fun apps(): List<LaunchableApp> = context.packageManager.queryIntentActivities(
        Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER),0)
        .map { LaunchableApp(it.activityInfo.packageName,it.loadLabel(context.packageManager).toString()) }
        .filter { it.packageName != context.packageName }.distinctBy { it.packageName }
        .sortedBy { DeviceActionPolicy.normalized(it.label) }

    fun intentFor(action: DeviceAction): Intent? = when(action) {
        is DeviceAction.OpenApp -> context.packageManager.getLaunchIntentForPackage(action.app.packageName)
            ?: throw ActivityNotFoundException("Application non disponible.")
        is DeviceAction.Dial -> Intent(Intent.ACTION_DIAL,Uri.fromParts("tel",action.number,null))
        is DeviceAction.CallNumber -> Intent(Intent.ACTION_CALL,Uri.fromParts("tel",action.number,null))
        is DeviceAction.CallContact -> null
        is DeviceAction.Navigate -> if(action.direction=="accueil") Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME) else null
        is DeviceAction.Sms -> Intent(Intent.ACTION_SENDTO,Uri.fromParts("smsto",action.number,null)).putExtra("sms_body",action.body)
        is DeviceAction.Map -> Intent(Intent.ACTION_VIEW,Uri.parse("https://www.google.com/maps/dir/").buildUpon().appendQueryParameter("api","1").appendQueryParameter("destination",action.query).build())
        is DeviceAction.Search -> Intent(Intent.ACTION_VIEW,Uri.parse("https://www.google.com/search").buildUpon().appendQueryParameter("q",action.query).build())
        is DeviceAction.Website -> Intent(Intent.ACTION_VIEW,Uri.parse(action.url))
        is DeviceAction.Alarm -> Intent(AlarmClock.ACTION_SET_ALARM).putExtra(AlarmClock.EXTRA_HOUR,action.hour)
            .putExtra(AlarmClock.EXTRA_MINUTES,action.minute).putExtra(AlarmClock.EXTRA_MESSAGE,"ANGE").putExtra(AlarmClock.EXTRA_SKIP_UI,false)
        is DeviceAction.Timer -> Intent(AlarmClock.ACTION_SET_TIMER).putExtra(AlarmClock.EXTRA_LENGTH,action.seconds)
            .putExtra(AlarmClock.EXTRA_MESSAGE,"ANGE").putExtra(AlarmClock.EXTRA_SKIP_UI,false)
        is DeviceAction.Calendar -> Intent(Intent.ACTION_INSERT,CalendarContract.Events.CONTENT_URI).putExtra(CalendarContract.Events.TITLE,action.event)
        is DeviceAction.Settings -> Intent(when(action.kind) { "Wi-Fi" -> Settings.ACTION_WIFI_SETTINGS; "Bluetooth" -> Settings.ACTION_BLUETOOTH_SETTINGS; "son" -> Settings.ACTION_SOUND_SETTINGS; else -> Settings.ACTION_SETTINGS })
        DeviceAction.Camera -> Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA)
        is DeviceAction.Volume -> null
    }
    fun execute(action: DeviceAction): String {
        if (action is DeviceAction.Navigate) {
            require(action.direction=="accueil") { "Cette version standard ne contrôle pas les boutons Retour, Récentes ou le défilement des autres applications. Utilisez les commandes Android à l’écran." }
        }
        if (action is DeviceAction.CallNumber) require(context.checkSelfPermission(android.Manifest.permission.CALL_PHONE) == android.content.pm.PackageManager.PERMISSION_GRANTED) { "Autorisation Téléphone nécessaire pour lancer l’appel." }
        if(action is DeviceAction.Volume) {
            val audio=context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            require(!audio.isVolumeFixed) { "Le volume est fixe sur cet appareil." }
            val max=audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
            audio.setStreamVolume(AudioManager.STREAM_MUSIC,(max*action.percent/100f).toInt(),AudioManager.FLAG_SHOW_UI)
            return "Volume multimédia demandé : ${action.percent} %."
        }
        val intent=intentFor(action) ?: error("Action indisponible.")
        if(context !is Activity) intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try { context.startActivity(intent) } catch (_: ActivityNotFoundException) {
            if(action is DeviceAction.Map) {
                context.startActivity(Intent(Intent.ACTION_VIEW,Uri.parse("https://www.google.com/maps/search/").buildUpon().appendQueryParameter("api","1").appendQueryParameter("query",action.query).build()).apply {
                    if(context !is Activity) addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                })
            } else throw IllegalStateException("Aucune application installée ne prend en charge cette action.")
        }
        return when(action) {
            is DeviceAction.Sms -> "Brouillon demandé à l’application SMS. Aucun message envoyé par ANGE."
            is DeviceAction.Dial -> "Numéro préparé dans Téléphone. Appuyez vous-même sur Appeler."
            is DeviceAction.CallNumber -> "Appel demandé à Téléphone : ${action.label}."
            is DeviceAction.Navigate -> "Ouverture de l’accueil demandée à Android."
            is DeviceAction.Alarm, is DeviceAction.Timer -> "Demande transmise à l’Horloge. Vérifiez l’heure ou la durée dans cette application."
            is DeviceAction.Calendar -> "Événement proposé à l’Agenda. Choisissez la date puis enregistrez-le."
            else -> "Ouverture demandée : ${action.title}."
        }
    }
}
