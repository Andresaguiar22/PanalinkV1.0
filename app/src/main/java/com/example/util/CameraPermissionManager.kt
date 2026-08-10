package com.example.util

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

object CameraPermissionManager {
    val REQUIRED_PERMISSIONS = arrayOf(
        Manifest.permission.CAMERA,
        Manifest.permission.RECORD_AUDIO
    )

    fun hasPermissions(context: Context): Boolean {
        return REQUIRED_PERMISSIONS.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    fun isPermissionPermanentlyDenied(activity: Activity, permission: String): Boolean {
        val isGranted = ContextCompat.checkSelfPermission(activity, permission) == PackageManager.PERMISSION_GRANTED
        val showRationale = ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)
        return !isGranted && !showRationale
    }

    fun openAppSettings(context: Context) {
        try {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", context.packageName, null)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

class CameraPermissionState(
    val requestPermissions: () -> Unit,
    val showExplanationDialog: Boolean,
    val isPermanentlyDenied: Boolean,
    val dismissDialog: () -> Unit,
    val openSettings: () -> Unit
)

@Composable
fun rememberCameraPermissionState(
    onPermissionsGranted: () -> Unit,
    onPermissionDenied: () -> Unit = {}
): CameraPermissionState {
    val context = LocalContext.current
    val activity = context as? Activity
    
    var showDialog by remember { mutableStateOf(false) }
    var permanentlyDenied by remember { mutableStateOf(false) }
    
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val allGranted = results.values.all { it }
        if (allGranted) {
            showDialog = false
            onPermissionsGranted()
        } else {
            val hasPermanentDenial = results.filter { !it.value }.any { (permission, _) ->
                activity?.let { CameraPermissionManager.isPermissionPermanentlyDenied(it, permission) } ?: false
            }
            permanentlyDenied = hasPermanentDenial
            showDialog = true
            onPermissionDenied()
        }
    }
    
    val requestTrigger = {
        val hasAll = CameraPermissionManager.hasPermissions(context)
        if (hasAll) {
            onPermissionsGranted()
        } else {
            val hasPermanentDenialBefore = CameraPermissionManager.REQUIRED_PERMISSIONS.any { permission ->
                activity?.let { CameraPermissionManager.isPermissionPermanentlyDenied(it, permission) } ?: false
            }
            permanentlyDenied = hasPermanentDenialBefore
            if (hasPermanentDenialBefore) {
                showDialog = true
            } else {
                launcher.launch(CameraPermissionManager.REQUIRED_PERMISSIONS)
            }
        }
    }
    
    return remember(requestTrigger, showDialog, permanentlyDenied) {
        CameraPermissionState(
            requestPermissions = requestTrigger,
            showExplanationDialog = showDialog,
            isPermanentlyDenied = permanentlyDenied,
            dismissDialog = { showDialog = false },
            openSettings = {
                showDialog = false
                CameraPermissionManager.openAppSettings(context)
            }
        )
    }
}

@Composable
fun CameraPermissionDialog(
    showExplanation: Boolean,
    isPermanentlyDenied: Boolean,
    onDismiss: () -> Unit,
    onRequestPermission: () -> Unit,
    onOpenSettings: () -> Unit
) {
    if (showExplanation) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = {
                Text(
                    text = if (isPermanentlyDenied) "Permiso Requerido" else "Acceso a Cámara y Audio Requerido",
                    style = MaterialTheme.typography.titleMedium
                )
            },
            text = {
                Text(
                    text = if (isPermanentlyDenied) {
                        "Has denegado el acceso a la cámara o micrófono de forma permanente. Por favor, abre la configuración de la aplicación para conceder los permisos correspondientes."
                    } else {
                        "Para usar esta función, Panalink necesita acceder a tu cámara y micrófono. Esto te permitirá tomar fotos y grabar videos reales en tiempo real."
                    }
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (isPermanentlyDenied) {
                            onOpenSettings()
                        } else {
                            onRequestPermission()
                        }
                    }
                ) {
                    Text(if (isPermanentlyDenied) "Abrir Configuración ⚙️" else "Conceder Permisos 📸")
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text("Cancelar")
                }
            }
        )
    }
}
