package com.example.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cast
import androidx.compose.material.icons.filled.CastConnected
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.MusicTrack
import com.example.service.WifiMusicService
import com.example.ui.components.QrCodeCanvas
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.DarkCard
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.NeonViolet
import com.example.ui.theme.SignalGreen
import com.example.ui.theme.WarningAmber
import com.example.viewmodel.NetPulseViewModel

@Composable
fun WifiMusicScreen(
    viewModel: NetPulseViewModel,
    modifier: Modifier = Modifier
) {
    val musicService = viewModel.musicService
    val isPlaying by musicService.isPlaying.collectAsState()
    val isServerActive by musicService.isServerActive.collectAsState()
    val serverUrl by musicService.serverUrl.collectAsState()
    val clientCount by musicService.connectedClientsCount.collectAsState()
    val currentTrack by musicService.currentTrack.collectAsState()
    val progressSec by musicService.playbackProgressSec.collectAsState()
    val amplitudes by musicService.visualizerAmplitudes.collectAsState()
    val currentEq by musicService.equalizerPreset.collectAsState()
    val volumeGain by musicService.volumeGain.collectAsState()

    var showQrDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // --- Header Title ---
        item {
            Column {
                Text(
                    text = "Wi-Fi Music Sync & Stream",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "Stream synchronized music across all phones on the same Wi-Fi",
                    fontSize = 13.sp,
                    color = Color(0xFF94A3B8)
                )
            }
        }

        // --- Wi-Fi LAN Audio Broadcast Card ---
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("wifi_broadcast_card"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = DarkCard),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (isServerActive) SignalGreen else Color(0xFF1E293B)
                )
            ) {
                Column(
                    modifier = Modifier
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    (if (isServerActive) SignalGreen else CyberCyan).copy(alpha = 0.12f),
                                    Color.Transparent
                                )
                            )
                        )
                        .padding(18.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (isServerActive) Icons.Default.CastConnected else Icons.Default.Cast,
                                contentDescription = null,
                                tint = if (isServerActive) SignalGreen else CyberCyan
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isServerActive) "Wi-Fi Broadcast Live" else "Wi-Fi Music Server",
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }

                        if (isServerActive) {
                            Box(
                                modifier = Modifier
                                    .background(SignalGreen.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "$clientCount Listeners Active",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = SignalGreen
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = if (isServerActive)
                            "Any phone, tablet, or PC on this Wi-Fi can open the URL or scan the QR code to hear this audio stream in sync!"
                        else
                            "Turn on Wi-Fi broadcast to synchronize music playback across multiple phones on the same wireless network.",
                        fontSize = 12.sp,
                        color = Color(0xFF94A3B8),
                        lineHeight = 16.sp
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    if (isServerActive && serverUrl.isNotEmpty()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(DarkSurface, RoundedCornerShape(12.dp))
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Stream URL (Local Wi-Fi)", fontSize = 11.sp, color = Color(0xFF64748B))
                                Text(serverUrl, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = CyberCyan)
                            }

                            IconButton(
                                onClick = { showQrDialog = true },
                                modifier = Modifier
                                    .background(Color(0xFF1E293B), CircleShape)
                                    .testTag("show_music_qr_button")
                            ) {
                                Icon(Icons.Default.QrCode, contentDescription = "Show QR", tint = CyberCyan)
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    Button(
                        onClick = { viewModel.toggleWifiMusicServer() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isServerActive) Color(0xFFEF4444) else SignalGreen
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                            .testTag("toggle_wifi_music_server_button")
                    ) {
                        Icon(
                            imageVector = if (isServerActive) Icons.Default.CastConnected else Icons.Default.Cast,
                            contentDescription = null,
                            tint = Color(0xFF090D16),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isServerActive) "Stop Wi-Fi Broadcast" else "Start Wi-Fi Multi-Phone Broadcast",
                            color = Color(0xFF090D16),
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }

        // --- Now Playing & Audio Synthesizer Player ---
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("music_player_card"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = DarkCard),
                border = androidx.compose.foundation.BorderStroke(1.dp, NeonViolet.copy(alpha = 0.35f))
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Album art banner & Visualizer
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(130.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(
                                Brush.linearGradient(
                                    listOf(
                                        Color(0xFF1F1235),
                                        Color(0xFF0E1A33),
                                        Color(0xFF14243B)
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        // Dancing 16-bar spectrum visualizer
                        AudioSpectrumCanvas(
                            amplitudes = amplitudes,
                            isPlaying = isPlaying,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(90.dp)
                                .padding(horizontal = 16.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Track info
                    Text(
                        text = currentTrack.title,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "${currentTrack.artist} &bull; ${currentTrack.genre}",
                        fontSize = 13.sp,
                        color = NeonViolet
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Progress bar
                    val progressFraction = (progressSec.toFloat() / currentTrack.durationSec.toFloat()).coerceIn(0f, 1f)
                    LinearProgressIndicator(
                        progress = { progressFraction },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = NeonViolet,
                        trackColor = DarkSurface
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(formatSec(progressSec), fontSize = 11.sp, color = Color(0xFF64748B))
                        Text(formatSec(currentTrack.durationSec), fontSize = 11.sp, color = Color(0xFF64748B))
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Controls Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = {
                                val idx = WifiMusicService.availableTracks.indexOf(currentTrack)
                                val prevIdx = if (idx > 0) idx - 1 else WifiMusicService.availableTracks.size - 1
                                musicService.selectTrack(WifiMusicService.availableTracks[prevIdx])
                            },
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(Icons.Default.SkipPrevious, contentDescription = "Previous", tint = Color.White, modifier = Modifier.size(28.dp))
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        IconButton(
                            onClick = { musicService.togglePlayPause() },
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(NeonViolet)
                                .testTag("play_pause_button")
                        ) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (isPlaying) "Pause" else "Play",
                                tint = Color.White,
                                modifier = Modifier.size(36.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        IconButton(
                            onClick = {
                                val idx = WifiMusicService.availableTracks.indexOf(currentTrack)
                                val nextIdx = (idx + 1) % WifiMusicService.availableTracks.size
                                musicService.selectTrack(WifiMusicService.availableTracks[nextIdx])
                            },
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(Icons.Default.SkipNext, contentDescription = "Next", tint = Color.White, modifier = Modifier.size(28.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Volume & Gain booster slider
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.VolumeUp, contentDescription = "Volume", tint = NeonViolet, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Slider(
                            value = volumeGain,
                            onValueChange = { musicService.setVolume(it) },
                            valueRange = 0.2f..2.0f,
                            modifier = Modifier.weight(1f),
                            colors = SliderDefaults.colors(
                                thumbColor = NeonViolet,
                                activeTrackColor = NeonViolet,
                                inactiveTrackColor = DarkSurface
                            )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "${(volumeGain * 100).toInt()}%",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = NeonViolet
                        )
                    }
                }
            }
        }

        // --- Equalizer & Sound Mode Presets ---
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("equalizer_card"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = DarkCard)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Equalizer, contentDescription = null, tint = CyberCyan)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Acoustic Equalizer & Bass Mode", fontWeight = FontWeight.Bold, color = Color.White)
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        val presets = listOf("Bass Boost", "EDM Club", "Vocal Clear", "Treble Crisp", "Flat")
                        items(presets) { preset ->
                            val isSelected = currentEq == preset
                            FilterChip(
                                selected = isSelected,
                                onClick = { musicService.setEqualizerPreset(preset) },
                                label = { Text(preset) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = CyberCyan.copy(alpha = 0.2f),
                                    selectedLabelColor = CyberCyan,
                                    containerColor = DarkSurface,
                                    labelColor = Color(0xFF94A3B8)
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    enabled = true,
                                    selected = isSelected,
                                    borderColor = if (isSelected) CyberCyan else Color(0xFF26334D)
                                )
                            )
                        }
                    }
                }
            }
        }

        // --- Playlist selector ---
        item {
            Text(
                text = "Wi-Fi Music Channels",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )
        }

        items(WifiMusicService.availableTracks) { track ->
            val isCurrent = track.id == currentTrack.id
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { musicService.selectTrack(track) },
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = DarkCard),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (isCurrent) NeonViolet else Color(0xFF1E293B)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(
                                    if (isCurrent) NeonViolet.copy(alpha = 0.2f) else DarkSurface,
                                    RoundedCornerShape(8.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.MusicNote,
                                contentDescription = null,
                                tint = if (isCurrent) NeonViolet else Color(0xFF94A3B8)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column {
                            Text(track.title, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                            Text("${track.artist} &bull; ${track.bpm} BPM", fontSize = 11.sp, color = Color(0xFF94A3B8))
                        }
                    }

                    Text(formatSec(track.durationSec), fontSize = 12.sp, color = Color(0xFF64748B))
                }
            }
        }
    }

    // QR Code Dialog for other devices on the Wi-Fi network to connect
    if (showQrDialog) {
        AlertDialog(
            onDismissRequest = { showQrDialog = false },
            title = {
                Text("Scan to Stream on Local Wi-Fi", color = Color.White, fontWeight = FontWeight.Bold)
            },
            text = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Any phone or laptop connected to your Wi-Fi can scan this to open the live web audio stream:",
                        fontSize = 12.sp,
                        color = Color(0xFF94A3B8),
                        modifier = Modifier.padding(bottom = 14.dp)
                    )

                    QrCodeCanvas(
                        content = serverUrl,
                        size = 180.dp,
                        qrColor = Color(0xFF090D16),
                        backgroundColor = Color.White
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                    Text(serverUrl, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = SignalGreen)
                }
            },
            confirmButton = {
                Button(
                    onClick = { showQrDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = SignalGreen)
                ) {
                    Text("Close", color = Color(0xFF090D16), fontWeight = FontWeight.Bold)
                }
            },
            containerColor = DarkCard,
            shape = RoundedCornerShape(16.dp)
        )
    }
}

@Composable
fun AudioSpectrumCanvas(
    amplitudes: List<Float>,
    isPlaying: Boolean,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val barCount = amplitudes.size
        val barWidth = (size.width / (barCount * 1.5f))
        val spacing = barWidth * 0.5f

        for (i in 0 until barCount) {
            val amp = if (isPlaying) amplitudes[i] else 0.1f
            val barHeight = (size.height * amp).coerceAtLeast(6.dp.toPx())
            val x = i * (barWidth + spacing)
            val y = (size.height - barHeight) / 2f

            drawRoundRect(
                brush = Brush.verticalGradient(
                    colors = listOf(CyberCyan, NeonViolet),
                    startY = y,
                    endY = y + barHeight
                ),
                topLeft = Offset(x, y),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
            )
        }
    }
}

private fun formatSec(seconds: Int): String {
    val m = seconds / 60
    val s = seconds % 60
    return String.format("%d:%02d", m, s)
}
