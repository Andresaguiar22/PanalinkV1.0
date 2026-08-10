package com.example.ui.settings.providers

import android.content.Context
import com.example.data.supabase.SupabaseClient
import com.example.ui.settings.models.ControlCenterUiState
import com.example.ui.settings.repository.ActivityRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

interface DashboardSummaryProvider {
    fun getDashboardSummary(): Flow<ControlCenterUiState>
}

class RealDashboardSummaryProvider(private val context: Context) : DashboardSummaryProvider {
    private val activityRepo = ActivityRepository(context)

    override fun getDashboardSummary(): Flow<ControlCenterUiState> = flow {
        val user = SupabaseClient.currentUser
        val name = user?.email?.substringBefore("@") ?: "Usuario Pana"
        val handle = if (user?.email != null) "@${user.email!!.substringBefore("@")}" else "@usuario_pana"
        val activity = activityRepo.loadActivitySummary()

        emit(
            ControlCenterUiState(
                userName = name,
                userHandle = handle,
                presenceStatus = if (activity.isOnline) "En línea" else "Desconectado",
                activeDevicesCount = activity.activeDevices.size,
                isOnline = activity.isOnline,
                storageUsedSummary = activity.storageUsed,
                appVersion = "2.0.0",
                profileSummary = "Perfil de usuario PanaLink",
                presenceSummary = if (activity.isOnline) "En línea • Conectado" else "Desconectado",
                privacySummary = "Privacidad protegida",
                securitySummary = "Seguridad activa",
                chatsSummary = "${activity.messagesCount} mensajes locales",
                notificationsSummary = "Notificaciones activas",
                customizationSummary = "Tema Oscuro PanaLink",
                storageSummary = "${activity.storageUsed} utilizados localmente",
                activitySummary = "Diagnóstico: ${activity.connectionStatus}"
            )
        )
    }
}

class MockDashboardSummaryProvider : DashboardSummaryProvider {
    override fun getDashboardSummary(): Flow<ControlCenterUiState> = flow {
        emit(
            ControlCenterUiState(
                userName = "Usuario Pana",
                userHandle = "@usuario_pana",
                presenceStatus = "En línea",
                activeDevicesCount = 1,
                storageSummary = "0 MB utilizados",
                activitySummary = "Diagnóstico del sistema"
            )
        )
    }
}
