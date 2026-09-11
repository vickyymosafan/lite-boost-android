package com.optimizer.android.presentation

import android.content.Intent
import android.net.VpnService
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.dp
import com.optimizer.android.LocalFirewallService
import com.optimizer.android.ui.components.SectionHeader
import com.optimizer.android.ui.components.StatusCard
import com.optimizer.android.ui.components.TerminalLog
import com.optimizer.android.ui.theme.FeatureType
import com.optimizer.android.ui.theme.OmnixTheme
import com.optimizer.android.ui.theme.OmnixThemeColors
import com.optimizer.android.ui.theme.OmnixType
import dagger.hilt.android.AndroidEntryPoint
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@AndroidEntryPoint
class ShieldActivity : ComponentActivity() {

    private val viewModel: ShieldViewModel by viewModels()

    private val vpnLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            startService(LocalFirewallService.startIntent(this))
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            OmnixTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    ShieldDashboard()
                }
            }
        }
    }

    @Composable
    private fun ShieldDashboard() {
        val colors = OmnixThemeColors.colors
        val accent = colors.accent(FeatureType.SHIELD)
        val stats by viewModel.stats.collectAsState()
        val blockedLog by viewModel.blockedLog.collectAsState()
        val statuses by viewModel.listStatuses.collectAsState()
        val paused by viewModel.paused.collectAsState()
        val lastUpdate by viewModel.lastUpdate.collectAsState()
        val running by viewModel.vpnRunning.collectAsState()
        var allowInput by remember { mutableStateOf("") }
        var allowTick by remember { mutableIntStateOf(0) }
        val allowList = remember(allowTick) { viewModel.allowlistSnapshot() }
        val dateFormat = remember { SimpleDateFormat("dd MMM HH:mm", Locale.getDefault()) }

        Column(Modifier.fillMaxSize().padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("WEB SHIELD", style = OmnixType.display, color = colors.ink)
                Spacer(Modifier.width(10.dp))
                Box(
                    Modifier
                        .size(12.dp)
                        .background(
                            when {
                                running && !paused -> accent
                                running -> colors.grid
                                else -> colors.grid.copy(alpha = 0.3f)
                            },
                            CircleShape
                        )
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    when {
                        running && !paused -> "AKTIF"
                        running -> "PAUSED"
                        else -> "OFF"
                    },
                    style = OmnixType.label, color = colors.grid
                )
            }

            SectionHeader("01", "STATS")
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatusCard(Modifier.weight(1f), "TOTAL", "${stats.total}")
                StatusCard(Modifier.weight(1f), "ALLOWED", "${stats.allowed}")
            }
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatusCard(Modifier.weight(1f), "BLOCKED", "${stats.blocked}")
                StatusCard(Modifier.weight(1f), "IGNORED", "${stats.ignored}")
            }

            SectionHeader("02", "BLOCKED LOG")
            TerminalLog(logs = blockedLog.map { "${it.domain} [${it.listTitle}]" })

            SectionHeader("03", "FILTER LISTS")
            statuses.forEach { st ->
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(st.id.title, style = OmnixType.title, color = colors.ink)
                        Text("${st.ruleCount} rules", style = OmnixType.label, color = colors.grid)
                    }
                    Switch(checked = st.enabled, onCheckedChange = { viewModel.setListEnabled(st.id, it) })
                }
            }
            Text(
                "Update terakhir: " + (lastUpdate?.let { dateFormat.format(Date(it)) } ?: "bundel bawaan"),
                style = OmnixType.label, color = colors.grid
            )
            Spacer(Modifier.height(4.dp))
            OutlinedButton(
                onClick = { viewModel.updateLists() },
                shape = RectangleShape,
                border = BorderStroke(2.dp, accent),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = accent)
            ) { Text("UPDATE LISTS SEKARANG", style = OmnixType.label) }

            SectionHeader("04", "ALLOWLIST")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = allowInput,
                    onValueChange = { allowInput = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("domain.com", style = OmnixType.mono, color = colors.grid) },
                    textStyle = OmnixType.mono.copy(color = colors.ink),
                    singleLine = true
                )
                Button(
                    onClick = {
                        viewModel.addAllow(allowInput)
                        allowInput = ""
                        allowTick++
                    },
                    shape = RectangleShape,
                    colors = ButtonDefaults.buttonColors(containerColor = accent, contentColor = colors.base)
                ) { Text("ADD") }
            }
            Spacer(Modifier.height(8.dp))
            LazyColumn(modifier = Modifier.height(120.dp)) {
                items(allowList, key = { it }) { domain ->
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(domain, style = OmnixType.mono, color = colors.ink, modifier = Modifier.weight(1f))
                        Text(
                            "HAPUS",
                            style = OmnixType.label,
                            color = colors.danger,
                            modifier = Modifier.clickable {
                                viewModel.removeAllow(domain)
                                allowTick++
                            }
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { viewModel.setPaused(!paused) },
                    enabled = running,
                    shape = RectangleShape,
                    colors = ButtonDefaults.buttonColors(containerColor = accent, contentColor = colors.base)
                ) { Text(if (paused) "RESUME" else "PAUSE") }
                OutlinedButton(
                    onClick = {
                        if (running) {
                            startService(LocalFirewallService.stopIntent(this@ShieldActivity))
                        } else {
                            val intent = VpnService.prepare(this@ShieldActivity)
                            if (intent != null) vpnLauncher.launch(intent)
                            else startService(LocalFirewallService.startIntent(this@ShieldActivity))
                        }
                    },
                    shape = RectangleShape,
                    border = BorderStroke(2.dp, colors.ink),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.ink)
                ) { Text(if (running) "STOP SHIELD" else "START SHIELD") }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}
