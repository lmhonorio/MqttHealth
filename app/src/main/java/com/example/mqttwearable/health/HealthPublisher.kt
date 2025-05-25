package com.example.mqttwearable.health

import android.content.Context
import android.util.Log
import androidx.health.services.client.HealthServices
import androidx.health.services.client.MeasureCallback
import androidx.health.services.client.MeasureClient
import androidx.health.services.client.PassiveListenerCallback
import androidx.health.services.client.PassiveMonitoringClient
import androidx.health.services.client.data.Availability
import androidx.health.services.client.data.DataPointContainer
import androidx.health.services.client.data.DataType
import androidx.health.services.client.data.DataTypeAvailability
import androidx.health.services.client.data.DeltaDataType
import androidx.health.services.client.data.PassiveListenerConfig
import androidx.health.services.client.getCapabilities
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.example.mqttwearable.mqtt.MqttHandler

public class HealthPublisher(
    context: Context,
    private val mqttHandler: MqttHandler
) {
    private val passiveMonitoringClient: PassiveMonitoringClient =
        HealthServices.getClient(context).passiveMonitoringClient

    private val measureClient: MeasureClient =
        HealthServices.getClient(context).measureClient

    // Callback que recebe os lotes de dados enquanto o app está rodando em foreground
    private val passiveListener = object : PassiveListenerCallback {
        override fun onNewDataPointsReceived(dataPoints: DataPointContainer) {
            dataPoints.getData(DataType.HEART_RATE_BPM)
                .forEach { send("HeartRate: ${it.value} bpm") }

            dataPoints.getData(DataType.STEPS)
                .forEach { send("Steps: ${it.value}") }

            dataPoints.getData(DataType.CALORIES)
                .forEach { send("Calories: ${it.value} kcal") }

            dataPoints.getData(DataType.DISTANCE)
                .forEach { send("Distance: ${it.value} m") }

            dataPoints.getData(DataType.FLOORS)
                .forEach { send("Floors: ${it.value} unit") }

            dataPoints.getData(DataType.DISTANCE_DAILY)
                .forEach { send("Daily Distance : ${it.value} m") }

            dataPoints.getData(DataType.ELEVATION_GAIN)
                .forEach { send("ELEVATION_GAIN : ${it.value} m") }
        }

        private fun send(message: String) {
            Log.d("HealthPublisher", "Sending via MQTT: $message")
            CoroutineScope(Dispatchers.IO).launch {
                mqttHandler.publish("health/data", message)
            }
        }
    }



    /** Registra o listener para os tipos de dados desejados. */
    suspend fun startPassiveMeasure() {
        val types = setOf(
            DataType.CALORIES,        // calorias
            DataType.DISTANCE,        // distância (m)
            DataType.ELEVATION_GAIN,
            DataType.FLOORS,
            DataType.DISTANCE_DAILY,
            DataType.STEPS,
            DataType.HEART_RATE_BPM  // batimentos
        )
        val config = PassiveListenerConfig.builder()
            .setDataTypes(types)
            .build()
        passiveMonitoringClient.setPassiveListenerCallback(config, passiveListener)
        Log.d("HealthPublisher", "PassiveListener registered for $types")

        val capabilities = passiveMonitoringClient.getCapabilities().supportedDataTypesPassiveMonitoring
        Log.d("HealthPublisher", "Supported passive types: ${capabilities}")
        mqttHandler.publish("teste",  "Supported passive types: ${capabilities}")
    }

    /** Cancela o listener */
    fun stopPassiveMeasure() {
        passiveMonitoringClient.clearPassiveListenerCallbackAsync() // :contentReference[oaicite:1]{index=1}
        Log.d("HealthPublisher", "PassiveListener unregistered")
    }

}
