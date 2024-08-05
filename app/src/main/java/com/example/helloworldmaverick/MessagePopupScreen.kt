package com.example.helloworldmaverick

import UIKit.app.Screen
import UIKit.app.data.TouchDirection
import UIKit.app.resources.Font
import UIKit.widgets.Text
import android.util.Log
import com.everysight.evskit.android.Evs
import com.google.gson.Gson
import UIKit.app.data.EvsColor

data class Message(
    val Ident_from: String,
    val Ident_to: String,
    val Text: String,
    val Picto: String?,
    val Type: Int,
    val HaptPattern: String?
)

class MessagePopupScreen(private val message: String) : Screen() {
    private val sender = Text()
    private val messages = Text()
    private val gson = Gson()

    companion object {
        private const val TAG = "MessagePopupScreen"
    }

    override fun onCreate() {
        super.onCreate()

        val message = gson.fromJson(message, Message::class.java)
        val to = "de : ${message.Ident_from}"

        sender.setResource(Font.StockFont.Medium)
            .setText(to)
            .setCenter(getWidth() / 3)
            .setY(getHeight() / 3)
            .addToScreen(this)

        messages.setResource(Font.StockFont.Small)
            .setText(message.Text)
            .setCenter(getWidth() / 2)
            .setY(getHeight() / 2)
            .setForegroundColor(EvsColor.Green.rgba)
            .addToScreen(this)
    }

    override fun onUpdateUI(timestampMs: Long) {
        super.onUpdateUI(timestampMs)
    }

    override fun onTouch(touch: TouchDirection) {
        Log.d(TAG, "Touch detected: $touch")
        if (touch == TouchDirection.tap) {
            Evs.instance().screens().removeTopmostScreen()
        }
    }
}
