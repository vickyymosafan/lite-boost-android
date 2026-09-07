package com.optimizer.android.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.optimizer.android.ui.theme.FeatureType
import com.optimizer.android.ui.theme.OmnixTheme
import com.optimizer.android.ui.theme.OmnixThemeColors

@Preview(name = "StatusCards", showBackground = true, backgroundColor = 0xFF0A0A0A, widthDp = 360)
@Composable
private fun PreviewStatusCards() {
    OmnixTheme {
        Surface {
            Column(Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                StatusCard(title = "STORAGE", value = "12480 MB")
                StatusCard(title = "TEMP", value = "43 °C", alert = true)
            }
        }
    }
}

@Preview(name = "SuperpowerCards", showBackground = true, backgroundColor = 0xFF0A0A0A, widthDp = 360)
@Composable
private fun PreviewSuperpowerCards() {
    OmnixTheme {
        Surface {
            Column(Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                SuperpowerCard(
                    title = "JUNK & CACHE CLEANER",
                    icon = Icons.Filled.CleaningServices,
                    onClick = {},
                    accent = OmnixThemeColors.colors.accent(FeatureType.CLEANER)
                )
                SuperpowerCard(title = "WORK PROFILE ENGINE", icon = Icons.Filled.CleaningServices, onClick = {})
            }
        }
    }
}

@Preview(name = "SectionHeader + BrutalBar", showBackground = true, backgroundColor = 0xFF0A0A0A, widthDp = 360)
@Composable
private fun PreviewHeaderBar() {
    OmnixTheme {
        Surface {
            Column(Modifier.padding(8.dp)) {
                SectionHeader("01", "SYSTEM STATUS")
                BrutalBar(0.62f, OmnixThemeColors.colors.accent(FeatureType.CLEANER))
            }
        }
    }
}

@Preview(name = "TerminalLog filled", showBackground = true, backgroundColor = 0xFF0A0A0A, widthDp = 360)
@Composable
private fun PreviewTerminalLog() {
    OmnixTheme {
        TerminalLog(logs = listOf("BOOT SISTEM BERHASIL.", "MEMULAI SCANNING...", "12 BERKAS DIHAPUS."))
    }
}

@Preview(name = "TerminalLog empty", showBackground = true, backgroundColor = 0xFF0A0A0A, widthDp = 360)
@Composable
private fun PreviewTerminalLogEmpty() {
    OmnixTheme {
        TerminalLog(logs = emptyList())
    }
}
