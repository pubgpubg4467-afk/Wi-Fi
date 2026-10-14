package com.example.ui.screens

import android.content.Intent
import android.provider.Settings
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BluetoothAudio
import androidx.compose.material.icons.filled.BluetoothConnected
import androidx.compose.material.icons.filled.BluetoothSearching
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speaker
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Watch
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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AudioStreamType
import com.example.model.BluetoothDeviceInfo
import com.example.model.BluetoothScenario
import com.example.model.DeviceType
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.DarkCard
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.NeonViolet
import com.example.ui.theme.SignalGreen
import com.example.ui.theme.WarningAmber
import com.example.viewmodel.NetPulseViewModel

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BluetoothHubScreen(
    viewModel: NetPulseViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val bluetoothHelper = viewModel.bluetoothHelper
    val isBtEnabled by bluetoothHelper.isBluetoothEnabled.collectAsState()
    val isScanning by bluetoothHelper.isScanning.collectAsState()
    val pairedDevices by bluetoothHelper.pairedDevices.collectAsState()
    val discoveredDevices by bluetoothHelper.discoveredDevices.collectAsState()
    val routingMatrix by bluetoothHelper.routingMatrix.collectAsState()
    val activeScenario by bluetoothHelper.activeScenario.collectAsState()

    var deviceFilter by remember { mutableStateOf("All") }
    var selectedDeviceForConfig by remember { mutableStateOf<BluetoothDeviceInfo?>(null) }
    var showRenameDialogFor by remember { mutableStateOf<BluetoothDeviceInfo?>(null) }

    val connectedCount = pairedDevices.count { it.isConnected }

    val filteredPairedDevices = remember(pairedDevices, deviceFilter) {
        when (deviceFilter) {
            "Connected" -> pairedDevices.filter { it.isConnected }
            "Audio Gear" -> pairedDevices.filter { it.type == DeviceType.HEADPHONES || it.type == DeviceType.SPEAKER || it.type == DeviceType.CAR_AUDIO }
            "Wearables" -> pairedDevices.filter { it.type == DeviceType.SMARTWATCH }
            else -> pairedDevices
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "bt_spin")
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
        // --- Header Title ---
        item {
            Column {
                Text(
                    text = "Centralized Bluetooth System",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "Manage all paired accessories, dual audio streams & output routing",
                    fontSize = 13.sp,
                    color = Color(0xFF94A3B8)
                )
            }
        }

        // --- Command Center Status & Quick Actions ---
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("bluetooth_command_center"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = DarkCard),
                border = androidx.compose.foundation.BorderStroke(1.dp, CyberCyan.copy(alpha = 0.4f))
            ) {
                Column(
                    modifier = Modifier
                        .background(
                            Brush.verticalGradient(
                                listOf(
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
                                    .size(40.dp)
                                    .background(CyberCyan.copy(alpha = 0.2f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.BluetoothConnected, contentDescription = null, tint = CyberCyan)
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("Bluetooth Command Hub", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 16.sp)
                                Text(
                                    text = "$connectedCount of ${pairedDevices.size} Devices Connected to 1 Mobile",
                                    fontSize = 12.sp,
                                    color = if (connectedCount > 0) SignalGreen else Color(0xFF94A3B8)
                                )
                            }
                        }

                        IconButton(
                            onClick = {
                                try {
                                    val intent = Intent(Settings.ACTION_BLUETOOTH_SETTINGS)
                                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    context.startActivity(intent)
                                } catch (_: Exception) {}
                            },
                            modifier = Modifier
                                .background(DarkSurface, CircleShape)
                                .size(36.dp)
                                .testTag("open_system_bluetooth_settings")
                        ) {
                            Icon(Icons.Default.OpenInNew, contentDescription = "System Settings", tint = CyberCyan, modifier = Modifier.size(18.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Connect All / Disconnect All / Refresh Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { viewModel.connectAllBluetooth() },
                            colors = ButtonDefaults.buttonColors(containerColor = CyberCyan),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .testTag("connect_all_bluetooth_button")
                        ) {
                            Icon(Icons.Default.Link, contentDescription = null, tint = Color(0xFF090D16), modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Connect All", color = Color(0xFF090D16), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }

                        OutlinedButton(
                            onClick = { viewModel.disconnectAllBluetooth() },
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155)),
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .testTag("disconnect_all_bluetooth_button")
                        ) {
                            Icon(Icons.Default.LinkOff, contentDescription = null, tint = Color(0xFF94A3B8), modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Disconnect All", color = Color(0xFF94A3B8), fontSize = 13.sp)
                        }
                    }
                }
            }
        }

        // --- Interactive Audio Routing Matrix Card ---
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("audio_routing_matrix_card"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = DarkCard),
                border = androidx.compose.foundation.BorderStroke(1.dp, NeonViolet.copy(alpha = 0.4f))
            ) {
                Column(
                    modifier = Modifier
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    NeonViolet.copy(alpha = 0.10f),
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
                            Icon(Icons.Default.GraphicEq, contentDescription = null, tint = NeonViolet)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Audio Routing Matrix", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 16.sp)
                        }

                        Box(
                            modifier = Modifier
                                .background(NeonViolet.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = if (routingMatrix.isDualAudioActive) "Dual Audio Streaming" else "Single Output",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = NeonViolet
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Media Audio Routing Selector
                    val mediaDeviceName = pairedDevices.firstOrNull { it.address == routingMatrix.mediaDeviceAddress }?.name ?: "Phone Built-in Speaker"
                    AudioStreamRow(
                        streamTitle = "Media Audio (Music & Video)",
                        icon = Icons.Default.MusicNote,
                        iconTint = CyberCyan,
                        currentDeviceName = mediaDeviceName,
                        devices = pairedDevices,
                        selectedAddress = routingMatrix.mediaDeviceAddress,
                        onDeviceSelected = { viewModel.routeAudioStream(AudioStreamType.MEDIA, it) }
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Call Audio Routing Selector
                    val callDeviceName = pairedDevices.firstOrNull { it.address == routingMatrix.callDeviceAddress }?.name ?: "Phone Earpiece"
                    AudioStreamRow(
                        streamTitle = "Calls & Microphone",
                        icon = Icons.Default.Call,
                        iconTint = SignalGreen,
                        currentDeviceName = callDeviceName,
                        devices = pairedDevices,
                        selectedAddress = routingMatrix.callDeviceAddress,
                        onDeviceSelected = { viewModel.routeAudioStream(AudioStreamType.CALLS, it) }
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Notifications Routing Selector
                    val notifDeviceName = if (routingMatrix.notificationDeviceAddress == "PHONE_BUILTIN") "Phone Speaker"
                    else (pairedDevices.firstOrNull { it.address == routingMatrix.notificationDeviceAddress }?.name ?: "Phone Speaker")
                    AudioStreamRow(
                        streamTitle = "Notifications & Alarms",
                        icon = Icons.Default.Notifications,
                        iconTint = WarningAmber,
                        currentDeviceName = notifDeviceName,
                        devices = pairedDevices,
                        selectedAddress = routingMatrix.notificationDeviceAddress,
                        onDeviceSelected = { viewModel.routeAudioStream(AudioStreamType.NOTIFICATIONS, it) }
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Dual Audio Section
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(DarkSurface, RoundedCornerShape(12.dp))
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Dual Audio Multi-Stream", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp)
                            Text("Broadcast audio synchronously to two Bluetooth speakers or headphones", fontSize = 11.sp, color = Color(0xFF94A3B8))
                        }

                        Switch(
                            checked = routingMatrix.isDualAudioActive,
                            onCheckedChange = { viewModel.toggleDualAudio() },
                            colors = SwitchDefaults.colors(checkedThumbColor = NeonViolet, checkedTrackColor = NeonViolet.copy(alpha = 0.3f)),
                            modifier = Modifier.testTag("dual_audio_toggle_switch")
                        )
                    }

                    if (routingMatrix.isDualAudioActive) {
                        Spacer(modifier = Modifier.height(12.dp))

                        // Dual Volume Sliders
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(DarkSurface, RoundedCornerShape(12.dp))
                                .padding(12.dp)
                        ) {
                            Text("Independent Dual Output Volume", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Primary (${(routingMatrix.masterVolume * 100).toInt()}%)",
                                    fontSize = 11.sp,
                                    color = CyberCyan,
                                    modifier = Modifier.width(90.dp)
                                )
                                Slider(
                                    value = routingMatrix.masterVolume,
                                    onValueChange = { viewModel.setMatrixVolumes(it, routingMatrix.secondaryVolume) },
                                    modifier = Modifier.weight(1f),
                                    colors = SliderDefaults.colors(thumbColor = CyberCyan, activeTrackColor = CyberCyan)
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Secondary (${(routingMatrix.secondaryVolume * 100).toInt()}%)",
                                    fontSize = 11.sp,
                                    color = NeonViolet,
                                    modifier = Modifier.width(90.dp)
                                )
                                Slider(
                                    value = routingMatrix.secondaryVolume,
                                    onValueChange = { viewModel.setMatrixVolumes(routingMatrix.masterVolume, it) },
                                    modifier = Modifier.weight(1f),
                                    colors = SliderDefaults.colors(thumbColor = NeonViolet, activeTrackColor = NeonViolet)
                                )
                            }
                        }
                    }
                }
            }
        }

        // --- Smart Scenario Profiles ---
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("bluetooth_scenarios_card"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = DarkCard)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "Smart Connection Automations",
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 15.sp
                    )
                    Text(
                        text = "One-tap scenario optimization for multi-device routing",
                        fontSize = 12.sp,
                        color = Color(0xFF94A3B8)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(BluetoothScenario.entries.toTypedArray()) { scenario ->
                            val isSelected = activeScenario == scenario
                            FilterChip(
                                selected = isSelected,
                                onClick = { viewModel.applyBluetoothScenario(scenario) },
                                label = { Text(scenario.title) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = when (scenario) {
                                            BluetoothScenario.DRIVING -> Icons.Default.DirectionsCar
                                            BluetoothScenario.WORKOUT -> Icons.Default.DirectionsRun
                                            BluetoothScenario.PARTY -> Icons.Default.Speaker
                                            BluetoothScenario.FOCUS -> Icons.Default.Headphones
                                            else -> Icons.Default.Tune
                                        },
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = CyberCyan.copy(alpha = 0.2f),
                                    selectedLabelColor = CyberCyan,
                                    selectedLeadingIconColor = CyberCyan,
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

        // --- Paired Devices Section & Filter Chips ---
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Paired Devices (${filteredPairedDevices.size})",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )

                    Text(
                        text = "Tap gear to configure",
                        fontSize = 12.sp,
                        color = Color(0xFF64748B)
                    )
                }

                // Filter chips
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("All", "Connected", "Audio Gear", "Wearables").forEach { filter ->
                        val isSelected = deviceFilter == filter
                        FilterChip(
                            selected = isSelected,
                            onClick = { deviceFilter = filter },
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
                            )
                        )
                    }
                }
            }
        }

        // --- Paired Devices Rows ---
        items(filteredPairedDevices, key = { it.address }) { device ->
            val isMediaActive = routingMatrix.mediaDeviceAddress == device.address
            val isCallActive = routingMatrix.callDeviceAddress == device.address
            val isSecondaryActive = routingMatrix.isDualAudioActive && routingMatrix.secondaryMediaDeviceAddress == device.address

            EnhancedPairedDeviceCard(
                device = device,
                isMediaActive = isMediaActive,
                isCallActive = isCallActive,
                isSecondaryActive = isSecondaryActive,
                onToggleConnection = { viewModel.toggleBluetoothConnection(device.address) },
                onConfigureClick = { selectedDeviceForConfig = device },
                onToggleFavorite = { viewModel.toggleFavorite(device.address) },
                onQuickMediaRoute = { viewModel.routeAudioStream(AudioStreamType.MEDIA, device.address) }
            )
        }

        // --- Nearby Discovery / Scanner Section ---
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("bluetooth_scanner_card"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = DarkCard)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Discover Nearby Bluetooth", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 15.sp)
                            Text("Scan for unbonded headphones, speakers & BLE devices", fontSize = 12.sp, color = Color(0xFF94A3B8))
                        }

                        IconButton(
                            onClick = { viewModel.startBluetoothScan() },
                            modifier = Modifier
                                .size(44.dp)
                                .background(DarkSurface, CircleShape)
                                .testTag("scan_nearby_bluetooth_button")
                        ) {
                            Icon(
                                imageVector = if (isScanning) Icons.Default.Refresh else Icons.Default.BluetoothSearching,
                                contentDescription = "Scan",
                                tint = NeonViolet,
                                modifier = if (isScanning) Modifier.rotate(rotation) else Modifier
                            )
                        }
                    }

                    if (discoveredDevices.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = "Found ${discoveredDevices.size} Accessories Nearby",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = NeonViolet
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        discoveredDevices.forEach { device ->
                            DiscoveredDeviceItem(
                                device = device,
                                onPairClick = { viewModel.pairBluetoothDevice(device) }
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }
            }
        }
    }

    // Modal: Device Configuration Sheet
    selectedDeviceForConfig?.let { dev ->
        DeviceConfigDialog(
            device = dev,
            onDismiss = { selectedDeviceForConfig = null },
            onToggleMedia = { viewModel.toggleDeviceMedia(dev.address) },
            onToggleCall = { viewModel.toggleDeviceCall(dev.address) },
            onSetVolume = { vol -> viewModel.setDeviceVolume(dev.address, vol) },
            onSetCodec = { codec, lat -> viewModel.setDeviceCodec(dev.address, codec, lat) },
            onRename = { showRenameDialogFor = dev },
            onUnpair = {
                viewModel.unpairDevice(dev.address)
                selectedDeviceForConfig = null
            }
        )
    }

    // Modal: Rename Device Dialog
    showRenameDialogFor?.let { dev ->
        var renameText by remember { mutableStateOf(dev.name) }
        AlertDialog(
            onDismissRequest = { showRenameDialogFor = null },
            title = { Text("Rename Bluetooth Device", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = renameText,
                    onValueChange = { renameText = it },
                    label = { Text("Device Alias / Custom Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.renameDevice(dev.address, renameText.trim())
                        showRenameDialogFor = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CyberCyan)
                ) {
                    Text("Save", color = Color(0xFF090D16), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialogFor = null }) {
                    Text("Cancel", color = Color(0xFF94A3B8))
                }
            },
            containerColor = DarkCard,
            shape = RoundedCornerShape(16.dp)
        )
    }
}

