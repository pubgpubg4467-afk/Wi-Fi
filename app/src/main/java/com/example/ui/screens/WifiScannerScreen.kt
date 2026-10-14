package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Router
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Speed
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ConnectedWifiDetails
import com.example.model.WifiNetworkInfo
import com.example.ui.components.QrCodeCanvas
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.DarkCard
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.NeonViolet
import com.example.ui.theme.SignalGreen
import com.example.ui.theme.WarningAmber
import com.example.viewmodel.NetPulseViewModel

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WifiScannerScreen(
    viewModel: NetPulseViewModel,
    modifier: Modifier = Modifier
) {
    val connectedWifi by viewModel.connectedWifi.collectAsState()
    val scannedNetworks by viewModel.scannedNetworks.collectAsState()
    val isScanning by viewModel.isWifiScanning.collectAsState()
    val bandFilter by viewModel.wifiBandFilter.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()

    var selectedNetworkForDetails by remember { mutableStateOf<WifiNetworkInfo?>(null) }
    var showQrDialog by remember { mutableStateOf(false) }

    val filteredNetworks = remember(scannedNetworks, bandFilter, searchQuery) {
        scannedNetworks.filter { net ->
            val matchesSearch = net.ssid.contains(searchQuery, ignoreCase = true) ||
                    net.bssid.contains(searchQuery, ignoreCase = true)
            val matchesBand = when (bandFilter) {
                "5 GHz" -> net.band == "5 GHz"
                "2.4 GHz" -> net.band == "2.4 GHz"
                "Open" -> net.security == "Open"
                else -> true
            }
            matchesSearch && matchesBand
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // --- Connected Wi-Fi Hero Card ---
        item {
            connectedWifi?.let { details ->
                ConnectedWifiCard(
                    details = details,
                    onShareClick = { showQrDialog = true }
                )
            }
        }

        // --- Controls: Search & Band Filters ---
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { viewModel.setSearchQuery(it) },
                        placeholder = { Text("Search SSID or MAC...", color = Color(0xFF64748B)) },
                        leadingIcon = {
                            Icon(Icons.Default.Search, contentDescription = "Search", tint = CyberCyan)
                        },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("search_wifi_input"),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyberCyan,
                            unfocusedBorderColor = Color(0xFF26334D),
                            focusedContainerColor = DarkSurface,
                            unfocusedContainerColor = DarkSurface,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )

                    Spacer(modifier = Modifier.width(10.dp))

                    IconButton(
                        onClick = { viewModel.refreshWifiData() },
                        modifier = Modifier
                            .size(52.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(DarkSurface)
                            .testTag("refresh_wifi_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh scan",
                            tint = CyberCyan,
                            modifier = if (isScanning) Modifier.rotate(rotation) else Modifier
                        )
                    }
                }

                // Filter chips
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("All", "5 GHz", "2.4 GHz", "Open").forEach { filter ->
                        val isSelected = bandFilter == filter
                        FilterChip(
                            selected = isSelected,
                            onClick = { viewModel.setWifiFilter(filter) },
                            label = { Text(filter) },
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
                            ),
                            modifier = Modifier.testTag("filter_chip_$filter")
                        )
                    }
                }
            }
        }

        // --- Networks Count & Header ---
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Discovered Access Points (${filteredNetworks.size})",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )

                if (isScanning) {
                    Text(
                        text = "Scanning RF Channels...",
                        fontSize = 12.sp,
                        color = CyberCyan
                    )
                }
            }
        }

        // --- Scanned Network Cards ---
        items(filteredNetworks, key = { it.bssid }) { network ->
            WifiNetworkRowCard(
                network = network,
                onClick = { selectedNetworkForDetails = network }
            )
        }
    }

    // Network Details Modal
    selectedNetworkForDetails?.let { net ->
        NetworkDetailsDialog(
            network = net,
            onDismiss = { selectedNetworkForDetails = null }
        )
    }

    // QR Code Share Dialog
    if (showQrDialog && connectedWifi != null) {
        ShareWifiQrDialog(
            details = connectedWifi!!,
            onDismiss = { showQrDialog = false }
        )
    }
}

