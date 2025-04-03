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
    private var targetSentence = "hello"

    // Permission request code
    private val recordAudioPermissionRequestCode = 1
    private var fullRecognizedText = ""
    private var lastPartialText = ""
    private val recognizedWords = mutableSetOf<String>()



    // Mantra list
    private val mantras = listOf(
        "Om Namah Shivaya",
        "Hare Krishna Hare Rama",
        "Gayatri Mantra",
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
                    )
                        .show()
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

                matchCount = 0
                matchCountView.text = matchCount.toString()
                startListeningWithDelay()
                recognizedTextView.text = ""
                fullRecognizedText = ""
                lastPartialText = ""
                recognizedWords.clear()
            }
        }
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
                override fun onReadyForSpeech(params: Bundle?) {}
                override fun onBeginningOfSpeech() {
                    fullRecognizedText = ""
                    lastPartialText = ""
                }
                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() {
                    Log.d("onEndOfSpeech","called")
                }
                // Handle errors with automatic retry
                override fun onError(error: Int) {
                    Log.d("SpeechError", "Error Code: $error")

                    when (error) {
                        SpeechRecognizer.ERROR_NO_MATCH, SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> {
                            if (isListening) startListening() // Restart without delay
                        }
                        else -> stopListening() // Stop for other errors
                    }
                }

                override fun onResults(results: Bundle?) {
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    matches?.let {
                        fullRecognizedText += matches.joinToString(" ") + " " // Keep track of full sentence
                        recognizedTextView.text = fullRecognizedText.trim()

                        // process and count words
                        processAndCountWords(matches)


                        if (matchCount >= matchLimit) {
                            triggerAlarm()
                            stopListening()
                            return
                        }

                    }
                    if (isListening) startListening()
                }
                override fun onPartialResults(partialResults: Bundle?) {
                    val partialMatches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    partialMatches?.let { matches ->
                        val newPartialText = matches.joinToString(" ").trim()
                        recognizedTextView.text = newPartialText
                        // process and count words
                        processAndCountWords(matches)
                        if (matchCount >= matchLimit) {
                            triggerAlarm()
                            stopListening()
                            return
                        }


                        // Update recognized text
                        recognizedTextView.text = newPartialText
                    }
                }


                override fun onEvent(eventType: Int, params: Bundle?) {}
            })

        }
    }
    // process and count words
    private fun processAndCountWords(matches: ArrayList<String>) {
        val targetWords = targetSentence.lowercase(Locale.getDefault()).split("\\s+".toRegex())
        val targetWordCount = targetWords.size
        val newWordsInSentence = mutableSetOf<String>() // Temporary set for each sentence
        for (match in matches) {
            val words = match.split("\\s+".toRegex()).filter { it.isNotEmpty() }
            for (word in words) {
                val lowerCaseWord = word.lowercase(Locale.getDefault())
                if (!recognizedWords.contains(lowerCaseWord)) {
                    newWordsInSentence.add(lowerCaseWord)
                }
            }
        }

        // Calculate similarity and decide whether to count
        val currentSentence = matches.joinToString(" ").lowercase(Locale.getDefault()).split("\\s+".toRegex())
        val similarity = calculateSimilarity(targetWords, currentSentence)

        if (currentSentence.size >= targetWordCount * 0.75) { // Check if at least 75% of words are present.
            if(similarity >= targetWordCount*0.6){ // Check if at least 60% are similar
                matchCount++ // Count the match
                matchCountView.text = matchCount.toString()
                recognizedWords.addAll(newWordsInSentence) // Add new words to recognizedWords for the entire session
            }
        }
    }
    private fun calculateSimilarity(target: List<String>, current: List<String>): Int {
        var similarityCount = 0
        val longerList = if (target.size > current.size) target else current
        val shorterList = if (target.size > current.size) current else target
        for (word1 in longerList) {
            for (word2 in shorterList) {
                if (levenshteinDistance(word1, word2) <= 1) {
                    similarityCount++
                    break
                }
            }
        }
        return similarityCount
    }
    // Calculate Levenshtein distance between two strings
    private fun levenshteinDistance(s1: String, s2: String): Int {
        val dp = Array(s1.length + 1) { IntArray(s2.length + 1) }
        for (i in 0..s1.length) {
            dp[i][0] = i
        }
        for (j in 0..s2.length) {
            dp[0][j] = j
        }
        for (i in 1..s1.length) {
            for (j in 1..s2.length) {
                val cost = if (s1[i - 1] == s2[j - 1]) 0 else 1
                dp[i][j] = minOf(
                    dp[i - 1][j] + 1,
                    dp[i][j - 1] + 1,
                    dp[i - 1][j - 1] + cost
                )
            }
        }
        return dp[s1.length][s2.length]
    }

    // Start Listening (with Delay for Continuous Mode)
    private fun startListeningWithDelay() {
        @Suppress("DEPRECATION")
        Handler().postDelayed({ startListening() }, 1000)  // Adds 1-second delay for stability
    }

    // Start Listening
    @SuppressLint("SetTextI18n")
    private fun startListening() {
        if (speechRecognizer == null) {
            createSpeechRecognizer()
        }

        isListening = true
        btnStartStop.text = "STOP"

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
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
        speechRecognizer?.destroy()
        speechRecognizer = null
    }

    // Trigger alarm
    private fun triggerAlarm() {
        val toneGenerator=ToneGenerator(AudioManager.STREAM_MUSIC,100)
        toneGenerator.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD,200)
        Handler().postDelayed({toneGenerator.release()},300)
        AlertDialog.Builder(this)
            .setTitle("Limit Reached")
            .setMessage("The match limit has been reached.")
            .setPositiveButton("OK") { _, _ -> }
            .show()
    }
}
