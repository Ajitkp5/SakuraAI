package com.sakuraai

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log

class WakeWordService : Service(), RecognitionListener {

    private var recognizer: SpeechRecognizer? = null
    private val handler = Handler()
    private val TAG = "SakuraWake"

    override fun onCreate() {
        super.onCreate()
        startForeground(1001, buildNotification("Listening for 'Sakura'…"))
        setupRecognizer()
        startListening()
    }

    private fun buildNotification(text: String): Notification {
        val channelId = "sakura_hotword"
        val mgr = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val ch = NotificationChannel(channelId, "Sakura Hotword", NotificationManager.IMPORTANCE_MIN)
            mgr.createNotificationChannel(ch)
        }
        val intent = Intent(this, MainActivity::class.java)
        val pi = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, channelId)
        } else {
            Notification.Builder(this)
        }
        return builder
            .setContentTitle("Sakura AI")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentIntent(pi)
            .setOngoing(true)
            .build()
    }

    private fun setupRecognizer() {
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            Log.e(TAG, "Speech recognition not available")
            stopSelf()
            return
        }
        recognizer?.destroy()
        recognizer = SpeechRecognizer.createSpeechRecognizer(this).also {
            it.setRecognitionListener(this)
        }
    }

    private fun startListening() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, false)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "hi-IN,en-IN")
        }
        try {
            recognizer?.startListening(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to startListening: ${e.message}")
            handler.postDelayed({ startListening() }, 1000)
        }
    }

    private fun restart() {
        handler.postDelayed({
            setupRecognizer()
            startListening()
        }, 500)
    }

    override fun onReadyForSpeech(params: Bundle?) {}
    override fun onBeginningOfSpeech() {}
    override fun onRmsChanged(rmsdB: Float) {}
    override fun onBufferReceived(buffer: ByteArray?) {}
    override fun onEndOfSpeech() { restart() }

    override fun onError(error: Int) {
        Log.w(TAG, "Recognizer error: " + error)
        restart()
    }

    override fun onPartialResults(partialResults: Bundle?) {
        val list = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION) ?: return
        for (s in list) {
            if (heardWake(s)) {
                launchMain()
                handler.postDelayed({ startListening() }, 1200)
                return
            }
        }
    }

    override fun onResults(results: Bundle?) {
        val list = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION) ?: return
        for (s in list) {
            if (heardWake(s)) {
                launchMain()
                break
            }
        }
        restart()
    }

    private fun heardWake(text: String): Boolean {
        val t = text.lowercase().trim()
        return t.contains("sakura") or t.contains("साकुरा") or t.contains("सकुरा")
    }

    private fun launchMain() {
        val i = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            putExtra("wake", true)
        }
        startActivity(i)
    }

    override fun onBind(intent: Intent?) = null

    override fun onDestroy() {
        recognizer?.stopListening()
        recognizer?.cancel()
        recognizer?.destroy()
        super.onDestroy()
    }
}