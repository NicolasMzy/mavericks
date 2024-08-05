package com.example.helloworldmaverick

import UIKit.app.data.SensorRate
import UIKit.services.AppErrorCode
import UIKit.services.IEvsAppEvents
import UIKit.services.IEvsCommunicationEvents
import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import com.everysight.evskit.android.Evs
import com.example.helloworldmaverick.R
import org.eclipse.paho.client.mqttv3.MqttMessage
import java.io.ByteArrayOutputStream
import java.io.InputStream
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class SilentSkyConnectActivity : Activity(), IEvsCommunicationEvents, IEvsAppEvents {
    private lateinit var txtStatus: TextView
    private lateinit var mqttManager: MqttManager
    private val locationPermissionCode = 2

    companion object {
        private const val TAG = "SilentSkyConnectActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_silentskyconnect)

        txtStatus = findViewById(R.id.txtStatus)

        checkPerm()
        initSdk()
        connectToSimulator()

        findViewById<Button>(R.id.btnConfigure).setOnClickListener {
            Evs.instance().showUI("configure")
        }

        findViewById<Button>(R.id.btnSettings).setOnClickListener {
            Evs.instance().showUI("settings")
        }

        initMqtt()
    }


    private fun initMqtt() {
        val serverUri = "tcp://10.42.0.1:1883"

        mqttManager = MqttManager(this, serverUri)

        mqttManager.connect("maverick", "fans4all", {
            runOnUiThread {
                txtStatus.text = "Connected to MQTT broker"
            }
            // Subscribe to messages topic
            mqttManager.subscribe("messages", 1) { topic, message ->
                handleIncomingMessage(topic, message)
            }
            // Show the initialization screen
            Evs.instance().screens().addScreen(SilentSkyConnectInitializationScreen(this, mqttManager))
        }, { exception ->
            runOnUiThread {
                txtStatus.text = "Failed to connect to MQTT broker: ${exception?.message}"
            }
        })
    }

    private fun handleIncomingMessage(topic: String?, message: MqttMessage?) {
        runOnUiThread {
            txtStatus.text = "Message from $topic: ${message.toString()}"
        }
    }
    private fun disableSensors() {
        Log.i(TAG,"disableSensors")
        with(Evs.instance().sensors() ){
            disableInertialSensors()
        }
    }

    private fun enableSensor() {

        Log.i(TAG,"enableSensor")
        with(Evs.instance().sensors() ){
            enableSensorsFusion(SensorRate.rate1, false)
        }
    }


    override fun onDestroy() {
        super.onDestroy()
        Evs.instance().unregisterAppEvents(this)
        Evs.instance().comm().unregisterCommunicationEvents(this)
        Evs.instance().stop()
        mqttManager.disconnect()
        disableSensors()
    }

    private fun initSdk() {
        Log.d(TAG, "initSdk: Initializing SDK")
        Evs.init(this).start()
        Evs.startDefaultLogger()

        try {
            val inputStream: InputStream = resources.openRawResource(R.raw.sdk)
            val byteArrayOutputStream = ByteArrayOutputStream()
            var byte: Int = inputStream.read()
            while (byte != -1) {
                byteArrayOutputStream.write(byte)
                byte = inputStream.read()
            }
            val sdkKey = byteArrayOutputStream.toByteArray()
            Log.d(TAG, "initSdk: SDK key loaded successfully")
            Evs.instance().auth().setApiKey(sdkKey)
        } catch (e: Exception) {
            Log.e(TAG, "initSdk: Failed to load SDK key", e)
            runOnUiThread {
                txtStatus.text = "Failed to load SDK key"
            }
            return
        }

        Evs.instance().registerAppEvents(this)
        with(Evs.instance().comm()) {
            registerCommunicationEvents(this@SilentSkyConnectActivity)
            if (hasConfiguredDevice()) connect()
        }
    }

    private fun checkPerm() {
        val permissionsRequested = mutableListOf(
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissionsRequested.add(Manifest.permission.BLUETOOTH_SCAN)
            permissionsRequested.add(Manifest.permission.BLUETOOTH_CONNECT)
        } else {
            permissionsRequested.add(Manifest.permission.BLUETOOTH)
        }

        val permissionsToRequest = permissionsRequested.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (permissionsToRequest.isNotEmpty()) {
            Log.d(TAG, "Requesting permissions")
            ActivityCompat.requestPermissions(this, permissionsToRequest.toTypedArray(), locationPermissionCode)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == locationPermissionCode) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "Location permissions granted")
            } else {
                Log.d(TAG, "Location permissions denied")
            }
        }
    }

    private fun connectToSimulator() {
        val simulatorIp = "10.9.42.36"
        Log.d(TAG, "Attempting to set device info with IP: $simulatorIp")
        Evs.instance().comm().setDeviceInfo("udp://$simulatorIp", "Simulator")
        Log.d(TAG, "Attempting to connect to the simulator")
        Evs.instance().comm().connect()
    }

    override fun onAdapterStateChanged(isEnabled: Boolean) {
        runOnUiThread { txtStatus.text = "Adapter enabled=$isEnabled" }
    }

    override fun onConnected() {
        runOnUiThread { txtStatus.text = "${Evs.instance().comm().getDeviceName()} is Connected" }

    }

    override fun onConnecting() {
        runOnUiThread { txtStatus.text = "Connecting ${Evs.instance().comm().getDeviceName()}" }
    }

    override fun onDisconnected() {
        runOnUiThread { txtStatus.text = "Disconnected" }
    }

    override fun onFailedToConnect() {
        runOnUiThread { txtStatus.text = "${Evs.instance().comm().getDeviceName()} Failed to connect" }
    }

    override fun onReady() {
        Evs.instance().display().turnDisplayOn()
        enableSensor()
        Evs.instance().sensors().enableTouch(true)
    }

    override fun onError(errCode: AppErrorCode, description: String) {
        runOnUiThread { txtStatus.text = "Error: $errCode" }
    }
}
