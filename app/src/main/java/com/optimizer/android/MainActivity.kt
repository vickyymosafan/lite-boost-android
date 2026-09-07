package com.optimizer.android

import android.Manifest
import android.content.Intent
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
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FolderSpecial
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.MovieCreation
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.dp
import com.optimizer.android.presentation.AppEraserActivity
import com.optimizer.android.presentation.DialogType
import com.optimizer.android.presentation.MainUiState
import com.optimizer.android.presentation.MainViewModel
import com.optimizer.android.ui.components.BrutalBar
import com.optimizer.android.ui.components.NeoDialog
import com.optimizer.android.ui.components.SectionHeader
import com.optimizer.android.ui.components.StatusCard
import com.optimizer.android.ui.components.SuperpowerCard
import com.optimizer.android.ui.components.TerminalLog
import com.optimizer.android.ui.theme.FeatureType
import com.optimizer.android.ui.theme.OmnixTheme
import com.optimizer.android.ui.theme.OmnixThemeColors
import com.optimizer.android.ui.theme.OmnixType
import dagger.hilt.android.AndroidEntryPoint

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
            OmnixTheme {
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
        val colors = OmnixThemeColors.colors
        val scrollState = rememberScrollState()

        Column(
            modifier = Modifier
                .padding(16.dp)
                .verticalScroll(scrollState)
        ) {
            Text("OMNIX OS", style = OmnixType.display, color = colors.ink)
            Text("GOD-TIER SUPERAPP • NO ROOT REQUIRED", style = OmnixType.label, color = colors.grid)

            // 01 / SYSTEM STATUS
            SectionHeader("01", "SYSTEM STATUS")
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatusCard(Modifier.weight(1f), "STORAGE", "${uiState.storageStat.freeMb} MB", index = 1)
                StatusCard(Modifier.weight(1f), "RAM", "${uiState.ramStat.freeMb} MB", index = 2)
            }
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatusCard(
                    Modifier.weight(1f), "TEMP",
                    "${uiState.batteryStat.tempCelsius} °C",
                    alert = uiState.batteryStat.tempCelsius > 40f, index = 3
                )
                StatusCard(
                    Modifier.weight(1f), "HEALTH",
                    uiState.batteryStat.health.name,
                    alert = uiState.batteryStat.health.name != "GOOD", index = 4
                )
            }

            // 02 / CORE OPTIMIZERS
            SectionHeader("02", "CORE OPTIMIZERS")
            SuperpowerCard(
                title = "JUNK & CACHE CLEANER (2-STAGE)",
                icon = Icons.Filled.CleaningServices,
                onClick = { viewModel.toggleDialog(DialogType.JUNK) },
                accent = colors.accent(FeatureType.CLEANER),
                index = 5
            )
            AnimatedVisibility(
                visible = uiState.isScanning,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                BrutalBar(
                    progress = uiState.scanProgress,
                    accent = colors.accent(FeatureType.CLEANER),
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
            SuperpowerCard(
                title = "RAM SPEED BOOSTER",
                icon = Icons.Filled.Speed,
                onClick = { viewModel.toggleDialog(DialogType.RAM) },
                accent = colors.accent(FeatureType.CLEANER),
                index = 6
            )

            // 03 / SUPERPOWERS
            SectionHeader("03", "SUPERPOWERS")
            SuperpowerCard(
                title = "DEEP ROOT APP ERASER",
                icon = Icons.Filled.Delete,
                onClick = { startActivity(Intent(this@MainActivity, AppEraserActivity::class.java)) },
                accent = colors.accent(FeatureType.CLEANER),
                index = 7
            )
            SuperpowerCard(
                title = "WORK PROFILE ENGINE",
                icon = Icons.Filled.FolderSpecial,
                onClick = { viewModel.toggleDialog(DialogType.HIBERNATION) },
                index = 8 // accent default = ink (netral sistem)
            )
            SuperpowerCard(
                title = "ANTI-DELETE MESSAGE VAULT",
                icon = Icons.Filled.Message,
                onClick = {
                    viewModel.loadVaultContent()
                    viewModel.toggleDialog(DialogType.BLACKHOLE)
                },
                accent = colors.accent(FeatureType.VAULT),
                index = 9
            )
            SuperpowerCard(
                title = "DNS-LEVEL WEB SHIELD",
                icon = Icons.Filled.CloudOff,
                onClick = { viewModel.toggleDialog(DialogType.VPN) },
                accent = colors.accent(FeatureType.SHIELD),
                index = 10
            )

            // 04 / PRO FEATURES
            SectionHeader("04", "PRO FEATURES")
            SuperpowerCard(
                title = "iCLONE PRO CAMERA",
                icon = Icons.Filled.CameraAlt,
                onClick = { startActivity(Intent(this@MainActivity, ProCameraActivity::class.java)) },
                accent = colors.accent(FeatureType.CAMERA),
                index = 11
            )
            SuperpowerCard(
                title = "PRO STUDIO AI (EDITOR)",
                icon = Icons.Filled.MovieCreation,
                onClick = { startActivity(Intent(this@MainActivity, ProStudioActivity::class.java)) },
                accent = colors.accent(FeatureType.STUDIO),
                index = 12
            )

            // 05 / SYSTEM LOGS
            SectionHeader("05", "SYSTEM LOGS")
            TerminalLog(logs = uiState.logs)

            // LICENSE (dipertahankan, styling tetap brutalism)
            Spacer(Modifier.height(16.dp))
            OutlinedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RectangleShape,
                border = BorderStroke(2.dp, colors.ink),
                colors = CardDefaults.outlinedCardColors(containerColor = colors.base)
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text("LICENSE & COPYRIGHT", style = OmnixType.title, color = colors.ink)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "© 2026 vickymosafan. All Rights Reserved.\n\n" +
                            "This software (OMNIX OS) and its God-Tier Superpowers (Work Profile Engine, Anti-Delete Vault, DNS Web Shield, iClone Pro Camera, Pro Studio AI) are the exclusive intellectual property of vickymosafan.\n\n" +
                            "Unauthorized copying, modification, distribution, or use of this software without explicit permission is strictly prohibited.",
                        style = OmnixType.mono,
                        color = colors.grid
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
        }

        // --- DIALOGS (accent per fitur) ---
        val cleanAccent = colors.accent(FeatureType.CLEANER)
        val shieldAccent = colors.accent(FeatureType.SHIELD)
        val vaultAccent = colors.accent(FeatureType.VAULT)

        if (uiState.activeDialog == DialogType.VPN) {
            NeoDialog(
                title = "AKTIFKAN DNS SHIELD?",
                text = "Sistem akan mengaktifkan VPN Lokal untuk memblokir seluruh iklan dan situs kotor se-sistem via AdGuard DNS.",
                onConfirm = {
                    viewModel.toggleDialog(DialogType.NONE)
                    requestVpn()
                },
                onDismiss = { viewModel.toggleDialog(DialogType.NONE) },
                accent = shieldAccent
            )
        }
        if (uiState.activeDialog == DialogType.JUNK) {
            NeoDialog(
                title = "PINDAI & HAPUS CACHE SAMPAH?",
                text = "Sistem akan memindai berkas .tmp, .log, dan cache sisa aplikasi secara transparan tanpa merusak data penting Anda.",
                onConfirm = { viewModel.startJunkScan() },
                onDismiss = { viewModel.toggleDialog(DialogType.NONE) },
                accent = cleanAccent
            )
        }
        if (uiState.activeDialog == DialogType.RAM) {
            NeoDialog(
                title = "BOOST RAM & PERFORMANCE?",
                text = "Sistem akan memicu pembersihan alokasi memori latar belakang dan mengoptimalkan kecepatan RAM.",
                onConfirm = { viewModel.boostRam() },
                onDismiss = { viewModel.toggleDialog(DialogType.NONE) },
                accent = cleanAccent
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
            NeoDialog(
                title = "ANTI-DELETE VAULT",
                text = "",
                onConfirm = {
                    viewModel.toggleDialog(DialogType.NONE)
                    requestNotificationAccess()
                },
                onDismiss = { viewModel.toggleDialog(DialogType.NONE) },
                accent = vaultAccent,
                confirmLabel = "ENABLE INTERCEPTOR",
                content = {
                    Text(
                        uiState.vaultContent,
                        style = OmnixType.mono,
                        color = colors.ink,
                        modifier = Modifier.heightIn(max = 300.dp)
                    )
                }
            )
        }
    }
}