@Composable
fun ConnectedWifiCard(
    details: ConnectedWifiDetails,
    onShareClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("connected_wifi_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = DarkCard),
        border = androidx.compose.foundation.BorderStroke(1.dp, CyberCyan.copy(alpha = 0.4f))
    ) {
        Column(
            modifier = Modifier
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            CyberCyan.copy(alpha = 0.12f),
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
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(SignalGreen, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "ACTIVE CONNECTION",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = SignalGreen,
                        letterSpacing = 1.sp
                    )
                }

                Box(
                    modifier = Modifier
                        .background(CyberCyan.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = details.standard,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = CyberCyan
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = details.ssid,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "${details.band} &bull; Channel ${details.channel} (${details.frequencyMhz} MHz)",
                        fontSize = 13.sp,
                        color = Color(0xFF94A3B8)
                    )
                }

                IconButton(
                    onClick = onShareClick,
                    modifier = Modifier
                        .background(Color(0xFF1E293B), CircleShape)
                        .testTag("share_wifi_qr_button")
                ) {
                    Icon(
                        Icons.Default.QrCode,
                        contentDescription = "Share Wi-Fi QR",
                        tint = CyberCyan
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Signal bar
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Signal: ${details.rssi} dBm (${details.signalPercent}%)",
                        fontSize = 12.sp,
                        color = CyberCyan,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "Speed: ${details.linkSpeedMbps} Mbps",
                        fontSize = 12.sp,
                        color = ElectricBlue,
                        fontWeight = FontWeight.Medium
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = { details.signalPercent / 100f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = CyberCyan,
                    trackColor = Color(0xFF1E293B)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Mini Specs Grid
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DarkSurface, RoundedCornerShape(12.dp))
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("IP Address", fontSize = 11.sp, color = Color(0xFF64748B))
                    Text(details.ipAddress, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                }
                Column {
                    Text("Gateway", fontSize = 11.sp, color = Color(0xFF64748B))
                    Text(details.gatewayIp, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                }
                Column {
                    Text("Link Speed", fontSize = 11.sp, color = Color(0xFF64748B))
                    Text("${details.linkSpeedMbps} Mbps", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = SignalGreen)
                }
            }
        }
    }
}

@Composable
fun WifiNetworkRowCard(
    network: WifiNetworkInfo,
    onClick: () -> Unit
) {
    val signalColor = when {
        network.signalPercent >= 70 -> SignalGreen
        network.signalPercent >= 40 -> CyberCyan
        else -> WarningAmber
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("wifi_row_${network.bssid}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = DarkCard),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (network.isConnected) CyberCyan else Color(0xFF1E293B)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Signal Level Indicator
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(signalColor.copy(alpha = 0.15f), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.Wifi,
                        contentDescription = "Signal",
                        tint = signalColor,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "${network.signalPercent}%",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = signalColor
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            // Details
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = network.ssid,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    if (network.isConnected) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .background(SignalGreen.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text("Connected", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = SignalGreen)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${network.band} &bull; Ch ${network.channel} &bull; ${network.rssi} dBm",
                        fontSize = 12.sp,
                        color = Color(0xFF94A3B8)
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "~${network.estimatedDistanceMeters}m away",
                        fontSize = 11.sp,
                        color = ElectricBlue
                    )
                    Text(
                        text = "&bull;",
                        fontSize = 10.sp,
                        color = Color(0xFF64748B)
                    )
                    Text(
                        text = network.security,
                        fontSize = 11.sp,
                        color = if (network.security == "Open") WarningAmber else Color(0xFF94A3B8)
                    )
                }
            }

            // Lock / Security Icon
            Icon(
                imageVector = if (network.security == "Open") Icons.Default.LockOpen else Icons.Default.Lock,
                contentDescription = network.security,
                tint = if (network.security == "Open") WarningAmber else Color(0xFF64748B),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
fun NetworkDetailsDialog(
    network: WifiNetworkInfo,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Router, contentDescription = null, tint = CyberCyan)
                Spacer(modifier = Modifier.width(8.dp))
                Text(network.ssid, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                DetailItem("BSSID (MAC)", network.bssid)
                DetailItem("Signal Strength", "${network.rssi} dBm (${network.signalPercent}%)")
                DetailItem("Frequency", "${network.frequencyMhz} MHz (${network.band})")
                DetailItem("Channel Number", "Channel ${network.channel}")
                DetailItem("Estimated Range", "${network.estimatedDistanceMeters} meters (~${(network.estimatedDistanceMeters * 3.28).toInt()} ft)")
                DetailItem("Max Link Speed", "${network.linkSpeedMbps} Mbps")
                DetailItem("Security Protocol", network.security)
                DetailItem("WPS Push Button", if (network.isWpsSupported) "Supported" else "Not Enabled")
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = CyberCyan)
            ) {
                Text("Close", color = Color(0xFF090D16), fontWeight = FontWeight.Bold)
            }
        },
        containerColor = DarkCard,
        shape = RoundedCornerShape(16.dp)
    )
}

@Composable
fun ShareWifiQrDialog(
    details: ConnectedWifiDetails,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Share Wi-Fi Access", color = Color.White, fontWeight = FontWeight.Bold)
        },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Scan with any phone camera to connect directly to ${details.ssid}",
                    fontSize = 13.sp,
                    color = Color(0xFF94A3B8),
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // Render standard Wi-Fi QR string format: WIFI:T:WPA;S:SSID;P:PASSWORD;;
                QrCodeCanvas(
                    content = "WIFI:S:${details.ssid};T:WPA;P:netpulse_secure;;",
                    size = 180.dp,
                    qrColor = Color(0xFF090D16),
                    backgroundColor = Color.White
                )

                Spacer(modifier = Modifier.height(14.dp))
                Text("Network: ${details.ssid}", fontWeight = FontWeight.Bold, color = CyberCyan)
                Text("Security: ${details.standard}", fontSize = 12.sp, color = Color(0xFF64748B))
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = CyberCyan)
            ) {
                Text("Done", color = Color(0xFF090D16), fontWeight = FontWeight.Bold)
            }
        },
        containerColor = DarkCard,
        shape = RoundedCornerShape(16.dp)
    )
}

@Composable
fun DetailItem(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontSize = 12.sp, color = Color(0xFF64748B))
        Text(value, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
    }
}
