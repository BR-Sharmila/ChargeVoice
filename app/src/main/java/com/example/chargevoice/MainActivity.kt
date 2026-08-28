package com.example.chargevoice

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.speech.tts.TextToSpeech
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.content.ContextCompat
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.chargevoice.ui.theme.ChargeVoiceTheme
import java.util.Locale

class MainActivity : ComponentActivity() {

    private var textToSpeech: TextToSpeech? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val preferences =
            getSharedPreferences(
                "ChargeVoice",
                MODE_PRIVATE
            )

        val savedLanguage =
            preferences.getString(
                "language",
                "English"
            ) ?: "English"

        textToSpeech =
            TextToSpeech(this) { status ->

                if (status == TextToSpeech.SUCCESS) {
                    textToSpeech?.setLanguage(Locale.ENGLISH)
                }
            }

        if (
            Build.VERSION.SDK_INT >=
            Build.VERSION_CODES.TIRAMISU
        ) {

            if (
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {

                requestPermissions(
                    arrayOf(
                        Manifest.permission.POST_NOTIFICATIONS
                    ),
                    100
                )
            }
        }

        startChargeVoiceService()

        setContent {

            ChargeVoiceTheme {

                ChargeVoiceScreen(
                    savedLanguage = savedLanguage,

                    onSaveLanguage = { language ->

                        preferences.edit()
                            .putString(
                                "language",
                                language
                            )
                            .apply()
                    },

                    onTestVoice = { language ->
                        speak(language)
                    }
                )
            }
        }
    }

    private fun startChargeVoiceService() {

        val serviceIntent =
            Intent(
                this,
                ChargingService::class.java
            )

        ContextCompat.startForegroundService(
            this,
            serviceIntent
        )
    }

    private fun speak(language: String) {

        val locale: Locale
        val message: String

        when (language) {

            "Kannada" -> {

                locale = Locale("kn", "IN")

                message =
                    "ನಿಮ್ಮ ಫೋನ್ ಚಾರ್ಜ್ ಆಗುತ್ತಿದೆ."
            }

            "Hindi" -> {

                locale = Locale("hi", "IN")

                message =
                    "आपका फोन चार्ज हो रहा है।"
            }

            else -> {

                locale = Locale.ENGLISH

                message =
                    "Your phone is charging."
            }
        }

        val result =
            textToSpeech?.setLanguage(locale)

        if (
            result != TextToSpeech.LANG_MISSING_DATA &&
            result != TextToSpeech.LANG_NOT_SUPPORTED
        ) {

            textToSpeech?.speak(
                message,
                TextToSpeech.QUEUE_FLUSH,
                null,
                "test_voice"
            )
        }
    }

    override fun onDestroy() {

        textToSpeech?.stop()
        textToSpeech?.shutdown()
        textToSpeech = null

        super.onDestroy()
    }
}

@Composable
fun ChargeVoiceScreen(
    savedLanguage: String,
    onSaveLanguage: (String) -> Unit,
    onTestVoice: (String) -> Unit
) {

    var selectedLanguage by remember {
        mutableStateOf(savedLanguage)
    }

    var savedMessage by remember {
        mutableStateOf("")
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(30.dp),

        horizontalAlignment =
            Alignment.CenterHorizontally,

        verticalArrangement =
            Arrangement.Center
    ) {

        Text(
            text = "🔋 Charge Voice"
        )

        Spacer(
            modifier = Modifier.height(20.dp)
        )

        Text(
            text = "Select Language"
        )

        Spacer(
            modifier = Modifier.height(10.dp)
        )

        Row(
            verticalAlignment =
                Alignment.CenterVertically
        ) {

            RadioButton(
                selected =
                    selectedLanguage == "Kannada",

                onClick = {
                    selectedLanguage = "Kannada"
                }
            )

            Text(
                text = "ಕನ್ನಡ"
            )
        }

        Row(
            verticalAlignment =
                Alignment.CenterVertically
        ) {

            RadioButton(
                selected =
                    selectedLanguage == "Hindi",

                onClick = {
                    selectedLanguage = "Hindi"
                }
            )

            Text(
                text = "हिन्दी"
            )
        }

        Row(
            verticalAlignment =
                Alignment.CenterVertically
        ) {

            RadioButton(
                selected =
                    selectedLanguage == "English",

                onClick = {
                    selectedLanguage = "English"
                }
            )

            Text(
                text = "English"
            )
        }

        Spacer(
            modifier = Modifier.height(20.dp)
        )

        Button(
            onClick = {

                onSaveLanguage(
                    selectedLanguage
                )

                savedMessage =
                    "Language saved successfully!"
            }
        ) {

            Text(
                text = "SAVE"
            )
        }

        Spacer(
            modifier = Modifier.height(10.dp)
        )

        Button(
            onClick = {

                onTestVoice(
                    selectedLanguage
                )
            }
        ) {

            Text(
                text = "🔊 TEST VOICE"
            )
        }

        Spacer(
            modifier = Modifier.height(15.dp)
        )

        Text(
            text =
                "Selected: $selectedLanguage"
        )

        Spacer(
            modifier = Modifier.height(10.dp)
        )

        Text(
            text = savedMessage
        )
    }
}