package org.privacysentinel.app

import android.Manifest
import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build

/** null = the app does not request the permission; true/false = granted or not. */
data class AppAccess(
    val packageName: String,
    val label: String,
    val camera: Boolean?,
    val mic: Boolean?
)

object PermissionAuditor {

    fun scan(ctx: Context): List<AppAccess> {
        val pm = ctx.packageManager
        val packages: List<PackageInfo> =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.getInstalledPackages(
                    PackageManager.PackageInfoFlags.of(PackageManager.GET_PERMISSIONS.toLong())
                )
            } else {
                @Suppress("DEPRECATION")
                pm.getInstalledPackages(PackageManager.GET_PERMISSIONS)
            }

        return packages.mapNotNull { p ->
            if (p.packageName == ctx.packageName) return@mapNotNull null
            val requested = p.requestedPermissions ?: return@mapNotNull null
            val flags = p.requestedPermissionsFlags ?: return@mapNotNull null

            fun stateOf(permission: String): Boolean? {
                val i = requested.indexOf(permission)
                if (i < 0) return null
                return (flags[i] and PackageInfo.REQUESTED_PERMISSION_GRANTED) != 0
            }

            val camera = stateOf(Manifest.permission.CAMERA)
            val mic = stateOf(Manifest.permission.RECORD_AUDIO)
            if (camera == null && mic == null) return@mapNotNull null

            val label = p.applicationInfo?.loadLabel(pm)?.toString() ?: p.packageName
            AppAccess(p.packageName, label, camera, mic)
        }.sortedWith(
            compareByDescending<AppAccess> { it.camera == true || it.mic == true }
                .thenBy { it.label.lowercase() }
        )
    }
}
