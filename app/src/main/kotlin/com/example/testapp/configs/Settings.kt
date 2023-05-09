package com.example.testapp.configs

import kotlinx.serialization.Serializable

@Serializable
data class Settings(
    val paramName: String,
    val minVal: Int,
    val maxVal: Int,
    val freq: String
)
