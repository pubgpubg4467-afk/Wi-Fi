package com.example.service

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import com.example.model.MusicTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.OutputStream
import java.net.ServerSocket
import java.net.Socket
import kotlin.math.PI
import kotlin.math.sin

class WifiMusicService(private val context: Context) {

    private val sampleRate = 22050
    private var audioTrack: AudioTrack? = null
    private var playbackJob: Job? = null
    private var serverJob: Job? = null
    private var serverSocket: ServerSocket? = null

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _isServerActive = MutableStateFlow(false)
    val isServerActive: StateFlow<Boolean> = _isServerActive.asStateFlow()

    private val _serverUrl = MutableStateFlow("")
    val serverUrl: StateFlow<String> = _serverUrl.asStateFlow()

    private val _connectedClientsCount = MutableStateFlow(0)
    val connectedClientsCount: StateFlow<Int> = _connectedClientsCount.asStateFlow()

    private val _currentTrack = MutableStateFlow(availableTracks[0])
    val currentTrack: StateFlow<MusicTrack> = _currentTrack.asStateFlow()

    private val _playbackProgressSec = MutableStateFlow(0)
    val playbackProgressSec: StateFlow<Int> = _playbackProgressSec.asStateFlow()

    private val _visualizerAmplitudes = MutableStateFlow(List(16) { 0.2f })
    val visualizerAmplitudes: StateFlow<List<Float>> = _visualizerAmplitudes.asStateFlow()

    private val _equalizerPreset = MutableStateFlow("Bass Boost")
    val equalizerPreset: StateFlow<String> = _equalizerPreset.asStateFlow()

    private val _volumeGain = MutableStateFlow(1.0f)
    val volumeGain: StateFlow<Float> = _volumeGain.asStateFlow()

    companion object {
        val availableTracks = listOf(
            MusicTrack(
                id = "track_1",
                title = "Cyberwave Pulse",
                artist = "NetPulse Audio Labs",
                genre = "Synthwave / Electronic",
                durationSec = 195,
                bpm = 120,
                baseFreqHz = 220f,
                waveformType = "synth"
            ),
            MusicTrack(
                id = "track_2",
                title = "Lo-Fi Wi-Fi Rain",
                artist = "Chill Horizon",
                genre = "Lo-Fi Beats",
                durationSec = 240,
                bpm = 84,
                baseFreqHz = 174f,
                waveformType = "chill"
            ),
            MusicTrack(
                id = "track_3",
                title = "Deep Space Ambient",
                artist = "Aether Resonance",
                genre = "Ambient / Binaural",
                durationSec = 310,
                bpm = 68,
                baseFreqHz = 136.1f,
                waveformType = "ambient"
            ),
            MusicTrack(
                id = "track_4",
                title = "Neon Highway Groove",
                artist = "SubZero Funk",
                genre = "Electro Bass",
                durationSec = 210,
                bpm = 112,
                baseFreqHz = 164.8f,
                waveformType = "cyber"
            )
        )
    }

    fun selectTrack(track: MusicTrack) {
        val wasPlaying = _isPlaying.value
        if (wasPlaying) {
            pause()
        }
        _currentTrack.value = track
        _playbackProgressSec.value = 0
        if (wasPlaying) {
            play()
        }
    }

    fun togglePlayPause() {
        if (_isPlaying.value) {
            pause()
        } else {
            play()
        }
    }

    fun setEqualizerPreset(preset: String) {
        _equalizerPreset.value = preset
    }

    fun setVolume(vol: Float) {
        _volumeGain.value = vol.coerceIn(0.1f, 2.0f)
        try {
            audioTrack?.setVolume(vol.coerceIn(0f, 1f))
        } catch (_: Exception) {}
    }

    fun startWifiServer(localIp: String, port: Int = 8088) {
        if (_isServerActive.value) return
        val url = "http://$localIp:$port"
        _serverUrl.value = url

        serverJob = CoroutineScope(Dispatchers.IO).launch {
            try {
                serverSocket = ServerSocket(port)
                _isServerActive.value = true

                while (isActive) {
                    val clientSocket = serverSocket?.accept() ?: break
                    _connectedClientsCount.value += 1
                    launch(Dispatchers.IO) {
                        handleHttpClient(clientSocket)
                        _connectedClientsCount.value = (_connectedClientsCount.value - 1).coerceAtLeast(0)
                    }
                }
            } catch (_: Exception) {
                _isServerActive.value = false
            }
        }
    }

    fun stopWifiServer() {
        try {
            serverSocket?.close()
        } catch (_: Exception) {}
        serverJob?.cancel()
        _isServerActive.value = false
        _connectedClientsCount.value = 0
    }

