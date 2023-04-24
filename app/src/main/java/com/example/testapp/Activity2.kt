package com.example.testapp

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.Editable
import android.util.Log
import android.view.View
import android.widget.Button
import androidx.core.view.isVisible
import androidx.interpolator.view.animation.FastOutLinearInInterpolator
import com.example.testapp.databinding.Activity2Binding

class Activity2 : AppCompatActivity() {
    lateinit var fastLayout : Activity2Binding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        fastLayout = Activity2Binding.inflate(layoutInflater)
        setContentView(fastLayout.root)
        fastLayout.devName.text = intent.getStringExtra("device")
        if (fastLayout.devName.text == "Глюкометр Dexcome") {
            fastLayout.param1.text = "Уровень сахара"
            fastLayout.minVal1.hint = "0"
            fastLayout.maxVal1.hint = "1000"
            fastLayout.freq1.hint = "1:300"
            fastLayout.param2.isVisible = false
            fastLayout.minVal2.isVisible = false
            fastLayout.maxVal2.isVisible = false
            fastLayout.freq2.isVisible = false
            fastLayout.param3.isVisible = false
            fastLayout.minVal3.isVisible = false
            fastLayout.maxVal3.isVisible = false
            fastLayout.freq3.isVisible = false

        }
        if (fastLayout.devName.text == "Hexoskin") {
            fastLayout.param1.text = "Heart Rate"
            fastLayout.minVal1.hint = "30"
            fastLayout.maxVal1.hint = "220"
            fastLayout.freq1.hint = "1:1"
            fastLayout.param2.text = "Respiration\nRate"
            fastLayout.minVal2.hint = "2"
            fastLayout.maxVal2.hint = "60"
            fastLayout.freq2.hint = "1:1"
            fastLayout.param3.text = "Accelerometer\nRate"
            fastLayout.minVal3.hint = "-4096"
            fastLayout.maxVal3.hint = "4095"
            fastLayout.freq3.hint = "64:1"
        }
        if (fastLayout.devName.text == "Универсальное устройство") {
            fastLayout.param1.text = "Heart Rate"
            fastLayout.minVal1.hint = "30"
            fastLayout.maxVal1.hint = "220"
            fastLayout.freq1.hint = "1:1"
            fastLayout.param2.isVisible = false
            fastLayout.minVal2.isVisible = false
            fastLayout.maxVal2.isVisible = false
            fastLayout.freq2.isVisible = false
            fastLayout.param3.isVisible = false
            fastLayout.minVal3.isVisible = false
            fastLayout.maxVal3.isVisible = false
            fastLayout.freq3.isVisible = false
        }
    }

    fun onClickGoStart(view: View) {
        val intent = Intent(this, Activity3::class.java)
        val par1 = arrayOf(fastLayout.param1.text.replace("\n".toRegex(), " ") as CharSequence,
                fastLayout.minVal1.text,
                fastLayout.maxVal1.text,
                fastLayout.freq1.text )
        /**val par2 = arrayOf(fastLayout.param2.text.replace("\n".toRegex(), " ") as CharSequence,
                fastLayout.minVal2.text,
                fastLayout.maxVal2.text,
                fastLayout.freq2.text )
        val par3 = arrayOf(fastLayout.param3.text.replace("\n".toRegex(), " ") as CharSequence,
                fastLayout.minVal3.text,
                fastLayout.maxVal3.text,
                fastLayout.freq3.text )*/
        intent.putExtra("device", fastLayout.devName.text)
        intent.putExtra("param1", par1)
        //intent.putExtra("param2", par2)
        //intent.putExtra("param3", par3)
        startActivity(intent)
    }
}