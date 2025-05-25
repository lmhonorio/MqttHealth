/* While this template provides a good starting point for using Wear Compose, you can always
 * take a look at https://github.com/android/wear-os-samples/tree/main/ComposeStarter to find the
 * most up to date changes to the libraries and their usages.
 */

package com.example.mqttwearable.presentation


import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.renderscript.Element
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.compose.ui.platform.LocalContext

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.TimeText
import androidx.wear.tooling.preview.devices.WearDevices
import androidx.compose.runtime.mutableStateOf
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.wear.compose.material.Button
import androidx.compose.foundation.layout.height
import androidx.compose.ui.unit.dp
import android.util.Log
import androidx.wear.compose.material.ButtonDefaults
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import org.eclipse.paho.client.mqttv3.MqttClient
import org.eclipse.paho.client.mqttv3.MqttConnectOptions
import org.eclipse.paho.client.mqttv3.MqttMessage
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import androidx.compose.runtime.remember
import androidx.health.services.client.data.DataType
import androidx.health.services.client.data.DeltaDataType
import androidx.lifecycle.lifecycleScope
import com.example.mqttwearable.R
import com.example.mqttwearable.mqtt.MqttHandler
import com.example.mqttwearable.health.HealthPublisher
import androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions
import androidx.activity.result.ActivityResultLauncher

//private const val SERVER_URI = "tcp://192.168.0.157:1883"
private const val SERVER_URI = "tcp://192.168.68.102:1883" //mosquitto_sub -h 192.168.0.112 -p 1883 -t "teste" -v
private const val TOPIC = "teste"


class MainActivity : ComponentActivity() {

    private lateinit var mqttHandler: MqttHandler

    private lateinit var healthPublisher: HealthPublisher

    private val requiredPermissions = arrayOf(
        android.Manifest.permission.ACTIVITY_RECOGNITION,
        android.Manifest.permission.BODY_SENSORS,
        android.Manifest.permission.ACCESS_FINE_LOCATION
    )

    // 2) Crie o launcher que vai pedir essas permissões
    private lateinit var permissionsLauncher: ActivityResultLauncher<Array<String>>



    val activeTypes: Set<DeltaDataType<*, *>> = setOf(
        DataType.STEPS,     // é um DeltaDataType<Int, SampleDataPoint<Int>>
        DataType.CALORIES,  // é um DeltaDataType<Float, SampleDataPoint<Float>>
        DataType.DISTANCE,   // é um DeltaDataType<Float, SampleDataPoint<Float>>
        DataType.PACE,   // é um DeltaDataType<Float, SampleDataPoint<Float>>
        DataType.HEART_RATE_BPM
//        DataType.ABSOLUTE_ELEVATION
    )



    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()

        super.onCreate(savedInstanceState)

        setTheme(android.R.style.Theme_DeviceDefault)

        mqttHandler = MqttHandler(applicationContext)
        healthPublisher = HealthPublisher(applicationContext, mqttHandler)

        permissionsLauncher = registerForActivityResult(RequestMultiplePermissions()) { results ->
            // results é um Map<String, Boolean>
            if (results.values.all { it }) {
                // ✅ Todas concedidas: agora podemos registrar o HealthPublisher
                lifecycleScope.launch {
                    //healthPublisher.startPassiveMeasure()
                    healthPublisher.startActiveMeasurementsBatch(activeTypes)
                }
            } else {
                // ❌ Alguma permissão foi negada
                Log.e("MainActivity", "Permissões de Health Services não concedidas")
                // Aqui você pode mostrar um diálogo ou desabilitar funcionalidades
            }
        }



        // Conectar ao broker
        CoroutineScope(Dispatchers.IO).launch {
            mqttHandler.connect(
                brokerUrl = SERVER_URI,
                clientId = "wearable-${System.currentTimeMillis()}"
            ) { success ->
                if (success) Log.d("MainActivity", "Connected to MQTT broker")
                else Log.e("MainActivity", "Failed to connect to MQTT broker")
            }
        }


