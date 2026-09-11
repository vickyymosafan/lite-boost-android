package com.optimizer.android.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.dp
import com.optimizer.android.ui.theme.OmnixThemeColors
import com.optimizer.android.ui.theme.OmnixType

@Composable
fun TerminalLog(logs: List<String>, modifier: Modifier = Modifier) {
    val colors = OmnixThemeColors.colors
    val blink = rememberInfiniteTransition(label = "cursor")
    val cursorAlpha by blink.animateFloat(
        initialValue = 1f, targetValue = 0f,
        animationSpec = infiniteRepeatable(tween(500), RepeatMode.Reverse),
        label = "cursorAlpha"
    )
    val visible = remember(logs) { logs.takeLast(50) }
    val listState = rememberLazyListState()

    LaunchedEffect(visible.size) {
        if (visible.isNotEmpty()) listState.animateScrollToItem(visible.lastIndex)
    }

    Box(
        modifier
            .fillMaxWidth()
            .height(200.dp)
            .border(2.dp, colors.ink, RectangleShape)
            .background(colors.base)
            .padding(12.dp)
    ) {
        if (visible.isEmpty()) {
            // Empty state — bukan layar kosong
            Text(
                "> AWAITING SIGNAL...▮",
                style = OmnixType.mono,
                color = colors.grid.copy(alpha = 0.4f + 0.6f * cursorAlpha)
            )
        } else {
            LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
                itemsIndexed(
                    visible,
                    key = { i, s -> "$i-${s.hashCode()}" }
                ) { _, log ->
                    Text(
                        "> $log",
                        style = OmnixType.mono,
                        color = colors.ink,
                        modifier = Modifier
                            .padding(vertical = 2.dp)
                            .animateItem()
                    )
                }
                item(key = "cursor") {
                    Text(
                        "> ▮",
                        style = OmnixType.mono,
                        color = colors.grid.copy(alpha = 0.2f + 0.8f * cursorAlpha)
                    )
                }
            }
        }
    }
}
