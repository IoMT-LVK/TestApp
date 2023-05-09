package com.example.testapp.configs

import kotlinx.serialization.Serializable

@Serializable
data class DeviceConfig(
    val id: Long,
    val general: GeneralConfig,
    val characteristics: Map<String, CharacteristicConfig>
) {
    companion object {
        val miBandAsDataClass = DeviceConfig(
            id = 1,
            general = GeneralConfig(
                name = "Mi Band",
                nameRegex = ".*",
                type = "bracelet"
            ),
            characteristics = mapOf(
                "heartRate" to CharacteristicConfig(
                    prettyName = "Heart Rate",
                    serviceUuid = "0000180d-0000-1000-8000-00805f9b34fb",
                    characteristicUuid = "00002a37-0000-1000-8000-00805f9b34fb"
                )
            )
        )
        const val miBandAsJson = """{
        "characteristics": {
          "heartRate": {
            "name": "Heart Rate",
            "sensor_uuid": "00002a37-0000-1000-8000-00805f9b34fb",
            "service_uuid": "0000180d-0000-1000-8000-00805f9b34fb"
          }
        },
        "general": {
          "name": "Mi Band",
          "name_regex": ".*",
          "type": "bracelet"
        },
        "id": 1
      }"""
    }
}