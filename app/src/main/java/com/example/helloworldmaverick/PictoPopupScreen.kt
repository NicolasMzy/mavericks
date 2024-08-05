package com.example.helloworldmaverick

import UIKit.app.Screen
import UIKit.app.data.EvsColor
import UIKit.app.data.TouchDirection
import UIKit.app.resources.Font
import UIKit.app.resources.ImgSrc
import UIKit.widgets.Image
import UIKit.widgets.Text
import android.util.Log
import com.google.gson.Gson
import com.everysight.evskit.android.Evs

class PictoPopupScreen(private val jsonString: String) : Screen() {
    private val gson = Gson()
    private val status = Text()
    private val image = Image()

    companion object {
        private const val TAG = "PictoPopupScreen"
    }

    override fun onCreate() {
        super.onCreate()

        // Parse the JSON string
        val message = gson.fromJson(jsonString, Message::class.java)

        // Display sender and text
        val displayMessage = "de : ${message.Ident_from}"

        status.setResource(Font.StockFont.Medium)
            .setText(displayMessage)
            .setCenter(getWidth() / 3)
            .setY(getHeight() / 5)
            .setForegroundColor(EvsColor.White)
            .addToScreen(this)

        // Display image if Picto is available
        message.Picto?.let {
            val pictoPath = "pictogram/$it.png"
            image.setResource(ImgSrc(pictoPath,ImgSrc.Slot.s1))
                .setWidthHeight(100f, 100f)
                .setX(getWidth() / 3 - 50f)
                .setY(getHeight() / 3)
                .addToScreen(this)
        }
    }

    override fun onUpdateUI(timestampMs: Long) {
        super.onUpdateUI(timestampMs)
        // Ajouter une logique de mise à jour si nécessaire
    }

    override fun onTouch(touch: TouchDirection) {
        Log.d(TAG, "Touch detected: $touch")
        if (touch == TouchDirection.tap) {
            Evs.instance().screens().removeTopmostScreen()
        }
    }
}
