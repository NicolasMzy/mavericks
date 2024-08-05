package com.example.helloworldmaverick

import UIKit.app.Animator
import UIKit.app.Screen
import UIKit.app.data.TouchDirection
import UIKit.widgets.Image
import UIKit.widgets.UIElement
import com.everysight.evskit.android.Evs
import UIKit.app.resources.ImgSrc
import android.util.Log

class BouncingMailScreen(private val nextScreen: Screen) : Screen() {
    private val mailImage = Image()

    companion object {
        private const val TAG = "BouncingMailScreen"
    }

    override fun onCreate() {
        super.onCreate()

        mailImage.setResource(ImgSrc("email.png",ImgSrc.Slot.s1))
            .setWidthHeight(100f, 100f)
            .setX(getWidth() / 2 - 50f)
            .setY(getHeight() / 2 - 50f)
            .addToScreen(this)
    }

    override fun onResume() {
        super.onResume()
        mailImage.animator()
            .scaleX(30f).scaleY(50f)
            .duration(1000)
            .translateTo(getWidth() / 2 - 80f, getHeight() / 4)
            .bounce()
            .start()
    }

    override fun onTouch(touch: TouchDirection) {
        Log.d(TAG, "Touch detected: $touch")
        if (touch == TouchDirection.tap) {
            Evs.instance().screens().removeTopmostScreen()
            Evs.instance().screens().addScreen(nextScreen)
        }
    }
}
