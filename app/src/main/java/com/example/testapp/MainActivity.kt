package com.example.testapp

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.example.testapp.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var fastLayout : ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        fastLayout = ActivityMainBinding.inflate(layoutInflater)
        setContentView(fastLayout.root)
    }

    fun onClickGo2(view: View) {
        val intent = Intent(this, Activity2::class.java)
        val b = view as Button
        intent.putExtra("device", b.text.toString())
        startActivity(intent)
    }
}