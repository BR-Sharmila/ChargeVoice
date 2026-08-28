package com.example.chargevoice

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import android.os.IBinder
import android.speech.tts.TextToSpeech
import java.util.Locale

class ChargingService : Service() {

    private var tts: TextToSpeech? = null
    private var ttsReady = false

    private var pendingMessage: String? = null
    private var pendingLocale: Locale? = null

    private var isCharging = false
    private var fullBatteryAnnounced = false
    private var lowBatteryAnnounced = false
    private var lastSpokenPercentage = -1

    companion object {

        private const val CHANNEL_ID =
            "charge_voice_channel"

        private const val NOTIFICATION_ID =
            1001

        private const val LOW_BATTERY_LEVEL =
            20
    }

    private val chargingReceiver =
        object : BroadcastReceiver() {

            override fun onReceive(
                context: Context?,
                intent: Intent?
            ) {

                when (intent?.action) {

                    Intent.ACTION_POWER_CONNECTED -> {

                        isCharging = true
                        fullBatteryAnnounced = false
                        lastSpokenPercentage = -1

                        speakForSelectedLanguage(
                            "charging"
                        )
                    }

                    Intent.ACTION_POWER_DISCONNECTED -> {

                        isCharging = false

                        speakForSelectedLanguage(
                            "disconnected"
                        )
                    }

                    Intent.ACTION_BATTERY_CHANGED -> {

                        val level =
                            intent.getIntExtra(
                                BatteryManager.EXTRA_LEVEL,
                                -1
                            )

                        val scale =
                            intent.getIntExtra(
                                BatteryManager.EXTRA_SCALE,
                                -1
                            )

                        if (
                            level >= 0 &&
                            scale > 0
                        ) {

                            val percentage =
                                (level * 100) / scale

                            val status =
                                intent.getIntExtra(
                                    BatteryManager.EXTRA_STATUS,
                                    -1
                                )

                            val charging =
                                status ==
                                        BatteryManager.BATTERY_STATUS_CHARGING ||
                                        status ==
                                        BatteryManager.BATTERY_STATUS_FULL

                            isCharging =
                                charging

                            if (
                                percentage >= 100 &&
                                charging &&
                                !fullBatteryAnnounced
                            ) {

                                fullBatteryAnnounced =
                                    true

                                speakForSelectedLanguage(
                                    "full"
                                )
                            }

                            if (
                                percentage <= LOW_BATTERY_LEVEL &&
                                !charging &&
                                !lowBatteryAnnounced
                            ) {

                                lowBatteryAnnounced =
                                    true

                                speakForSelectedLanguage(
                                    "low"
                                )
                            }

                            if (
                                percentage >
                                LOW_BATTERY_LEVEL
                            ) {

                                lowBatteryAnnounced =
                                    false
                            }

                            if (
                                charging &&
                                percentage < 100
                            ) {

                                if (
                                    percentage == 25 ||
                                    percentage == 50 ||
                                    percentage == 75 ||
                                    percentage == 90
                                ) {

                                    if (
                                        percentage !=
                                        lastSpokenPercentage
                                    ) {

                                        lastSpokenPercentage =
                                            percentage

                                        speakBatteryPercentage(
                                            percentage
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

    override fun onCreate() {

        super.onCreate()

        createNotificationChannel()

        startForeground(
            NOTIFICATION_ID,
            createNotification()
        )

        initializeTTS()

        registerChargingReceiver()

        checkCurrentChargingState()
    }

    private fun initializeTTS() {

        tts =
            TextToSpeech(
                applicationContext
            ) { status ->

                if (
                    status ==
                    TextToSpeech.SUCCESS
                ) {

                    ttsReady = true

                    val message =
                        pendingMessage

                    val locale =
                        pendingLocale

                    if (
                        message != null &&
                        locale != null
                    ) {

                        speakNow(
                            message,
                            locale
                        )

                        pendingMessage =
                            null

                        pendingLocale =
                            null
                    }
                }
            }
    }

    private fun getSelectedLanguage(): String {

        val preferences =
            getSharedPreferences(
                "ChargeVoice",
                MODE_PRIVATE
            )

        return preferences.getString(
            "language",
            "English"
        ) ?: "English"
    }

    private fun speakForSelectedLanguage(
        type: String
    ) {

        when (
            getSelectedLanguage()
        ) {

            "Kannada" -> {

                val message =
                    when (type) {

                        "charging" ->
                            "ನಿಮ್ಮ ಫೋನ್ ಚಾರ್ಜ್ ಆಗುತ್ತಿದೆ."

                        "disconnected" ->
                            "ನಿಮ್ಮ ಫೋನ್ ಚಾರ್ಜರ್‌ನಿಂದ ಸಂಪರ್ಕ ಕಡಿತಗೊಂಡಿದೆ."

                        "full" ->
                            "ನಿಮ್ಮ ಫೋನ್ ಸಂಪೂರ್ಣವಾಗಿ ಚಾರ್ಜ್ ಆಗಿದೆ."

                        "low" ->
                            "ಬ್ಯಾಟರಿ ಕಡಿಮೆಯಾಗಿದೆ. ದಯವಿಟ್ಟು ನಿಮ್ಮ ಫೋನ್ ಚಾರ್ಜ್ ಮಾಡಿ."

                        else ->
                            ""
                    }

                speakWhenReady(
                    message,
                    Locale("kn", "IN")
                )
            }

            "Hindi" -> {

                val message =
                    when (type) {

                        "charging" ->
                            "आपका फोन चार्ज हो रहा है।"

                        "disconnected" ->
                            "आपका फोन चार्जर से डिस्कनेक्ट हो गया है।"

                        "full" ->
                            "आपका फोन पूरी तरह चार्ज हो गया है।"

                        "low" ->
                            "बैटरी कम है। कृपया अपना फोन चार्ज करें।"

                        else ->
                            ""
                    }

                speakWhenReady(
                    message,
                    Locale("hi", "IN")
                )
            }

            else -> {

                val message =
                    when (type) {

                        "charging" ->
                            "Your phone is now charging."

                        "disconnected" ->
                            "Charger disconnected."

                        "full" ->
                            "Your phone is fully charged."

                        "low" ->
                            "Battery is low. Please charge your phone."

                        else ->
                            ""
                    }

                speakWhenReady(
                    message,
                    Locale.ENGLISH
                )
            }
        }
    }

    private fun speakBatteryPercentage(
        percentage: Int
    ) {

        when (
            getSelectedLanguage()
        ) {

            "Kannada" -> {

                speakWhenReady(
                    "ಬ್ಯಾಟರಿ ಶೇಕಡಾ $percentage ರಷ್ಟು ಇದೆ.",
                    Locale("kn", "IN")
                )
            }

            "Hindi" -> {

                speakWhenReady(
                    "बैटरी $percentage प्रतिशत है।",
                    Locale("hi", "IN")
                )
            }

            else -> {

                speakWhenReady(
                    "Battery is $percentage percent.",
                    Locale.ENGLISH
                )
            }
        }
    }

    private fun speakWhenReady(
        message: String,
        locale: Locale
    ) {

        if (
            message.isEmpty()
        ) {
            return
        }

        if (
            ttsReady &&
            tts != null
        ) {

            speakNow(
                message,
                locale
            )

        } else {

            pendingMessage =
                message

            pendingLocale =
                locale
        }
    }

    private fun speakNow(
        message: String,
        locale: Locale
    ) {

        val result =
            tts?.setLanguage(
                locale
            )

        if (
            result !=
            TextToSpeech.LANG_MISSING_DATA &&
            result !=
            TextToSpeech.LANG_NOT_SUPPORTED
        ) {

            tts?.speak(
                message,
                TextToSpeech.QUEUE_FLUSH,
                null,
                "charge_voice"
            )
        }
    }

    private fun registerChargingReceiver() {

        val filter =
            IntentFilter().apply {

                addAction(
                    Intent.ACTION_POWER_CONNECTED
                )

                addAction(
                    Intent.ACTION_POWER_DISCONNECTED
                )

                addAction(
                    Intent.ACTION_BATTERY_CHANGED
                )
            }

        if (
            Build.VERSION.SDK_INT >=
            Build.VERSION_CODES.TIRAMISU
        ) {

            registerReceiver(
                chargingReceiver,
                filter,
                Context.RECEIVER_NOT_EXPORTED
            )

        } else {

            @Suppress("DEPRECATION")
            registerReceiver(
                chargingReceiver,
                filter
            )
        }
    }

    private fun checkCurrentChargingState() {

        val batteryManager =
            getSystemService(
                BATTERY_SERVICE
            ) as BatteryManager

        isCharging =
            batteryManager.isCharging
    }

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int
    ): Int {

        return START_STICKY
    }

    private fun createNotificationChannel() {

        if (
            Build.VERSION.SDK_INT >=
            Build.VERSION_CODES.O
        ) {

            val channel =
                NotificationChannel(
                    CHANNEL_ID,
                    "ChargeVoice",
                    NotificationManager.IMPORTANCE_LOW
                )

            channel.description =
                "Monitors phone charging status"

            val manager =
                getSystemService(
                    NotificationManager::class.java
                )

            manager.createNotificationChannel(
                channel
            )
        }
    }

    private fun createNotification(): Notification {

        return if (
            Build.VERSION.SDK_INT >=
            Build.VERSION_CODES.O
        ) {

            Notification.Builder(
                this,
                CHANNEL_ID
            )
                .setContentTitle(
                    "ChargeVoice is active"
                )
                .setContentText(
                    "Monitoring charging status"
                )
                .setSmallIcon(
                    android.R.drawable.ic_lock_idle_charging
                )
                .setOngoing(true)
                .build()

        } else {

            @Suppress("DEPRECATION")
            Notification.Builder(this)
                .setContentTitle(
                    "ChargeVoice is active"
                )
                .setContentText(
                    "Monitoring charging status"
                )
                .setSmallIcon(
                    android.R.drawable.ic_lock_idle_charging
                )
                .setOngoing(true)
                .build()
        }
    }

    override fun onDestroy() {

        try {

            unregisterReceiver(
                chargingReceiver
            )

        } catch (
            e: Exception
        ) {
        }

        tts?.stop()
        tts?.shutdown()

        tts = null
        ttsReady = false

        super.onDestroy()
    }

    override fun onBind(
        intent: Intent?
    ): IBinder? {

        return null
    }
}