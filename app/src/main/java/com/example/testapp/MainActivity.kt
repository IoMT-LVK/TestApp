package com.example.testapp

import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.example.testapp.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    lateinit var fastLayout : ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        fastLayout = ActivityMainBinding.inflate(layoutInflater)
        setContentView(fastLayout.root)
    }

    fun onClickGo2(view: View) {
        val intent = Intent(this, Activity2::class.java)
        val b = view as Button
        intent.putExtra("device", b.getText().toString())
        startActivity(intent)
    }

    private val bluetoothAdapter: BluetoothAdapter by lazy {
        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothManager.adapter
    }

    override fun onResume() {
        super.onResume()
        if (!bluetoothAdapter.isEnabled) {
            promptEnableBluetooth()
        }
    }

    private fun promptEnableBluetooth() {
        if (!bluetoothAdapter.isEnabled) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            get_permission.launch(enableBtIntent)
        }
    }

    val get_permission = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { res: ActivityResult ->
        if (res.resultCode!= Activity.RESULT_OK) {
            promptEnableBluetooth()
        }
    }


}