package com.example.leisurepace

import android.media.MediaPlayer
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class MainActivity : AppCompatActivity() {

    private lateinit var timeDisplay: TextView
    private lateinit var footstepDisplay: TextView // Combined footstep label and value
    private lateinit var kcalDisplay: TextView
    private lateinit var distanceDisplay: TextView
    private lateinit var startStopButton: Button
    private lateinit var resetButton: Button
    private lateinit var weightInput: EditText
    private lateinit var bpmSpinner: Spinner
    private lateinit var timeSpinner: Spinner
    private lateinit var runningGif: ImageView
    private lateinit var ivBackground: ImageView
    private var mediaPlayer: MediaPlayer? = null

    private var timerRunning = false
    private var bpmSelected = false
    private var timeSelected = false
    private var timeLeftInMillis = 1800000L
    private var totalTimeInMillis = 1800000L
    private lateinit var countDownTimer: CountDownTimer

    private var bpm = 180
    private var steps = 0
    private var kcal = 0.0
    private var weight = 59.0 // Default weight if no previous record
    private val met = 6.0
    private val strideLength = 0.78
    private var distance = 0.0

    private val handler = Handler(Looper.getMainLooper())
    private var currentImageIndex = 0
    private val backgroundImages = arrayOf(
        R.drawable.main_bg1,
        R.drawable.main_bg2,
        R.drawable.main_bg3,
        R.drawable.main_bg4,
        R.drawable.main_bg5,
        R.drawable.main_bg6
    )

    private val imageSwitcherRunnable = object : Runnable {
        override fun run() {
            if (timerRunning) {
                updateBackgroundImage()
                handler.postDelayed(this, 10000) // Reschedule every 10 seconds
            }
        }
    }

    private enum class ButtonMode { START, STOP, CONTINUE }
    private var buttonMode = ButtonMode.START

    private lateinit var firestore: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        firestore = FirebaseFirestore.getInstance()

        initializeViews()

        retrieveLastWeight()

        // Start/Stop/Continue Button Click Listener
        startStopButton.setOnClickListener {
            when (buttonMode) {
                ButtonMode.START -> startTimer()
                ButtonMode.STOP -> stopTimer()
                ButtonMode.CONTINUE -> continueTimer()
            }
        }

        // Reset Button Click Listener
        resetButton.setOnClickListener {
            resetTimer()
        }

        // BPM Spinner Listener
        bpmSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: android.view.View?, position: Int, id: Long) {
                val selectedBpm = parent?.getItemAtPosition(position).toString()
                updateBpm(selectedBpm)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        // Time Spinner Listener
        timeSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: android.view.View?, position: Int, id: Long) {
                val selectedTime = parent?.getItemAtPosition(position).toString()
                updateTime(selectedTime)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun initializeViews() {
        timeDisplay = findViewById(R.id.timer)
        footstepDisplay = findViewById(R.id.footstepDisplay) // Updated ID
        kcalDisplay = findViewById(R.id.kcal_display)
        distanceDisplay = findViewById(R.id.distance_display)
        weightInput = findViewById(R.id.weight_input)
        startStopButton = findViewById(R.id.start_button)
        resetButton = findViewById(R.id.Reset)
        bpmSpinner = findViewById(R.id.bpm_spinner)
        timeSpinner = findViewById(R.id.time_spinner)
        runningGif = findViewById(R.id.runningGif)
        ivBackground = findViewById(R.id.iv_background)

        ivBackground.setImageResource(R.drawable.main_bg1)
        startStopButton.isEnabled = false
        startStopButton.text = "Start"
    }

    private fun retrieveLastWeight() {
        firestore.collection("RunningRecords")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(1)
            .get()
            .addOnSuccessListener { documents ->
                if (!documents.isEmpty) {
                    val lastRecord = documents.documents[0]
                    weight = lastRecord.getDouble("weight") ?: weight
                    weightInput.setText(weight.toString())
                } else {
                    weightInput.setText(weight.toString())
                }
            }
            .addOnFailureListener {
                weightInput.setText(weight.toString())
            }
    }

    private fun updateBackgroundImage() {
        runOnUiThread {
            Glide.with(this)
                .load(backgroundImages[currentImageIndex])
                .placeholder(R.drawable.main_bg1)
                .into(ivBackground)
            currentImageIndex = (currentImageIndex + 1) % backgroundImages.size
        }
    }

    private fun updateBpm(selectedBpm: String) {
        bpm = when (selectedBpm) {
            "120 BPM" -> 120
            "150 BPM" -> 150
            "180 BPM" -> 180
            "210 BPM" -> 210
            else -> 180
        }
        bpmSelected = true
        checkIfReadyToStart()
        if (timerRunning) stopTimer()
    }

    private fun updateTime(selectedTime: String) {
        val selectedTimeInMinutes = when (selectedTime) {
            "10 minutes" -> 10L
            "20 minutes" -> 20L
            "30 minutes" -> 30L
            "40 minutes" -> 40L
            else -> 30L
        }
        timeLeftInMillis = selectedTimeInMinutes * 60000
        totalTimeInMillis = timeLeftInMillis
        timeSelected = true
        checkIfReadyToStart()
        if (timerRunning) stopTimer()
    }

    private fun checkIfReadyToStart() {
        startStopButton.isEnabled = bpmSelected && timeSelected
    }

    private fun startTimer() {
        if (validateWeight()) {
            resetAllMetrics()
            showRunningGif()
            playBpmSound(bpm)

            updateBackgroundImage()
            handler.postDelayed(imageSwitcherRunnable, 10000)

            countDownTimer = object : CountDownTimer(timeLeftInMillis, 1000) {
                override fun onTick(millisUntilFinished: Long) {
                    timeLeftInMillis = millisUntilFinished
                    updateTimerText()
                    calculateSteps(millisUntilFinished)
                    updateStepCount()
                    updateCalories()
                    updateDistance()
                }

                override fun onFinish() {
                    timerRunning = false
                    startStopButton.text = "Start"
                    buttonMode = ButtonMode.START
                    hideRunningGif()
                    handler.removeCallbacks(imageSwitcherRunnable)
                }
            }.start()

            timerRunning = true
            startStopButton.text = "Stop"
            buttonMode = ButtonMode.STOP
        }
    }

    private fun stopTimer() {
        if (this::countDownTimer.isInitialized) countDownTimer.cancel()
        timerRunning = false
        stopBpmSound()
        hideRunningGif()

        handler.removeCallbacks(imageSwitcherRunnable)

        startStopButton.text = "Continue"
        buttonMode = ButtonMode.CONTINUE
    }

    private fun continueTimer() {
        showRunningGif()
        playBpmSound(bpm)

        handler.postDelayed(imageSwitcherRunnable, 10000)

        countDownTimer = object : CountDownTimer(timeLeftInMillis, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                timeLeftInMillis = millisUntilFinished
                updateTimerText()
                calculateSteps(millisUntilFinished)
                updateStepCount()
                updateCalories()
                updateDistance()
            }

            override fun onFinish() {
                timerRunning = false
                startStopButton.text = "Start"
                buttonMode = ButtonMode.START
                hideRunningGif()
                handler.removeCallbacks(imageSwitcherRunnable)
            }
        }.start()

        timerRunning = true
        startStopButton.text = "Stop"
        buttonMode = ButtonMode.STOP
    }

    private fun resetTimer() {
        if (this::countDownTimer.isInitialized) countDownTimer.cancel()
        timerRunning = false
        stopBpmSound()
        hideRunningGif()
        handler.removeCallbacks(imageSwitcherRunnable)

        resetAllMetrics()

        currentImageIndex = 0
        ivBackground.setImageResource(R.drawable.main_bg1)
        startStopButton.text = "Start"
        buttonMode = ButtonMode.START

        Toast.makeText(this, "Reset successfully!", Toast.LENGTH_SHORT).show()
    }

    private fun resetAllMetrics() {
        steps = 0
        kcal = 0.0
        distance = 0.0
        timeLeftInMillis = totalTimeInMillis
        updateStepCount()
        updateCalories()
        updateDistance()
        updateTimerText()
    }

    private fun validateWeight(): Boolean {
        val weightStr = weightInput.text.toString()
        if (weightStr.isEmpty()) {
            Toast.makeText(this, "Please enter your weight", Toast.LENGTH_SHORT).show()
            return false
        }
        weight = weightStr.toDouble()
        return true
    }

    private fun calculateSteps(millisUntilFinished: Long) {
        val elapsedTimeInMinutes = (totalTimeInMillis - millisUntilFinished) / 60000.0
        steps = (bpm * elapsedTimeInMinutes).toInt()
    }

    private fun updateCalories() {
        val caloriesPerStep = met * weight * 3.5 / 200
        kcal = steps * caloriesPerStep
        kcalDisplay.text = String.format("Cal: %.2f kcal", kcal)
    }

    private fun updateDistance() {
        distance = (steps * strideLength) / 1000
        distanceDisplay.text = String.format("Km: %.2f", distance)
    }

    private fun updateTimerText() {
        val minutes = (timeLeftInMillis / 1000) / 60
        val seconds = (timeLeftInMillis / 1000) % 60
        timeDisplay.text = String.format("%02d:%02d", minutes, seconds)
    }

    private fun updateStepCount() {
        footstepDisplay.text = "Steps: $steps" // Updated to reflect combined component
    }

    private fun showRunningGif() {
        runningGif.visibility = android.view.View.VISIBLE
        Glide.with(this)
            .asGif()
            .load(R.drawable.run)
            .into(runningGif)
    }

    private fun hideRunningGif() {
        runningGif.visibility = android.view.View.GONE
    }

    private fun playBpmSound(bpm: Int) {
        mediaPlayer?.release()
        mediaPlayer = null

        val soundResId = when (bpm) {
            120 -> R.raw.bpm120
            150 -> R.raw.bpm150
            180 -> R.raw.bpm180
            210 -> R.raw.bpm210
            else -> R.raw.bpm180
        }

        mediaPlayer = MediaPlayer.create(this, soundResId)
        mediaPlayer?.isLooping = true
        mediaPlayer?.start()
    }

    private fun stopBpmSound() {
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
    }

    private fun saveSessionData() {
        val runningRecords = hashMapOf(
            "weight" to weight,
            "duration" to totalTimeInMillis - timeLeftInMillis,
            "calories" to kcal,
            "distanceKm" to distance,
            "steps" to steps,
        )

        firestore.collection("RunningRecords")
            .add(runningRecords)
            .addOnSuccessListener {
                Toast.makeText(this, "Session data saved!", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopBpmSound()
        handler.removeCallbacks(imageSwitcherRunnable)
    }
}
