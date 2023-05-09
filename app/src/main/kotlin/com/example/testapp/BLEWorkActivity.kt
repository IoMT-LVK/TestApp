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
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresPermission
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.example.testapp.configs.DeviceConfig
import com.example.testapp.configs.Settings
import com.example.testapp.databinding.BleWorkBinding
import kotlinx.coroutines.*
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone.Companion.currentSystemDefault
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.io.FileOutputStream
import java.io.OutputStreamWriter
import java.util.*

private const val RUNTIME_PERMISSION_REQUEST_CODE = 2

class BLEWorkActivity : AppCompatActivity() {
    lateinit var fastLayout : BleWorkBinding

    @RequiresApi(Build.VERSION_CODES.S)
    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        fastLayout = BleWorkBinding.inflate(layoutInflater)
        setContentView(fastLayout.root)
        paramSettings = Json.decodeFromString(intent.getStringExtra("settings").toString())
        var txt = "Current Settings: " + intent.getStringExtra("device")
        for (sett in paramSettings) {
            txt += "\n${sett.paramName}: \n\tminVal - ${sett.minVal}, maxVal - ${sett.maxVal},"
            txt += "\n\tfrequency - ${sett.freq}"
        }
        fastLayout.curSett.text = txt
        gattTableSettings = Json.decodeFromString(intent.getStringExtra("config").toString())
        charKeys = gattTableSettings.characteristics.keys.toList()
        for (param in paramSettings) {
            paramMap[param.paramName] = param
        }
        scope = CoroutineScope(Dispatchers.Default)
        startServer()
    }

    @RequiresApi(Build.VERSION_CODES.S)
    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    override fun onDestroy() {
        stopServer()
        scope.cancel()
        Log.d("BLE", "Server closed, coroutines closed.")
        super.onDestroy()
    }

    private lateinit var gattServer: BluetoothGattServer
    private lateinit var gattTableSettings: DeviceConfig
    private lateinit var charKeys: List<String>

    private var connectedDevice: BluetoothDevice? = null

    private var paramSettings: List<Settings> = listOf()
    private var paramMap: MutableMap<String, Settings> = mutableMapOf()
    private var newBytesByUUID: MutableMap<UUID, ByteArray> = mutableMapOf()

    private lateinit var scope: CoroutineScope

    private lateinit var fileOutputStream: FileOutputStream
    private lateinit var outputWriter: OutputStreamWriter
    private lateinit var fileName: String
    private var fileExist = false

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
            this@BLEWorkActivity.runOnUiThread {
                fastLayout.swButton.text = if (value) "Остановить работу" else "Начать работу"
            }
        }

    @RequiresApi(Build.VERSION_CODES.S)
    @RequiresPermission(
        allOf = [
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.BLUETOOTH_ADVERTISE,
        ]
    )
    fun onClickListener(@Suppress("UNUSED_PARAMETER") view: View) {
        if (isAdvertising) {
            stopBleAdvertising()
            outputWriter.close()
            fileOutputStream.close()
            if (connectedDevice != null) {
                gattServer.cancelConnection(connectedDevice)
            }
            fileExist = true
        } else {
            if (fileExist) {
                val file = getFileStreamPath(fileName)
                file.delete()
                fileExist = false
            }
            fileName = "${System.currentTimeMillis() / 1000}.txt"
            fileOutputStream = openFileOutput(fileName, Context.MODE_PRIVATE)
            Log.d("BLE", "File name: $fileName")
            outputWriter = OutputStreamWriter(fileOutputStream)
            startBleAdvertising()
        }
    }

    fun onClickShare(@Suppress("UNUSED_PARAMETER") view: View) {
        if (!fileExist) {
            return
        }
        val intent = Intent(Intent.ACTION_SEND)
        intent.type = "text/*"
        val file = getFileStreamPath(fileName)
        val fileUri = FileProvider.getUriForFile(
            this,
            "com.example.testapp.fileprovider",
            file
        )
        Log.d("BLE", "file length ${file.length()}")
        intent.putExtra(Intent.EXTRA_STREAM, fileUri)
        startActivity(Intent.createChooser(intent, "share $fileName with"))
    }

    @RequiresApi(Build.VERSION_CODES.S)
    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    private fun newService(id: Int): BluetoothGattService {
        val charConfig = gattTableSettings.characteristics[charKeys[id]]
        val service = BluetoothGattService(
            UUID.fromString(charConfig!!.serviceUuid),
            BluetoothGattService.SERVICE_TYPE_PRIMARY,
        )
        val characteristic = BluetoothGattCharacteristic(
            UUID.fromString(charConfig.characteristicUuid),
            BluetoothGattCharacteristic.PROPERTY_READ or BluetoothGattCharacteristic.PROPERTY_NOTIFY,
            BluetoothGattCharacteristic.PERMISSION_READ,
        )
        service.addCharacteristic(characteristic)
        newBytesByUUID[UUID.fromString(charConfig.characteristicUuid)] = byteArrayOf()
        scope.launch { updateData(characteristic, charKeys[id]) }
        return service
    }

    @RequiresApi(Build.VERSION_CODES.S)
    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    private fun startServer() {
        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        gattServer = bluetoothManager.openGattServer(this, gattServerCallback)
        gattServer.addService(newService(0))
    }

    @RequiresApi(Build.VERSION_CODES.S)
    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    private fun stopServer() {
        gattServer.close()
    }

    private val gattServerCallback: BluetoothGattServerCallback = object: BluetoothGattServerCallback() {
        private var i: Int = 1

        //Callback indicating when a remote device has been connected or disconnected.
        @RequiresApi(Build.VERSION_CODES.S)
        @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
        override fun onConnectionStateChange(
            device: BluetoothDevice?,
            status: Int,
            newState: Int
        ) {
            super.onConnectionStateChange(device, status, newState)
            Log.d("BLE", "onConnectionStateChange $status -> $newState")
            when(newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    (device?.name ?: "unknown").also { fastLayout.phoneName.text = it }
                    (device?.address ?: "unknown").also { fastLayout.phoneAddr.text = it }
                    connectedDevice = device
                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    "Имя устройства".also { fastLayout.phoneName.text = it }
                    "Адрес устройства".also { fastLayout.phoneAddr.text = it }
                    connectedDevice = null
                }
            }
            Log.d("BLE", "onConnectionStateChange: device $connectedDevice")
        }

        //Indicates whether a local service has been added successfully.
        @RequiresApi(Build.VERSION_CODES.S)
        @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
        override fun onServiceAdded(status: Int, service: BluetoothGattService?) {
            Log.d("BLE", "Gatt server service was added.")
            super.onServiceAdded(status, service)
            if (i < charKeys.size) {
                gattServer.addService(newService(i))
                i += 1
            }
        }

        //A remote client has requested to read a local characteristic.
        @RequiresApi(Build.VERSION_CODES.S)
        @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
        override fun onCharacteristicReadRequest(
            device: BluetoothDevice?,
            requestId: Int,
            offset: Int, characteristic:
            BluetoothGattCharacteristic?
        ) {
            super.onCharacteristicReadRequest(device, requestId, offset, characteristic)
            Log.d("BLE", "READ called onCharacteristicReadRequest ${characteristic?.uuid ?: "UNDEFINED"}")
            gattServer.sendResponse(
                device, requestId,
                BluetoothGatt.GATT_SUCCESS, 0,
                newBytesByUUID[characteristic?.uuid],
            )
        }

        //A remote client has requested to read a local descriptor.
        @RequiresApi(Build.VERSION_CODES.S)
        @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
        override fun onDescriptorReadRequest(device: BluetoothDevice?, requestId: Int, offset: Int, descriptor: BluetoothGattDescriptor?) {
            super.onDescriptorReadRequest(device, requestId, offset, descriptor)
            Log.d("BLE", "READ called onDescriptorReadRequest ${descriptor?.uuid ?: "UNDEFINED"}")
            /**if (descriptor?.uuid == heartRateDescriptorUUID) {
                gattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, 0, newValueBytes)
            }*/
        }

        //Callback invoked when a notification or indication has been sent to a remote device.
        @RequiresApi(Build.VERSION_CODES.S)
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

    @RequiresApi(Build.VERSION_CODES.S)
    @RequiresPermission(Manifest.permission.BLUETOOTH_ADVERTISE)
    private fun startBleAdvertising() {
        if (!hasRequiredRuntimePermissions()) { requestRelevantRuntimePermissions() }
        else {
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
                    .build()

                it.startAdvertising(settings, data, advertiseCallback)
            }
            Log.d("BLE", "Successful advertising")
            isAdvertising = true
        }
    }

    @RequiresApi(Build.VERSION_CODES.S)
    @RequiresPermission(Manifest.permission.BLUETOOTH_ADVERTISE)
    private fun stopBleAdvertising() {
        bluetoothAdvertiser.stopAdvertising(advertiseCallback)
        Log.d("BLE", "Stop advertising")
        isAdvertising = false
    }

    private fun LocalDateTime.Companion.now() = Clock.System.now().toLocalDateTime(currentSystemDefault())

    @Suppress("DEPRECATION")
    @RequiresApi(Build.VERSION_CODES.S)
    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    private suspend fun updateData(
        char: BluetoothGattCharacteristic,
        name: String
    ) {
        Log.d("BLE", "Attempting to get characteristic.")
        val prettyName = gattTableSettings.characteristics[name]!!.prettyName
        val minVal = paramMap[prettyName]!!.minVal
        val maxVal = paramMap[prettyName]!!.maxVal
        val freq = paramMap[prettyName]!!.freq.split(':') // f[0] раз : f[1] сек
        val delay: Long = (freq[1].toLong() * 1000L) / freq[0].toLong() // каждые (f[1]/f[0]) сек
        Log.d("BLE", "Characteristic $prettyName got.")
        Log.d("BLE", "Settings: ${minVal}, ${maxVal}, $freq")
        while(true) {
            delay (delay)
            if (connectedDevice == null || !isAdvertising) {
                Log.d("BLE", "ConnectedDevice ${connectedDevice}, Advertising $isAdvertising")
                continue
            }
            val value = (minVal..maxVal).random()
            newBytesByUUID[char.uuid] = byteArrayOf(value.toByte())
            char.value = newBytesByUUID[char.uuid]
            //readCharacteristic.getDescriptor(heartRateDescriptorUUID).value = newValueBytes
            Log.d("BLE", "Sending notification ${char.value}")
            val isNotified = gattServer.notifyCharacteristicChanged(connectedDevice, char, false)
            Log.d("BLE", if (isNotified) { "Notification sent." } else { "Notification is not sent." })
            val time = LocalDateTime.now()
            withContext(Dispatchers.IO) {
                outputWriter.write("$time $name ${value}\n")
            }
            Log.d("BLE", "$time $name ${value}\n")
        }
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
                    hasPermission(Manifest.permission.BLUETOOTH_ADMIN)
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
                setPositiveButton(android.R.string.ok) { _, _ ->
                    ActivityCompat.requestPermissions(
                        this@BLEWorkActivity,
                        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.BLUETOOTH_ADMIN),
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
                setPositiveButton(android.R.string.ok) { _, _ ->
                    ActivityCompat.requestPermissions(
                        this@BLEWorkActivity,
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


    @RequiresApi(Build.VERSION_CODES.S)
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
                        Log.d("BLE", "Permissions OK!")
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
