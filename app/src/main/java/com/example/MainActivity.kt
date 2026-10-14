package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BluetoothConnected
import androidx.compose.material.icons.filled.CellTower
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.screens.BluetoothHubScreen
import com.example.ui.screens.RangeRadarScreen
import com.example.ui.screens.SpeedAndCostScreen
import com.example.ui.screens.WifiMusicScreen
import com.example.ui.screens.WifiScannerScreen
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkCard
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.NeonViolet
import com.example.ui.theme.SignalGreen
import com.example.viewmodel.NetPulseViewModel

enum class MainTab(
    val titleRes: Int,
    val icon: ImageVector,
    val testTag: String
) {
    WIFI(R.string.tab_wifi, Icons.Default.Wifi, "tab_wifi"),
    RANGE(R.string.tab_range, Icons.Default.Radar, "tab_range"),
    SPEED_COST(R.string.tab_speed_cost, Icons.Default.Speed, "tab_speed_cost"),
    MUSIC(R.string.tab_music, Icons.Default.MusicNote, "tab_music"),
    BLUETOOTH(R.string.tab_bluetooth, Icons.Default.BluetoothConnected, "tab_bluetooth")
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                MainAppContent()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppContent() {
    val context = LocalContext.current
    val viewModel: NetPulseViewModel = viewModel()
    var currentTab by remember { mutableStateOf(MainTab.WIFI) }

    val connectedWifi by viewModel.connectedWifi.collectAsState()
    val isMusicPlaying by viewModel.musicService.isPlaying.collectAsState()
    val isMusicServerActive by viewModel.musicService.isServerActive.collectAsState()
    val pairedDevices by viewModel.bluetoothHelper.pairedDevices.collectAsState()
    val connectedBtCount = pairedDevices.count { it.isConnected }

    // Permissions launcher for Location and Bluetooth
    val permissionsToRequest = remember {
        val list = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            list.add(Manifest.permission.BLUETOOTH_CONNECT)
            list.add(Manifest.permission.BLUETOOTH_SCAN)
        }
        list.toTypedArray()
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) {
        viewModel.refreshWifiData()
        viewModel.bluetoothHelper.refreshPairedDevices()
    }

    LaunchedEffect(Unit) {
        val needsPermissions = permissionsToRequest.any {
            ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
        }
        if (needsPermissions) {
            permissionLauncher.launch(permissionsToRequest)
        }
    }

    // Back handling: If on secondary tab, return to Wi-Fi tab
    BackHandler(enabled = currentTab != MainTab.WIFI) {
        currentTab = MainTab.WIFI
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = DarkBackground,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .background(CyberCyan.copy(alpha = 0.2f), RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.CellTower,
                                contentDescription = null,
                                tint = CyberCyan,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "NetPulse",
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 18.sp,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .background(SignalGreen, CircleShape)
                                )
                            }
                            Text(
                                text = connectedWifi?.ssid ?: "Wi-Fi & Bluetooth Hub",
                                fontSize = 11.sp,
                                color = Color(0xFF94A3B8)
                            )
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            viewModel.refreshWifiData()
                            viewModel.bluetoothHelper.refreshPairedDevices()
                        },
                        modifier = Modifier.testTag("app_bar_refresh_button")
                    ) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = "Refresh",
                            tint = CyberCyan
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = DarkSurface
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = DarkSurface,
                tonalElevation = 8.dp
            ) {
                MainTab.entries.forEach { tab ->
                    val isSelected = currentTab == tab
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { currentTab = tab },
                        icon = {
                            when (tab) {
                                MainTab.MUSIC -> {
                                    if (isMusicPlaying || isMusicServerActive) {
                                        BadgedBox(
                                            badge = {
                                                Badge(containerColor = SignalGreen)
                                            }
                                        ) {
                                            Icon(tab.icon, contentDescription = stringResource(tab.titleRes))
                                        }
                                    } else {
                                        Icon(tab.icon, contentDescription = stringResource(tab.titleRes))
                                    }
                                }
                                MainTab.BLUETOOTH -> {
                                    if (connectedBtCount > 0) {
                                        BadgedBox(
                                            badge = {
                                                Badge(containerColor = CyberCyan) {
                                                    Text("$connectedBtCount", fontSize = 9.sp, color = Color(0xFF090D16))
                                                }
                                            }
                                        ) {
                                            Icon(tab.icon, contentDescription = stringResource(tab.titleRes))
                                        }
                                    } else {
                                        Icon(tab.icon, contentDescription = stringResource(tab.titleRes))
                                    }
                                }
                                else -> {
                                    Icon(tab.icon, contentDescription = stringResource(tab.titleRes))
                                }
                            }
                        },
                        label = {
                            Text(
                                text = stringResource(tab.titleRes),
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color(0xFF090D16),
                            selectedTextColor = CyberCyan,
                            indicatorColor = CyberCyan,
                            unselectedIconColor = Color(0xFF94A3B8),
                            unselectedTextColor = Color(0xFF64748B)
                        ),
                        modifier = Modifier.testTag(tab.testTag)
                    )
                }
            }
        }
    ) { innerPadding ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            color = DarkBackground
        ) {
            when (currentTab) {
                MainTab.WIFI -> WifiScannerScreen(viewModel = viewModel)
                MainTab.RANGE -> RangeRadarScreen(viewModel = viewModel)
                MainTab.SPEED_COST -> SpeedAndCostScreen(viewModel = viewModel)
                MainTab.MUSIC -> WifiMusicScreen(viewModel = viewModel)
                MainTab.BLUETOOTH -> BluetoothHubScreen(viewModel = viewModel)
            }
        }
    }
}
