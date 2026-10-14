package com.example.ui.screens

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CellTower
import androidx.compose.material.icons.filled.CompassCalibration
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.VolumeMute
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.WifiNetworkInfo
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.DarkCard
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.NeonViolet
import com.example.ui.theme.SignalGreen
import com.example.ui.theme.WarningAmber
import com.example.viewmodel.NetPulseViewModel
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun RangeRadarScreen(
    viewModel: NetPulseViewModel,
    modifier: Modifier = Modifier
) {
    val connectedWifi by viewModel.connectedWifi.collectAsState()
    val scannedNetworks by viewModel.scannedNetworks.collectAsState()
    val channelCongestion by viewModel.channelCongestion.collectAsState()
    val isWalkingActive by viewModel.isRadarWalkingActive.collectAsState()
    val isAudioBeepEnabled by viewModel.isRadarAudioBeepEnabled.collectAsState()
    val walkingHistory by viewModel.walkingRssiHistory.collectAsState()

    val currentRssi = connectedWifi?.rssi ?: -52
    val distanceM = connectedWifi?.let {
        ((10.0 + (it.rssi + 50) * -0.3).coerceIn(1.2, 45.0) * 10).toInt() / 10.0
    } ?: 3.5

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
                    text = "Wi-Fi Extra Range & Radar",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "Signal expansion radar, dead zone mapper & channel booster",
                    fontSize = 13.sp,
                    color = Color(0xFF94A3B8)
                )
            }
        }

        // --- Circular Radar Scope ---
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("radar_scope_card"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = DarkCard),
                border = androidx.compose.foundation.BorderStroke(1.dp, CyberCyan.copy(alpha = 0.3f))
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CompassCalibration, contentDescription = null, tint = CyberCyan)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Spatial RF Radar", fontWeight = FontWeight.Bold, color = Color.White)
                        }

                        Box(
                            modifier = Modifier
                                .background(CyberCyan.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "Range: ~${distanceM}m",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = CyberCyan
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Radar Canvas with animated sweep
                    RadarScopeCanvas(
                        networks = scannedNetworks,
                        modifier = Modifier.size(240.dp)
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "Access points mapped by physical distance & signal attenuation",
                        fontSize = 11.sp,
                        color = Color(0xFF64748B)
                    )
                }
            }
        }

        // --- Walking Signal Tracer (Dead Zone Hunter) ---
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("walking_tracer_card"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = DarkCard),
                border = androidx.compose.foundation.BorderStroke(1.dp, NeonViolet.copy(alpha = 0.3f))
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.DirectionsWalk, contentDescription = null, tint = NeonViolet)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Dead Zone Hunter", fontWeight = FontWeight.Bold, color = Color.White)
                            }
                            Text(
                                text = "Walk around to track signal drops in real time",
                                fontSize = 12.sp,
                                color = Color(0xFF94A3B8)
                            )
                        }

                        IconButton(
                            onClick = { viewModel.toggleRadarAudioBeep() },
                            modifier = Modifier
                                .background(DarkSurface, CircleShape)
                                .testTag("toggle_radar_beep_button")
                        ) {
                            Icon(
                                imageVector = if (isAudioBeepEnabled) Icons.Default.VolumeUp else Icons.Default.VolumeMute,
                                contentDescription = "Audio Geiger Beep",
                                tint = if (isAudioBeepEnabled) NeonViolet else Color(0xFF64748B)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Live Signal Waveform Chart
                    LiveRssiChart(
                        rssiHistory = walkingHistory,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(90.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Current RSSI: $currentRssi dBm",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (currentRssi >= -60) SignalGreen else WarningAmber
                            )
                            val statusText = when {
                                currentRssi >= -55 -> "Ultra Strong (Peak Throughput)"
                                currentRssi >= -70 -> "Good Signal (Stable 4K)"
                                currentRssi >= -82 -> "Moderate (Wall Obstruction)"
                                else -> "Dead Zone Detected!"
                            }
                            Text(text = statusText, fontSize = 11.sp, color = Color(0xFF94A3B8))
                        }

                        Button(
                            onClick = { viewModel.toggleWalkingRadar() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isWalkingActive) Color(0xFFEF4444) else NeonViolet
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("toggle_walking_radar_button")
                        ) {
                            Icon(
                                imageVector = if (isWalkingActive) Icons.Default.Stop else Icons.Default.PlayArrow,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (isWalkingActive) "Stop" else "Start Walk")
                        }
                    }
                }
            }
        }

        // --- Channel Congestion & Interference Analyzer ---
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("channel_congestion_card"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = DarkCard)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CellTower, contentDescription = null, tint = CyberCyan)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Channel Interference Rating", fontWeight = FontWeight.Bold, color = Color.White)
                        }

                        Box(
                            modifier = Modifier
                                .background(SignalGreen.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "Optimal: Ch 1 & 36",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = SignalGreen
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "2.4 GHz Spectrum (Wall-Piercing Channels)",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF94A3B8)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // 2.4 GHz spectrum channels
                    channelCongestion.filter { it.band == "2.4 GHz" }.take(6).forEach { ch ->
                        ChannelBarRow(ch.channel, ch.count, ch.qualityScore, ch.status)
                        Spacer(modifier = Modifier.height(6.dp))
                    }
                }
            }
        }

        // --- Range Booster Optimizer Tips ---
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("range_booster_tips_card"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = DarkCard),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E293B))
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Lightbulb, contentDescription = null, tint = WarningAmber)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Wi-Fi Range Booster Recommendations", fontWeight = FontWeight.Bold, color = Color.White)
                    }

                    BoosterTipItem(
                        title = "Use 2.4 GHz For Long Distance & Thick Walls",
                        desc = "2.4 GHz signals travel through solid concrete & brick walls much better than 5 GHz. Switch to 2.4 GHz when far from the router."
                    )

                    BoosterTipItem(
                        title = "Avoid Crowded Channels (Choose 1, 6, or 11)",
                        desc = "Channels 1, 6, and 11 are non-overlapping in 2.4 GHz. Switching router to Channel 1 reduces packet drop by up to 35%."
                    )

                    BoosterTipItem(
                        title = "Elevate Router Position",
                        desc = "Keep the router at least 1.5m (5 ft) above the floor. Avoid placing it behind metal TVs, mirrors, or near microwaves."
                    )
                }
            }
        }
    }
}

