package com.example.testapp.configs

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GeneralConfig(
    var name: String,
    @SerialName("name_regex") var nameRegex: String,
    var type: String
)
