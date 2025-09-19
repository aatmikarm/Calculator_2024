package com.aatmik.calculator.fragment

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.media.MediaPlayer
import android.os.Bundle
import android.os.Vibrator
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Toast
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.aatmik.calculator.databinding.FragmentLevelCalculatorBinding
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.math.*

class LevelCalculatorFragment : Fragment(), SensorEventListener {

    private lateinit var binding: FragmentLevelCalculatorBinding
    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null
    private var vibrator: Vibrator? = null

    private var cameraExecutor: ExecutorService? = null
    private var isCameraEnabled = false
    private var isCalibrated = false
    private var isHolding = false

    // Calibration offsets
    private var xOffset = 0f
    private var yOffset = 0f

    // Current readings
    private var currentXAngle = 0f
    private var currentYAngle = 0f

    // Settings
    private var soundEnabled = true
    private var vibrationEnabled = true
    private var sensitivity = 1.0f // degrees for "level" threshold


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentLevelCalculatorBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        activity?.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN)
        setupSensors()
        setupCamera()
        setupListeners()
        checkSensorAvailability()

        cameraExecutor = Executors.newSingleThreadExecutor()
    }

    private fun setupSensors() {
        sensorManager = requireActivity().getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        vibrator = requireActivity().getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    }

    private fun setupCamera() {
        if (allPermissionsGranted()) {
            startCamera()
        } else {
            requestCameraPermission()
        }
    }

    private fun setupListeners() {
        binding.apply {
            backIv.setOnClickListener {
                activity?.onBackPressedDispatcher?.onBackPressed()
            }

            btnCalibrate.setOnClickListener {
                calibrateLevel()
            }

            btnHold.setOnClickListener {
                toggleHold()
            }

            btnCamera.setOnClickListener {
                toggleCamera()
            }

            btnSound.setOnClickListener {
                toggleSound()
            }

            switchVibration.setOnCheckedChangeListener { _, isChecked ->
                vibrationEnabled = isChecked
            }

            // Sensitivity adjustment
            seekBarSensitivity.setOnSeekBarChangeListener(object : android.widget.SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: android.widget.SeekBar?, progress: Int, fromUser: Boolean) {
                    sensitivity = (progress + 1) * 0.5f // 0.5 to 5.0 degrees
                    tvSensitivity.text = "Sensitivity: ±${String.format("%.1f", sensitivity)}°"
                }
                override fun onStartTrackingTouch(seekBar: android.widget.SeekBar?) {}
                override fun onStopTrackingTouch(seekBar: android.widget.SeekBar?) {}
            })
        }
    }

    private fun checkSensorAvailability() {
        when {
            accelerometer == null -> {
                showError("This device doesn't have an accelerometer. Level functionality will not work.")
                binding.tvLevelStatus.text = "Accelerometer not available"
            }
            else -> {
                binding.tvLevelStatus.text = "Hold device flat against surface to measure level"
            }
        }
    }

    private fun calibrateLevel() {
        if (accelerometer != null) {
            xOffset = currentXAngle
            yOffset = currentYAngle
            isCalibrated = true

            binding.tvLevelStatus.text = "Calibrated! Current position set as level reference."
            Toast.makeText(requireContext(), "Level calibrated successfully", Toast.LENGTH_SHORT).show()

            // Visual feedback
            binding.btnCalibrate.text = "Recalibrate"
            binding.viewCalibratedIndicator.visibility = View.VISIBLE
        }
    }

    private fun toggleHold() {
        isHolding = !isHolding
        binding.btnHold.text = if (isHolding) "Unfreeze" else "Hold Reading"
        binding.viewHoldIndicator.visibility = if (isHolding) View.VISIBLE else View.GONE

        if (isHolding) {
            binding.tvLevelStatus.text = "Reading frozen. Tap 'Unfreeze' to continue measuring."
        }
    }

    private fun toggleCamera() {
        isCameraEnabled = !isCameraEnabled
        binding.btnCamera.text = if (isCameraEnabled) "Hide Camera" else "Show Camera"

        if (isCameraEnabled) {
            if (allPermissionsGranted()) {
                binding.cameraPreview.visibility = View.VISIBLE
                binding.levelOverlay.visibility = View.VISIBLE
                startCamera()
            } else {
                requestCameraPermission()
            }
        } else {
            binding.cameraPreview.visibility = View.GONE
            binding.levelOverlay.visibility = View.GONE
        }
    }

    private fun toggleSound() {
        soundEnabled = !soundEnabled
        binding.btnSound.text = if (soundEnabled) "Sound: ON" else "Sound: OFF"
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())

        cameraProviderFuture.addListener({
            try {
                // Get the camera provider
                val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

                // Build the preview use case
                val preview = Preview.Builder()
                    .build()
                    .also {
                        it.setSurfaceProvider(binding.cameraPreview.surfaceProvider)
                    }

                // Select back camera as default
                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                try {
                    // Unbind use cases before rebinding
                    cameraProvider.unbindAll()

                    // Bind use cases to camera
                    cameraProvider.bindToLifecycle(
                        this, cameraSelector, preview
                    )

                } catch (exc: Exception) {
                    showError("Use case binding failed: ${exc.message}")
                }

            } catch (exc: Exception) {
                showError("Failed to start camera: ${exc.message}")
            }
        }, ContextCompat.getMainExecutor(requireContext()))
    }

    private fun allPermissionsGranted(): Boolean {
        return ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestCameraPermission() {
        ActivityCompat.requestPermissions(
            requireActivity(),
            arrayOf(Manifest.permission.CAMERA),
            CAMERA_PERMISSION_REQUEST_CODE
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startCamera()
            } else {
                Toast.makeText(requireContext(), "Camera permission required for camera level view", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        startListening()
    }

    override fun onPause() {
        super.onPause()
        stopListening()
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor?.shutdown()
    }

    private fun startListening() {
        accelerometer?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }
    }

    private fun stopListening() {
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (isHolding) return // Don't update if readings are frozen

        event?.let {
            if (it.sensor.type == Sensor.TYPE_ACCELEROMETER) {
                val x = it.values[0]
                val y = it.values[1]
                val z = it.values[2]

                // Calculate angles
                currentXAngle = atan2(y, sqrt(x * x + z * z)) * 180 / PI.toFloat()
                currentYAngle = atan2(-x, sqrt(y * y + z * z)) * 180 / PI.toFloat()

                // Apply calibration offset
                val adjustedXAngle = currentXAngle - xOffset
                val adjustedYAngle = currentYAngle - yOffset

                updateLevelDisplay(adjustedXAngle, adjustedYAngle)
                updateLevelOverlay(adjustedXAngle, adjustedYAngle)
                checkLevelStatus(adjustedXAngle, adjustedYAngle)
            }
        }
    }

    private fun updateLevelDisplay(xAngle: Float, yAngle: Float) {
        binding.apply {
            // Update angle displays
            tvXAngle.text = "${String.format("%.1f", abs(xAngle))}°"
            tvYAngle.text = "${String.format("%.1f", abs(yAngle))}°"

            // Update bubble positions
            val bubbleX = (xAngle / 45f * 100).coerceIn(-100f, 100f)
            val bubbleY = (yAngle / 45f * 100).coerceIn(-100f, 100f)

            // Move bubble views (you'd implement custom views for this)
            tvBubbleX.text = if (xAngle > 0) "→" else if (xAngle < 0) "←" else "•"
            tvBubbleY.text = if (yAngle > 0) "↑" else if (yAngle < 0) "↓" else "•"

            // Update percentage grade
            val gradeX = tan(abs(xAngle) * PI / 180) * 100
            val gradeY = tan(abs(yAngle) * PI / 180) * 100

            tvGradeX.text = "${String.format("%.1f", gradeX)}%"
            tvGradeY.text = "${String.format("%.1f", gradeY)}%"
        }
    }

    private fun updateLevelOverlay(xAngle: Float, yAngle: Float) {
        if (!isCameraEnabled) return

        // Update camera overlay elements
        binding.apply {
            // Update crosshair color based on level
            val isLevel = abs(xAngle) < sensitivity && abs(yAngle) < sensitivity
            levelOverlay.setBackgroundColor(
                if (isLevel)
                    ContextCompat.getColor(requireContext(), android.R.color.holo_green_light)
                else
                    ContextCompat.getColor(requireContext(), android.R.color.holo_red_light)
            )
            levelOverlay.alpha = 0.3f
        }
    }

    private fun checkLevelStatus(xAngle: Float, yAngle: Float) {
        val isLevel = abs(xAngle) < sensitivity && abs(yAngle) < sensitivity

        binding.apply {
            if (isLevel) {
                tvLevelStatus.text = "LEVEL ✓"
                tvLevelStatus.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.holo_green_dark))
                viewLevelIndicator.visibility = View.VISIBLE
                viewLevelIndicator.setBackgroundColor(ContextCompat.getColor(requireContext(), android.R.color.holo_green_light))

                // Provide feedback
                provideLevelFeedback()

            } else {
                tvLevelStatus.text = "Adjusting needed"
                tvLevelStatus.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.holo_red_dark))
                viewLevelIndicator.visibility = View.VISIBLE
                viewLevelIndicator.setBackgroundColor(ContextCompat.getColor(requireContext(), android.R.color.holo_red_light))

                // Show adjustment hints
                val hintX = if (xAngle > sensitivity) "Tilt left" else if (xAngle < -sensitivity) "Tilt right" else ""
                val hintY = if (yAngle > sensitivity) "Tilt down" else if (yAngle < -sensitivity) "Tilt up" else ""

                tvLevelStatus.text = "$hintX $hintY".trim().ifEmpty { "Minor adjustment needed" }
            }
        }
    }

    private fun provideLevelFeedback() {
        // Sound feedback
        if (soundEnabled) {
            try {
               // val mediaPlayer = MediaPlayer.create(requireContext(), android.R.raw.click)
                //mediaPlayer?.start()
               // mediaPlayer?.setOnCompletionListener { it.release() }
            } catch (e: Exception) {
                // Handle sound error silently
            }
        }

        // Vibration feedback
        if (vibrationEnabled && vibrator?.hasVibrator() == true) {
            vibrator?.vibrate(100) // 100ms vibration
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        when (accuracy) {
            SensorManager.SENSOR_STATUS_UNRELIABLE -> {
                binding.tvLevelStatus.text = "Sensor accuracy low. Try calibrating."
            }
        }
    }

    private fun showError(message: String) {
        binding.tvLevelStatus.text = message
        Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
    }

    companion object {
        private const val TAG = "LevelCalculatorFragment"
        const val CAMERA_PERMISSION_REQUEST_CODE = 1002
        const val LEVEL_THRESHOLD = 1.0f // degrees
    }
}