@Composable
fun RadarScopeCanvas(
    networks: List<WifiNetworkInfo>,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "radar_sweep")
    val sweepAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "sweep"
    )

    Canvas(modifier = modifier) {
        val center = Offset(size.width / 2, size.height / 2)
        val maxRadius = size.width / 2

        // Background dark radar fill
        drawCircle(
            color = Color(0xFF0D1525),
            radius = maxRadius,
            center = center
        )

        // Concentric distance rings
        val rings = listOf(0.33f, 0.66f, 1.0f)
        rings.forEach { frac ->
            drawCircle(
                color = CyberCyan.copy(alpha = 0.25f),
                radius = maxRadius * frac,
                center = center,
                style = Stroke(width = 1.dp.toPx())
            )
        }

        // Crosshairs
        drawLine(
            color = CyberCyan.copy(alpha = 0.2f),
            start = Offset(center.x, 0f),
            end = Offset(center.x, size.height),
            strokeWidth = 1.dp.toPx()
        )
        drawLine(
            color = CyberCyan.copy(alpha = 0.2f),
            start = Offset(0f, center.y),
            end = Offset(size.width, center.y),
            strokeWidth = 1.dp.toPx()
        )

        // Rotating sweep line
        val sweepRad = Math.toRadians(sweepAngle.toDouble())
        val sweepEnd = Offset(
            (center.x + maxRadius * cos(sweepRad)).toFloat(),
            (center.y + maxRadius * sin(sweepRad)).toFloat()
        )
        drawLine(
            color = CyberCyan,
            start = center,
            end = sweepEnd,
            strokeWidth = 2.dp.toPx()
        )

        // Plot access point blips
        networks.take(8).forEachIndexed { index, net ->
            val angleRad = Math.toRadians((index * 45.0 + 20.0) % 360.0)
            val distFactor = (net.estimatedDistanceMeters / 35.0).coerceIn(0.2, 0.95)
            val blipPos = Offset(
                (center.x + maxRadius * distFactor * cos(angleRad)).toFloat(),
                (center.y + maxRadius * distFactor * sin(angleRad)).toFloat()
            )

            val blipColor = if (net.isConnected) SignalGreen else if (net.band == "5 GHz") CyberCyan else NeonViolet

            // Blip glow
            drawCircle(
                color = blipColor.copy(alpha = 0.4f),
                radius = 7.dp.toPx(),
                center = blipPos
            )
            // Blip center
            drawCircle(
                color = blipColor,
                radius = 4.dp.toPx(),
                center = blipPos
            )
        }

        // User center indicator (Mobile device)
        drawCircle(
            color = Color.White,
            radius = 5.dp.toPx(),
            center = center
        )
    }
}

@Composable
fun LiveRssiChart(
    rssiHistory: List<Int>,
    modifier: Modifier = Modifier
) {
    Canvas(
        modifier = modifier
            .background(DarkSurface, RoundedCornerShape(12.dp))
            .padding(8.dp)
    ) {
        if (rssiHistory.isEmpty()) return@Canvas

        val w = size.width
        val h = size.height
        val minDbm = -95f
        val maxDbm = -35f
        val range = maxDbm - minDbm

        val stepX = w / (rssiHistory.size - 1).coerceAtLeast(1)

        val path = Path()
        rssiHistory.forEachIndexed { i, rssi ->
            val normY = 1f - ((rssi.toFloat() - minDbm) / range).coerceIn(0f, 1f)
            val x = i * stepX
            val y = normY * h
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }

        // Draw line graph
        drawPath(
            path = path,
            color = NeonViolet,
            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
        )

        // Highlight latest point
        val latestRssi = rssiHistory.last()
        val latestNormY = 1f - ((latestRssi.toFloat() - minDbm) / range).coerceIn(0f, 1f)
        val latestX = (rssiHistory.size - 1) * stepX
        val latestY = latestNormY * h

        drawCircle(
            color = CyberCyan,
            radius = 5.dp.toPx(),
            center = Offset(latestX, latestY)
        )
    }
}

@Composable
fun ChannelBarRow(channel: Int, count: Int, qualityScore: Int, status: String) {
    val barColor = when {
        qualityScore >= 75 -> SignalGreen
        qualityScore >= 50 -> WarningAmber
        else -> Color(0xFFEF4444)
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Ch $channel",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier.width(44.dp)
        )

        LinearProgressIndicator(
            progress = { qualityScore / 100f },
            modifier = Modifier
                .weight(1f)
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp)),
            color = barColor,
            trackColor = DarkSurface
        )

        Spacer(modifier = Modifier.width(10.dp))

        Text(
            text = "$status (${count} APs)",
            fontSize = 11.sp,
            color = barColor,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.width(110.dp)
        )
    }
}

@Composable
fun BoosterTipItem(title: String, desc: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(DarkSurface, RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        Text(title, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = CyberCyan)
        Spacer(modifier = Modifier.height(4.dp))
        Text(desc, fontSize = 12.sp, color = Color(0xFF94A3B8), lineHeight = 16.sp)
    }
}
