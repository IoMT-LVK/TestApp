package com.example.testapp

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.bluetooth.*
import android.bluetooth.le.BluetoothLeAdvertiser
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.testapp.databinding.Activity3Binding
import kotlinx.android.synthetic.main.activity_3.*

private const val RUNTIME_PERMISSION_REQUEST_CODE = 2

class Activity3 : AppCompatActivity() {
    lateinit var fastLayout : Activity3Binding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        fastLayout = Activity3Binding.inflate(layoutInflater)
        setContentView(fastLayout.root)
        isAdvertising = false
        val txt = "Current Settings: " + intent.getStringExtra("device") +
                "\n" + intent.getCharSequenceArrayExtra("param1")?.get(0) + ":\n\tminVal - " +
                intent.getCharSequenceArrayExtra("param1")?.get(1) + "; maxVal - " +
                intent.getCharSequenceArrayExtra("param1")?.get(2) + ";\n\tfrequency - " +
                intent.getCharSequenceArrayExtra("param1")?.get(3) +
                "\n" + intent.getCharSequenceArrayExtra("param2")?.get(0) + ":\n\tminVal - " +
                intent.getCharSequenceArrayExtra("param2")?.get(1) + "; maxVal - " +
                intent.getCharSequenceArrayExtra("param2")?.get(2) + ";\n\tfrequency - " +
                intent.getCharSequenceArrayExtra("param2")?.get(3) +
                "\n" + intent.getCharSequenceArrayExtra("param3")?.get(0) + ":\n\tminVal - " +
                intent.getCharSequenceArrayExtra("param3")?.get(1) + "; maxVal - " +
                intent.getCharSequenceArrayExtra("param3")?.get(2) + ";\n\tfrequency - " +
                intent.getCharSequenceArrayExtra("param3")?.get(3)
        fastLayout.curSett.text = txt
    }

    private val bluetoothAdapter: BluetoothAdapter by lazy {
        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothManager.adapter
    }

    private val bluetoothAdvertiser: BluetoothLeAdvertiser by lazy {
        bluetoothAdapter.bluetoothLeAdvertiser
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

    private val get_permission = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { res: ActivityResult ->
        if (res.resultCode!= Activity.RESULT_OK) {
            promptEnableBluetooth()
        }
    }

    fun onClickListener(view: View) {
        if (isAdvertising) {
            stopBleAdvertising()
        } else {
            startBleAdvertising()
        }
    }

    private var isAdvertising = false
        set(value) {
            field = value
            runOnUiThread { fastLayout.swButton.text = if (value) "Остановить работу" else "Начать работу" }
        }

    private fun startBleAdvertising() {
        if (!hasRequiredRuntimePermissions()) {
            requestRelevantRuntimePermissions()
        } else {

            isAdvertising = true
        }
    }

    private fun stopBleAdvertising() {

        isAdvertising = false
    }

    private fun Context.hasPermission(permissionType: String): Boolean {
        return ContextCompat.checkSelfPermission(this, permissionType) ==
                PackageManager.PERMISSION_GRANTED
    }

    private fun Context.hasRequiredRuntimePermissions(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            hasPermission(Manifest.permission.BLUETOOTH_SCAN) &&
                    hasPermission(Manifest.permission.BLUETOOTH_CONNECT)
        } else {
            hasPermission(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    private fun Activity.requestRelevantRuntimePermissions() {
        if (hasRequiredRuntimePermissions()) { return }
        when {
            Build.VERSION.SDK_INT < Build.VERSION_CODES.S -> {
                requestLocationPermission()
            }
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                requestBluetoothPermissions()
            }
        }
    }

    private fun requestLocationPermission() {
        runOnUiThread {
            val alert = AlertDialog.Builder(this)

            with(alert) {
                setTitle("Требуется разрешение на местоположение")
                setMessage(
                    "Начиная с версии Android 6, система требует, чтобы приложения получали " +
                        "доступ к местоположению для сканирования устройств BLE."
                )
                setCancelable(false)
                setPositiveButton(android.R.string.ok) { dialog, which ->
                    ActivityCompat.requestPermissions(
                        this@Activity3,
                        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                        RUNTIME_PERMISSION_REQUEST_CODE
                    )
                }
                show()
            }
        }
    }
    private fun requestBluetoothPermissions() {
        runOnUiThread {
            val alert = AlertDialog.Builder(this)

            with(alert) {
                setTitle("Требуется разрешение Bluetooth")
                setMessage(
                    "Начиная с версии Android 12, система требует, чтобы приложения получали " +
                            "доступ к Bluetooth для сканирования устройств BLE."
                )
                setCancelable(false)
                setPositiveButton(android.R.string.ok) { dialog, which ->
                    ActivityCompat.requestPermissions(
                        this@Activity3,
                        arrayOf(
                            Manifest.permission.BLUETOOTH_SCAN,
                            Manifest.permission.BLUETOOTH_CONNECT
                        ),
                        RUNTIME_PERMISSION_REQUEST_CODE
                    )
                }
                show()
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            RUNTIME_PERMISSION_REQUEST_CODE -> {
                val containsPermanentDenial = permissions.zip(grantResults.toTypedArray()).any {
                    it.second == PackageManager.PERMISSION_DENIED &&
                            !ActivityCompat.shouldShowRequestPermissionRationale(this, it.first)
                }
                val containsDenial = grantResults.any { it == PackageManager.PERMISSION_DENIED }
                val allGranted = grantResults.all { it == PackageManager.PERMISSION_GRANTED }
                when {
                    containsPermanentDenial -> {
                        // TODO: Handle permanent denial (e.g., show AlertDialog with justification)
                        // Note: The user will need to navigate to App Settings and manually grant
                        // permissions that were permanently denied
                    }
                    containsDenial -> {
                        requestRelevantRuntimePermissions()
                    }
                    allGranted && hasRequiredRuntimePermissions() -> {
                        startBleAdvertising()
                    }
                    else -> {
                        // Unexpected scenario encountered when handling permissions
                        recreate()
                    }
                }
            }
        }
    }
}