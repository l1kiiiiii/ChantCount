package com.example.baselication

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import android.view.View
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.util.Locale

class MainActivity : AppCompatActivity() {

    // Speech recognition variables
    private var speechRecognizer: SpeechRecognizer? = null
    private var isListening = false

    // UI elements
    private lateinit var btnStartStop: Button
    private lateinit var targetSentenceView: EditText
    private lateinit var matchCountView: TextView
    private lateinit var matchLimitView: EditText
    private lateinit var recognizedTextView: TextView
    private lateinit var mantraSpinner: Spinner

    // App logic variables
    private var matchCount = 0
    private var matchLimit = 0
    private var targetSentence = ""
    private var currentWordIndex = 0

    // Permission request code
    private val recordAudioPermissionRequestCode = 1
    private var fullRecognizedText = ""
    private var lastPartialText = ""


    // Mantra list
    private val mantras = listOf(
        "Om Namah Shivaya",
        "Hare Krishna Hare Rama",
        "Gayatri Mantra", "hello",
        "Om Shanti Shanti Shanti",
        "Sarve Bhavantu Sukhinah",
        "Asatoma Sadgamaya"
    )

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        // Initialize UI elements
        btnStartStop = findViewById(R.id.btnstartstop)
        targetSentenceView = findViewById(R.id.myEditText)
        matchCountView = findViewById(R.id.matchcount)
        matchLimitView = findViewById(R.id.matchlimit)
        recognizedTextView = findViewById(R.id.recognizedText)
        mantraSpinner = findViewById(R.id.mantraSpinner)

