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
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.BatteryStd
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.NotificationsOff
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
import androidx.core.content.ContextCompat
import androidx.work.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.TimeUnit

// GEN-Z NEO-BRUTALISM COLORS
val PitchBlack = Color(0xFF000000)
val CrispWhite = Color(0xFFFFFFFF)
val NeonGreen = Color(0xFF00FF00)

class MainActivity : ComponentActivity() {

    private val logs = mutableStateListOf<String>("SYSTEM BOOT OK.")
    
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions[Manifest.permission.READ_EXTERNAL_STORAGE] == true) {
            logs.add("ACCESS GRANTED.")
        }
    }

    private val vpnLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            startService(Intent(this, LocalFirewallService::class.java))
            logs.add("🔥 VPN FIREWALL: ACTIVE")
        } else {
            logs.add("VPN FIREWALL: DENIED")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        checkPermissions()

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
                    OptimizerDashboard(logs)
                }
            }
        }
    }

    private fun checkPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                logs.add("REQUESTING ROOT-LEVEL STORAGE ACCESS...")
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
            logs.add("🔥 VPN FIREWALL: ACTIVE")
        }
    }

    private fun requestAccessibility() {
        logs.add("REQUESTING ACCESSIBILITY FOR HIBERNATION...")
        startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
    }

    private fun requestNotificationAccess() {
        logs.add("REQUESTING NOTIFICATION LISTENER...")
        startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
    }

    @Composable
    fun OptimizerDashboard(consoleLogs: List<String>) {
        val coroutineScope = rememberCoroutineScope()
        var storageStat by remember { mutableStateOf(OptimizerUtils.getStorageStatus()) }
        var ramStat by remember { mutableStateOf(OptimizerUtils.getRamStatus(this@MainActivity)) }
        var batteryStat by remember { mutableStateOf(OptimizerUtils.getBatteryStatus(this@MainActivity)) }
        
        // State for Junk Cleaner
        var isScanning by remember { mutableStateOf(false) }
        var scannedFiles by remember { mutableStateOf<List<File>?>(null) }
        
        // Dialog States
        var showVpnDialog by remember { mutableStateOf(false) }
        var showHibernationDialog by remember { mutableStateOf(false) }
        var showBlackholeDialog by remember { mutableStateOf(false) }

        Column(modifier = Modifier.padding(16.dp)) {
            Text("LITE BOOST", fontSize = 32.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp)
            Text("NO ROOT REQUIRED.", fontSize = 12.sp, fontFamily = FontFamily.Monospace, color = Color.Gray)
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // --- SYSTEM STATUS DASHBOARD ---
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatusCard(modifier = Modifier.weight(1f), title = "STORAGE", value = "${storageStat.freeMb} MB")
                StatusCard(modifier = Modifier.weight(1f), title = "RAM", value = "${ramStat.freeMb} MB")
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatusCard(modifier = Modifier.weight(1f), title = "TEMP", value = "${batteryStat.tempCelsius} °C", alert = batteryStat.tempCelsius > 40f)
                StatusCard(modifier = Modifier.weight(1f), title = "HEALTH", value = batteryStat.healthString.uppercase(), alert = batteryStat.healthString != "Good")
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // --- SUPERPOWERS ---
            Text("ULTIMATE SUPERPOWERS", fontSize = 14.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            Spacer(modifier = Modifier.height(8.dp))

            SuperpowerCard(
                title = "LOCAL FIREWALL",
                icon = Icons.Filled.VpnLock,
                onClick = { showVpnDialog = true }
            )
            SuperpowerCard(
                title = "TRUE HIBERNATION",
                icon = Icons.Filled.Memory,
                onClick = { showHibernationDialog = true }
            )
            SuperpowerCard(
                title = "NOTIFICATION BLACKHOLE",
                icon = Icons.Filled.NotificationsOff,
                onClick = { showBlackholeDialog = true }
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // --- CONSOLE LOG ---
            Text("SYSTEM LOGS", fontSize = 14.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(PitchBlack)
                    .border(2.dp, CrispWhite, RoundedCornerShape(0.dp))
                    .padding(12.dp)
            ) {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(consoleLogs.reversed()) { log ->
                        Text(text = "> $log", color = NeonGreen, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                    }
                }
            }
        }

        // --- DIALOGS ---
        if (showVpnDialog) {
            NeoDialog(
                title = "ENABLE FIREWALL?",
                text = "Ini akan memblokir akses internet menggunakan VPN Lokal.",
                onConfirm = { 
                    showVpnDialog = false
                    requestVpn() 
                },
                onDismiss = { showVpnDialog = false }
            )
        }
        if (showHibernationDialog) {
            NeoDialog(
                title = "TRUE HIBERNATION?",
                text = "Layar Anda akan diambil alih sesaat untuk mematikan paksa (Force Stop) seluruh aplikasi.",
                onConfirm = { 
                    showHibernationDialog = false
                    requestAccessibility() 
                },
                onDismiss = { showHibernationDialog = false }
            )
        }
        if (showBlackholeDialog) {
            NeoDialog(
                title = "ENABLE BLACKHOLE?",
                text = "Semua notifikasi masuk akan ditelan dan disembunyikan. HP Anda akan sunyi.",
                onConfirm = { 
                    showBlackholeDialog = false
                    requestNotificationAccess() 
                },
                onDismiss = { showBlackholeDialog = false }
            )
        }
    }

    @Composable
    fun StatusCard(modifier: Modifier = Modifier, title: String, value: String, alert: Boolean = false) {
        val color = if (alert) Color.Red else CrispWhite
        OutlinedCard(
            modifier = modifier,
            shape = RoundedCornerShape(0.dp),
            border = BorderStroke(2.dp, color),
            colors = CardDefaults.outlinedCardColors(containerColor = PitchBlack)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(title, fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = color)
                Spacer(modifier = Modifier.height(4.dp))
                Text(value, fontWeight = FontWeight.Black, fontSize = 20.sp, color = color)
            }
        }
    }

    @Composable
    fun SuperpowerCard(title: String, icon: ImageVector, onClick: () -> Unit) {
        OutlinedCard(
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            shape = RoundedCornerShape(0.dp),
            border = BorderStroke(2.dp, CrispWhite),
            colors = CardDefaults.outlinedCardColors(containerColor = PitchBlack),
            onClick = onClick
        ) {
            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(24.dp), tint = CrispWhite)
                Spacer(modifier = Modifier.width(16.dp))
                Text(title, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = CrispWhite)
            }
        }
    }

    @Composable
    fun NeoDialog(title: String, text: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
        AlertDialog(
            onDismissRequest = onDismiss,
            shape = RoundedCornerShape(0.dp),
            containerColor = PitchBlack,
            titleContentColor = CrispWhite,
            textContentColor = CrispWhite,
            title = { Text(title, fontWeight = FontWeight.Black) },
            text = { Text(text, fontFamily = FontFamily.Monospace) },
            confirmButton = {
                Button(
                    onClick = onConfirm, 
                    shape = RoundedCornerShape(0.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = CrispWhite, contentColor = PitchBlack)
                ) {
                    Text("GASKAN", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(0.dp),
                    border = BorderStroke(2.dp, CrispWhite),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = CrispWhite)
                ) {
                    Text("BATAL", fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}
