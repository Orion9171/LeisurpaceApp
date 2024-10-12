package com.example.leisurepace

import android.media.MediaPlayer
import android.os.Bundle
import android.os.CountDownTimer
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide

class HomeFragment : Fragment() {

    private lateinit var timeDisplay: TextView
    private lateinit var stepCountDisplay: TextView
    private lateinit var kcalDisplay: TextView
    private lateinit var distanceDisplay: TextView
    private lateinit var startStopButton: Button
    private lateinit var weightInput: EditText
    private lateinit var bpmSpinner: Spinner
    private lateinit var timeSpinner: Spinner
    private lateinit var runningGif: ImageView
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
    private var weight = 70.0
    private val met = 6.0
    private val strideLength = 0.78
    private var distance = 0.0

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_home, container, false)

        timeDisplay = view.findViewById(R.id.timer)
        stepCountDisplay = view.findViewById(R.id.footstep)
        kcalDisplay = view.findViewById(R.id.kcal_display)
        distanceDisplay = view.findViewById(R.id.distance_display)
        weightInput = view.findViewById(R.id.weight_input)
        startStopButton = view.findViewById(R.id.start_button)
        bpmSpinner = view.findViewById(R.id.bpm_spinner)
        timeSpinner = view.findViewById(R.id.time_spinner)
        runningGif = view.findViewById(R.id.runningGif)

        startStopButton.isEnabled = false

        startStopButton.setOnClickListener {
            if (timerRunning) stopTimer() else startTimer()
        }

        bpmSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selectedBpm = parent?.getItemAtPosition(position).toString()
                updateBpm(selectedBpm)
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        timeSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selectedTime = parent?.getItemAtPosition(position).toString()
                updateTime(selectedTime)
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        return view
    }

    private fun updateBpm(selectedBpm: String) {
        bpm = when (selectedBpm) {
            "120 BPM" -> 120
            "150 BPM" -> 150
            "180 BPM" -> 180
            "210 BPM" -> 210
            "240 BPM" -> 240
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
            "50 minutes" -> 50L
            "60 minutes" -> 60L
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

    private fun playBpmSound(bpm: Int) {
        mediaPlayer?.release()
        mediaPlayer = null

        val soundResId = when (bpm) {
            120 -> R.raw.bpm120
            150 -> R.raw.bpm150
            180 -> R.raw.bpm180
            210 -> R.raw.bpm210
            240 -> R.raw.bpm240
            else -> R.raw.bpm180
        }

        mediaPlayer = MediaPlayer.create(requireContext(), soundResId)
        mediaPlayer?.isLooping = true // Ensure the sound loops seamlessly
        mediaPlayer?.start()
    }

    private fun startTimer() {
        if (validateWeight()) {
            resetAllMetrics()
            showRunningGif()
            playBpmSound(bpm)

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
                    startStopButton.text = "開始"
                    hideRunningGif()
                }
            }.start()

            timerRunning = true
            startStopButton.text = "重啟"
        }
    }

    private fun stopTimer() {
        if (this::countDownTimer.isInitialized) countDownTimer.cancel()
        timerRunning = false
        resetAllMetrics()
        stopBpmSound()
        hideRunningGif()
        startStopButton.text = "開始"
    }

    private fun showRunningGif() {
        runningGif.visibility = View.VISIBLE
        Glide.with(this)
            .asGif()
            .load(R.drawable.run)
            .into(runningGif)
    }

    private fun hideRunningGif() {
        runningGif.visibility = View.GONE
    }

    private fun stopBpmSound() {
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
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
            Toast.makeText(context, "Please enter your weight", Toast.LENGTH_SHORT).show()
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
        kcalDisplay.text = String.format("Calories: %.2f kcal", kcal)
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
        stepCountDisplay.text = steps.toString()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        stopBpmSound()
    }
}
