package com.optimizer.android

import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class AppItem(val name: String, val packageName: String)

class AppEraserActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            var apps by remember { mutableStateOf<List<AppItem>>(emptyList()) }
            var isLoading by remember { mutableStateOf(true) }

            LaunchedEffect(Unit) {
                apps = withContext(Dispatchers.IO) {
                    val pm = packageManager
                    pm.getInstalledApplications(PackageManager.GET_META_DATA)
                        .filter { (it.flags and ApplicationInfo.FLAG_SYSTEM) == 0 && it.packageName != packageName }
                        .map { AppItem(it.loadLabel(pm).toString(), it.packageName) }
                        .sortedBy { it.name.lowercase() }
                }
                isLoading = false
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
                    .padding(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { finish() }, Modifier.size(32.dp)) { 
                        Icon(Icons.Filled.ArrowBack, contentDescription = null, tint = Color.White) 
                    }
                    Text("DEEP ROOT APP ERASER", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text("DANGER: 'NUKE' will uninstall the app and forcefully purge all residual cache folders in /sdcard/Android/data & obb.", 
                    color = Color.Red, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                Spacer(modifier = Modifier.height(16.dp))

                if (isLoading) {
                    Text("SCANNING APPS...", color = Color.Green, fontFamily = FontFamily.Monospace)
                } else {
                    LazyColumn(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(apps) { app ->
                            OutlinedCard(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(0.dp),
                                border = BorderStroke(1.dp, Color.White),
                                colors = CardDefaults.outlinedCardColors(containerColor = Color(0xFF111111))
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp).fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(app.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        Text(app.packageName, color = Color.Gray, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                                    }
                                    Button(
                                        onClick = { nukeApp(app.packageName) },
                                        shape = RoundedCornerShape(0.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color.Red, contentColor = Color.White),
                                        modifier = Modifier.height(36.dp)
                                    ) {
                                        Icon(Icons.Filled.Warning, null, Modifier.size(14.dp))
                                        Text(" NUKE", fontWeight = FontWeight.Black, fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun nukeApp(pkg: String) {
        val intent = Intent(Intent.ACTION_DELETE)
        intent.data = Uri.parse("package:$pkg")
        startActivity(intent)
    }
}
