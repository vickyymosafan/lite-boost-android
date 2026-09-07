package com.optimizer.android.ui.theme

import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import kotlinx.coroutines.delay

object OmnixHaptics {
    // Tap kartu / toggle — satu tick ringan
    fun tick(h: HapticFeedback) {
        h.performHapticFeedback(HapticFeedbackType.TextHandleMove)
    }

    // Konfirmasi dialog sukses — double tick
    suspend fun doubleTick(h: HapticFeedback) {
        h.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        delay(80)
        h.performHapticFeedback(HapticFeedbackType.TextHandleMove)
    }

    // Alert pertama muncul — satu buzz panjang ringan
    fun buzz(h: HapticFeedback) {
        h.performHapticFeedback(HapticFeedbackType.LongPress)
    }
}
