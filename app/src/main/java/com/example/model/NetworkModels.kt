package com.example.model

enum class DeviceType {
    HEADPHONES,
    SPEAKER,
    CAR_AUDIO,
    SMARTWATCH,
    PHONE_COMPUTER,
    SMART_HOME,
    OTHER
}

enum class AudioStreamType {
    MEDIA,
    CALLS,
    NOTIFICATIONS
}

enum class BluetoothScenario(val title: String, val desc: String) {
    DEFAULT("Standard Auto", "Balanced connections for all paired accessories"),
    DRIVING("Car Commute", "Prioritizes Car Audio for Calls & Navigation; silences wrist audio"),
    WORKOUT("Gym & Run", "Connects Earbuds + Watch; activates Dual Audio for partner"),
    PARTY("Multi-Speaker Sync", "Dual audio broadcast across all portable Bluetooth speakers"),
    FOCUS("Focus & ANC", "Directs audio exclusively to Hi-Res Noise Cancelling Headphones")
}

data class WifiNetworkInfo(
    val ssid: String,
    val bssid: String,
    val rssi: Int, // dBm, e.g. -45 dBm
    val signalPercent: Int, // 0 - 100%
    val frequencyMhz: Int, // e.g. 2412, 5180
    val band: String, // "2.4 GHz", "5 GHz", "6 GHz"
    val channel: Int, // e.g. 1, 6, 11, 36, etc.
    val security: String, // "WPA3-Personal", "WPA2-PSK", "Open", "WPA/WPA2"
    val estimatedDistanceMeters: Double,
    val linkSpeedMbps: Int,
    val isConnected: Boolean = false,
    val isWpsSupported: Boolean = false
)

data class ConnectedWifiDetails(
    val ssid: String,
    val bssid: String,
    val ipAddress: String,
    val gatewayIp: String,
    val subnetMask: String,
    val dnsAddresses: String,
    val macAddress: String,
    val linkSpeedMbps: Int,
    val frequencyMhz: Int,
    val band: String,
    val channel: Int,
    val rssi: Int,
    val signalPercent: Int,
    val standard: String = "Wi-Fi 6 (802.11ax)",
    val isMetered: Boolean = false
)

data class ChannelCongestion(
    val channel: Int,
    val band: String,
    val count: Int,
    val qualityScore: Int, // 0 to 100 (100 is best / least congested)
    val status: String // "Best Choice", "Moderate", "Congested"
)

data class SpeedTestResult(
    val pingMs: Int = 0,
    val jitterMs: Int = 0,
    val downloadSpeedMbps: Double = 0.0,
    val uploadSpeedMbps: Double = 0.0,
    val isTesting: Boolean = false,
    val testPhase: String = "Idle", // "Idle", "Ping", "Download", "Upload", "Completed"
    val progress: Float = 0f, // 0f to 1f
    val connectionGrade: String = "A+"
)

data class WifiCostConfig(
    val monthlyPlanCost: Double = 49.99,
    val currencySymbol: String = "$",
    val monthlyLimitGb: Double = 500.0,
    val dataUsedGb: Double = 186.4,
    val billingCycleDay: Int = 1,
    val daysRemainingInCycle: Int = 14,
    val isUnlimited: Boolean = false
) {
    val remainingGb: Double
        get() = if (isUnlimited) 999.0 else (monthlyLimitGb - dataUsedGb).coerceAtLeast(0.0)

    val usagePercent: Float
        get() = if (isUnlimited) 0.35f else (dataUsedGb / monthlyLimitGb).toFloat().coerceIn(0f, 1f)

    val costPerGbUsed: Double
        get() = if (dataUsedGb > 0) monthlyPlanCost / dataUsedGb else 0.0

    val projectedUsageGb: Double
        get() = if (daysRemainingInCycle > 0) {
            val daysElapsed = 30 - daysRemainingInCycle
            val dailyRate = if (daysElapsed > 0) dataUsedGb / daysElapsed else dataUsedGb
            dailyRate * 30
        } else dataUsedGb
}

data class BluetoothDeviceInfo(
    val address: String,
    val name: String,
    val type: DeviceType,
    val isConnected: Boolean,
    val isPaired: Boolean,
    val rssi: Int = -60,
    val batteryPercent: Int = -1, // -1 means unknown/unavailable
    val audioProfile: String = "A2DP Stereo",
    val latencyMs: Int = 35, // e.g. aptX Low Latency 35ms, SBC 150ms
    val isMediaAudioEnabled: Boolean = true,
    val isCallAudioEnabled: Boolean = true,
    val volumePercent: Int = 80,
    val codec: String = "aptX HD (48kHz/24-bit)",
    val priority: Int = 1,
    val isFavorite: Boolean = false
)

data class AudioRoutingMatrix(
    val mediaDeviceAddress: String = "",
    val callDeviceAddress: String = "",
    val notificationDeviceAddress: String = "",
    val secondaryMediaDeviceAddress: String = "",
    val isDualAudioActive: Boolean = false,
    val masterVolume: Float = 0.8f,
    val secondaryVolume: Float = 0.7f
)

data class MusicTrack(
    val id: String,
    val title: String,
    val artist: String,
    val genre: String,
    val durationSec: Int,
    val bpm: Int,
    val baseFreqHz: Float,
    val waveformType: String // "chill", "synth", "ambient", "cyber"
)