    private fun handleHttpClient(socket: Socket) {
        try {
            val out = socket.getOutputStream()
            val track = _currentTrack.value
            val html = """
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="utf-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                    <title>NetPulse Wi-Fi Music Stream</title>
                    <style>
                        body { background: #090D16; color: #F1F5F9; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; display: flex; flex-direction: column; align-items: center; justify-content: center; min-height: 100vh; margin: 0; padding: 20px; text-align: center; }
                        .card { background: #162032; border: 1px solid #00E5FF44; border-radius: 20px; padding: 30px; max-width: 420px; width: 100%; box-shadow: 0 10px 40px #00000088; }
                        h1 { color: #00E5FF; margin: 0 0 8px 0; font-size: 24px; }
                        .sub { color: #94A3B8; font-size: 14px; margin-bottom: 24px; }
                        .track-name { font-size: 20px; font-weight: bold; color: #FFF; margin: 12px 0 4px; }
                        .artist { color: #8B5CF6; font-size: 15px; margin-bottom: 20px; }
                        .badge { display: inline-block; background: #00E5FF22; color: #00E5FF; border: 1px solid #00E5FF66; padding: 6px 14px; border-radius: 20px; font-size: 13px; font-weight: 600; margin-bottom: 20px; }
                        .status { color: #10B981; font-weight: bold; margin-top: 16px; font-size: 14px; }
                        audio { width: 100%; margin-top: 16px; border-radius: 8px; }
                        .pulse { width: 80px; height: 80px; border-radius: 50%; background: radial-gradient(circle, #00E5FF 0%, #8B5CF6 100%); margin: 0 auto 20px; box-shadow: 0 0 25px #00E5FF88; animation: pulse 2s infinite ease-in-out; }
                        @keyframes pulse { 0%, 100% { transform: scale(0.95); opacity: 0.8; } 50% { transform: scale(1.08); opacity: 1; } }
                    </style>
                </head>
                <body>
                    <div class="card">
                        <div class="pulse"></div>
                        <span class="badge">Wi-Fi Multi-Room Sync</span>
                        <h1>NetPulse Live Music</h1>
                        <div class="sub">Streaming across your local Wi-Fi network</div>
                        <div class="track-name">${track.title}</div>
                        <div class="artist">${track.artist} &bull; ${track.genre}</div>
                        <p class="status">&#x25CF; Synchronized with Host Mobile Device</p>
                    </div>
                </body>
                </html>
            """.trimIndent()

            val response = "HTTP/1.1 200 OK\r\n" +
                    "Content-Type: text/html; charset=UTF-8\r\n" +
                    "Content-Length: ${html.toByteArray().size}\r\n" +
                    "Connection: close\r\n\r\n" +
                    html

            out.write(response.toByteArray())
            out.flush()
        } catch (_: Exception) {
        } finally {
            try { socket.close() } catch (_: Exception) {}
        }
    }

    private fun play() {
        _isPlaying.value = true
        initAudioTrack()

        playbackJob?.cancel()
        playbackJob = CoroutineScope(Dispatchers.Default).launch {
            val bufferSize = sampleRate / 10 // 100ms chunk
            val buffer = ShortArray(bufferSize)
            var phase = 0.0
            var tick = 0

            while (isActive && _isPlaying.value) {
                val track = _currentTrack.value
                val baseFreq = track.baseFreqHz
                val gain = _volumeGain.value
                val preset = _equalizerPreset.value

                // Synthesize melodic musical chord tones
                val chordMultiplier = when ((tick / 8) % 4) {
                    0 -> 1.0f    // Tonic
                    1 -> 1.25f   // Major third
                    2 -> 1.5f    // Fifth
                    else -> 1.875f // Seventh
                }

                val bassBoost = if (preset == "Bass Boost") 1.8f else 1.0f
                val trebleCrisp = if (preset == "Treble Crisp") 1.5f else 1.0f

                for (i in 0 until bufferSize) {
                    val t = (phase + i) / sampleRate
                    // Dual oscillator synth waveform + sub-bass
                    val noteFreq = baseFreq * chordMultiplier
                    val wave1 = sin(2 * PI * noteFreq * t)
                    val wave2 = sin(2 * PI * (noteFreq * 2.01) * t) * 0.4 * trebleCrisp
                    val subBass = sin(2 * PI * (baseFreq * 0.5) * t) * 0.6 * bassBoost
                    val beatPulse = sin(2 * PI * (track.bpm / 60.0) * t).coerceAtLeast(0.0) * 0.3

                    val sampleVal = ((wave1 + wave2 + subBass + beatPulse) * 8000 * gain).toInt().coerceIn(-32767, 32767)
                    buffer[i] = sampleVal.toShort()
                }

                phase += bufferSize
                audioTrack?.write(buffer, 0, bufferSize)

                tick++
                if (tick % 10 == 0) {
                    _playbackProgressSec.value = (_playbackProgressSec.value + 1) % track.durationSec

                    // Update real-time dancing visualizer bars
                    val newAmps = List(16) { barIdx ->
                        val barFreq = (barIdx + 1) * 0.4
                        val anim = (sin(tick * 0.3 + barFreq) * 0.4 + 0.55).toFloat().coerceIn(0.1f, 0.98f)
                        anim
                    }
                    _visualizerAmplitudes.value = newAmps
                }
            }
        }
    }

    private fun pause() {
        _isPlaying.value = false
        playbackJob?.cancel()
        playbackJob = null
        try {
            audioTrack?.pause()
            audioTrack?.flush()
        } catch (_: Exception) {}
        _visualizerAmplitudes.value = List(16) { 0.15f }
    }

    private fun initAudioTrack() {
        if (audioTrack == null) {
            val minBuf = AudioTrack.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            )
            audioTrack = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(minBuf * 2)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()
        }
        audioTrack?.play()
    }

    fun release() {
        stopWifiServer()
        pause()
        audioTrack?.release()
        audioTrack = null
    }
}
