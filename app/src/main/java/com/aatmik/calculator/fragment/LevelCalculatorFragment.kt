package com.aatmik.calculator.fragment

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.os.Vibrator
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.aatmik.calculator.databinding.FragmentLevelCalculatorBinding
import kotlin.math.*

class LevelCalculatorFragment : Fragment(), SensorEventListener {

    private lateinit var binding: FragmentLevelCalculatorBinding
    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null
    private var vibrator: Vibrator? = null

    // State variables
    private var isCalibrated = false
    private var isHolding = false
    private var sensitivity = 1.0f
    private var isCameraEnabled = false

    // Calibration offsets
    private var xOffset = 0f
    private var yOffset = 0f

    // Current readings
    private var currentXAngle = 0f
    private var currentYAngle = 0f

    companion object {
        private const val CAMERA_PERMISSION_REQUEST_CODE = 1001
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentLevelCalculatorBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupSensors()
        setupListeners()
    }

    private fun setupSensors() {
        sensorManager = requireContext().getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        vibrator = requireContext().getSystemService(Context.VIBRATOR_SERVICE) as Vibrator

        if (accelerometer == null) {
            binding.tvStatus.text = "No Accelerometer Available"
        }
    }

    private fun setupListeners() {
        binding.apply {
            backIv.setOnClickListener {
                activity?.onBackPressedDispatcher?.onBackPressed()
            }

            btnCalibrate.setOnClickListener {
                calibrate()
            }

            btnHold.setOnClickListener {
                toggleHold()
            }

            switchCamera.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    if (checkCameraPermission()) {
                        enableCamera()
                    } else {
                        requestCameraPermission()
                        switchCamera.isChecked = false
                    }
                } else {
                    disableCamera()
                }
            }

            seekBarSensitivity.setOnSeekBarChangeListener(
                object : android.widget.SeekBar.OnSeekBarChangeListener {
                    override fun onProgressChanged(seekBar: android.widget.SeekBar?, progress: Int, fromUser: Boolean) {
                        sensitivity = (progress + 1) * 0.5f // 0.5° to 5.0°
                        tvSensitivity.text = "Sensitivity: ±${String.format("%.1f", sensitivity)}°"
                    }
                    override fun onStartTrackingTouch(seekBar: android.widget.SeekBar?) {}
                    override fun onStopTrackingTouch(seekBar: android.widget.SeekBar?) {}
                }
            )
        }
    }

    private fun calibrate() {
        xOffset = currentXAngle
        yOffset = currentYAngle
        isCalibrated = true
        binding.btnCalibrate.text = "Recalibrate"
    }

    private fun toggleHold() {
        isHolding = !isHolding
        binding.btnHold.text = if (isHolding) "Unfreeze" else "Hold"
    }

    private fun enableCamera() {
        isCameraEnabled = true
        startCamera()
        // Show camera behind the level UI
        binding.cameraPreview.visibility = View.VISIBLE
        // Make level elements semi-transparent so camera shows through
        binding.mainContent.alpha = 0.8f
    }

    private fun disableCamera() {
        isCameraEnabled = false
        stopCamera()
        // Hide camera
        binding.cameraPreview.visibility = View.GONE
        // Make level elements fully opaque
        binding.mainContent.alpha = 1.0f
    }

    private fun checkCameraPermission(): Boolean {
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

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())

        cameraProviderFuture.addListener({
            try {
                val cameraProvider = cameraProviderFuture.get()
                val preview = Preview.Builder().build()
                preview.setSurfaceProvider(binding.cameraPreview.surfaceProvider)

                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(this, cameraSelector, preview)

            } catch (exc: Exception) {
                Toast.makeText(requireContext(), "Camera failed to start", Toast.LENGTH_SHORT).show()
                binding.switchCamera.isChecked = false
            }
        }, ContextCompat.getMainExecutor(requireContext()))
    }

    private fun stopCamera() {
        try {
            ProcessCameraProvider.getInstance(requireContext()).get().unbindAll()
        } catch (e: Exception) {
            // Camera already stopped
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                binding.switchCamera.isChecked = true
                enableCamera()
            } else {
                Toast.makeText(requireContext(), "Camera permission required", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        accelerometer?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (isHolding || event?.sensor?.type != Sensor.TYPE_ACCELEROMETER) return

        val x = event.values[0]  // Left/Right tilt
        val y = event.values[1]  // Forward/Backward tilt
        val z = event.values[2]  // Up/Down (gravity)

        // For portrait mode (phone held vertically):
        // X-axis: Left/Right tilt (roll)
        // Y-axis: Forward/Backward tilt (pitch)

        // Calculate tilt angles in degrees for portrait orientation
        currentXAngle = atan2(x, sqrt(y * y + z * z)) * 180 / PI.toFloat()  // Roll (left/right)
        currentYAngle = atan2(y, sqrt(x * x + z * z)) * 180 / PI.toFloat()  // Pitch (forward/back)

        // Apply calibration
        val adjustedX = currentXAngle - xOffset
        val adjustedY = currentYAngle - yOffset

        updateDisplay(adjustedX, adjustedY)
        updateBubble(adjustedX, adjustedY)
    }

    private fun updateDisplay(xAngle: Float, yAngle: Float) {
        binding.apply {
            // Update angle readings
            tvXAngle.text = "${String.format("%.1f", abs(xAngle))}°"
            tvYAngle.text = "${String.format("%.1f", abs(yAngle))}°"

            // Check if level
            val isLevel = abs(xAngle) <= sensitivity && abs(yAngle) <= sensitivity

            if (isLevel) {
                tvStatus.text = "LEVEL"
                tvStatus.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.holo_green_dark))
                // Vibrate when level
                vibrator?.vibrate(50)
            } else {
                tvStatus.text = "NOT LEVEL"
                tvStatus.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.holo_red_dark))
            }
        }
    }

    private fun updateBubble(xAngle: Float, yAngle: Float) {
        // Move bubble based on tilt - adjusted for portrait mode
        val maxOffset = 100f // Maximum pixels to move bubble

        // For portrait mode:
        // X angle controls horizontal movement (left/right)
        // Y angle controls vertical movement (forward/back)
        val bubbleX = (xAngle / 45f * maxOffset).coerceIn(-maxOffset, maxOffset)
        val bubbleY = (yAngle / 45f * maxOffset).coerceIn(-maxOffset, maxOffset)

        binding.bubble.translationX = bubbleX
        binding.bubble.translationY = bubbleY // No inversion needed for portrait

        // Change bubble color based on level
        val isLevel = abs(xAngle) <= sensitivity && abs(yAngle) <= sensitivity
        val color = if (isLevel) {
            ContextCompat.getColor(requireContext(), android.R.color.holo_green_light)
        } else {
            ContextCompat.getColor(requireContext(), android.R.color.holo_red_light)
        }
        binding.bubble.setBackgroundColor(color)
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Handle accuracy changes if needed
    }
}