package org.privacysentinel.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat

/** Restarts monitoring after reboot if the user had it enabled. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED && Prefs.monitoringEnabled(context)) {
            ContextCompat.startForegroundService(
                context, Intent(context, MonitorService::class.java)
            )
        }
    }
}
