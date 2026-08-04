package com.optimizer.android

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.work.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.TimeUnit

class MainActivity : ComponentActivity() {

    private val logs = mutableStateListOf<String>("System Ready.")
    
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions[Manifest.permission.READ_EXTERNAL_STORAGE] == true) {
            logs.add("Storage Permission Granted.")
        } else {
            logs.add("Storage Permission Denied! Cannot clean junk.")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        checkPermissions()

        setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
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
                logs.add("Meminta izin All Files Access (Android 11+)...")
                try {
                    val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                    intent.data = Uri.parse("package:$packageName")
                    startActivity(intent)
                } catch (e: Exception) {
                    val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                    startActivity(intent)
                }
            } else {
                logs.add("Storage Permission Granted (Android 11+).")
            }
        } else {
            val permissions = arrayOf(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
            val missing = permissions.filter { 
                ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED 
            }
            if (missing.isNotEmpty()) {
                requestPermissionLauncher.launch(missing.toTypedArray())
            }
        }
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

        Column(modifier = Modifier.padding(16.dp)) {
            Text("Android Optimizer", style = MaterialTheme.typography.headlineMedium)
            Text("Pro Dashboard", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // --- SYSTEM STATUS DASHBOARD ---
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatusCard(
                    modifier = Modifier.weight(1f),
                    title = "Sisa Storage",
                    value = "${storageStat.freeMb} MB",
                    icon = Icons.Filled.Storage
                )
                StatusCard(
                    modifier = Modifier.weight(1f),
                    title = "Sisa RAM",
                    value = "${ramStat.freeMb} MB",
                    icon = Icons.Filled.Memory
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatusCard(
                    modifier = Modifier.weight(1f),
                    title = "Suhu Baterai",
                    value = "${batteryStat.tempCelsius} °C",
                    icon = Icons.Filled.Thermostat,
                    alert = batteryStat.tempCelsius > 40f
                )
                StatusCard(
                    modifier = Modifier.weight(1f),
                    title = "Kesehatan",
                    value = batteryStat.healthString,
                    icon = if (batteryStat.healthString == "Good") Icons.Filled.BatteryStd else Icons.Filled.BatteryAlert,
                    alert = batteryStat.healthString != "Good"
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // --- ACTION CARDS ---
            ActionCard(
                title = "Junk Cleaner",
                description = "Pindai sampah dan konfirmasi sebelum menghapus.",
                icon = Icons.Filled.DeleteSweep,
                buttonText = if (isScanning) "Memindai..." else if (scannedFiles != null) "Scan Ulang" else "Mulai Scan",
                buttonEnabled = !isScanning,
                onClick = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !Environment.isExternalStorageManager()) {
                        logs.add("Error: Izin All Files Access belum diberikan!")
                        checkPermissions()
                    } else {
                        isScanning = true
                        scannedFiles = null
                        coroutineScope.launch {
                            val result = withContext(Dispatchers.IO) {
                                OptimizerUtils.scanJunk { log -> logs.add(log) }
                            }
                            scannedFiles = result
                            isScanning = false
                        }
                    }
                }
            )
            
            // Delete confirmation button (Tahap 2)
            if (scannedFiles != null && !isScanning) {
                if (scannedFiles!!.isNotEmpty()) {
                    Button(
                        onClick = {
                            val filesToDelete = scannedFiles!!
                            scannedFiles = null // hide button
                            coroutineScope.launch {
                                withContext(Dispatchers.IO) {
                                    OptimizerUtils.deleteJunkFiles(filesToDelete) { log -> logs.add(log) }
                                }
                                // Refresh stats
                                storageStat = OptimizerUtils.getStorageStatus()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                    ) {
                        Text("🗑️ Hapus ${scannedFiles!!.size} File Sekarang!")
                    }
                } else {
                    Text("Penyimpanan sudah bersih!", color = Color.Green, modifier = Modifier.padding(bottom = 8.dp))
                }
            }

            ActionCard(
                title = "Fast Reboot",
                description = "Matikan paksa aplikasi latar belakang.",
                icon = Icons.Filled.Memory,
                buttonText = "Boost Now",
                buttonEnabled = true,
                onClick = {
                    coroutineScope.launch {
                        withContext(Dispatchers.IO) {
                            OptimizerUtils.fastReboot(this@MainActivity) { log -> logs.add(log) }
                        }
                        // Refresh stats
                        ramStat = OptimizerUtils.getRamStatus(this@MainActivity)
                    }
                }
            )

            AutoPilotCard(
                logs = logs,
                checkPermissions = { checkPermissions() }
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // --- CONSOLE LOG ---
            Text("Console Log:", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF1E1E1E), RoundedCornerShape(8.dp))
                    .border(1.dp, Color.DarkGray, RoundedCornerShape(8.dp))
                    .padding(8.dp)
            ) {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(consoleLogs.reversed()) { log ->
                        Text(text = "> $log", color = Color(0xFF00FF00), fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }

    @Composable
    fun StatusCard(modifier: Modifier = Modifier, title: String, value: String, icon: ImageVector, alert: Boolean = false) {
        Card(
            modifier = modifier,
            colors = CardDefaults.cardColors(containerColor = if (alert) Color(0xFF330000) else MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Icon(icon, contentDescription = null, tint = if (alert) Color.Red else MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(4.dp))
                Text(title, style = MaterialTheme.typography.labelMedium, color = if (alert) Color.Red else Color.Unspecified)
                Text(value, style = MaterialTheme.typography.titleLarge, color = if (alert) Color.Red else Color.Unspecified)
            }
        }
    }

    @Composable
    fun ActionCard(title: String, description: String, icon: ImageVector, buttonText: String, buttonEnabled: Boolean, onClick: () -> Unit) {
        Card(
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(40.dp), tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(title, style = MaterialTheme.typography.titleMedium)
                    Text(description, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                }
                Button(onClick = onClick, enabled = buttonEnabled) {
                    Text(buttonText)
                }
            }
        }
    }

    @Composable
    fun AutoPilotCard(logs: MutableList<String>, checkPermissions: () -> Unit) {
        val workManager = WorkManager.getInstance(this@MainActivity)
        var isAutoPilotEnabled by remember { mutableStateOf(false) }

        Card(
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Storage, contentDescription = null, modifier = Modifier.size(40.dp), tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Auto-Pilot Cleaner", style = MaterialTheme.typography.titleMedium)
                    Text("Pembersihan otomatis setiap 3 hari.", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                }
                Switch(
                    checked = isAutoPilotEnabled,
                    onCheckedChange = { checked ->
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !Environment.isExternalStorageManager()) {
                            logs.add("Error: Izin All Files Access belum diberikan!")
                            checkPermissions()
                        } else {
                            isAutoPilotEnabled = checked
                            if (checked) {
                                val constraints = Constraints.Builder()
                                    .setRequiresBatteryNotLow(true)
                                    .build()
                                val workRequest = PeriodicWorkRequestBuilder<AutoPilotWorker>(3, TimeUnit.DAYS)
                                    .setConstraints(constraints)
                                    .build()
                                workManager.enqueueUniquePeriodicWork(
                                    "AutoPilotCleaner",
                                    ExistingPeriodicWorkPolicy.UPDATE,
                                    workRequest
                                )
                                logs.add("🤖 Auto-Pilot DIAKTIFKAN. Jadwal 3 hari didaftarkan.")
                            } else {
                                workManager.cancelUniqueWork("AutoPilotCleaner")
                                logs.add("🤖 Auto-Pilot DIMATIKAN.")
                            }
                        }
                    }
                )
            }
        }
    }
}