        // Setup spinner
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, mantras)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        mantraSpinner.adapter = adapter

        mantraSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>,
                view: View?,
                position: Int,
                id: Long
            ) {
                targetSentence = mantras[position]
                targetSentenceView.setText(targetSentence)
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        // Check permission and initialize
        Log.d("MainActivity", "onCreate: Calling checkPermissionAndStart()")
        checkPermissionAndStart()

        btnStartStop.setOnClickListener {
            if (isListening) {
                stopListening()
            } else {
                // Update targetSentence and matchLimit from EditTexts
                val newTargetSentence = targetSentenceView.text.toString().trim()
                if (newTargetSentence.isNotEmpty()) {
                    targetSentence = newTargetSentence
                } else {
                    Toast.makeText(
                        this,
                        "Please enter a target sentence.",
                        Toast.LENGTH_SHORT
                    ).show()
                    return@setOnClickListener
                }

                try {
                    val newMatchLimit = matchLimitView.text.toString().toInt()
                    if (newMatchLimit > 0) {
                        matchLimit = newMatchLimit
                    } else {
                        Toast.makeText(
                            this,
                            "Please enter a valid match limit.",
                            Toast.LENGTH_SHORT
                        ).show()
                        return@setOnClickListener
                    }
                } catch (e: NumberFormatException) {
                    Toast.makeText(
                        this,
                        "Please enter a valid number for match limit.",
                        Toast.LENGTH_SHORT
                    ).show()
                    return@setOnClickListener
                }

                resetState()
                isListening = true                     //  This line is critical
                btnStartStop.text = "STOP"             //  Update button UI
                startListeningWithDelay()              //  Actually start speech recognition
            }
        }

    }

    @SuppressLint("SetTextI18n")
    private fun resetState() {
        matchCount = 0
        currentWordIndex = 0
        matchCountView.text = "0"
        recognizedTextView.text = ""
        fullRecognizedText = ""
        lastPartialText = ""
    }



    // Permission check
    private fun checkPermissionAndStart() {
        if (ContextCompat.checkSelfPermission(
                this, Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.RECORD_AUDIO),
                recordAudioPermissionRequestCode
            )
        } else {
            createSpeechRecognizer()
        }
    }

    // Permission result handling
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == recordAudioPermissionRequestCode && grantResults.isNotEmpty() &&
            grantResults[0] == PackageManager.PERMISSION_GRANTED
        ) {
            createSpeechRecognizer()
        } else {
            showPermissionAlert()
        }
    }

    // Alert for denied permissions
    private fun showPermissionAlert() {
        AlertDialog.Builder(this)
            .setTitle("Permission Required")
            .setMessage("Go to Settings > Permissions > Enable Microphone")
            .setPositiveButton("OK") { _, _ -> }
            .show()
    }

    // Create the speech recognizer
    private fun createSpeechRecognizer() {
        if (speechRecognizer == null && SpeechRecognizer.isRecognitionAvailable(this)) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
            speechRecognizer?.setRecognitionListener(object : RecognitionListener {

                // Called when the recognizer is ready to start listening
                override fun onReadyForSpeech(params: Bundle?) {
                    // Optional: Log or update UI
                }

                override fun onBeginningOfSpeech() {
                    lastPartialText = ""
                }

                override fun onRmsChanged(rmsdB: Float) {
                    // Optional: Show mic level
                }

                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() {
                    // Optional: Notify end of speech
                }

                // Handle recognition errors
                override fun onError(error: Int) {
                    when (error) {
                        SpeechRecognizer.ERROR_NO_MATCH -> {
                            if (isListening) {
                                startListeningWithDelay()
                            }
                        }

                        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> {
                            if (isListening) {
                                startListeningWithDelay()
                            }
                        }

                        SpeechRecognizer.ERROR_CLIENT -> {
                            if (isListening) {
                                stopAndDestroyRecognizer()
                                createSpeechRecognizer()
                                startListeningWithDelay()
                            }
                        }

                        else -> {
                            stopAndDestroyRecognizer()
                            Toast.makeText(
                                applicationContext,
                                "Recognition Error: $error",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }

                // Final recognition result
                override fun onResults(results: Bundle?) {
                    val matches =
                        results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    matches?.let {
                        val combined = matches.joinToString(" ")
                        fullRecognizedText += "$combined "
                        recognizedTextView.text = fullRecognizedText.trim()
                        processAndCountWords(ArrayList(listOf(combined)))
                        if (isListening && matchCount < matchLimit) {
                            startListeningWithDelay()
                        }
                    }
                }

                // Partial results (live transcription)
                override fun onPartialResults(partialResults: Bundle?) {
                    val partialMatches =
                        partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    partialMatches?.let {
                        lastPartialText = it.joinToString(" ").trim()
                        recognizedTextView.text = lastPartialText
                    }
                }

                override fun onEvent(eventType: Int, params: Bundle?) {}
            })
        }
    }


    private fun processAndCountWords(matches: ArrayList<String>) {
        if (matches.isEmpty()) return

        val recognizedWords = matches.joinToString(" ")
            .lowercase(Locale.ROOT)
            .split("\\s+".toRegex())

        val targetWords = targetSentence
            .lowercase(Locale.ROOT)
            .split("\\s+".toRegex())

        val targetLength = targetWords.size

        if (recognizedWords.size < targetLength) return

        // Slide through the recognized words with a window the size of the target
        for (i in 0..recognizedWords.size - targetLength) {
            val window = recognizedWords.subList(i, i + targetLength)
            if (window == targetWords) {
                matchCount++
                matchCountView.text = matchCount.toString()

                if (matchCount >= matchLimit) {
                    triggerAlarm()
                    stopAndDestroyRecognizer()
                    return
                }
            }
        }
    }



    // Start Listening (with Delay for Continuous Mode)
    private fun startListeningWithDelay() {
        Handler(Looper.getMainLooper()).postDelayed({
            if (speechRecognizer == null) {
                createSpeechRecognizer()
            }
            startListening()
        }, 500)
    }



    // Start Listening
    @SuppressLint("SetTextI18n")
    private fun startListening() {
        if (speechRecognizer == null) {
            createSpeechRecognizer()
        }

        if (!isListening) {
            return
        }
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        }

        speechRecognizer?.startListening(intent)

    }

    // Stop Listening
    @SuppressLint("SetTextI18n")


    private fun stopListening() {
        isListening = false
        btnStartStop.text = "START"
        speechRecognizer?.stopListening()
        speechRecognizer?.cancel()

    }

    private fun stopAndDestroyRecognizer() {
        stopListening()
        speechRecognizer?.destroy()
        speechRecognizer = null
    }

    @SuppressLint("SetTextI18n")
    private fun resetStateAfterLimit() {
        currentWordIndex = 0
        matchCount = 0
        matchCountView.text = "0"
        recognizedTextView.text = ""
        fullRecognizedText = ""
    }




    // Trigger alarm
    private fun triggerAlarm() {
        stopListening()
        val toneGenerator = ToneGenerator(AudioManager.STREAM_MUSIC, 100)
        toneGenerator.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 200)

        Handler(Looper.getMainLooper()).postDelayed({ toneGenerator.release() }, 300)

        AlertDialog.Builder(this)
            .setTitle("Limit Reached")
            .setMessage("The match limit has been reached.")
            .setPositiveButton("OK") { _, _ ->
                resetStateAfterLimit()

                // Re-enable listening
                isListening = true
                btnStartStop.text = "STOP"
                startListeningWithDelay()
            }
            .show()
    }
}