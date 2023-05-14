package com.example.testapp.configs

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CharacteristicConfig(
    @SerialName("name") val prettyName: String,
    @SerialName("service_uuid") var serviceUuid: String? = null,
    @SerialName("characteristic_uuid") var characteristicUuid: String? = null
)
