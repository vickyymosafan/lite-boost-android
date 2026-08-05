package com.optimizer.android.presentation

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.optimizer.android.PurgeResidualsWorker
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class AppEraserActivity : ComponentActivity() {

    private val viewModel: AppEraserViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        viewModel.loadApps()
        
        setContent {
            val uiState by viewModel.uiState.collectAsState()
            
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
                    .padding(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { finish() }, modifier = Modifier.size(32.dp)) { 
                        Icon(Icons.Filled.ArrowBack, contentDescription = null, tint = Color.White) 
                    }
                    Text("PENGHAPUS APLIKASI DEEP ROOT", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text("PERINGATAN: 'HAPUS' akan mencopot pemasangan aplikasi dan membersihkan paksa semua folder sisa di /sdcard/Android/data & obb.", 
                    color = Color.Red, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                Spacer(modifier = Modifier.height(16.dp))

                if (uiState.isLoading) {
                    Text("MEMINDAI APLIKASI...", color = Color.Green, fontFamily = FontFamily.Monospace)
                } else {
                    LazyColumn(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(uiState.apps) { app ->
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
                                        Icon(Icons.Filled.Warning, null, modifier = Modifier.size(14.dp))
                                        Text(" HAPUS", fontWeight = FontWeight.Black, fontSize = 12.sp)
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
        // Queue aggressive cleanup
        val data = workDataOf(PurgeResidualsWorker.KEY_PACKAGE_NAME to pkg)
        val request = OneTimeWorkRequestBuilder<PurgeResidualsWorker>()
            .setInputData(data)
            .build()
        WorkManager.getInstance(this).enqueue(request)

        // Trigger system uninstaller
        val intent = Intent(Intent.ACTION_DELETE)
        intent.data = Uri.parse("package:$pkg")
        startActivity(intent)
    }
}
