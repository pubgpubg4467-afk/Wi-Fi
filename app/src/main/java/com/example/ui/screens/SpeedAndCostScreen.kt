package com.example.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.SpeedTestResult
import com.example.model.WifiCostConfig
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
fun SpeedAndCostScreen(
    viewModel: NetPulseViewModel,
    modifier: Modifier = Modifier
) {
    val speedResult by viewModel.speedTestResult.collectAsState()
    val speedHistory by viewModel.speedHistory.collectAsState()
    val costConfig by viewModel.costConfig.collectAsState()

    var showEditCostDialog by remember { mutableStateOf(false) }

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
                    text = "Speed & Cost Monitor",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "Live throughput benchmark & monthly Wi-Fi expense calculator",
                    fontSize = 13.sp,
                    color = Color(0xFF94A3B8)
                )
            }
        }

        // --- Speedometer Gauge Card ---
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("speedometer_card"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = DarkCard),
                border = androidx.compose.foundation.BorderStroke(1.dp, CyberCyan.copy(alpha = 0.35f))
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Speed, contentDescription = null, tint = CyberCyan)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Real Speedometer", fontWeight = FontWeight.Bold, color = Color.White)
                        }

                        if (speedResult.connectionGrade.isNotEmpty()) {
                            Box(
                                modifier = Modifier
                                    .background(SignalGreen.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = speedResult.connectionGrade,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = SignalGreen
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Animated Speed Gauge
                    SpeedometerGauge(
                        speedMbps = speedResult.downloadSpeedMbps,
                        maxSpeed = 200.0,
                        modifier = Modifier.size(240.dp, 160.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Phase description & Progress bar
                    if (speedResult.isTesting) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = speedResult.testPhase,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = CyberCyan
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            LinearProgressIndicator(
                                progress = { speedResult.progress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp)),
                                color = CyberCyan,
                                trackColor = DarkSurface
                            )
                        }
                    } else {
                        Text(
                            text = if (speedResult.downloadSpeedMbps > 0) "Benchmark complete" else "Ready to test latency & throughput",
                            fontSize = 13.sp,
                            color = Color(0xFF94A3B8)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Metrics Row: Ping, Jitter, Upload
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(DarkSurface, RoundedCornerShape(14.dp))
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        MetricItem("Ping", "${speedResult.pingMs} ms", CyberCyan)
                        MetricItem("Jitter", "${speedResult.jitterMs} ms", ElectricBlue)
                        MetricItem("Upload", "${speedResult.uploadSpeedMbps} Mb/s", NeonViolet)
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = { viewModel.startSpeedTest() },
                        enabled = !speedResult.isTesting,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CyberCyan,
                            disabledContainerColor = CyberCyan.copy(alpha = 0.4f)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("start_speed_test_button")
                    ) {
                        Icon(
                            Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = Color(0xFF090D16)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (speedResult.isTesting) "Testing Network..." else "Run Speed Test",
                            color = Color(0xFF090D16),
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                }
            }
        }

        // --- Wi-Fi Cost & Data Usage Card ---
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("wifi_cost_card"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = DarkCard),
                border = androidx.compose.foundation.BorderStroke(1.dp, SignalGreen.copy(alpha = 0.35f))
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AttachMoney, contentDescription = null, tint = SignalGreen)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Wi-Fi Plan & Cost Tracker", fontWeight = FontWeight.Bold, color = Color.White)
                        }

                        IconButton(
                            onClick = { showEditCostDialog = true },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit Plan", tint = SignalGreen, modifier = Modifier.size(18.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Cost & Usage Summary Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Monthly Plan Cost", fontSize = 12.sp, color = Color(0xFF94A3B8))
                            Text(
                                "${costConfig.currencySymbol}${String.format("%.2f", costConfig.monthlyPlanCost)}/mo",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text("Cost Per GB Consumed", fontSize = 12.sp, color = Color(0xFF94A3B8))
                            Text(
                                "${costConfig.currencySymbol}${String.format("%.3f", costConfig.costPerGbUsed)} / GB",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = SignalGreen
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Data Usage Progress Bar
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Used: ${costConfig.dataUsedGb} GB",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = CyberCyan
                            )
                            Text(
                                text = if (costConfig.isUnlimited) "Unlimited Plan" else "Limit: ${costConfig.monthlyLimitGb} GB",
                                fontSize = 12.sp,
                                color = Color(0xFF94A3B8)
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        LinearProgressIndicator(
                            progress = { costConfig.usagePercent },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = if (costConfig.usagePercent > 0.85f) WarningAmber else SignalGreen,
                            trackColor = DarkSurface
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Projection stats grid
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(DarkSurface, RoundedCornerShape(12.dp))
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Days Remaining", fontSize = 11.sp, color = Color(0xFF64748B))
                            Text("${costConfig.daysRemainingInCycle} days", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                        Column {
                            Text("Remaining Cap", fontSize = 11.sp, color = Color(0xFF64748B))
                            Text(
                                if (costConfig.isUnlimited) "Unlimited" else "${String.format("%.1f", costConfig.remainingGb)} GB",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = SignalGreen
                            )
                        }
                        Column {
                            Text("Projected Finish", fontSize = 11.sp, color = Color(0xFF64748B))
                            Text("${String.format("%.1f", costConfig.projectedUsageGb)} GB", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = ElectricBlue)
                        }
                    }
                }
            }
        }

        // --- Recent Tests History ---
        if (speedHistory.isNotEmpty()) {
            item {
                Text(
                    text = "Recent Speed Tests",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
            }

            items(speedHistory) { test ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkCard)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                "${test.downloadSpeedMbps} Mbps Download",
                                fontWeight = FontWeight.Bold,
                                color = CyberCyan,
                                fontSize = 14.sp
                            )
                            Text(
                                "Ping: ${test.pingMs} ms &bull; Upload: ${test.uploadSpeedMbps} Mbps",
                                fontSize = 11.sp,
                                color = Color(0xFF94A3B8)
                            )
                        }

                        Box(
                            modifier = Modifier
                                .background(SignalGreen.copy(alpha = 0.2f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(test.connectionGrade.take(2), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = SignalGreen)
                        }
                    }
                }
            }
        }
    }

    // Edit Cost & Plan Dialog
    if (showEditCostDialog) {
        EditWifiCostDialog(
            config = costConfig,
            onDismiss = { showEditCostDialog = false },
            onSave = { cost, currency, limit, used, unlimited ->
                viewModel.updateCostConfig(cost, currency, limit, used, unlimited)
                showEditCostDialog = false
            }
        )
    }
}

