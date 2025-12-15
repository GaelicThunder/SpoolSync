package dev.gaelicthunder.spoolsync.util

import android.nfc.NdefMessage
import android.nfc.NdefRecord
import com.google.gson.Gson
import dev.gaelicthunder.spoolsync.data.FilamentProfile

object NFCHelper {

    private const val MIME_TYPE = "application/vnd.openprinttag"
    private val gson = Gson()

    fun createOpenPrintTagMessage(profile: FilamentProfile): NdefMessage {
        val jsonPayload = gson.toJson(
            mapOf(
                "version" to "1.0",
                "material" to profile.material,
                "manufacturer" to profile.brand,
                "name" to profile.name,
                "color" to mapOf(
                    "hex" to (profile.colorHex ?: "#FFFFFF").replace("#", ""),
                    "name" to profile.name
                ),
                "temperatures" to mapOf(
                    "nozzle" to mapOf(
                        "min" to (profile.minTemp ?: 200),
                        "max" to (profile.maxTemp ?: 230)
                    ),
                    "bed" to (profile.bedTemp ?: 60)
                ),
                "density" to profile.density,
                "diameter" to profile.diameter
            )
        )

        val mimeRecord = NdefRecord.createMime(MIME_TYPE, jsonPayload.toByteArray(Charsets.UTF_8))
        return NdefMessage(arrayOf(mimeRecord))
    }

    fun parseOpenPrintTagMessage(message: NdefMessage): FilamentProfile? {
        return try {
            val record = message.records.firstOrNull { 
                String(it.type) == MIME_TYPE 
            } ?: return null

            val jsonString = String(record.payload, Charsets.UTF_8)
            val data = gson.fromJson(jsonString, Map::class.java)

            FilamentProfile(
                name = data["name"] as? String ?: "Unknown",
                brand = data["manufacturer"] as? String ?: "Unknown",
                material = data["material"] as? String ?: "PLA",
                colorHex = "#${(data["color"] as? Map<*, *>)?.get("hex") as? String ?: "FFFFFF"}",
                minTemp = ((data["temperatures"] as? Map<*, *>)?.get("nozzle") as? Map<*, *>)?.get("min") as? Int,
                maxTemp = ((data["temperatures"] as? Map<*, *>)?.get("nozzle") as? Map<*, *>)?.get("max") as? Int,
                bedTemp = (data["temperatures"] as? Map<*, *>)?.get("bed") as? Int,
                density = (data["density"] as? Double)?.toFloat() ?: 1.24f,
                diameter = (data["diameter"] as? Double)?.toFloat() ?: 1.75f,
                isCustom = true,
                isFavorite = false
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
