package com.example.testapp.configs

import kotlinx.serialization.Serializable

@Serializable
data class DeviceConfig(
    val id: Long,
    val general: GeneralConfig,
    val characteristics: Map<String, CharacteristicConfig>
)