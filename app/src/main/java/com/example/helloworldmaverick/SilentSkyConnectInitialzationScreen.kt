package com.example.helloworldmaverick

import UIKit.app.Screen
import UIKit.app.data.TouchDirection
import UIKit.app.resources.Font
import UIKit.widgets.Text
import android.content.Context
import android.util.Log
import com.everysight.evskit.android.Evs
import UIKit.app.resources.ImgSrc
import UIKit.widgets.Image

class SilentSkyConnectInitializationScreen(private val context: Context, private val mqttManager: MqttManager) : Screen() {
    private val status = Text()
    private val logo = Image()
    private val title = Text()
    private var refTimestampMs = 0L

    companion object {
        private const val TAG = "SilentSkyConnectInitializationScreen"
    }

    override fun onCreate() {
        super.onCreate()

        val logoSrc = ImgSrc("SSC.png", ImgSrc.Slot.s1)
        logo.setResource(logoSrc)
            .setX(getWidth() / 2 - logoSrc.imageWidth / 2)
            .setY(getHeight() / 3 - logoSrc.imageHeight / 2)
            .setWidth(100f)
            .setHeight(100f)
        add(logo)

        title.setResource(Font.StockFont.Medium)
            .setText("Siilent Sky Connect")
            .setCenter(getWidth() / 2)
            .setY(logo.getY() + logo.getHeight() + 20)
            .addToScreen(this)

        status.setResource(Font.StockFont.Small)
            .setText("Tap to Continue")
            .setCenter(getWidth() / 2)
            .setY(logo.getY() + logo.getHeight() + 70)
            .addToScreen(this)
    }

    override fun onUpdateUI(timestampMs: Long) {
        super.onUpdateUI(timestampMs)
        val dots = ((timestampMs - refTimestampMs) / 500 % 4).toInt()
        val loadingText = "Tap to Continue" + ".".repeat(dots)
        status.setText(loadingText)
    }

    override fun onTouch(touch: TouchDirection) {
        Log.d(TAG, "Touch detected: $touch")
        if (touch == TouchDirection.tap) {
            navigateToNextScreen()
        }
    }

    private fun navigateToNextScreen() {
        Log.d(TAG, "Navigating to the next screen")
        Evs.instance().screens().removeTopmostScreen()
        Evs.instance().screens().addScreen(MainScreen(context, mqttManager))
    }
}

