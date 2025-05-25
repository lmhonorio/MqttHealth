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
            // Heart rate (Float)
            dataPoints.getData(DataType.HEART_RATE_BPM).forEach { hrPoint ->
                send("HeartRate: ${hrPoint.value}")
            }
            // Steps (Int or Long, conforme dispositivo)
            dataPoints.getData(DataType.STEPS).forEach { stepPoint ->
                send("Steps: ${stepPoint.value}")
            }
            // Calories (Float)
            dataPoints.getData(DataType.CALORIES).forEach { calPoint ->
                send("Calories: ${calPoint.value}")
            }
            // Distance (Float, metros)
            dataPoints.getData(DataType.DISTANCE).forEach { distPoint ->
                send("Distance: ${distPoint.value}")
            }
        }

        private fun send(message: String) {
            Log.d("HealthPublisher", "Sending via MQTT: $message")
            CoroutineScope(Dispatchers.IO).launch {
                mqttHandler.publish("health/data", message)
            }
        }
    }


    // CALLBACK para medições ativas
    private val measureCallback = object : MeasureCallback {
        override fun onAvailabilityChanged(
            dataType: DeltaDataType<*, *>,
            availability: Availability
        ) {
            if (availability is DataTypeAvailability) {
                Log.d("HealthPublisher", "Availability for $dataType: $availability")
            }
        }

        override fun onDataReceived(dataPoints: DataPointContainer) {
            // Exemplo para HEART_RATE_BPM; repita register/unregister para cada tipo ativo
            dataPoints.getData(DataType.HEART_RATE_BPM).forEach { point ->
                send("Active HEART_RATE_BPM: ${point.value}")
            }
            // Steps (Int or Long, conforme dispositivo)
            dataPoints.getData(DataType.STEPS).forEach { stepPoint ->
                send("Steps: ${stepPoint.value}")
            }
            // Calories (Float)
            dataPoints.getData(DataType.CALORIES).forEach { calPoint ->
                send("Calories: ${calPoint.value}")
            }
            // Distance (Float, metros)
            dataPoints.getData(DataType.DISTANCE).forEach { distPoint ->
                send("Distance: ${distPoint.value}")
            }
        }

        override fun onRegistered() {
            super.onRegistered()
            Log.d("HealthPublisher", "MeasureCallback registered")
        }

        override fun onRegistrationFailed(throwable: Throwable) {
            super.onRegistrationFailed(throwable)
            Log.e("HealthPublisher", "MeasureCallback failed", throwable)
        }

        private fun send(message: String) {
            CoroutineScope(Dispatchers.IO).launch {
                mqttHandler.publish("health/data", message)
            }
        }
    }

    /** Registra medições ativas “batch” (um por um) para os tipos desejados. */
    fun startActiveMeasurementsBatch(types: Set<DeltaDataType<*, *>>) {
        types.forEach { dt ->
            measureClient.registerMeasureCallback(dt, measureCallback)  // :contentReference[oaicite:0]{index=0}
            Log.d("HealthPublisher", "Active listener started for $dt")
        }
    }

    /** Cancela todas as medições ativas registradas em batch. */
    fun stopActiveMeasurementsBatch(types: Set<DeltaDataType<*, *>>) {
        types.forEach { dt ->
            measureClient.unregisterMeasureCallbackAsync(dt, measureCallback)  // :contentReference[oaicite:1]{index=1}
            Log.d("HealthPublisher", "Active listener stopped for $dt")
        }
    }



    // para registrar o callback ativo:
    fun startActiveMeasure() {
        measureClient.registerMeasureCallback(
            DataType.HEART_RATE_BPM,
            measureCallback
        )  // :contentReference[oaicite:1]{index=1}
        measureClient.registerMeasureCallback(
            DataType.STEPS,
            measureCallback
        )
        measureClient.registerMeasureCallback(
            DataType.CALORIES,
            measureCallback
        )
        measureClient.registerMeasureCallback(
            DataType.DISTANCE,
            measureCallback
        )
        measureClient.registerMeasureCallback(
            DataType.FLOORS,
            measureCallback
        )
    }



    // para desregistrar:
    fun stopActiveMeasure() {

        measureClient.unregisterMeasureCallbackAsync(
            DataType.HEART_RATE_BPM,
            measureCallback
        )  // :contentReference[oaicite:1]{index=1}
        measureClient.unregisterMeasureCallbackAsync(
            DataType.STEPS,
            measureCallback
        )
        measureClient.unregisterMeasureCallbackAsync(
            DataType.CALORIES,
            measureCallback
        )
        measureClient.unregisterMeasureCallbackAsync(
            DataType.DISTANCE,
            measureCallback
        )
        measureClient.unregisterMeasureCallbackAsync(
            DataType.FLOORS,
            measureCallback
        )
    }

    /** Registra o listener para os tipos de dados desejados. */
    fun startPassiveMeasure() {
        val config = PassiveListenerConfig.builder()
            .setDataTypes(
                setOf(
                    DataType.HEART_RATE_BPM,
                    DataType.STEPS,
                    DataType.CALORIES,
                    DataType.DISTANCE
                )
            )
            .build()

        passiveMonitoringClient.setPassiveListenerCallback(
            config,
            passiveListener
        ) // :contentReference[oaicite:0]{index=0}

        Log.d("HealthPublisher", "PassiveListener registered")
    }

    /** Cancela o listener */
    fun stopPassiveMeasure() {
        passiveMonitoringClient.clearPassiveListenerCallbackAsync() // :contentReference[oaicite:1]{index=1}
        Log.d("HealthPublisher", "PassiveListener unregistered")
    }

}
