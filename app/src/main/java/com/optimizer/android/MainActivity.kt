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
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
        
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Android Optimizer", style = MaterialTheme.typography.headlineMedium)
            Text("Simple. Lightweight. No Root.", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Button(
                onClick = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !Environment.isExternalStorageManager()) {
                        logs.add("Error: Izin All Files Access belum diberikan!")
                        checkPermissions()
                    } else {
                        coroutineScope.launch {
                            withContext(Dispatchers.IO) {
                                OptimizerUtils.cleanJunk { log ->
                                    logs.add(log)
                                }
                            }
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Bersihkan Sampah (%temp%)")
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Button(
                onClick = {
                    coroutineScope.launch {
                        withContext(Dispatchers.IO) {
                            OptimizerUtils.fastReboot(this@MainActivity) { log ->
                                logs.add(log)
                            }
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
            ) {
                Text("Fast Reboot & Speed Up")
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            Text("Console Log:", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            
            // Console Box
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black, RoundedCornerShape(8.dp))
                    .padding(8.dp)
            ) {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(consoleLogs.reversed()) { log ->
                        Text(text = "> $log", color = Color.Green, fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}
