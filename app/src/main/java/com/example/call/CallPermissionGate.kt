package com.example.call

import android.app.Activity
import android.content.Context
import android.widget.Toast
import androidx.core.app.ActivityCompat

/**
 * Gate that ensures RECORD_AUDIO/CAMERA runtime permissions are granted before
 * starting a call. This prevents WebRTC from "timing out" due to missing access.
 */
object CallPermissionGate {
    private const val REQUEST_CODE = 77

    fun canStartCall(context: Context): Boolean = PermissionManager.hasCallPermissions(context)

    fun startCallIfPermitted(
        activity: Activity?,
        context: Context,
        targetUserId: String,
        targetUserName: String,
        type: CallType
    ): Boolean {
        val hasPerms = canStartCall(context)
        if (hasPerms) {
            CallManager.getInstance(context).startCall(
                targetUserId = targetUserId,
                targetUserName = targetUserName,
                type = type
            )
            return true
        }
        // Request permissions; the user must retry the call after granting.
        val act = activity ?: context as? Activity
        if (act != null) {
            ActivityCompat.requestPermissions(act, PermissionManager.REQUIRED_PERMISSIONS, REQUEST_CODE)
        }
        Toast.makeText(
            context,
            "Primero concede permisos de micrófono/cámara y vuelve a intentar la llamada",
            Toast.LENGTH_LONG
        ).show()
        return false
    }
}