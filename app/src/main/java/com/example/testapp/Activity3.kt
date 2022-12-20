package com.example.testapp

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.example.testapp.databinding.Activity2Binding
import com.example.testapp.databinding.Activity3Binding

class Activity3 : AppCompatActivity() {
    lateinit var fastLayout : Activity3Binding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        fastLayout = Activity3Binding.inflate(layoutInflater)
        setContentView(fastLayout.root)

    }
}