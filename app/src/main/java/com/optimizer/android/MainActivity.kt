package com.optimizer.android

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.net.VpnService
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.BatteryStd
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FolderSpecial
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.MovieCreation
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.VpnLock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.collectAsState
import com.optimizer.android.presentation.AppEraserActivity
import com.optimizer.android.domain.model.BatteryHealth
import com.optimizer.android.presentation.MainViewModel
import com.optimizer.android.presentation.MainUiState
import com.optimizer.android.presentation.DialogType
import dagger.hilt.android.AndroidEntryPoint

import com.optimizer.android.ui.components.CrispWhite
import com.optimizer.android.ui.components.NeonGreen
import com.optimizer.android.ui.components.NeoDialog
import com.optimizer.android.ui.components.PitchBlack
import com.optimizer.android.ui.components.StatusCard
import com.optimizer.android.ui.components.SuperpowerCard

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()
    
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions[Manifest.permission.READ_EXTERNAL_STORAGE] == true) {
            viewModel.log("ACCESS GRANTED.")
        }
    }

    private val vpnLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            startService(Intent(this, LocalFirewallService::class.java))
            viewModel.log("🔥 VPN FIREWALL: ACTIVE")
        } else {
            viewModel.log("VPN FIREWALL: DENIED")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        checkPermissions()
        viewModel.loadVaultContent()

        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(
                    background = PitchBlack,
                    surface = PitchBlack,
                    onBackground = CrispWhite,
                    onSurface = CrispWhite,
                    primary = CrispWhite
                )
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val uiState by viewModel.uiState.collectAsState()
                    OptimizerDashboard(uiState)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshSystemStatus()
    }

    private fun checkPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                viewModel.log("REQUESTING ROOT-LEVEL STORAGE ACCESS...")
                try {
                    val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                    intent.data = Uri.parse("package:$packageName")
                    startActivity(intent)
                } catch (e: Exception) {
                    val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                    startActivity(intent)
                }
            }
        }
    }

    private fun requestVpn() {
        val intent = VpnService.prepare(this)
        if (intent != null) {
            vpnLauncher.launch(intent)
        } else {
            startService(Intent(this, LocalFirewallService::class.java))
            viewModel.log("🔥 VPN FIREWALL: ACTIVE")
        }
    }

    private fun requestNotificationAccess() {
        viewModel.log("REQUESTING NOTIFICATION LISTENER...")
        startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
    }
    
    private fun requestWorkProfile() {
        viewModel.log("PROVISIONING WORK PROFILE...")
        try {
            val intent = Intent(android.app.admin.DevicePolicyManager.ACTION_PROVISION_MANAGED_PROFILE)
            intent.putExtra(
                android.app.admin.DevicePolicyManager.EXTRA_PROVISIONING_DEVICE_ADMIN_COMPONENT_NAME,
                android.content.ComponentName(this, WorkProfileReceiver::class.java)
            )
            startActivity(intent)
        } catch (e: Exception) {
            viewModel.log("FAILED: Work Profile not supported.")
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun OptimizerDashboard(uiState: MainUiState) {
        val scrollState = rememberScrollState()

        Column(modifier = Modifier
            .padding(16.dp)
            .verticalScroll(scrollState)
        ) {
            Text("OMNIX OS", fontSize = 32.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp)
            Text("GOD-TIER SUPERAPP • NO ROOT REQUIRED", fontSize = 12.sp, fontFamily = FontFamily.Monospace, color = Color.Gray)
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // --- SYSTEM STATUS DASHBOARD ---
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatusCard(modifier = Modifier.weight(1f), title = "STORAGE", value = "${uiState.storageStat.freeMb} MB")
                StatusCard(modifier = Modifier.weight(1f), title = "RAM", value = "${uiState.ramStat.freeMb} MB")
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatusCard(modifier = Modifier.weight(1f), title = "TEMP", value = "${uiState.batteryStat.tempCelsius} °C", alert = uiState.batteryStat.tempCelsius > 40f)
                StatusCard(modifier = Modifier.weight(1f), title = "HEALTH", value = uiState.batteryStat.health.name, alert = uiState.batteryStat.health.name != "GOOD")
            }
            
            Spacer(modifier = Modifier.height(24.dp))

            // --- CORE SYSTEM OPTIMIZERS ---
            Text("CORE SYSTEM OPTIMIZERS", fontSize = 14.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            Spacer(modifier = Modifier.height(8.dp))

            SuperpowerCard(
                title = "JUNK & CACHE CLEANER (2-STAGE)",
                icon = Icons.Filled.CleaningServices,
                onClick = { viewModel.toggleDialog(DialogType.JUNK) }
            )
            SuperpowerCard(
                title = "RAM SPEED BOOSTER",
                icon = Icons.Filled.Speed,
                onClick = { viewModel.toggleDialog(DialogType.RAM) }
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // --- ULTIMATE SUPERPOWERS ---
            Text("ULTIMATE SUPERPOWERS", fontSize = 14.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            Spacer(modifier = Modifier.height(8.dp))

            SuperpowerCard(
                title = "DEEP ROOT APP ERASER",
                icon = Icons.Filled.Delete,
                onClick = { startActivity(Intent(this@MainActivity, AppEraserActivity::class.java)) }
            )
            SuperpowerCard(
                title = "WORK PROFILE ENGINE",
                icon = Icons.Filled.FolderSpecial,
                onClick = { viewModel.toggleDialog(DialogType.HIBERNATION) } 
            )
            SuperpowerCard(
                title = "ANTI-DELETE MESSAGE VAULT",
                icon = Icons.Filled.Message,
                onClick = { 
                    viewModel.loadVaultContent()
                    viewModel.toggleDialog(DialogType.BLACKHOLE) 
                } 
            )
            SuperpowerCard(
                title = "DNS-LEVEL WEB SHIELD",
                icon = Icons.Filled.CloudOff,
                onClick = { viewModel.toggleDialog(DialogType.VPN) } 
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // --- PRO FEATURES ---
            Text("PRO FEATURES", fontSize = 14.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            Spacer(modifier = Modifier.height(8.dp))

            SuperpowerCard(
                title = "iCLONE PRO CAMERA",
                icon = Icons.Filled.CameraAlt,
                onClick = { startActivity(Intent(this@MainActivity, ProCameraActivity::class.java)) }
            )
            
            SuperpowerCard(
                title = "PRO STUDIO AI (EDITOR)",
                icon = Icons.Filled.MovieCreation,
                onClick = { startActivity(Intent(this@MainActivity, ProStudioActivity::class.java)) }
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // --- CONSOLE LOG ---
            Text("SYSTEM LOGS", fontSize = 14.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .background(PitchBlack)
                    .border(2.dp, CrispWhite, RoundedCornerShape(0.dp))
                    .padding(12.dp)
            ) {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(uiState.logs.reversed()) { log ->
                        Text(text = "> $log", color = NeonGreen, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // --- LICENSE & CREDITS ---
            OutlinedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(0.dp),
                border = BorderStroke(2.dp, CrispWhite),
                colors = CardDefaults.outlinedCardColors(containerColor = PitchBlack)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("LICENSE & COPYRIGHT", fontWeight = FontWeight.Black, fontSize = 14.sp, color = CrispWhite, letterSpacing = 1.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "© 2026 vickymosafan. All Rights Reserved.\n\n" +
                               "This software (OMNIX OS) and its God-Tier Superpowers (Work Profile Engine, Anti-Delete Vault, DNS Web Shield, iClone Pro Camera, Pro Studio AI) are the exclusive intellectual property of vickymosafan.\n\n" +
                               "Unauthorized copying, modification, distribution, or use of this software without explicit permission is strictly prohibited.",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        color = Color.Gray,
                        lineHeight = 14.sp
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // --- DIALOGS ---
        if (uiState.activeDialog == DialogType.VPN) {
            NeoDialog(
                title = "AKTIFKAN DNS SHIELD?",
                text = "Sistem akan mengaktifkan VPN Lokal untuk memblokir seluruh iklan dan situs kotor se-sistem via AdGuard DNS.",
                onConfirm = { 
                    viewModel.toggleDialog(DialogType.NONE)
                    requestVpn()
                },
                onDismiss = { viewModel.toggleDialog(DialogType.NONE) }
            )
        }
        if (uiState.activeDialog == DialogType.JUNK) {
            NeoDialog(
                title = "PINDAI & HAPUS CACHE SAMPAH?",
                text = "Sistem akan memindai berkas .tmp, .log, dan cache sisa aplikasi secara transparan tanpa merusak data penting Anda.",
                onConfirm = { viewModel.startJunkScan() },
                onDismiss = { viewModel.toggleDialog(DialogType.NONE) }
            )
        }
        if (uiState.activeDialog == DialogType.RAM) {
            NeoDialog(
                title = "BOOST RAM & PERFORMANCE?",
                text = "Sistem akan memicu pembersihan alokasi memori latar belakang dan mengoptimalkan kecepatan RAM.",
                onConfirm = { viewModel.boostRam() },
                onDismiss = { viewModel.toggleDialog(DialogType.NONE) }
            )
        }
        if (uiState.activeDialog == DialogType.HIBERNATION) {
            NeoDialog(
                title = "CREATE WORK PROFILE?",
                text = "Sistem akan membuat ruang ganda terpisah untuk mengkloning aplikasi.",
                onConfirm = { 
                    viewModel.toggleDialog(DialogType.NONE)
                    requestWorkProfile()
                },
                onDismiss = { viewModel.toggleDialog(DialogType.NONE) }
            )
        }
        if (uiState.activeDialog == DialogType.BLACKHOLE) {
            AlertDialog(
                onDismissRequest = { viewModel.toggleDialog(DialogType.NONE) },
                shape = RoundedCornerShape(0.dp),
                containerColor = PitchBlack,
                title = { Text("ANTI-DELETE VAULT", fontWeight = FontWeight.Black, color = CrispWhite) },
                text = {
                    LazyColumn(modifier = Modifier.heightIn(max = 300.dp)) {
                        item {
                            Text(uiState.vaultContent, fontFamily = FontFamily.Monospace, fontSize = 12.sp, color = CrispWhite)
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = { 
                            viewModel.toggleDialog(DialogType.NONE)
                            requestNotificationAccess() 
                        },
                        shape = RoundedCornerShape(0.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = CrispWhite, contentColor = PitchBlack)
                    ) {
                        Text("ENABLE INTERCEPTOR", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    OutlinedButton(
                        onClick = { viewModel.toggleDialog(DialogType.NONE) },
                        shape = RoundedCornerShape(0.dp),
                        border = BorderStroke(2.dp, CrispWhite),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = CrispWhite)
                    ) {
                        Text("TUTUP", fontWeight = FontWeight.Bold)
                    }
                }
            )
        }
    }
}
