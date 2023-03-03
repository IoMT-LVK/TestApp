package com.example.testapp

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.Editable
import android.util.Log
import android.view.View
import android.widget.Button
import androidx.interpolator.view.animation.FastOutLinearInInterpolator
import com.example.testapp.databinding.Activity2Binding

class Activity2 : AppCompatActivity() {
    lateinit var fastLayout : Activity2Binding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        fastLayout = Activity2Binding.inflate(layoutInflater)
        setContentView(fastLayout.root)
        fastLayout.devName.text = intent.getStringExtra("device")
        val name = fastLayout.devName.text as String
        if (fastLayout.devName.text == "Глюкометр Dexcome") {
            fastLayout.param1.text = "Уровень сахара"
            fastLayout.minVal1.hint = "0"
            fastLayout.maxVal1.hint = "1000"
        }
    }

    fun onClickGoStart(view: View) {
        val intent = Intent(this, Activity3::class.java)
        val min1 = fastLayout.minVal1.text
        val max1 = fastLayout.maxVal1.text
        intent.putExtra("device", fastLayout.devName.text)
        intent.putExtra("vals", arrayOf(min1, max1))
        intent.putExtra("frec", fastLayout.freq1.text)
        startActivity(intent)
    }
}