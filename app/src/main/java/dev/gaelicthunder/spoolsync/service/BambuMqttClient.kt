package dev.gaelicthunder.spoolsync.service

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import dev.gaelicthunder.spoolsync.data.FilamentProfile
import org.eclipse.paho.android.service.MqttAndroidClient
import org.eclipse.paho.client.mqttv3.*
import java.security.SecureRandom
import java.security.cert.X509Certificate
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

class BambuMqttClient(
    private val context: Context,
    private val printerIp: String,
    private val serialNumber: String,
    private val accessCode: String
) {

    private var mqttClient: MqttAndroidClient? = null
    private val gson = Gson()
    private val serverUri = "ssl://$printerIp:8883"
    private val clientId = "SpoolSync_${System.currentTimeMillis()}"

    fun connect(onConnected: () -> Unit, onError: (String) -> Unit) {
        mqttClient = MqttAndroidClient(context, serverUri, clientId)

        val options = MqttConnectOptions().apply {
            userName = "bblp"
            password = accessCode.toCharArray()
            isAutomaticReconnect = true
            isCleanSession = true
            connectionTimeout = 10
            keepAliveInterval = 60
            socketFactory = getInsecureSocketFactory()
        }

        try {
            mqttClient?.connect(options, null, object : IMqttActionListener {
                override fun onSuccess(asyncActionToken: IMqttToken?) {
                    Log.d(TAG, "Connected to Bambu printer")
                    onConnected()
                }

                override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                    Log.e(TAG, "Connection failed: ${exception?.message}")
                    onError(exception?.message ?: "Unknown error")
                }
            })
        } catch (e: MqttException) {
            Log.e(TAG, "MQTT Exception: ${e.message}")
            onError(e.message ?: "MQTT Exception")
        }
    }

    fun setAmsFilament(amsId: Int, trayId: Int, filament: FilamentProfile) {
        val client = mqttClient ?: return
        if (!client.isConnected) {
            Log.w(TAG, "Not connected to printer")
            return
        }

        val topic = "device/$serialNumber/request"
        
        val payloadMap = mapOf(
            "print" to mapOf(
                "command" to "ams_filament_setting",
                "ams_id" to amsId,
                "tray_id" to trayId,
                "tray_info_idx" to (filament.vendorId.ifEmpty { "GFL99" }),
                "tray_color" to filament.getBambuColor(),
                "nozzle_temp_min" to (filament.minTemp ?: 200),
                "nozzle_temp_max" to (filament.maxTemp ?: 230),
                "tray_type" to filament.material
            )
        )

        val jsonPayload = gson.toJson(payloadMap)
        val message = MqttMessage(jsonPayload.toByteArray())
        message.qos = 1

        try {
            client.publish(topic, message)
            Log.d(TAG, "Sent filament config to AMS $amsId Slot $trayId")
        } catch (e: MqttException) {
            Log.e(TAG, "Publish failed: ${e.message}")
        }
    }

    fun disconnect() {
        try {
            mqttClient?.disconnect()
            mqttClient = null
        } catch (e: Exception) {
            Log.e(TAG, "Disconnect error: ${e.message}")
        }
    }

    private fun getInsecureSocketFactory(): javax.net.ssl.SSLSocketFactory {
        val trustAllCerts = arrayOf<TrustManager>(object : X509TrustManager {
            override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
            override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
            override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
        })

        val sslContext = SSLContext.getInstance("TLS")
        sslContext.init(null, trustAllCerts, SecureRandom())
        return sslContext.socketFactory
    }

    companion object {
        private const val TAG = "BambuMqttClient"
    }
}
