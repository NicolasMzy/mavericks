package com.example.helloworldmaverick

import UIKit.app.Screen
import UIKit.app.resources.Font
import UIKit.widgets.Text
import UIKit.widgets.Image
import android.content.Context
import android.location.Location
import org.json.JSONObject
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.PI
import kotlin.math.atan2
import UIKit.services.IEvsYprSensorsEvents
import UIKit.app.data.YprData
import UIKit.app.data.CalibrationStatus
import UIKit.app.data.EvsColor
import UIKit.app.data.TouchDirection
import UIKit.app.resources.ImgSrc
import com.everysight.evskit.android.Evs
import android.util.Log
import kotlin.math.tan

class ControlTowersScreen(
    private val context: Context,
    private val mqttManager: MqttManager,
    private val controlTowerManager: ControlTowerManager,
    private var currentLocation: Location
) : Screen(), IEvsYprSensorsEvents {
    private val controlTowers = mutableListOf<ControlTowerDisplay>()
    private var currentYaw: Float = 0f
    private var currentPitch: Float = 0f
    private var currentRoll: Float = 0f

    private val waitingText = Text()

    companion object {
        private const val TAG = "ControlTowersScreen"
        private const val OFF_SCREEN_POSITION = -1000f // Position pour masquer les éléments hors de l'écran
    }

    data class ControlTowerDisplay(val text: Text, val image: Image, val distanceText: Text)

    override fun onCreate() {
        super.onCreate()
        Evs.instance().sensors().registerYprSensorsEvents(this)
        setScreenRenderRate(ScreenRenderRate.video)

        waitingText.setResource(Font.StockFont.Small)
            .setText("Attente de la config de vol")
            .setCenter(getWidth() / 2)
            .setY(getHeight() / 2)
            .setForegroundColor(EvsColor.White)
            .addToScreen(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        Evs.instance().sensors().unregisterYprSensorsEvents(this)
    }

    override fun onYpr(timestampMs: Long, yprData: YprData, calibrationStatus: CalibrationStatus) {
        currentYaw = adjustYaw(yprData.yaw)
        currentPitch = yprData.pitch
        currentRoll = yprData.roll
        Log.d(TAG, "Yaw updated: $currentYaw, Pitch updated: $currentPitch, Roll updated: $currentRoll")
        updateControlTowersPositions()
    }
    override fun onTouch(touch: TouchDirection) {
        if (touch == TouchDirection.tap) {
            Log.d(TAG, "Tap detected, returning to MainScreen")
            Evs.instance().screens().removeTopmostScreen()
            Evs.instance().screens().addScreen(MainScreen(context, mqttManager))
        }
    }

    fun updateControlTowers(flightPlan: JSONObject) {
        waitingText.setVisibility(false)

        // Clear existing control towers
        controlTowers.forEach {
            remove(it.text)
            remove(it.image)
            remove(it.distanceText)
        }
        controlTowers.clear()

        val flightPlanArray = flightPlan.getJSONArray("FlightPlan")
        for (i in 0 until flightPlanArray.length()) {
            val tower = flightPlanArray.getJSONObject(i)
            val identTo = tower.getString("Ident_to")
            val controlTower = controlTowerManager.getControlTower(identTo)
            if (controlTower != null) {
                val text = Text().apply {
                    setResource(Font.StockFont.Small)
                    setText(identTo)
                    setCenter(getWidth() / 2)
                    setY(getHeight() / 2)
                    addToScreen(this@ControlTowersScreen)
                }
                val image = Image().apply {
                    setResource(ImgSrc("OACI.png", ImgSrc.Slot.s1))
                    setWidth(50f)
                    setHeight(50f)
                    setX(text.getX() + 50)
                    setY(text.getY())
                    addToScreen(this@ControlTowersScreen)
                }
                val distanceText = Text().apply {
                    setResource(Font.StockFont.Small)
                    setForegroundColor(EvsColor.Green)
                    setText("0 km")
                    setX(text.getX() - 50)
                    setY(text.getY())
                    addToScreen(this@ControlTowersScreen)
                }
                controlTowers.add(ControlTowerDisplay(text, image, distanceText))
            }
        }
        updateControlTowersPositions()
    }

    fun updateControlTowersPositions(location: Location? = null) {
        location?.let { currentLocation = it }

        val screenWidth = getWidth().toFloat()
        val screenHeight = getHeight().toFloat()

        controlTowers.forEach { display ->
            val controlTower = controlTowerManager.getControlTower(display.text.getText())
            if (controlTower != null) {
                val bearing = currentLocation.bearingTo(controlTower.toLocation())
                val adjustedBearing = adjustBearing(bearing - currentYaw)

                // Ne pas afficher si l'angle est en dehors du champ de vision avant
                if (adjustedBearing < -90 || adjustedBearing > 90) {
                    display.text.setX(OFF_SCREEN_POSITION) // Positionner en dehors de l'écran
                    display.text.setY(OFF_SCREEN_POSITION) // Positionner en dehors de l'écran
                    display.image.setX(OFF_SCREEN_POSITION) // Positionner en dehors de l'écran
                    display.image.setY(OFF_SCREEN_POSITION) // Positionner en dehors de l'écran
                    display.distanceText.setX(OFF_SCREEN_POSITION) // Positionner en dehors de l'écran
                    display.distanceText.setY(OFF_SCREEN_POSITION) // Positionner en dehors de l'écran
                    return@forEach
                }

                val distance = currentLocation.distanceTo(controlTower.toLocation())
                val distanceInKm = distance / 1000.0

                val normalizedDistance = distance / 300

                val x = screenWidth / 2 + (normalizedDistance * sin(adjustedBearing.toRadians())).toFloat()
                val y = screenHeight / 2 

                display.text.setX(x)
                display.text.setY(y)
                display.image.setX(x + 50)
                display.image.setY(y)
                display.distanceText.setX(x - 70)
                display.distanceText.setY(y + 40)
                display.distanceText.setText(String.format("%.1f km", distanceInKm))

                Log.d(TAG, "Updated position of ${display.text.getText()} to x: $x, y: $y with bearing: $bearing, adjusted bearing: $adjustedBearing, distance: $distanceInKm km")
            }
        }
    }

    private fun Float.toRadians(): Double = this * (PI / 180)
    private fun Double.toDegrees(): Double = this * (180 / PI)

    private fun adjustYaw(yaw: Float): Float {
        return if (yaw > 180) yaw - 360 else yaw
    }

    private fun adjustBearing(bearing: Float): Float {
        var adjustedBearing = bearing
        if (adjustedBearing > 180) {
            adjustedBearing -= 360
        } else if (adjustedBearing < -180) {
            adjustedBearing += 360
        }
        return adjustedBearing
    }

}
