package com.example.testapp

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.testapp.databinding.UniversalDeviceBinding
import com.example.testapp.configs.DeviceConfig
import com.example.testapp.view_adapters.ConfigAdapter
import io.ktor.client.plugins.auth.*
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.android.*
import io.ktor.client.plugins.auth.providers.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json

internal const val BASE_URL = "https://iomt.lvk.cs.msu.ru"
internal const val API_V1 = "/api/v1"
internal const val TOKEN = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJ1c2VyLzEiLCJpc3MiOiJJb01UX1JFU1QiLCJleHAiOjE2OTA2MjQwNTQsImlhdCI6MTY4Mjg0ODA1NH0.vYB5NplD_yT1C1jUSgzCktjweUYwtTIV2eZVvbYfbiY"

class UniversalDeviceActivity : AppCompatActivity() {
    private lateinit var fastLayout : UniversalDeviceBinding

    private var configs: List<DeviceConfig>? = null

    private val client = HttpClient(Android) {
        install(ContentNegotiation) { json() }
        install(Auth) {
            bearer {
                refreshTokens {
                    BearerTokens(TOKEN, "")
                }
            }
        }
        install(Logging) {
            level = LogLevel.ALL
            logger = object : Logger {
                override fun log(message: String) {
                    Log.d("HttpClient", message)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        fastLayout = UniversalDeviceBinding.inflate(layoutInflater)
        setContentView(fastLayout.root)
        configs = runBlocking { getDeviceTypes("") }

        // getting the recyclerview by its id
        val recyclerview = fastLayout.configView
        // this creates a vertical layout Manager
        recyclerview.layoutManager = LinearLayoutManager(this)
        // This will pass the ArrayList to our Adapter
        val adapter = ConfigAdapter(configs!!)
        // Setting the Adapter with the recyclerview
        recyclerview.adapter = adapter
    }

    private suspend fun getDeviceTypes(substring: String): List<DeviceConfig> = client.get(
        URLBuilder("$BASE_URL$API_V1/device_types").apply {
            if(substring.isNotBlank()) {
                parameters.append("name", substring)
            }
        }.build(),
    ).body()

    fun onClickGoSettings(view: View) {
        val b = view as Button
        val intent = Intent(this, ValSettingsActivity::class.java)
        var chosenDevice: DeviceConfig? = null
        intent.putExtra("device", b.text.toString())
        for (dev in configs!!) {
            if (dev.general.name == b.text.toString()) {
                chosenDevice = dev
                break
            }
        }
        val charNames: ArrayList<String> = arrayListOf()
        for (char in chosenDevice!!.characteristics.values) {
            charNames.add(char.prettyName)
        }
        intent.putStringArrayListExtra("chars", charNames)
        val jsonConfig: String = Json.encodeToString(DeviceConfig.serializer(), chosenDevice)
        Log.d("HttpClient", jsonConfig)
        intent.putExtra("config", jsonConfig)
        client.close()
        startActivity(intent)
    }
}