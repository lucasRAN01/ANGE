package com.ange.mobile

import android.content.Intent
import android.provider.Settings
import android.speech.RecognizerIntent

internal object SpeechInputIntents {
    fun recognize(language: String, local: Boolean) = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        putExtra(RecognizerIntent.EXTRA_LANGUAGE, SpeechInputPolicy.languageTag(language))
        putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, local)
    }

    fun settings() = Intent(Settings.ACTION_VOICE_INPUT_SETTINGS)
}
