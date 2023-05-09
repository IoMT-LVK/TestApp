package com.example.testapp

import android.bluetooth.BluetoothClass.Device
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.example.testapp.configs.DeviceConfig
import com.example.testapp.databinding.ChooseDeviceBinding

class ChooseDeviceActivity : AppCompatActivity() {

    private lateinit var fastLayout : ChooseDeviceBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        fastLayout = ChooseDeviceBinding.inflate(layoutInflater)
        setContentView(fastLayout.root)
    }

    fun onClickGo2(view: View) {
        val b = view as Button
        val device = b.text.toString()
        if (device == "Универсальное устройство"){
            val intent = Intent(this, UniversalDeviceActivity::class.java)
            startActivity(intent)
        } else {
            val intent = Intent(this, ValSettingsActivity::class.java)
            intent.putExtra("device", device)
            var config = ""
            var deviceClass: DeviceConfig? = null
            if (device == "Smart часы") {
                config = DeviceConfig.miBandAsJson
                deviceClass = DeviceConfig.miBandAsDataClass
            }
            intent.putExtra("config", config)
            val charNames: ArrayList<String> = arrayListOf()
            for (char in deviceClass!!.characteristics.values) {
                charNames.add(char.prettyName)
            }
            intent.putStringArrayListExtra("chars", charNames)
            startActivity(intent)
        }
    }
}