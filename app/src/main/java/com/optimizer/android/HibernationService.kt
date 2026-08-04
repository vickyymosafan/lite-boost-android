package com.optimizer.android

import android.accessibilityservice.AccessibilityService
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import com.optimizer.android.utils.clickIfMatches
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.cancelChildren

class HibernationService : AccessibilityService() {

    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.Default + job)

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null || event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return

        val rootNode = rootInActiveWindow ?: return
        
        scope.launch {
            try {
                val forceStopClicked = rootNode.clickIfMatches(listOf("Force stop", "Paksa berhenti"))
                if (forceStopClicked > 0) {
                    Log.d("HibernationService", "Mengeklik Force Stop ($forceStopClicked kali)")
                }

                val okClicked = rootNode.clickIfMatches(listOf("OK"))
                if (okClicked > 0) {
                    Log.d("HibernationService", "Mengeklik OK ($okClicked kali)")
                }
            } finally {
                rootNode.recycle()
            }
        }
    }

    override fun onInterrupt() {
        scope.coroutineContext.cancelChildren()
    }

    override fun onDestroy() {
        super.onDestroy()
        job.cancel()
    }
}