@Composable
fun AudioStreamRow(
    streamTitle: String,
    icon: ImageVector,
    iconTint: Color,
    currentDeviceName: String,
    devices: List<BluetoothDeviceInfo>,
    selectedAddress: String,
    onDeviceSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(DarkSurface, RoundedCornerShape(12.dp))
            .clickable { expanded = !expanded }
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(streamTitle, fontSize = 11.sp, color = Color(0xFF94A3B8))
                    Text(currentDeviceName, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }

            Box(
                modifier = Modifier
                    .background(iconTint.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = if (expanded) "Close" else "Route To",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = iconTint
                )
            }
        }

        // Expanded device routing options
        AnimatedVisibility(visible = expanded) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Phone Speaker Option
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            if (selectedAddress == "PHONE_BUILTIN") iconTint.copy(alpha = 0.2f) else DarkCard,
                            RoundedCornerShape(8.dp)
                        )
                        .clickable {
                            onDeviceSelected("PHONE_BUILTIN")
                            expanded = false
                        }
                        .padding(10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.PhoneAndroid, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("This Mobile Phone Speaker", color = Color.White, fontSize = 13.sp)
                    }
                    if (selectedAddress == "PHONE_BUILTIN") {
                        Icon(Icons.Default.Check, contentDescription = null, tint = iconTint, modifier = Modifier.size(16.dp))
                    }
                }

                // Connected Paired Bluetooth Devices
                devices.filter { it.isConnected }.forEach { dev ->
                    val isRouted = selectedAddress == dev.address
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                if (isRouted) iconTint.copy(alpha = 0.2f) else DarkCard,
                                RoundedCornerShape(8.dp)
                            )
                            .clickable {
                                onDeviceSelected(dev.address)
                                expanded = false
                            }
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(getDeviceIcon(dev.type), contentDescription = null, tint = if (isRouted) iconTint else Color(0xFF94A3B8), modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(dev.name, color = Color.White, fontSize = 13.sp, fontWeight = if (isRouted) FontWeight.Bold else FontWeight.Normal)
                        }
                        if (isRouted) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = iconTint, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EnhancedPairedDeviceCard(
    device: BluetoothDeviceInfo,
    isMediaActive: Boolean,
    isCallActive: Boolean,
    isSecondaryActive: Boolean,
    onToggleConnection: () -> Unit,
    onConfigureClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    onQuickMediaRoute: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onQuickMediaRoute)
            .testTag("device_card_${device.address}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkCard),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isMediaActive || isSecondaryActive) CyberCyan else Color(0xFF1E293B)
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Device Icon
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .background(
                            (if (device.isConnected) CyberCyan else Color(0xFF64748B)).copy(alpha = 0.15f),
                            RoundedCornerShape(12.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = getDeviceIcon(device.type),
                        contentDescription = null,
                        tint = if (device.isConnected) CyberCyan else Color(0xFF94A3B8),
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Title & Subtitle
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = device.name,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        IconButton(
                            onClick = onToggleFavorite,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = if (device.isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                                contentDescription = "Favorite",
                                tint = if (device.isFavorite) WarningAmber else Color(0xFF64748B),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = if (device.isConnected) "Connected" else "Paired",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (device.isConnected) SignalGreen else Color(0xFF64748B)
                        )

                        if (device.batteryPercent >= 0) {
                            Text("&bull;", fontSize = 10.sp, color = Color(0xFF64748B))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.BatteryFull, contentDescription = null, tint = SignalGreen, modifier = Modifier.size(12.dp))
                                Spacer(modifier = Modifier.width(2.dp))
                                Text("${device.batteryPercent}%", fontSize = 11.sp, color = SignalGreen)
                            }
                        }

                        Text("&bull;", fontSize = 10.sp, color = Color(0xFF64748B))
                        Text("${device.latencyMs}ms", fontSize = 11.sp, color = ElectricBlue)
                    }
                }

                // Connect / Disconnect Toggle Button
                Button(
                    onClick = onToggleConnection,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (device.isConnected) Color(0xFF1E293B) else CyberCyan
                    ),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.height(34.dp)
                ) {
                    Text(
                        text = if (device.isConnected) "Disconnect" else "Connect",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (device.isConnected) Color(0xFF94A3B8) else Color(0xFF090D16)
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                // Configure Gear Button
                IconButton(
                    onClick = onConfigureClick,
                    modifier = Modifier
                        .size(34.dp)
                        .background(DarkSurface, RoundedCornerShape(8.dp))
                ) {
                    Icon(Icons.Default.Settings, contentDescription = "Configure", tint = Color(0xFF94A3B8), modifier = Modifier.size(18.dp))
                }
            }

            // Routing Badges if connected
            if (device.isConnected) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isMediaActive) {
                        Box(
                            modifier = Modifier
                                .background(CyberCyan.copy(alpha = 0.2f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.MusicNote, contentDescription = null, tint = CyberCyan, modifier = Modifier.size(12.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Primary Media Audio", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = CyberCyan)
                            }
                        }
                    }

                    if (isSecondaryActive) {
                        Box(
                            modifier = Modifier
                                .background(NeonViolet.copy(alpha = 0.2f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Speaker, contentDescription = null, tint = NeonViolet, modifier = Modifier.size(12.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Dual Audio Secondary", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = NeonViolet)
                            }
                        }
                    }

                    if (isCallActive) {
                        Box(
                            modifier = Modifier
                                .background(SignalGreen.copy(alpha = 0.2f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Call, contentDescription = null, tint = SignalGreen, modifier = Modifier.size(12.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Active Calls", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = SignalGreen)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.weight(1f))
                    Text(device.codec.split(" ").first(), fontSize = 10.sp, color = Color(0xFF64748B))
                }
            }
        }
    }
}

@Composable
fun DeviceConfigDialog(
    device: BluetoothDeviceInfo,
    onDismiss: () -> Unit,
    onToggleMedia: () -> Unit,
    onToggleCall: () -> Unit,
    onSetVolume: (Int) -> Unit,
    onSetCodec: (String, Int) -> Unit,
    onRename: () -> Unit,
    onUnpair: () -> Unit
) {
    var volume by remember { mutableStateOf(device.volumePercent.toFloat()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(getDeviceIcon(device.type), contentDescription = null, tint = CyberCyan)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(device.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }

                IconButton(onClick = onRename, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Edit, contentDescription = "Rename", tint = CyberCyan, modifier = Modifier.size(16.dp))
                }
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                // Device Hardware Volume Slider
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Device Output Volume", fontSize = 12.sp, color = Color(0xFF94A3B8))
                        Text("${volume.toInt()}%", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = CyberCyan)
                    }
                    Slider(
                        value = volume,
                        onValueChange = {
                            volume = it
                            onSetVolume(it.toInt())
                        },
                        valueRange = 0f..100f,
                        colors = SliderDefaults.colors(thumbColor = CyberCyan, activeTrackColor = CyberCyan)
                    )
                }

                // Audio Stream Permissions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Media Audio Output", color = Color.White, fontSize = 13.sp)
                        Text("Music, video and game sounds", fontSize = 11.sp, color = Color(0xFF94A3B8))
                    }
                    Switch(
                        checked = device.isMediaAudioEnabled,
                        onCheckedChange = { onToggleMedia() },
                        colors = SwitchDefaults.colors(checkedThumbColor = CyberCyan)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Phone Calls & VoIP", color = Color.White, fontSize = 13.sp)
                        Text("Routing phone voice & microphone", fontSize = 11.sp, color = Color(0xFF94A3B8))
                    }
                    Switch(
                        checked = device.isCallAudioEnabled,
                        onCheckedChange = { onToggleCall() },
                        colors = SwitchDefaults.colors(checkedThumbColor = SignalGreen)
                    )
                }

                // Codec & Latency Mode
                Column {
                    Text("Bluetooth Audio Codec Profile", fontSize = 12.sp, color = Color(0xFF94A3B8))
                    Spacer(modifier = Modifier.height(6.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        val codecs = listOf(
                            Triple("LDAC (990 kbps)", 28, "Hi-Res"),
                            Triple("aptX HD", 35, "HD Sound"),
                            Triple("Low Latency (32ms)", 32, "Gaming"),
                            Triple("AAC", 65, "Standard")
                        )
                        items(codecs) { (cName, lat, label) ->
                            val isSel = device.codec == cName
                            FilterChip(
                                selected = isSel,
                                onClick = { onSetCodec(cName, lat) },
                                label = { Text(label) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = CyberCyan.copy(alpha = 0.2f),
                                    selectedLabelColor = CyberCyan,
                                    containerColor = DarkSurface,
                                    labelColor = Color(0xFF94A3B8)
                                )
                            )
                        }
                    }
                }

                // Unpair button
                OutlinedButton(
                    onClick = onUnpair,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFEF4444)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Unpair / Forget Device")
                }
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
fun DiscoveredDeviceItem(
    device: BluetoothDeviceInfo,
    onPairClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(DarkSurface, RoundedCornerShape(10.dp))
            .padding(10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(getDeviceIcon(device.type), contentDescription = null, tint = NeonViolet, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(device.name, fontWeight = FontWeight.SemiBold, color = Color.White, fontSize = 13.sp)
                Text("${device.audioProfile} &bull; ${device.rssi} dBm", fontSize = 11.sp, color = Color(0xFF94A3B8))
            }
        }

        Button(
            onClick = onPairClick,
            colors = ButtonDefaults.buttonColors(containerColor = NeonViolet),
            shape = RoundedCornerShape(8.dp),
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
            modifier = Modifier.height(32.dp)
        ) {
            Text("Pair & Connect", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
        }
    }
}

fun getDeviceIcon(type: DeviceType): ImageVector {
    return when (type) {
        DeviceType.HEADPHONES -> Icons.Default.Headphones
        DeviceType.SPEAKER -> Icons.Default.Speaker
        DeviceType.CAR_AUDIO -> Icons.Default.DirectionsCar
        DeviceType.SMARTWATCH -> Icons.Default.Watch
        DeviceType.PHONE_COMPUTER -> Icons.Default.PhoneAndroid
        else -> Icons.Default.Bluetooth
    }
}

