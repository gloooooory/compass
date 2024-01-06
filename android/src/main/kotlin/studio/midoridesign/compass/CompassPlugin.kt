package studio.midoridesign.compass

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.hardware.GeomagneticField
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.plugin.common.EventChannel
import io.flutter.plugin.common.EventChannel.StreamHandler

class CompassPlugin: FlutterPlugin, StreamHandler {
    private var channel: EventChannel? = null
    private var context: Context? = null
    private var sensorManager: SensorManager? = null
    private var rotationVectorSensor: Sensor? = null
    private var headingSensor: Sensor? = null
    private var sensorEventListener: SensorEventListener? = null
    private var locationManager: LocationManager? = null
    private var currentLocation: Location? = null
    private val locationListener: LocationListener
    private var lastAccuracySensorStatus: Int? = null
    private val headingChangeThreshold = 0.1f
    private var lastTrueHeading = 0f
    private var lastAzimuth = 0f
    private var filterCoefficient = 0.8f

    init {
        locationListener = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                currentLocation = location
            }
            override fun onStatusChanged(provider: String, status: Int, extras: Bundle) {}
            override fun onProviderEnabled(provider: String) {}
            override fun onProviderDisabled(provider: String) {}
        }
    }

    override fun onAttachedToEngine(flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
        context = flutterPluginBinding.applicationContext
        sensorManager = context?.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        rotationVectorSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        headingSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_HEADING)
        channel = EventChannel(flutterPluginBinding.binaryMessenger, "studio.midoridesign/compass")
        channel?.setStreamHandler(this)
        locationManager = context?.getSystemService(Context.LOCATION_SERVICE) as LocationManager?
        try {
            locationManager?.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0L, 0f, locationListener)
        } catch (e: SecurityException) {
            // TODO: handle exception
        }
    }

    override fun onDetachedFromEngine(binding: FlutterPlugin.FlutterPluginBinding) {
        unregisterListener()
        channel?.setStreamHandler(null)
    }

    override fun onListen(arguments: Any?, events: EventChannel.EventSink?) {
        sensorEventListener = events?.let { createSensorEventListener(it) }
        if (headingSensor != null) {
            sensorManager?.registerListener(sensorEventListener, headingSensor, SensorManager.SENSOR_DELAY_GAME)
        } else if (rotationVectorSensor != null) {
            sensorManager?.registerListener(sensorEventListener, rotationVectorSensor, SensorManager.SENSOR_DELAY_GAME)
        }
    }

    override fun onCancel(arguments: Any?) {
        unregisterListener()
    }

    private fun unregisterListener() {
        sensorManager?.unregisterListener(sensorEventListener)
    }

    private fun createSensorEventListener(events: EventChannel.EventSink): SensorEventListener {
        return object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                if (event.sensor.type == Sensor.TYPE_ROTATION_VECTOR) {
                    val rotationMatrix = FloatArray(9)
                    SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
                    val orientationAngles = FloatArray(3)
                    SensorManager.getOrientation(rotationMatrix, orientationAngles)

                    val azimuth = Math.toDegrees(orientationAngles[0].toDouble()).toFloat()
                    val filteredAzimuth = lowPassFilter(azimuth, lastAzimuth)
                    val trueHeading = calculateTrueHeading(filteredAzimuth)

                    if (Math.abs(lastTrueHeading - trueHeading) > headingChangeThreshold) {
                        lastTrueHeading = trueHeading
                        lastAzimuth = filteredAzimuth
                        notifyCompassChangeListeners(trueHeading)
                    }
                }
                else if (event.sensor.type == Sensor.TYPE_HEADING) {
                    val heading = event.values[0]
                    if (Math.abs(lastTrueHeading - heading) > headingChangeThreshold) {
                        lastTrueHeading = heading
                        notifyCompassChangeListeners(heading)
                    }
                }
            }


            override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {
                if (lastAccuracySensorStatus != accuracy) {
                    lastAccuracySensorStatus = accuracy
                    val shouldCalibrate = accuracy == SensorManager.SENSOR_STATUS_ACCURACY_LOW || 
                                          accuracy == SensorManager.SENSOR_STATUS_UNRELIABLE
                    events.success(shouldCalibrate)
                }
            }

            private fun calculateTrueHeading(azimuth: Float): Float {
                val declination = currentLocation?.let {
                    val geoField = GeomagneticField(
                        it.latitude.toFloat(),
                        it.longitude.toFloat(),
                        it.altitude.toFloat(),
                        System.currentTimeMillis()
                    )
                    geoField.declination
                } ?: 0f

                var trueHeading = azimuth + declination
                if (trueHeading < 0) trueHeading += 360
                if (trueHeading >= 360) trueHeading -= 360
                return trueHeading
            }

            private fun notifyCompassChangeListeners(heading: Float) {
                events.success(heading)
            }

            private fun lowPassFilter(input: Float, output: Float): Float {
                return output + filterCoefficient * (input - output)
            }
        }
    }
}
