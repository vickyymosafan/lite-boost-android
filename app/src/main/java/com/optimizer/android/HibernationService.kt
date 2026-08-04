package com.optimizer.android

import android.accessibilityservice.AccessibilityService
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import com.optimizer.android.utils.AccessibilityHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class HibernationService : AccessibilityService() {

    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.Default + job)

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val rootNode = rootInActiveWindow ?: return
        
        scope.launch {
            val forceStopClicked = AccessibilityHelper.performClickOnNodesByText(
                rootNode, 
                listOf("Force stop", "Paksa berhenti")
            )
            if (forceStopClicked > 0) {
                Log.d("HibernationService", "Mengeklik Force Stop ($forceStopClicked kali)")
            }

            val okClicked = AccessibilityHelper.performClickOnNodesByText(
                rootNode, 
                listOf("OK")
            )
            if (okClicked > 0) {
                Log.d("HibernationService", "Mengeklik OK ($okClicked kali)")
            }
            
            rootNode.recycle()
        }
    }

    override fun onInterrupt() {
        job.cancel()
    }

    override fun onDestroy() {
        super.onDestroy()
        job.cancel()
    }
}
