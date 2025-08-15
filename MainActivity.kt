package com.sakuraai

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import com.sakuraai.databinding.ActivityMainBinding
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import org.json.JSONObject
import java.util.*

class MainActivity : ComponentActivity(), TextToSpeech.OnInitListener {

    private lateinit var binding: ActivityMainBinding
    private var tts: TextToSpeech? = null
    private val client = OkHttpClient()

    private val notifPerm = registerForActivityResult(ActivityResultContracts.RequestPermission()) { }
    private val speechLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val data = result.data
        val text = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
        if (!text.isNullOrBlank()) {
            binding.lastHeard.text = text
            speak("ठीक है, मैं कर रही हूँ।")
            queryBackend(text)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        tts = TextToSpeech(this, this)

        binding.micButton.setOnClickListener { startSpeechOnce() }
        binding.sayHiButton.setOnClickListener { speak("Namaste! मैं Sakura हूँ। How can I help?") }
        binding.wakeSwitch.setOnCheckedChangeListener { _, checked ->
            if (checked) startWakeWord() else stopWakeWord()
        }

        ensurePermissions()
        ensureOverlayPermission()
        if (Build.VERSION.SDK_INT >= 33) {
            notifPerm.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    private fun ensurePermissions() {
        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), 100)
    }

    private fun ensureOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(this)) {
                val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
                startActivity(intent)
            }
        }
    }

    private fun startSpeechOnce() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "hi-IN,en-IN")
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak to Sakura")
        }
        try {
            speechLauncher.launch(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "Speech not available: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun queryBackend(prompt: String) {
        Thread {
            try {
                val json = JSONObject().put("prompt", prompt).toString()
                val body = RequestBody.create("application/json".toMediaType(), json)
                val req = Request.Builder()
                    .url("http://127.0.0.1:3000/api/chat")
                    .post(body)
                    .build()
                client.newCall(req).execute().use { res ->
                    val out = res.body?.string() ?: "{}"
                    runOnUiThread {
                        binding.responseText.text = out
                        speak(JSONObject(out).optString("reply", "Done"))
                    }
                }
            } catch (e: Exception) {
                runOnUiThread { Toast.makeText(this, "Backend error: ${e.message}", Toast.LENGTH_LONG).show() }
            }
        }.start()
    }

    private fun startWakeWord() {
        val i = Intent(this, WakeWordService::class.java)
        if (Build.VERSION.SDK_INT >= 26) {
            startForegroundService(i)
        } else {
            startService(i)
        }
        Toast.makeText(this, "Wake word listening started", Toast.LENGTH_SHORT).show()
    }

    private fun stopWakeWord() {
        val i = Intent(this, WakeWordService::class.java)
        stopService(i)
        Toast.makeText(this, "Wake word listening stopped", Toast.LENGTH_SHORT).show()
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val hindi = Locale("hi", "IN")
            val result = tts?.setLanguage(hindi)
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                tts?.language = Locale.UK
            }
        }
    }

    private fun speak(text: String) {
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "sakura-tts")
    }

    override fun onDestroy() {
        tts?.stop()
        tts?.shutdown()
        super.onDestroy()
    }
}