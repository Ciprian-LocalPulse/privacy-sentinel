package org.privacysentinel.app

import android.content.Context

/** Tiny wrapper around SharedPreferences. Stored only on the device. */
object Prefs {
    private fun sp(c: Context) = c.getSharedPreferences("settings", Context.MODE_PRIVATE)

    fun monitoringEnabled(c: Context): Boolean = sp(c).getBoolean("monitoring", false)
    fun setMonitoringEnabled(c: Context, value: Boolean) =
        sp(c).edit().putBoolean("monitoring", value).apply()

    fun alertAll(c: Context): Boolean = sp(c).getBoolean("alert_all", false)
    fun setAlertAll(c: Context, value: Boolean) =
        sp(c).edit().putBoolean("alert_all", value).apply()
}
