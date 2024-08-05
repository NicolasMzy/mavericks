package com.example.helloworldmaverick

import UIKit.app.Screen
import UIKit.app.resources.Font
import UIKit.app.resources.ImgSrc
import UIKit.widgets.Image
import UIKit.widgets.Text
import UIKit.app.data.TouchDirection
import UIKit.app.resources.UIResource
import android.util.Log
import com.everysight.evskit.android.Evs

class GlassesControlScreen : Screen() {

    private val text = Text()
    private val images = mutableListOf<Image>()
    private var currentImageIndex = 0

    companion object {
        private const val TAG = "GlassesControlScreen"
        private val screenImages = arrayListOf<ImgSrc>()
        init {
            screenImages.add(ImgSrc("pictogram/BONJOUR.png", ImgSrc.Slot.s1))
            screenImages.add(ImgSrc("pictogram/CAP.png", ImgSrc.Slot.s2))
            screenImages.add(ImgSrc("pictogram/CHANGE_RADIO.png", ImgSrc.Slot.s3))
            screenImages.add(ImgSrc("pictogram/FINALE_MG.png", ImgSrc.Slot.s4))
            screenImages.add(ImgSrc("pictogram/METEO DEF.png", ImgSrc.Slot.s5))
            screenImages.add(ImgSrc("pictogram/NUMERO.png", ImgSrc.Slot.s6))
            screenImages.add(ImgSrc("pictogram/POINT_ATTENTE_PISTE.png", ImgSrc.Slot.s7))
            screenImages.add(ImgSrc("pictogram/REMIZE.png", ImgSrc.Slot.s8))
            screenImages.add(ImgSrc("pictogram/TRAFFIC_TAXIWAY.png", ImgSrc.Slot.s9))

            // Add more images as needed
        }
    }

    override fun onCreate() {
        super.onCreate()
        text.setResource(Font.StockFont.Small).setCenter(getWidth() / 2).setY(getHeight() / 3)
        add(text)
        requestResourcesUpload(screenImages)
        text.setVisibility(true)
        text.setText("Loading...")
    }

    override fun onResourceUploadResult(resource: UIResource) {
        text.setText("${resource.getResourceName()} was loaded")
        text.setText("${resource.getResourceName()} has given ")
    }

    override fun onResourcesUploadEnd() {
        super.onResourcesUploadEnd()
        text.setVisibility(false)
        val logoSize = 100f // Increase the size of the logos
        screenImages.forEachIndexed { index, imgSrc ->
            val image = Image().setResource(imgSrc)
                .setX(getWidth()/2 - imgSrc.imageWidth /2)
                .setY(getHeight()/2 - imgSrc.imageHeight /2)
                .setWidth(logoSize)
                .setHeight(logoSize)
            if (index != 0) {
                image.setVisibility(false)
            }
            images.add(image as Image)
            add(image)
        }
    }

    override fun onUpdateUI(timestampMs: Long) {
        super.onUpdateUI(timestampMs)
        val dots = ((timestampMs - System.currentTimeMillis()) / 500 % 4).toInt()
        val loadingText = "Tap to Continue" + ".".repeat(dots)
        text.setText(loadingText)
    }

    override fun onTouch(touch: TouchDirection) {
        Log.d(TAG, "Touch detected: $touch")
        when (touch) {
            TouchDirection.forward -> showNextImage()
            TouchDirection.backward -> showPreviousImage()
            else -> {}
        }
    }

    private fun showNextImage() {
        if (images.isNotEmpty()) {
            images[currentImageIndex].setVisibility(false)
            currentImageIndex = (currentImageIndex + 1) % images.size
            images[currentImageIndex].setVisibility(true)
        }
    }

    private fun showPreviousImage() {
        if (images.isNotEmpty()) {
            images[currentImageIndex].setVisibility(false)
            currentImageIndex = (currentImageIndex - 1 + images.size) % images.size
            images[currentImageIndex].setVisibility(true)
        }
    }

}
