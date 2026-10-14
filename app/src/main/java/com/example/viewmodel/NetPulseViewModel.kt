package com.example.viewmodel

import android.app.Application
import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.model.BluetoothDeviceInfo
import com.example.model.ChannelCongestion
import com.example.model.ConnectedWifiDetails
import com.example.model.MusicTrack
import com.example.model.SpeedTestResult
import com.example.model.WifiCostConfig
import com.example.model.WifiNetworkInfo
import com.example.service.BluetoothHelper
import com.example.service.SpeedTestEngine
import com.example.service.WifiHelper
import com.example.service.WifiMusicService
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class NetPulseViewModel(application: Application) : AndroidViewModel(application) {

    private val wifiHelper = WifiHelper(application)
    private val speedTestEngine = SpeedTestEngine()
    val musicService = WifiMusicService(application)
    val bluetoothHelper = BluetoothHelper(application)

    // Tone generator for Range Walking Radar audio beeps
    private var toneGenerator: ToneGenerator? = null

    // --- Wi-Fi Scan & Details ---
    private val _connectedWifi = MutableStateFlow<ConnectedWifiDetails?>(null)
    val connectedWifi: StateFlow<ConnectedWifiDetails?> = _connectedWifi.asStateFlow()

    private val _scannedNetworks = MutableStateFlow<List<WifiNetworkInfo>>(emptyList())
    val scannedNetworks: StateFlow<List<WifiNetworkInfo>> = _scannedNetworks.asStateFlow()

    private val _isWifiScanning = MutableStateFlow(false)
    val isWifiScanning: StateFlow<Boolean> = _isWifiScanning.asStateFlow()

    private val _wifiBandFilter = MutableStateFlow("All") // "All", "2.4 GHz", "5 GHz", "Open"
    val wifiBandFilter: StateFlow<String> = _wifiBandFilter.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // --- Range & Channels ---
    private val _channelCongestion = MutableStateFlow<List<ChannelCongestion>>(emptyList())
    val channelCongestion: StateFlow<List<ChannelCongestion>> = _channelCongestion.asStateFlow()

    private val _walkingRssiHistory = MutableStateFlow<List<Int>>(listOf(-52, -50, -54, -48, -46, -49, -51))
    val walkingRssiHistory: StateFlow<List<Int>> = _walkingRssiHistory.asStateFlow()

    private val _isRadarWalkingActive = MutableStateFlow(false)
    val isRadarWalkingActive: StateFlow<Boolean> = _isRadarWalkingActive.asStateFlow()

    private val _isRadarAudioBeepEnabled = MutableStateFlow(true)
    val isRadarAudioBeepEnabled: StateFlow<Boolean> = _isRadarAudioBeepEnabled.asStateFlow()

    private var walkingJob: Job? = null

    // --- Speed Test & Cost ---
    private val _speedTestResult = MutableStateFlow(SpeedTestResult())
    val speedTestResult: StateFlow<SpeedTestResult> = _speedTestResult.asStateFlow()

    private val _speedHistory = MutableStateFlow<List<SpeedTestResult>>(emptyList())
    val speedHistory: StateFlow<List<SpeedTestResult>> = _speedHistory.asStateFlow()

    private val _costConfig = MutableStateFlow(WifiCostConfig())
    val costConfig: StateFlow<WifiCostConfig> = _costConfig.asStateFlow()

    init {
        try {
            toneGenerator = ToneGenerator(AudioManager.STREAM_MUSIC, 60)
        } catch (_: Exception) {}

        refreshWifiData()
    }

    fun refreshWifiData() {
        viewModelScope.launch {
            _isWifiScanning.value = true
            _connectedWifi.value = wifiHelper.getConnectedWifiDetails()
            val networks = wifiHelper.scanWifiNetworks()
            _scannedNetworks.value = networks
            _channelCongestion.value = wifiHelper.analyzeChannels(networks)
            delay(500)
            _isWifiScanning.value = false
        }
    }

    fun setWifiFilter(filter: String) {
        _wifiBandFilter.value = filter
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    // --- Range & Walking Radar ---
    fun toggleWalkingRadar() {
        if (_isRadarWalkingActive.value) {
            stopWalkingRadar()
        } else {
            startWalkingRadar()
        }
    }

    private fun startWalkingRadar() {
        _isRadarWalkingActive.value = true
        walkingJob?.cancel()
        walkingJob = viewModelScope.launch {
            while (isActive && _isRadarWalkingActive.value) {
                val conn = wifiHelper.getConnectedWifiDetails()
                _connectedWifi.value = conn

                // Simulate slight variance as user walks around rooms
                val variance = ((-3..3).random())
                val currentRssi = (conn.rssi + variance).coerceIn(-95, -30)

                val history = _walkingRssiHistory.value.toMutableList()
                if (history.size >= 20) {
                    history.removeAt(0)
                }
                history.add(currentRssi)
                _walkingRssiHistory.value = history

                // Audio beep rate scales with signal proximity:
                // Better signal = higher pitch and faster beeps (geiger counter / radar effect)
                if (_isRadarAudioBeepEnabled.value) {
                    try {
                        val toneType = if (currentRssi >= -50) ToneGenerator.TONE_PROP_PROMPT else ToneGenerator.TONE_PROP_BEEP
                        toneGenerator?.startTone(toneType, 40)
                    } catch (_: Exception) {}
                }

                val sleepTime = when {
                    currentRssi >= -50 -> 400L
                    currentRssi >= -65 -> 700L
                    else -> 1200L
                }
                delay(sleepTime)
            }
        }
    }

    private fun stopWalkingRadar() {
        _isRadarWalkingActive.value = false
        walkingJob?.cancel()
        walkingJob = null
    }

    fun toggleRadarAudioBeep() {
        _isRadarAudioBeepEnabled.value = !_isRadarAudioBeepEnabled.value
    }

    // --- Speed Test ---
    fun startSpeedTest() {
        if (_speedTestResult.value.isTesting) return

        viewModelScope.launch {
            speedTestEngine.runSpeedTest().collect { result ->
                _speedTestResult.value = result
                if (!result.isTesting && result.downloadSpeedMbps > 0) {
                    val currentHistory = _speedHistory.value.toMutableList()
                    currentHistory.add(0, result)
                    if (currentHistory.size > 10) currentHistory.removeLast()
                    _speedHistory.value = currentHistory
                }
            }
        }
    }

    // --- Cost & Usage ---
    fun updateCostConfig(
        monthlyCost: Double,
        currency: String,
        limitGb: Double,
        usedGb: Double,
        isUnlimited: Boolean
    ) {
        _costConfig.value = _costConfig.value.copy(
            monthlyPlanCost = monthlyCost,
            currencySymbol = currency,
            monthlyLimitGb = limitGb,
            dataUsedGb = usedGb,
            isUnlimited = isUnlimited
        )
    }

    // --- Wi-Fi Music ---
    fun toggleWifiMusicServer() {
        if (musicService.isServerActive.value) {
            musicService.stopWifiServer()
        } else {
            val ip = _connectedWifi.value?.ipAddress ?: "192.168.1.108"
            musicService.startWifiServer(ip)
        }
    }

    // --- Bluetooth Hub & Centralized Management ---
    fun toggleBluetoothConnection(address: String) {
        bluetoothHelper.toggleDeviceConnection(address)
    }

    fun connectAllBluetooth() {
        bluetoothHelper.connectAllDevices()
    }

    fun disconnectAllBluetooth() {
        bluetoothHelper.disconnectAllDevices()
    }

    fun routeAudioStream(streamType: com.example.model.AudioStreamType, address: String) {
        bluetoothHelper.routeStream(streamType, address)
    }

    fun setSecondaryMediaDevice(address: String) {
        bluetoothHelper.setSecondaryMediaDevice(address)
    }

    fun toggleDualAudio() {
        bluetoothHelper.toggleDualAudio()
    }

    fun setMatrixVolumes(master: Float, secondary: Float) {
        bluetoothHelper.setMatrixVolumes(master, secondary)
    }

    fun applyBluetoothScenario(scenario: com.example.model.BluetoothScenario) {
        bluetoothHelper.applyScenario(scenario)
    }

    fun toggleDeviceMedia(address: String) {
        bluetoothHelper.toggleDeviceMediaAudio(address)
    }

    fun toggleDeviceCall(address: String) {
        bluetoothHelper.toggleDeviceCallAudio(address)
    }

    fun setDeviceVolume(address: String, volume: Int) {
        bluetoothHelper.setDeviceVolume(address, volume)
    }

    fun setDeviceCodec(address: String, codec: String, latency: Int) {
        bluetoothHelper.setDeviceCodec(address, codec, latency)
    }

    fun renameDevice(address: String, newName: String) {
        bluetoothHelper.renameDevice(address, newName)
    }

    fun toggleFavorite(address: String) {
        bluetoothHelper.toggleFavorite(address)
    }

    fun unpairDevice(address: String) {
        bluetoothHelper.unpairDevice(address)
    }

    fun startBluetoothScan() {
        bluetoothHelper.startScan()
    }

    fun pairBluetoothDevice(device: BluetoothDeviceInfo) {
        bluetoothHelper.pairDevice(device)
    }

    override fun onCleared() {
        super.onCleared()
        stopWalkingRadar()
        toneGenerator?.release()
        musicService.release()
        bluetoothHelper.release()
    }
}