@Composable
fun SpeedometerGauge(
    speedMbps: Double,
    maxSpeed: Double,
    modifier: Modifier = Modifier
) {
    val animatedSpeed by animateFloatAsState(
        targetValue = speedMbps.toFloat(),
        animationSpec = tween(400),
        label = "gauge_needle"
    )

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height * 0.85f)
            val radius = size.width * 0.42f
            val strokeWidth = 14.dp.toPx()

            val startAngle = 180f
            val sweepTotal = 180f

            // Background arc
            drawArc(
                color = Color(0xFF1E293B),
                startAngle = startAngle,
                sweepAngle = sweepTotal,
                useCenter = false,
                topLeft = Offset(center.x - radius, center.y - radius),
                size = Size(radius * 2, radius * 2),
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )

            // Active glowing gradient arc
            val fraction = (animatedSpeed / maxSpeed.toFloat()).coerceIn(0f, 1f)
            val activeSweep = sweepTotal * fraction
            if (activeSweep > 0) {
                drawArc(
                    brush = Brush.horizontalGradient(
                        colors = listOf(CyberCyan, ElectricBlue, NeonViolet)
                    ),
                    startAngle = startAngle,
                    sweepAngle = activeSweep,
                    useCenter = false,
                    topLeft = Offset(center.x - radius, center.y - radius),
                    size = Size(radius * 2, radius * 2),
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
            }

            // Needle angle
            val needleAngleDeg = startAngle + (sweepTotal * fraction)
            val needleRad = Math.toRadians(needleAngleDeg.toDouble())
            val needleLen = radius * 0.82f
            val needleEnd = Offset(
                (center.x + needleLen * cos(needleRad)).toFloat(),
                (center.y + needleLen * sin(needleRad)).toFloat()
            )

            // Draw needle line
            drawLine(
                color = CyberCyan,
                start = center,
                end = needleEnd,
                strokeWidth = 3.dp.toPx(),
                cap = StrokeCap.Round
            )

            // Center pivot
            drawCircle(
                color = Color.White,
                radius = 7.dp.toPx(),
                center = center
            )
            drawCircle(
                color = Color(0xFF090D16),
                radius = 3.dp.toPx(),
                center = center
            )
        }

        // Digital display
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = String.format("%.1f", animatedSpeed),
                fontSize = 36.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White
            )
            Text(
                text = "Mbps Download",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = CyberCyan
            )
        }
    }
}

@Composable
fun MetricItem(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, fontSize = 11.sp, color = Color(0xFF94A3B8))
        Spacer(modifier = Modifier.height(2.dp))
        Text(value, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = color)
    }
}

@Composable
fun EditWifiCostDialog(
    config: WifiCostConfig,
    onDismiss: () -> Unit,
    onSave: (cost: Double, currency: String, limit: Double, used: Double, unlimited: Boolean) -> Unit
) {
    var costText by remember { mutableStateOf(config.monthlyPlanCost.toString()) }
    var currencyText by remember { mutableStateOf(config.currencySymbol) }
    var limitText by remember { mutableStateOf(config.monthlyLimitGb.toString()) }
    var usedText by remember { mutableStateOf(config.dataUsedGb.toString()) }
    var isUnlimited by remember { mutableStateOf(config.isUnlimited) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Configure Wi-Fi Plan & Cost", color = Color.White, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = costText,
                    onValueChange = { costText = it },
                    label = { Text("Monthly Bill Cost") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = currencyText,
                    onValueChange = { currencyText = it },
                    label = { Text("Currency Symbol ($ / € / £ / ₹)") },
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Unlimited Data Plan", color = Color.White)
                    Switch(
                        checked = isUnlimited,
                        onCheckedChange = { isUnlimited = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = SignalGreen)
                    )
                }

                if (!isUnlimited) {
                    OutlinedTextField(
                        value = limitText,
                        onValueChange = { limitText = it },
                        label = { Text("Monthly Data Cap (GB)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                OutlinedTextField(
                    value = usedText,
                    onValueChange = { usedText = it },
                    label = { Text("Current Data Used (GB)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val cost = costText.toDoubleOrNull() ?: 49.99
                    val limit = limitText.toDoubleOrNull() ?: 500.0
                    val used = usedText.toDoubleOrNull() ?: 180.0
                    onSave(cost, currencyText, limit, used, isUnlimited)
                },
                colors = ButtonDefaults.buttonColors(containerColor = SignalGreen)
            ) {
                Text("Save Plan", color = Color(0xFF090D16), fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color(0xFF94A3B8))
            }
        },
        containerColor = DarkCard,
        shape = RoundedCornerShape(16.dp)
    )
}
