package com.aatmik.calculator.fragment

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.LocationManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.animation.Animation
import android.view.animation.RotateAnimation
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.aatmik.calculator.databinding.FragmentCompassBinding
import kotlin.math.roundToInt

class CompassFragment : Fragment(), SensorEventListener {

    private lateinit var binding: FragmentCompassBinding
    private lateinit var sensorManager: SensorManager
    private var magnetometer: Sensor? = null
    private var accelerometer: Sensor? = null
    private var locationManager: LocationManager? = null

    private var lastAccelerometer = FloatArray(3)
    private var lastMagnetometer = FloatArray(3)
    private var lastAccelerometerSet = false
    private var lastMagnetometerSet = false

    private var rotationMatrix = FloatArray(9)
    private var orientation = FloatArray(3)
    private var currentDegree = 0f

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentCompassBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        activity?.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN)
        setupSensors()
        setupLocationManager()
        setupListeners()
        checkSensorAvailability()
    }

    private fun setupSensors() {
        sensorManager = requireActivity().getSystemService(Context.SENSOR_SERVICE) as SensorManager
        magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    }

    private fun setupLocationManager() {
        locationManager = requireActivity().getSystemService(Context.LOCATION_SERVICE) as LocationManager
    }

    private fun setupListeners() {
        binding.apply {
            backIv.setOnClickListener {
                activity?.onBackPressedDispatcher?.onBackPressed()
            }

            btnCalibrate.setOnClickListener {
                calibrateCompass()
            }

            btnGetLocation.setOnClickListener {
                getCurrentLocation()
            }
        }
    }

    private fun checkSensorAvailability() {
        when {
            magnetometer == null && accelerometer == null -> {
                showError("This device doesn't have the required sensors for compass functionality.")
            }
            magnetometer == null -> {
                showError("Magnetometer not available. Compass will not work properly.")
            }
            accelerometer == null -> {
                showError("Accelerometer not available. Compass accuracy may be reduced.")
            }
            else -> {
                binding.tvCompassStatus.text = "Compass ready. Hold device flat for best accuracy."
            }
        }
    }

    private fun calibrateCompass() {
        binding.tvCompassStatus.text = "Calibrating... Move your device in a figure-8 pattern"
        Toast.makeText(requireContext(), "Move your device in a figure-8 pattern to calibrate", Toast.LENGTH_LONG).show()

        // Reset calibration values
        lastAccelerometerSet = false
        lastMagnetometerSet = false

        // After 5 seconds, show calibration complete
        binding.root.postDelayed({
            binding.tvCompassStatus.text = "Calibration complete. Hold device flat for accuracy."
        }, 5000)
    }

    private fun getCurrentLocation() {
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestLocationPermission()
            return
        }

        try {
            val lastLocation = locationManager?.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            if (lastLocation != null) {
                val latitude = String.format("%.6f", lastLocation.latitude)
                val longitude = String.format("%.6f", lastLocation.longitude)
                binding.tvLocation.text = "Lat: $latitude°\nLon: $longitude°"
                binding.tvLocation.visibility = View.VISIBLE
            } else {
                binding.tvLocation.text = "Location not available"
                binding.tvLocation.visibility = View.VISIBLE
                Toast.makeText(requireContext(), "Please enable GPS and try again", Toast.LENGTH_SHORT).show()
            }
        } catch (e: SecurityException) {
            showError("Location permission denied")
        }
    }

    private fun requestLocationPermission() {
        ActivityCompat.requestPermissions(
            requireActivity(),
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
            LOCATION_PERMISSION_REQUEST_CODE
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getCurrentLocation()
            } else {
                Toast.makeText(requireContext(), "Location permission required for coordinates", Toast.LENGTH_SHORT).show()
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

    private fun startListening() {
        magnetometer?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
        }
        accelerometer?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
        }
    }

    private fun stopListening() {
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        event?.let {
            when (it.sensor.type) {
                Sensor.TYPE_ACCELEROMETER -> {
                    System.arraycopy(it.values, 0, lastAccelerometer, 0, it.values.size)
                    lastAccelerometerSet = true
                }
                Sensor.TYPE_MAGNETIC_FIELD -> {
                    System.arraycopy(it.values, 0, lastMagnetometer, 0, it.values.size)
                    lastMagnetometerSet = true
                }
            }

            if (lastAccelerometerSet && lastMagnetometerSet) {
                if (SensorManager.getRotationMatrix(rotationMatrix, null, lastAccelerometer, lastMagnetometer)) {
                    SensorManager.getOrientation(rotationMatrix, orientation)
                    val azimuthInRadians = orientation[0]
                    val azimuthInDegrees = Math.toDegrees(azimuthInRadians.toDouble()).toFloat()

                    // Convert to 0-360 range
                    val normalizedAzimuth = if (azimuthInDegrees < 0) {
                        azimuthInDegrees + 360
                    } else {
                        azimuthInDegrees
                    }

                    updateCompass(normalizedAzimuth)
                }
            }
        }
    }

    private fun updateCompass(degree: Float) {
        binding.apply {
            // Update degree text
            val roundedDegree = degree.roundToInt()
            tvDegree.text = "${roundedDegree}°"

            // Update cardinal direction
            tvCardinalDirection.text = getCardinalDirection(roundedDegree)

            // Rotate compass needle
            val rotateAnimation = RotateAnimation(
                currentDegree,
                -degree,
                Animation.RELATIVE_TO_SELF, 0.5f,
                Animation.RELATIVE_TO_SELF, 0.5f
            )
            rotateAnimation.duration = 200
            rotateAnimation.fillAfter = true

            ivCompassNeedle.startAnimation(rotateAnimation)
            currentDegree = -degree

            // Update status
            tvCompassStatus.text = when {
                roundedDegree in 0..10 || roundedDegree in 350..360 -> "Pointing North"
                roundedDegree in 80..100 -> "Pointing East"
                roundedDegree in 170..190 -> "Pointing South"
                roundedDegree in 260..280 -> "Pointing West"
                else -> "Hold device flat for better accuracy"
            }
        }
    }

    private fun getCardinalDirection(degree: Int): String {
        return when (degree) {
            in 0..11 -> "N"
            in 12..33 -> "NNE"
            in 34..56 -> "NE"
            in 57..78 -> "ENE"
            in 79..101 -> "E"
            in 102..123 -> "ESE"
            in 124..146 -> "SE"
            in 147..168 -> "SSE"
            in 169..191 -> "S"
            in 192..213 -> "SSW"
            in 214..236 -> "SW"
            in 237..258 -> "WSW"
            in 259..281 -> "W"
            in 282..303 -> "WNW"
            in 304..326 -> "NW"
            in 327..348 -> "NNW"
            in 349..360 -> "N"
            else -> "Unknown"
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        when (accuracy) {
            SensorManager.SENSOR_STATUS_UNRELIABLE -> {
                binding.tvCompassStatus.text = "Compass accuracy is low. Try calibrating."
            }
            SensorManager.SENSOR_STATUS_ACCURACY_LOW -> {
                binding.tvCompassStatus.text = "Compass accuracy is low. Try calibrating."
            }
            SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM -> {
                binding.tvCompassStatus.text = "Compass accuracy is medium."
            }
            SensorManager.SENSOR_STATUS_ACCURACY_HIGH -> {
                binding.tvCompassStatus.text = "Compass accuracy is high."
            }
        }
    }

    private fun showError(message: String) {
        binding.tvCompassStatus.text = message
        Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
    }

    companion object {
        private const val TAG = "CompassFragment"
        const val LOCATION_PERMISSION_REQUEST_CODE = 1001
    }
}