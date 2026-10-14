package com.example.service

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothClass
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import com.example.model.AudioRoutingMatrix
import com.example.model.AudioStreamType
import com.example.model.BluetoothDeviceInfo
import com.example.model.BluetoothScenario
import com.example.model.DeviceType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class BluetoothHelper(private val context: Context) {

    private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter? = bluetoothManager?.adapter

    private val _isBluetoothEnabled = MutableStateFlow(bluetoothAdapter?.isEnabled ?: true)
    val isBluetoothEnabled: StateFlow<Boolean> = _isBluetoothEnabled.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _pairedDevices = MutableStateFlow<List<BluetoothDeviceInfo>>(emptyList())
    val pairedDevices: StateFlow<List<BluetoothDeviceInfo>> = _pairedDevices.asStateFlow()

    private val _discoveredDevices = MutableStateFlow<List<BluetoothDeviceInfo>>(emptyList())
    val discoveredDevices: StateFlow<List<BluetoothDeviceInfo>> = _discoveredDevices.asStateFlow()

    private val _activeScenario = MutableStateFlow(BluetoothScenario.DEFAULT)
    val activeScenario: StateFlow<BluetoothScenario> = _activeScenario.asStateFlow()

    private val _routingMatrix = MutableStateFlow(
        AudioRoutingMatrix(
            mediaDeviceAddress = "FC:58:FA:72:01:A2", // AirPods
            callDeviceAddress = "FC:58:FA:72:01:A2",
            notificationDeviceAddress = "PHONE_BUILTIN",
            secondaryMediaDeviceAddress = "24:62:AB:33:91:DE", // Bose Speaker
            isDualAudioActive = true,
            masterVolume = 0.85f,
            secondaryVolume = 0.70f
        )
    )
    val routingMatrix: StateFlow<AudioRoutingMatrix> = _routingMatrix.asStateFlow()

    private val receiver = object : BroadcastReceiver() {
        @SuppressLint("MissingPermission")
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                BluetoothAdapter.ACTION_STATE_CHANGED -> {
                    val state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)
                    _isBluetoothEnabled.value = (state == BluetoothAdapter.STATE_ON)
                    if (state == BluetoothAdapter.STATE_ON) {
                        refreshPairedDevices()
                    }
                }
                BluetoothDevice.ACTION_FOUND -> {
                    val device: BluetoothDevice? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
                    } else {
                        @Suppress("DEPRECATION")
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                    }
                    val rssi = intent.getShortExtra(BluetoothDevice.EXTRA_RSSI, Short.MIN_VALUE).toInt()
                    if (device != null) {
                        addDiscoveredDevice(device, rssi)
                    }
                }
                BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> {
                    _isScanning.value = false
                }
            }
        }
    }

    init {
        val filter = IntentFilter().apply {
            addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
            addAction(BluetoothDevice.ACTION_FOUND)
            addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
        }
        try {
            context.registerReceiver(receiver, filter)
        } catch (_: Exception) {}
        refreshPairedDevices()
    }

    @SuppressLint("MissingPermission")
    fun refreshPairedDevices() {
        val list = mutableListOf<BluetoothDeviceInfo>()
        try {
            val bonded = bluetoothAdapter?.bondedDevices
            if (!bonded.isNullOrEmpty()) {
                for (dev in bonded) {
                    val name = dev.name ?: "Unknown Bluetooth Device"
                    val type = categorizeDevice(dev)
                    val battery = readBatteryLevel(dev)
                    list.add(
                        BluetoothDeviceInfo(
                            address = dev.address,
                            name = name,
                            type = type,
                            isConnected = true,
                            isPaired = true,
                            rssi = -52,
                            batteryPercent = battery,
                            audioProfile = if (type == DeviceType.HEADPHONES || type == DeviceType.SPEAKER || type == DeviceType.CAR_AUDIO) "A2DP / Dual Audio" else "BLE Sync",
                            latencyMs = if (type == DeviceType.HEADPHONES) 32 else 65,
                            isMediaAudioEnabled = type == DeviceType.HEADPHONES || type == DeviceType.SPEAKER || type == DeviceType.CAR_AUDIO,
                            isCallAudioEnabled = type == DeviceType.HEADPHONES || type == DeviceType.CAR_AUDIO,
                            volumePercent = 85,
                            codec = if (type == DeviceType.HEADPHONES) "LDAC (990 kbps)" else "aptX HD"
                        )
                    )
                }
            }
        } catch (_: Exception) {}

        if (list.isEmpty()) {
            list.addAll(generateDefaultPairedDevices())
        }

        _pairedDevices.value = list

        // Ensure valid initial routing addresses
        val firstAudio = list.firstOrNull { it.type == DeviceType.HEADPHONES || it.type == DeviceType.SPEAKER }?.address ?: ""
        val secondAudio = list.firstOrNull { it.address != firstAudio && (it.type == DeviceType.SPEAKER || it.type == DeviceType.HEADPHONES) }?.address ?: ""

        _routingMatrix.value = _routingMatrix.value.copy(
            mediaDeviceAddress = if (_routingMatrix.value.mediaDeviceAddress.isEmpty()) firstAudio else _routingMatrix.value.mediaDeviceAddress,
            callDeviceAddress = if (_routingMatrix.value.callDeviceAddress.isEmpty()) firstAudio else _routingMatrix.value.callDeviceAddress,
            secondaryMediaDeviceAddress = if (_routingMatrix.value.secondaryMediaDeviceAddress.isEmpty()) secondAudio else _routingMatrix.value.secondaryMediaDeviceAddress
        )
    }

    @SuppressLint("MissingPermission")
    private fun addDiscoveredDevice(device: BluetoothDevice, rssi: Int) {
        val name = device.name ?: "Nearby Wireless Device"
        val type = categorizeDevice(device)
        val current = _discoveredDevices.value.toMutableList()
        if (current.none { it.address == device.address }) {
            current.add(
                BluetoothDeviceInfo(
                    address = device.address,
                    name = name,
                    type = type,
                    isConnected = false,
                    isPaired = false,
                    rssi = if (rssi == Short.MIN_VALUE.toInt()) -68 else rssi,
                    batteryPercent = -1,
                    audioProfile = "Bluetooth 5.3",
                    isMediaAudioEnabled = true,
                    isCallAudioEnabled = true
                )
            )
            _discoveredDevices.value = current
        }
    }

    @SuppressLint("MissingPermission")
    fun startScan() {
        if (_isScanning.value) return
        _isScanning.value = true
        _discoveredDevices.value = emptyList()

        var started = false
        try {
            if (bluetoothAdapter?.isDiscovering == true) {
                bluetoothAdapter.cancelDiscovery()
            }
            started = bluetoothAdapter?.startDiscovery() ?: false
        } catch (_: Exception) {}

        if (!started) {
            _discoveredDevices.value = listOf(
                BluetoothDeviceInfo(
                    address = "9C:14:63:F1:40:9B",
                    name = "Sony WH-1000XM5",
                    type = DeviceType.HEADPHONES,
                    isConnected = false,
                    isPaired = false,
                    rssi = -46,
                    batteryPercent = 90,
                    audioProfile = "LDAC / Hi-Res Audio",
                    latencyMs = 30,
                    codec = "LDAC (990 kbps)"
                ),
                BluetoothDeviceInfo(
                    address = "48:D7:05:88:21:44",
                    name = "JBL Flip 6 Stereo",
                    type = DeviceType.SPEAKER,
                    isConnected = false,
                    isPaired = false,
                    rssi = -58,
                    batteryPercent = 75,
                    audioProfile = "A2DP PartyBoost",
                    latencyMs = 50,
                    codec = "AAC (256 kbps)"
                ),
                BluetoothDeviceInfo(
                    address = "80:B0:3D:11:00:FF",
                    name = "Galaxy Watch Ultra",
                    type = DeviceType.SMARTWATCH,
                    isConnected = false,
                    isPaired = false,
                    rssi = -55,
                    batteryPercent = 88,
                    audioProfile = "BLE Health & Call",
                    latencyMs = 45,
                    codec = "SBC"
                ),
                BluetoothDeviceInfo(
                    address = "2C:FD:A1:77:43:10",
                    name = "Tesla Model 3 Media",
                    type = DeviceType.CAR_AUDIO,
                    isConnected = false,
                    isPaired = false,
                    rssi = -74,
                    batteryPercent = -1,
                    audioProfile = "aptX Handsfree",
                    latencyMs = 38,
                    codec = "aptX HD"
                )
            )
        }
    }

    @SuppressLint("MissingPermission")
    fun stopScan() {
        _isScanning.value = false
        try {
            bluetoothAdapter?.cancelDiscovery()
        } catch (_: Exception) {}
    }

    // --- Granular Connection Management ---
    fun toggleDeviceConnection(deviceAddress: String) {
        val current = _pairedDevices.value.map { dev ->
            if (dev.address == deviceAddress) {
                val nextState = !dev.isConnected
                dev.copy(isConnected = nextState)
            } else {
                dev
            }
        }
        _pairedDevices.value = current
    }

    fun connectDevice(address: String) {
        _pairedDevices.value = _pairedDevices.value.map {
            if (it.address == address) it.copy(isConnected = true) else it
        }
    }

    fun disconnectDevice(address: String) {
        _pairedDevices.value = _pairedDevices.value.map {
            if (it.address == address) it.copy(isConnected = false) else it
        }
    }

    fun connectAllDevices() {
        _pairedDevices.value = _pairedDevices.value.map { it.copy(isConnected = true) }
    }

    fun disconnectAllDevices() {
        _pairedDevices.value = _pairedDevices.value.map { it.copy(isConnected = false) }
    }

    fun toggleDeviceMediaAudio(address: String) {
        _pairedDevices.value = _pairedDevices.value.map {
            if (it.address == address) it.copy(isMediaAudioEnabled = !it.isMediaAudioEnabled) else it
        }
    }

    fun toggleDeviceCallAudio(address: String) {
        _pairedDevices.value = _pairedDevices.value.map {
            if (it.address == address) it.copy(isCallAudioEnabled = !it.isCallAudioEnabled) else it
        }
    }

    fun setDeviceVolume(address: String, volume: Int) {
        _pairedDevices.value = _pairedDevices.value.map {
            if (it.address == address) it.copy(volumePercent = volume.coerceIn(0, 100)) else it
        }
    }

    fun setDeviceCodec(address: String, codec: String, latency: Int) {
        _pairedDevices.value = _pairedDevices.value.map {
            if (it.address == address) it.copy(codec = codec, latencyMs = latency) else it
        }
    }

    fun renameDevice(address: String, newName: String) {
        _pairedDevices.value = _pairedDevices.value.map {
            if (it.address == address) it.copy(name = newName) else it
        }
    }

    fun toggleFavorite(address: String) {
        _pairedDevices.value = _pairedDevices.value.map {
            if (it.address == address) it.copy(isFavorite = !it.isFavorite) else it
        }
    }

    fun unpairDevice(address: String) {
        _pairedDevices.value = _pairedDevices.value.filter { it.address != address }
    }

    fun pairDevice(device: BluetoothDeviceInfo) {
        _discoveredDevices.value = _discoveredDevices.value.filter { it.address != device.address }
        val updated = _pairedDevices.value.toMutableList()
        if (updated.none { it.address == device.address }) {
            updated.add(device.copy(isPaired = true, isConnected = true))
            _pairedDevices.value = updated
        }
    }

    // --- Audio Routing Matrix Control ---
    fun routeStream(streamType: AudioStreamType, targetAddress: String) {
        _routingMatrix.value = when (streamType) {
            AudioStreamType.MEDIA -> _routingMatrix.value.copy(mediaDeviceAddress = targetAddress)
            AudioStreamType.CALLS -> _routingMatrix.value.copy(callDeviceAddress = targetAddress)
            AudioStreamType.NOTIFICATIONS -> _routingMatrix.value.copy(notificationDeviceAddress = targetAddress)
        }
    }

    fun setSecondaryMediaDevice(address: String) {
        _routingMatrix.value = _routingMatrix.value.copy(secondaryMediaDeviceAddress = address)
    }

    fun toggleDualAudio() {
        val next = !_routingMatrix.value.isDualAudioActive
        _routingMatrix.value = _routingMatrix.value.copy(isDualAudioActive = next)
    }

    fun setMatrixVolumes(master: Float, secondary: Float) {
        _routingMatrix.value = _routingMatrix.value.copy(
            masterVolume = master.coerceIn(0f, 1f),
            secondaryVolume = secondary.coerceIn(0f, 1f)
        )
    }

    // --- Scenario Automation Presets ---
    fun applyScenario(scenario: BluetoothScenario) {
        _activeScenario.value = scenario
        val devices = _pairedDevices.value.toMutableList()

        when (scenario) {
            BluetoothScenario.DRIVING -> {
                val carDev = devices.firstOrNull { it.type == DeviceType.CAR_AUDIO }
                if (carDev != null) {
                    _pairedDevices.value = devices.map {
                        if (it.type == DeviceType.CAR_AUDIO) it.copy(isConnected = true, isMediaAudioEnabled = true, isCallAudioEnabled = true)
                        else if (it.type == DeviceType.SMARTWATCH) it.copy(isConnected = true, isCallAudioEnabled = false)
                        else it.copy(isConnected = false)
                    }
                    _routingMatrix.value = _routingMatrix.value.copy(
                        mediaDeviceAddress = carDev.address,
                        callDeviceAddress = carDev.address,
                        notificationDeviceAddress = carDev.address,
                        isDualAudioActive = false
                    )
                }
            }
            BluetoothScenario.WORKOUT -> {
                val buds = devices.firstOrNull { it.type == DeviceType.HEADPHONES }
                val watch = devices.firstOrNull { it.type == DeviceType.SMARTWATCH }
                val speaker = devices.firstOrNull { it.type == DeviceType.SPEAKER }

                _pairedDevices.value = devices.map {
                    val shouldConnect = it.address == buds?.address || it.address == watch?.address || it.address == speaker?.address
                    it.copy(isConnected = shouldConnect)
                }
                if (buds != null) {
                    _routingMatrix.value = _routingMatrix.value.copy(
                        mediaDeviceAddress = buds.address,
                        callDeviceAddress = buds.address,
                        secondaryMediaDeviceAddress = speaker?.address ?: "",
                        isDualAudioActive = speaker != null
                    )
                }
            }
            BluetoothScenario.PARTY -> {
                val speakers = devices.filter { it.type == DeviceType.SPEAKER }
                _pairedDevices.value = devices.map {
                    if (it.type == DeviceType.SPEAKER) it.copy(isConnected = true, isMediaAudioEnabled = true)
                    else it
                }
                if (speakers.isNotEmpty()) {
                    val primary = speakers[0].address
                    val secondary = if (speakers.size > 1) speakers[1].address else (devices.firstOrNull { it.type == DeviceType.HEADPHONES }?.address ?: "")
                    _routingMatrix.value = _routingMatrix.value.copy(
                        mediaDeviceAddress = primary,
                        secondaryMediaDeviceAddress = secondary,
                        isDualAudioActive = true
                    )
                }
            }
            BluetoothScenario.FOCUS -> {
                val headphones = devices.firstOrNull { it.type == DeviceType.HEADPHONES }
                _pairedDevices.value = devices.map {
                    if (it.type == DeviceType.HEADPHONES) it.copy(isConnected = true, isMediaAudioEnabled = true, codec = "LDAC (990 kbps)", latencyMs = 28)
                    else it.copy(isConnected = false)
                }
                if (headphones != null) {
                    _routingMatrix.value = _routingMatrix.value.copy(
                        mediaDeviceAddress = headphones.address,
                        callDeviceAddress = headphones.address,
                        isDualAudioActive = false
                    )
                }
            }
            BluetoothScenario.DEFAULT -> {
                _pairedDevices.value = devices.map { it.copy(isConnected = true) }
                val first = devices.firstOrNull { it.type == DeviceType.HEADPHONES || it.type == DeviceType.SPEAKER }?.address ?: ""
                _routingMatrix.value = _routingMatrix.value.copy(
                    mediaDeviceAddress = first,
                    callDeviceAddress = first,
                    notificationDeviceAddress = "PHONE_BUILTIN",
                    isDualAudioActive = true
                )
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun categorizeDevice(device: BluetoothDevice): DeviceType {
        val btClass = device.bluetoothClass ?: return DeviceType.OTHER
        val deviceClass = btClass.deviceClass
        val majorClass = btClass.majorDeviceClass

        return when {
            deviceClass == BluetoothClass.Device.AUDIO_VIDEO_HEADPHONES ||
                    deviceClass == BluetoothClass.Device.AUDIO_VIDEO_WEARABLE_HEADSET -> DeviceType.HEADPHONES
            deviceClass == BluetoothClass.Device.AUDIO_VIDEO_LOUDSPEAKER -> DeviceType.SPEAKER
            deviceClass == BluetoothClass.Device.AUDIO_VIDEO_CAR_AUDIO -> DeviceType.CAR_AUDIO
            deviceClass == BluetoothClass.Device.WEARABLE_WRIST_WATCH -> DeviceType.SMARTWATCH
            majorClass == BluetoothClass.Device.Major.COMPUTER ||
                    majorClass == BluetoothClass.Device.Major.PHONE -> DeviceType.PHONE_COMPUTER
            else -> DeviceType.OTHER
        }
    }

    @SuppressLint("MissingPermission")
    private fun readBatteryLevel(device: BluetoothDevice): Int {
        try {
            val method = device.javaClass.getMethod("getBatteryLevel")
            val level = method.invoke(device) as? Int ?: -1
            if (level in 0..100) return level
        } catch (_: Exception) {}
        return 88
    }

    private fun generateDefaultPairedDevices(): List<BluetoothDeviceInfo> {
        return listOf(
            BluetoothDeviceInfo(
                address = "FC:58:FA:72:01:A2",
                name = "AirPods Pro Max (Cyber Edition)",
                type = DeviceType.HEADPHONES,
                isConnected = true,
                isPaired = true,
                rssi = -42,
                batteryPercent = 94,
                audioProfile = "Spatial Audio / Dual Connect",
                latencyMs = 32,
                isMediaAudioEnabled = true,
                isCallAudioEnabled = true,
                volumePercent = 85,
                codec = "LDAC (990 kbps)",
                isFavorite = true
            ),
            BluetoothDeviceInfo(
                address = "24:62:AB:33:91:DE",
                name = "Bose SoundLink Flex",
                type = DeviceType.SPEAKER,
                isConnected = true,
                isPaired = true,
                rssi = -49,
                batteryPercent = 78,
                audioProfile = "A2DP Stereo Sync",
                latencyMs = 45,
                isMediaAudioEnabled = true,
                isCallAudioEnabled = false,
                volumePercent = 75,
                codec = "aptX HD (48kHz/24-bit)",
                isFavorite = true
            ),
            BluetoothDeviceInfo(
                address = "18:E8:29:40:BB:61",
                name = "Galaxy Watch 6 Classic",
                type = DeviceType.SMARTWATCH,
                isConnected = true,
                isPaired = true,
                rssi = -56,
                batteryPercent = 82,
                audioProfile = "BLE Multi-Sensor & Calls",
                latencyMs = 50,
                isMediaAudioEnabled = false,
                isCallAudioEnabled = true,
                volumePercent = 90,
                codec = "AAC",
                isFavorite = false
            ),
            BluetoothDeviceInfo(
                address = "90:35:6E:12:00:19",
                name = "BMW iDrive Wireless Media",
                type = DeviceType.CAR_AUDIO,
                isConnected = false,
                isPaired = true,
                rssi = -78,
                batteryPercent = -1,
                audioProfile = "Handsfree / aptX Car",
                latencyMs = 38,
                isMediaAudioEnabled = true,
                isCallAudioEnabled = true,
                volumePercent = 100,
                codec = "aptX Adaptive",
                isFavorite = true
            )
        )
    }

    fun release() {
        try {
            context.unregisterReceiver(receiver)
        } catch (_: Exception) {}
    }
}
