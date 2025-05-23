import android.content.Context
import android.util.Log
import androidx.health.services.client.HealthServices
import androidx.health.services.client.PassiveListenerCallback
import androidx.health.services.client.PassiveMonitoringClient
import androidx.health.services.client.data.DataPointContainer
import androidx.health.services.client.data.DataType
import androidx.health.services.client.data.SampleDataPoint
import androidx.health.services.client.setPassiveListenerService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.example.mqttwearable.mqtt.MqttHandler

//https://developer.android.com/health-and-fitness/guides/health-services/active-data/measure-client?hl=pt-br

class HealthPublisher(
    context: Context,
    private val mqttHandler: MqttHandler
) {
    private val passiveMonitoringClient: PassiveMonitoringClient =
        HealthServices.getClient(context).passiveMonitoringClient

    private val passiveListener = object : PassiveListenerCallback {
        override fun onNewDataPointsReceived(dataPoints: DataPointContainer) {
            for (dataPoint in dataPoints.sampleDataPoints) {
                val message = when (dataPoint.type) {
                    DataType.HEART_RATE_BPM -> {
                        val bpm = dataPoint.getValue(DataType.HEART_RATE_BPM).asFloat()
                        "HeartRate: $bpm"
                    }
                    DataType.STEPS -> {
                        val steps = dataPoint.getValue(DataType.STEPS).asInt()
                        "Steps: $steps"
                    }
                    DataType.CALORIES -> {
                        val calories = dataPoint.getValue(DataType.CALORIES).asFloat()
                        "Calories: $calories"
                    }
                    DataType.DISTANCE -> {
                        val distance = dataPoint.getValue(DataType.DISTANCE).asFloat()
                        "Distance: $distance"
                    }
                    else -> "Unknown DataType"
                }

                Log.d("HealthPublisher", "Sending via MQTT: $message")
                CoroutineScope(Dispatchers.IO).launch {
                    mqttHandler.publish("health/data", message)
                }
            }
        }
    }

    suspend fun register() {
        passiveMonitoringClient.setPassiveListenerService(
            passiveListener,
            config = TODO()
        )
        Log.d("HealthPublisher", "PassiveListener registered")
    }

    suspend fun unregister() {
        passiveMonitoringClient.clearPassiveListener()
        Log.d("HealthPublisher", "PassiveListener unregistered")
    }
}
