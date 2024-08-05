package com.example.helloworldmaverick

import UIKit.app.Screen
import UIKit.app.data.EvsColor
import UIKit.app.data.TouchDirection
import UIKit.app.resources.Font
import UIKit.widgets.Text
import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import org.json.JSONObject
import java.text.SimpleDateFormat
import com.everysight.evskit.android.Evs
import java.util.*

class MainScreen(private val context: Context, private val mqttManager: MqttManager) : Screen() {
    private val gpsLabelText = Text()
    private val gpsDataText = Text()
    private val timeLabelText = Text()
    private val timeDataText = Text()
    private val windLabelText = Text()
    private val windDataText = Text()
    private lateinit var locationManager: LocationManager
    private lateinit var locationListener: LocationListener

    private val controlTowerManager = ControlTowerManager(context)
    private lateinit var controlTowersScreen: ControlTowersScreen

    companion object {
        private const val TAG = "MainScreen"
    }

    override fun onCreate() {
        super.onCreate()

        // Initial GPS position
        val initialLatitude = 44.8368
        val initialLongitude = -0.6859

        // GPS Label
        gpsLabelText.setResource(Font.StockFont.Small)
            .setText("GPS: ")
            .setCenter(getWidth() / 4)
            .setY(50f)
            .setForegroundColor(EvsColor.White)
            .addToScreen(this)

        // GPS Data
        gpsDataText.setResource(Font.StockFont.Small)
            .setText("44.837064 , -0.686091")
            .setX(gpsLabelText.getX() + gpsLabelText.getWidth() + 5)
            .setY(50f)
            .setForegroundColor(EvsColor.Green)
            .addToScreen(this)

        // Time Label
        timeLabelText.setResource(Font.StockFont.Small)
            .setText("Time: ")
            .setCenter(getWidth() / 4)
            .setY(100f)
            .setForegroundColor(EvsColor.White)
            .addToScreen(this)

        // Time Data
        timeDataText.setResource(Font.StockFont.Small)
            .setText("Loading...")
            .setX(timeLabelText.getX() + timeLabelText.getWidth() + 5)
            .setY(100f)
            .setForegroundColor(EvsColor.Green)
            .addToScreen(this)

        // Wind Label
        windLabelText.setResource(Font.StockFont.Small)
            .setText("Wind: ")
            .setCenter(getWidth() / 4)
            .setY(150f)
            .setForegroundColor(EvsColor.White)
            .addToScreen(this)

        // Wind Data
        windDataText.setResource(Font.StockFont.Small)
            .setText("20 deg")
            .setX(windLabelText.getX() + windLabelText.getWidth() + 5)
            .setY(150f)
            .setForegroundColor(EvsColor.Green)
            .addToScreen(this)

        // Initialize LocationManager
        locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

        // Initialize LocationListener
        locationListener = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                val latitude = String.format("%.4f", location.latitude)
                val longitude = String.format("%.4f", location.longitude)
                Log.d(TAG, "Location changed: Latitude: $latitude, Longitude: $longitude")
                gpsDataText.setText("$latitude, $longitude")

                controlTowersScreen.updateControlTowersPositions(location)
            }

            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {
                Log.d(TAG, "Status changed: $status")
            }

            override fun onProviderEnabled(provider: String) {
                Log.d(TAG, "Provider enabled: $provider")
            }

            override fun onProviderDisabled(provider: String) {
                Log.d(TAG, "Provider disabled: $provider")
            }
        }

        // Request GPS updates
        requestLocationUpdates()

        // Subscribe to ATIS response topic
        subscribeToAtisResponse()

        // Subscribe to plane config topic
        subscribeToPlaneConfig()

        // Send ATIS request
        sendAtisRequest("LFMK")

        // Update time every second
        updateTime()

        // Initialize and add ControlTowersScreen
        val initialLocation = Location("").apply {
            latitude = initialLatitude
            longitude = initialLongitude
        }

        // Initialize and add ControlTowersScreen
        controlTowersScreen = ControlTowersScreen(context, mqttManager,controlTowerManager,initialLocation)

    }



    @SuppressLint("MissingPermission")
    private fun requestLocationUpdates() {
        if (context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            context.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

            if (isLocationEnabled()) {
                Log.d(TAG, "Requesting location updates")
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 1f, locationListener)
            } else {
                Log.d(TAG, "Location is disabled")
                gpsDataText.setText("Disabled")
            }
        } else {
            Log.d(TAG, "Location permissions not granted")
            gpsDataText.setText("No permissions")
        }
    }

    private fun isLocationEnabled(): Boolean {
        return try {
            val locationMode = Settings.Secure.getInt(context.contentResolver, Settings.Secure.LOCATION_MODE)
            locationMode != Settings.Secure.LOCATION_MODE_OFF
        } catch (e: Settings.SettingNotFoundException) {
            false
        }
    }

    private fun subscribeToAtisResponse() {
        mqttManager.subscribe("atis_response", 1) { topic, message ->
            if (topic == "atis_response") {
                val atisResponse = JSONObject(message.toString())
                val wind = atisResponse.getJSONObject("Wind")
                val windOrientation = wind.getString("Orientation")
                runOnUiThread {
                    windDataText.setText("$windOrientation°")
                }
            }
        }
    }

    private fun subscribeToPlaneConfig() {
        mqttManager.subscribe("plane_config", 1) { topic, message ->
            if (topic == "plane_config") {
                Log.d(TAG, "Message arrived from topic $topic: ${message.toString()}")
                val planeConfig = JSONObject(message.toString())
                controlTowersScreen.updateControlTowers(planeConfig)
            }
        }
    }

    private fun sendAtisRequest(oaci: String) {
        val atisRequest = JSONObject().apply {
            put("OACI", oaci)
        }
        mqttManager.publish("atis_request", atisRequest.toString())
    }

    private fun updateTime() {
        val timer = Timer()
        timer.scheduleAtFixedRate(object : TimerTask() {
            override fun run() {
                val currentTime = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
                runOnUiThread {
                    timeDataText.setText(currentTime)
                }
            }
        }, 0, 1000)
    }


    private fun runOnUiThread(action: Runnable) {
        (context as Activity).runOnUiThread(action)
    }

    override fun onTouch(touch: TouchDirection) {
        Log.d(TAG, "Touch detected: $touch")
        if (touch == TouchDirection.tap) {
            navigateToControlTowersScreen()
        }
    }

    private fun navigateToControlTowersScreen() {
        Log.d(TAG, "Navigating to ControlTowersScreen")
        Evs.instance().screens().removeTopmostScreen()
        Evs.instance().screens().addScreen(controlTowersScreen)
    }
}
