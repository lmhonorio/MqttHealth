package com.example.mqttwearable.mqtt

import org.eclipse.paho.client.mqttv3.MqttClient
import org.eclipse.paho.client.mqttv3.MqttConnectOptions
import org.eclipse.paho.client.mqttv3.MqttException
import org.eclipse.paho.client.mqttv3.MqttMessage
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence

class MqttHandler {
    private var client: MqttClient? = null

    public fun connect(brokerUrl: String?, clientId: String) {
        try {
            // Set up the persistence layer
            val persistence: MemoryPersistence = MemoryPersistence()

            // Initialize the MQTT client
            client = MqttClient(brokerUrl, clientId, persistence)

            // Set up the connection options
            val connectOptions: MqttConnectOptions = MqttConnectOptions()
            connectOptions.setCleanSession(true)

            // Connect to the broker
            client?.connect(connectOptions)
        } catch (e: MqttException) {
            e.printStackTrace()
        }
    }

    public fun disconnect() {
        try {
            client?.disconnect()
        } catch (e: MqttException) {
            e.printStackTrace()
        }
    }

    public fun publish(topic: String?, message: String) {
        try {
            val mqttMessage: MqttMessage = MqttMessage(message.toByteArray())
            client?.publish(topic, mqttMessage)
        } catch (e: MqttException) {
            e.printStackTrace()
        }
    }

    public fun subscribe(topic: String?) {
        try {
            client?.subscribe(topic)
        } catch (e: MqttException) {
            e.printStackTrace()
        }
    }
}
