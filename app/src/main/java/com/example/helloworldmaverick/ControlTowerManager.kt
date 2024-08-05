package com.example.helloworldmaverick

import android.content.Context
import android.location.Location
import org.json.JSONObject
import java.io.IOException
import java.io.InputStream

class ControlTowerManager(private val context: Context) {
    private val controlTowers = mutableMapOf<String, ControlTower>()

    init {
        loadControlTowers()
    }

    private fun loadControlTowers() {
        try {
            val inputStream: InputStream = context.assets.open("control_towers.json")
            val size = inputStream.available()
            val buffer = ByteArray(size)
            inputStream.read(buffer)
            inputStream.close()

            val jsonString = String(buffer, Charsets.UTF_8)
            val jsonObject = JSONObject(jsonString)
            jsonObject.keys().forEach { key ->
                val tower = jsonObject.getJSONObject(key)
                val oaci = tower.getString("oaci")
                val latitude = tower.getDouble("latitude")
                val longitude = tower.getDouble("longitude")
                controlTowers[oaci] = ControlTower(oaci, latitude, longitude)
            }
        } catch (ex: IOException) {
            ex.printStackTrace()
        }
    }

    fun getControlTower(oaci: String): ControlTower? {
        return controlTowers[oaci]
    }
}

data class ControlTower(val oaci: String, val latitude: Double, val longitude: Double) {
    fun toLocation(): Location {
        val location = Location("")
        location.latitude = latitude
        location.longitude = longitude
        return location
    }
}