        setContent {
            WearApp("Android", MqttHandler(applicationContext))
        }
    }


    override fun onStart() {
        super.onStart()

        permissionsLauncher.launch(requiredPermissions)

    }

    override fun onStop() {
        super.onStop()
        // 4) Cancela o listener antes da Activity parar
       // healthPublisher.stopPassiveMeasure()
        //healthPublisher.stopActiveMeasure()
        healthPublisher.stopActiveMeasurementsBatch(activeTypes)
    }

    override fun onDestroy() {
        super.onDestroy()
        CoroutineScope(Dispatchers.IO).launch {
            mqttHandler.disconnect()
            Log.d("MainActivity", "MQTT disconnected")
        }
    }
}







@Composable
fun WearApp(greetingName: String, mqttHandler: MqttHandler) {
    val context = LocalContext.current
    var buttonClicked by remember { mutableStateOf(false) }

    MaterialTheme {
        Box(
            modifier = Modifier
                .fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            TimeText()
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colors.primary,
                    text = "Hello, $greetingName!"
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        buttonClicked != buttonClicked
                        Log.d("MainActivity", "Button clicked! State: $buttonClicked")

                        // MQTT Connection and Publish
                        CoroutineScope(Dispatchers.IO).launch {
                            try {

                                val persistence = MemoryPersistence()
                                val client = MqttClient(SERVER_URI, MqttClient.generateClientId(), persistence)
                                val options = MqttConnectOptions()
                                options.isCleanSession = true
                                client.connect(options)

                                val message = MqttMessage("Button clicked!".toByteArray())
                                client.publish(TOPIC, message)

                                client.disconnect()
                                Log.d("MainActivity", "MQTT message sent!")
                            } catch (e: Exception) {
                                Log.e("MainActivity", "MQTT error", e)
                            }
                        }
                        measureStepsNow(context, mqttHandler)

                    },
                    modifier = Modifier.size(ButtonDefaults.DefaultButtonSize),
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = MaterialTheme.colors.primary,
                        contentColor = MaterialTheme.colors.onPrimary
                    )
                ) {
                    Text(
                        text = if (buttonClicked) "Clicked!" else "Press Me",
                        style = MaterialTheme.typography.button
                    )
                }
            }
        }
    }
}

@Composable
fun Greeting(greetingName: String) {
    Text(
        modifier = Modifier.fillMaxWidth(),
        textAlign = TextAlign.Center,
        color = MaterialTheme.colors.primary,
        text = stringResource(R.string.hello_world, greetingName)
    )
}

@Preview(device = WearDevices.SMALL_ROUND, showSystemUi = true)
@Composable
fun DefaultPreview() {
    val context = LocalContext.current
    WearApp("Preview Android", MqttHandler(context))
}

/**
 * Faz uma leitura única do contador de passos e publica via MQTT.
 */
fun measureStepsNow(context: Context, mqttHandler: MqttHandler) {
    // Uso da API tipada para SensorManager, evita cast inseguro
    val sensorManager = context.getSystemService(SensorManager::class.java)
        ?: return
    val stepSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
        ?: return

    val listener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {
            sensorManager.unregisterListener(this)
            val steps = event.values.firstOrNull()?.toLong() ?: return
            CoroutineScope(Dispatchers.IO).launch {
                mqttHandler.publish(TOPIC, "Steps now: $steps")
                val persistence = MemoryPersistence()
                val client = MqttClient(SERVER_URI, MqttClient.generateClientId(), persistence)
                val options = MqttConnectOptions()
                options.isCleanSession = true
                client.connect(options)

                val message = MqttMessage("Steps now: $steps".toByteArray())
                client.publish(TOPIC, message)

                client.disconnect()
            }
        }
        override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {}
    }
    sensorManager.registerListener(listener, stepSensor, SensorManager.SENSOR_DELAY_NORMAL)
}