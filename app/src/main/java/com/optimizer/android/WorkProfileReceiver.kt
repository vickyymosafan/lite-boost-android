package com.optimizer.android

import android.app.admin.DeviceAdminReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class WorkProfileReceiver : DeviceAdminReceiver() {
    override fun onProfileProvisioningComplete(context: Context, intent: Intent) {
        super.onProfileProvisioningComplete(context, intent)
        Log.d("WorkProfile", "Work Profile Provisioning Complete!")
        
        // Mengaktifkan profil setelah selesai
        val manager = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as android.app.admin.DevicePolicyManager
        val componentName = android.content.ComponentName(context, WorkProfileReceiver::class.java)
        manager.setProfileEnabled(componentName)
    }
}
