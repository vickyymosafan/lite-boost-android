package com.optimizer.android

import android.accessibilityservice.AccessibilityService
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

class HibernationService : AccessibilityService() {

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Logika sederhana untuk demonstrasi (Tidak sempurna karena ID tombol Force Stop berbeda tiap HP).
        // Pada aplikasi sungguhan, kita menggunakan text matching seperti "Paksa berhenti" atau "Force stop".
        
        val rootNode = rootInActiveWindow ?: return
        
        // Cari tombol "Force stop" atau "Paksa berhenti"
        val forceStopNodes = rootNode.findAccessibilityNodeInfosByText("Force stop")
        val paksaBerhentiNodes = rootNode.findAccessibilityNodeInfosByText("Paksa berhenti")
        
        val allNodes = forceStopNodes + paksaBerhentiNodes
        
        for (node in allNodes) {
            if (node.isClickable && node.isEnabled) {
                node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                Log.d("HibernationService", "Mengeklik Force Stop")
            }
            node.recycle()
        }
        
        // Cari tombol konfirmasi "OK" atau "Force stop" pada dialog
        val okNodes = rootNode.findAccessibilityNodeInfosByText("OK")
        for (node in okNodes) {
            if (node.isClickable && node.isEnabled) {
                node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                Log.d("HibernationService", "Mengeklik OK")
            }
            node.recycle()
        }
        
        rootNode.recycle()
    }

    override fun onInterrupt() {
        // Dihentikan
    }
}
