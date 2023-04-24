package com.example.testapp

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.bluetooth.*
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.bluetooth.le.BluetoothLeAdvertiser
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.ParcelUuid
import android.util.Log
import android.view.View
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresPermission
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.testapp.databinding.Activity3Binding
import kotlinx.android.synthetic.main.activity_3.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import java.util.*

private const val RUNTIME_PERMISSION_REQUEST_CODE = 2

class Activity3 : AppCompatActivity() {
    lateinit var fastLayout : Activity3Binding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        fastLayout = Activity3Binding.inflate(layoutInflater)
        setContentView(fastLayout.root)
        //TODO: заменить на dataclass
        parameterList = mutableListOf(
            intent.getCharSequenceArrayExtra("param1")?.get(0))
        params[parameterList[0]] = mapOf(
            "minVal" to intent.getCharSequenceArrayExtra("param1")?.get(1),
            "maxVal" to intent.getCharSequenceArrayExtra("param1")?.get(2),
            "frequency" to intent.getCharSequenceArrayExtra("param1")?.get(3))
        val txt = "Current Settings: " + intent.getStringExtra("device") +
                "\n" + parameterList[0] +
                ":\n\tminVal - " + params[parameterList[0]]?.get("minVal") +
                "; maxVal - " + params[parameterList[0]]?.get("maxVal") +
                ";\n\tfrequency - " + params[parameterList[0]]?.get("frequency")
        fastLayout.curSett.text = txt
        startServer()
    }

    override fun onDestroy() {
        super.onDestroy()
        stopServer()
    }

    private lateinit var gattServer: BluetoothGattServer

    private val heartRateServiceUUID: UUID = UUID.fromString("0000180d-0000-1000-8000-00805f9b34fb")
    private val heartRateCharacteristicUUID: UUID = UUID.fromString("00002a37-0000-1000-8000-00805f9b34fb")
    private val heartRateDescriptorUUID: UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")

    private var connectedDevice: BluetoothDevice? = null

    private var parameterList: MutableList<CharSequence?> = mutableListOf()
    private val params: MutableMap<CharSequence?, Map<String, CharSequence?>> = mutableMapOf()
    private var newValueBytes = byteArrayOf()

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
            getPermission.launch(enableBtIntent)
        }
    }

    private val getPermission = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { res: ActivityResult ->
        if (res.resultCode!= Activity.RESULT_OK) {
            promptEnableBluetooth()
        }
    }

    private var isAdvertising = false
        set(value) {
            field = value
            //runOnUiThread { fastLayout.swButton.text = if (value) "Остановить работу" else "Начать работу" }
            this@Activity3.runOnUiThread(java.lang.Runnable {
                fastLayout.swButton.text = if (value) "Остановить работу" else "Начать работу"
            })
        }

    fun onClickListener(view: View) {
        if (isAdvertising) {
            stopBleAdvertising()
        } else {
            startBleAdvertising()
        }
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    private fun startServer() {
        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        gattServer = bluetoothManager.openGattServer(this, gattServerCallback).also {
            val heartRateService = BluetoothGattService(heartRateServiceUUID, BluetoothGattService.SERVICE_TYPE_PRIMARY)
            val heartRateCharacteristic = BluetoothGattCharacteristic(
                heartRateCharacteristicUUID,
                BluetoothGattCharacteristic.PROPERTY_READ or BluetoothGattCharacteristic.PROPERTY_NOTIFY,
                BluetoothGattCharacteristic.PERMISSION_READ
            )
            val heartRateDescriptor = BluetoothGattDescriptor(heartRateDescriptorUUID, BluetoothGattDescriptor.PERMISSION_READ)
            heartRateCharacteristic.addDescriptor(heartRateDescriptor)
            heartRateService.addCharacteristic(heartRateCharacteristic)
            it.addService(heartRateService)
        }
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    private fun stopServer() {
        gattServer.close()
    }

    private val gattServerCallback: BluetoothGattServerCallback = object: BluetoothGattServerCallback() {
        private val debugTag: String = "GattServer"

        //Callback indicating when a remote device has been connected or disconnected.
        @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
        override fun onConnectionStateChange(device: BluetoothDevice?, status: Int, newState: Int) {
            super.onConnectionStateChange(device, status, newState)
            Log.d("BLE", "onConnectionStateChange $status -> $newState")
            when(newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    (device?.name ?: "unknown").also { fastLayout.phoneName.text = it }
                    (device?.address ?: "unknown").also { fastLayout.phoneAddr.text = it }
                    connectedDevice = device
                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    "@string/phone_name".also { fastLayout.phoneName.text = it }
                    "@string/phone_addr".also { fastLayout.phoneAddr.text = it }
                    connectedDevice = null
                }
            }
        }

        //Indicates whether a local service has been added successfully.
        override fun onServiceAdded(status: Int, service: BluetoothGattService?) {
            Log.d("BLE", "Gatt server service was added.")
            super.onServiceAdded(status, service)
        }

        //A remote client has requested to read a local characteristic.
        @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
        override fun onCharacteristicReadRequest(device: BluetoothDevice?, requestId: Int, offset: Int, characteristic: BluetoothGattCharacteristic?) {
            super.onCharacteristicReadRequest(device, requestId, offset, characteristic)
            Log.d("BLE", "READ called onCharacteristicReadRequest ${characteristic?.uuid ?: "UNDEFINED"}")
            if (characteristic?.uuid == heartRateCharacteristicUUID) {
                gattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, 0, newValueBytes)
            }
        }

        //A remote client has requested to read a local descriptor.
        @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
        override fun onDescriptorReadRequest(device: BluetoothDevice?, requestId: Int, offset: Int, descriptor: BluetoothGattDescriptor?) {
            super.onDescriptorReadRequest(device, requestId, offset, descriptor)
            Log.d("BLE", "READ called onDescriptorReadRequest ${descriptor?.uuid ?: "UNDEFINED"}")
            if (descriptor?.uuid == heartRateDescriptorUUID) {
                gattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, 0, newValueBytes)
            }
        }

        //Callback invoked when a notification or indication has been sent to a remote device.
        @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
        override fun onNotificationSent(device: BluetoothDevice?, status: Int) {
            super.onNotificationSent(device, status)
            Log.d("BLE", "onNotificationSent with status $status")
        }
    }

    private val advertiseCallback = object : AdvertiseCallback() {
        override fun onStartSuccess(settingsInEffect: AdvertiseSettings) {
            Log.d("BLE", "LE Advertise Started.")
        }

        override fun onStartFailure(errorCode: Int) {
            Log.d("BLE", "LE Advertise Failed: $errorCode")
        }
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_ADVERTISE)
    private fun startBleAdvertising() {
        if (!hasRequiredRuntimePermissions()) { requestRelevantRuntimePermissions() }
        bluetoothAdvertiser.let {
            val settings = AdvertiseSettings.Builder()
                .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_BALANCED)
                .setConnectable(true)
                .setTimeout(0)
                .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_MEDIUM)
                .build()

            val data = AdvertiseData.Builder()
                .setIncludeDeviceName(true)
                .setIncludeTxPowerLevel(false)
                .addServiceUuid(ParcelUuid(heartRateServiceUUID))
                .build()

            it.startAdvertising(settings, data, advertiseCallback)
        }
        Log.d("BLE", "Successful advertising")
        isAdvertising = true
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_ADVERTISE)
    private fun stopBleAdvertising() {
        bluetoothAdvertiser.stopAdvertising(advertiseCallback)
        isAdvertising = false
    }

    private fun updateData() {
        val debugTag = "DataSender"
        if (connectedDevice == null) {
            Log.d("BLE", "No device is connected.")
            return
        }
        Log.d(debugTag, "Attempting to get characteristic.")
        val readCharacteristic = gattServer
            .getService(heartRateServiceUUID)
            .getCharacteristic(heartRateCharacteristicUUID)
        val minVal = params[parameterList[0]]?.get("minVal").toString().toInt()
        val maxVal = params[parameterList[0]]?.get("maxVal").toString().toInt()
        val freq = params[parameterList[0]]?.get("frequency").toString().split(':')
        val delay: Long = freq[1].toLong() / freq[0].toLong() // n раз : m сек
        Log.d("BLE", "Characteristic got.")
        object: Thread() {
            @Suppress("DEPRECATION")
            @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
            override fun run() {
                while(isAdvertising) {
                    newValueBytes = byteArrayOf((minVal..maxVal).random().toByte())
                    readCharacteristic.value = newValueBytes
                    readCharacteristic.getDescriptor(heartRateDescriptorUUID).value = newValueBytes
                    Log.d(debugTag, "Sending notification ${readCharacteristic.value}")
                    val isNotified = gattServer.notifyCharacteristicChanged(connectedDevice, readCharacteristic, false)
                    Log.d("BLE", if (isNotified) { "Notification sent." } else { "Notification is not sent." })
                    runBlocking { delay (delay) }
                }
            }
        }.start()
    }

    private fun Context.hasPermission(permissionType: String): Boolean {
        return ContextCompat.checkSelfPermission(this, permissionType) ==
                PackageManager.PERMISSION_GRANTED
    }

    private fun Context.hasRequiredRuntimePermissions(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            hasPermission(Manifest.permission.BLUETOOTH_SCAN) &&
                    hasPermission(Manifest.permission.BLUETOOTH_CONNECT) &&
                    hasPermission(Manifest.permission.BLUETOOTH_ADVERTISE)
        } else {
            hasPermission(Manifest.permission.ACCESS_FINE_LOCATION) &&
                    hasPermission(Manifest.permission.BLUETOOTH_ADVERTISE)
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

    @RequiresApi(Build.VERSION_CODES.S)
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
                            Manifest.permission.BLUETOOTH_CONNECT,
                            Manifest.permission.BLUETOOTH_ADVERTISE
                        ),
                        RUNTIME_PERMISSION_REQUEST_CODE
                    )
                }
                show()
            }
        }
    }


    @RequiresPermission(Manifest.permission.BLUETOOTH_ADVERTISE)
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
                        Log.d("permissions", "OK!")
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