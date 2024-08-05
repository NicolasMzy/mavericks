package com.example.helloworldmaverick

import android.content.Context
import android.util.Log
import info.mqtt.android.service.MqttAndroidClient
import org.eclipse.paho.client.mqttv3.IMqttActionListener
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken
import org.eclipse.paho.client.mqttv3.IMqttToken
import org.eclipse.paho.client.mqttv3.MqttCallback
import org.eclipse.paho.client.mqttv3.MqttConnectOptions
import org.eclipse.paho.client.mqttv3.MqttException
import org.eclipse.paho.client.mqttv3.MqttMessage
import java.util.UUID
import com.everysight.evskit.android.Evs
import com.google.gson.Gson

class MqttManager(private val context: Context, private val serverUri: String) {

    private val mqttClient: MqttAndroidClient = MqttAndroidClient(context, serverUri, UUID.randomUUID().toString())
    private val gson = Gson()

    // Map to store callbacks for each topic
    private val topicCallbacks = mutableMapOf<String, (String?, MqttMessage?) -> Unit>()

    init {
        mqttClient.setCallback(object : MqttCallback {
            override fun connectionLost(cause: Throwable?) {
                Log.d(TAG, "MQTT connection lost")
            }

            override fun messageArrived(topic: String?, message: MqttMessage?) {
                Log.d(TAG, "Message arrived from topic $topic: ${message.toString()}")
                topic?.let {
                    topicCallbacks[it]?.invoke(it, message)
                }

                if (topic == "messages") {
                    Log.d(TAG, "Message arrived from topic $topic: ${message.toString()}")
                    val parsedMessage = gson.fromJson(message.toString(), Message::class.java)
                    val nextScreen = if (parsedMessage.Picto != null) {
                        PictoPopupScreen(message.toString())
                    } else {
                        MessagePopupScreen(message.toString())
                    }
                    Evs.instance().screens().addScreen(BouncingMailScreen(nextScreen))
                }
            }

            override fun deliveryComplete(token: IMqttDeliveryToken?) {
                Log.d(TAG, "Message delivered")
            }
        })
    }

    fun connect(username: String?, password: String?, onSuccess: () -> Unit, onFailure: (Throwable?) -> Unit) {
        val options = MqttConnectOptions()
        options.userName = username
        options.password = password?.toCharArray()

        try {
            mqttClient.connect(options, null, object : IMqttActionListener {
                override fun onSuccess(asyncActionToken: IMqttToken?) {
                    Log.d(TAG, "MQTT connected")
                    onSuccess()
                }

                override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                    Log.d(TAG, "MQTT connection failed: ${exception?.message}")
                    onFailure(exception)
                }
            })
        } catch (e: MqttException) {
            e.printStackTrace()
            onFailure(e)
        }
    }

    fun subscribe(topic: String, qos: Int = 1, callback: (String?, MqttMessage?) -> Unit) {
        try {
            mqttClient.subscribe(topic, qos, null, object : IMqttActionListener {
                override fun onSuccess(asyncActionToken: IMqttToken?) {
                    Log.d(TAG, "Subscribed to topic: $topic")
                    topicCallbacks[topic] = callback
                }

                override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                    Log.d(TAG, "Failed to subscribe to topic: $topic")
                }
            })
        } catch (e: MqttException) {
            e.printStackTrace()
        }
    }

    fun publish(topic: String, message: String, qos: Int = 1, retained: Boolean = false) {
        try {
            val mqttMessage = MqttMessage()
            mqttMessage.payload = message.toByteArray()
            mqttMessage.qos = qos
            mqttMessage.isRetained = retained
            mqttClient.publish(topic, mqttMessage)
            Log.d(TAG, "Message published to topic: $topic")
        } catch (e: MqttException) {
            e.printStackTrace()
        }
    }

    fun disconnect() {
        try {
            mqttClient.disconnect(null, object : IMqttActionListener {
                override fun onSuccess(asyncActionToken: IMqttToken?) {
                    Log.d(TAG, "Successfully disconnected")
                }

                override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                    Log.d(TAG, "Failed to disconnect: ${exception?.message}")
                }
            })
        } catch (e: MqttException) {
            e.printStackTrace()
        }
    }

    companion object {
        private const val TAG = "MqttManager"
    }
}
