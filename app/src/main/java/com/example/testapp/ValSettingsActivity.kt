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
import android.widget.EditText
import android.widget.TextView
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.testapp.configs.Settings
import com.example.testapp.databinding.ValSettingsBinding
import com.example.testapp.view_adapters.SettingsAdapter
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class ValSettingsActivity : AppCompatActivity() {
    private lateinit var fastLayout : ValSettingsBinding

    private lateinit var config: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        fastLayout = ValSettingsBinding.inflate(layoutInflater)
        setContentView(fastLayout.root)
        fastLayout.devName.text = intent.getStringExtra("device")
        val chars = intent.getStringArrayListExtra("chars")
        Log.d("HttpClient", "$chars")
        config = intent.getStringExtra("config")!!

        // getting the recyclerview by its id
        val recyclerview = fastLayout.settingsView
        // this creates a vertical layout Manager
        recyclerview.layoutManager = LinearLayoutManager(this)
        // This will pass the ArrayList to our Adapter
        val adapter = SettingsAdapter(chars)
        // Setting the Adapter with the recyclerview
        recyclerview.adapter = adapter
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
            getPermission.launch(enableBtIntent)
        }
    }

    private val getPermission = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { res: ActivityResult ->
        if (res.resultCode!= Activity.RESULT_OK) {
            promptEnableBluetooth()
        }
    }

    private fun getSettings(rv: RecyclerView): List<Settings> {
        val itemCount = rv.adapter!!.itemCount
        var ans: MutableList<Settings> = mutableListOf()
        for (i in 0 until itemCount) {
            val holder = rv.findViewHolderForAdapterPosition(i)
            if (holder != null) {
                val paramName = (holder.itemView.findViewById<View>(R.id.name) as TextView).text as String
                val minVal = (holder.itemView.findViewById<View>(R.id.min_val) as EditText).text as CharSequence
                val maxVal = (holder.itemView.findViewById<View>(R.id.max_val) as EditText).text as CharSequence
                val freq = (holder.itemView.findViewById<View>(R.id.freq) as EditText).text as CharSequence
                ans.add(Settings(paramName, minVal.toString().toInt(), maxVal.toString().toInt(), freq.toString()))
            }
        }
        return ans
    }

    fun onClickGoStart(view: View) {
        val intent = Intent(this, BLEWorkActivity::class.java)
        intent.putExtra("device", fastLayout.devName.text)
        intent.putExtra("config", config)
        val paramList: List<Settings> = getSettings(fastLayout.settingsView)
        val jsonSettings: String = Json.encodeToString(paramList)
        Log.d("HttpClient", jsonSettings)
        Log.d("HttpClient", config)
        intent.putExtra("settings", jsonSettings)
        startActivity(intent)
    }
}