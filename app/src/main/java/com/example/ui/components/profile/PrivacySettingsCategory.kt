package com.example.ui.components.profile

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.PrivacySettingSwitch
import com.example.ui.viewmodel.ProfileViewModel

@Composable
fun PrivacySettingsCategory(viewModel: ProfileViewModel) {
    val entitlements by viewModel.entitlements.collectAsState()
    val privacySettings by viewModel.privacySettings.collectAsState()

    fun hasEntitlement(code: String): Boolean {
        return entitlements.any { it.featureCode == code && it.enabled }
    }

    fun isSettingEnabled(code: String): Boolean {
        val setting = privacySettings.firstOrNull { it.featureCode == code }
        return setting?.value?.get("enabled") as? Boolean ?: false
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Funciones Premium de Privacidad",
            color = Color(0xFF25D366),
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp
        )

        // --- General Privacy ---
        PrivacySettingSwitch(
            title = "Modo Fantasma 👻",
            description = "Oculta tu actividad general, última vez y estado en línea de forma invisible.",
            isPremium = true,
            hasEntitlement = hasEntitlement("ghost_mode"),
            isEnabled = isSettingEnabled("ghost_mode"),
            onCheckedChange = { viewModel.togglePrivacySetting("ghost_mode", it) }
        )

        PrivacySettingSwitch(
            title = "Siempre en línea 🟢",
            description = "Muestra tu estado siempre como 'en línea' aunque la app esté cerrada.",
            isPremium = true,
            hasEntitlement = hasEntitlement("always_online"),
            isEnabled = isSettingEnabled("always_online"),
            onCheckedChange = { viewModel.togglePrivacySetting("always_online", it) }
        )
        
        PrivacySettingSwitch(
            title = "Congelar última vez ❄️",
            description = "Pausa la actualización de tu última hora de conexión.",
            isPremium = true,
            hasEntitlement = hasEntitlement("freeze_last_seen"),
            isEnabled = isSettingEnabled("freeze_last_seen"),
            onCheckedChange = { viewModel.togglePrivacySetting("freeze_last_seen", it) }
        )

        Divider(color = Color(0xFF2A3942))

        // --- Typing / Recording ---
        PrivacySettingSwitch(
            title = "Ocultar 'escribiendo...'",
            description = "Los demás no verán cuando estés escribiendo un mensaje.",
            isPremium = true,
            hasEntitlement = hasEntitlement("hide_typing"),
            isEnabled = isSettingEnabled("hide_typing"),
            onCheckedChange = { viewModel.togglePrivacySetting("hide_typing", it) }
        )

        PrivacySettingSwitch(
            title = "Ocultar 'grabando audio...'",
            description = "Los demás no verán cuando estés grabando una nota de voz.",
            isPremium = true,
            hasEntitlement = hasEntitlement("hide_recording"),
            isEnabled = isSettingEnabled("hide_recording"),
            onCheckedChange = { viewModel.togglePrivacySetting("hide_recording", it) }
        )

        Divider(color = Color(0xFF2A3942))

        // --- Receipts ---
        PrivacySettingSwitch(
            title = "Ocultar ticks azules (leído) 🔵🔵",
            description = "Nadie sabrá que leíste sus mensajes en chats individuales.",
            isPremium = true,
            hasEntitlement = hasEntitlement("hide_blue_ticks"),
            isEnabled = isSettingEnabled("hide_blue_ticks"),
            onCheckedChange = { viewModel.togglePrivacySetting("hide_blue_ticks", it) }
        )

        PrivacySettingSwitch(
            title = "Ocultar ticks azules en Grupos",
            description = "Oculta tu confirmación de lectura en los grupos.",
            isPremium = true,
            hasEntitlement = hasEntitlement("hide_group_blue_ticks"),
            isEnabled = isSettingEnabled("hide_group_blue_ticks"),
            onCheckedChange = { viewModel.togglePrivacySetting("hide_group_blue_ticks", it) }
        )

        PrivacySettingSwitch(
            title = "Ocultar doble tick recibido ✔️✔️",
            description = "Oculta el segundo tick, pareciendo que no has recibido el mensaje.",
            isPremium = true,
            hasEntitlement = hasEntitlement("hide_double_ticks_received"),
            isEnabled = isSettingEnabled("hide_double_ticks_received"),
            onCheckedChange = { viewModel.togglePrivacySetting("hide_double_ticks_received", it) }
        )

        PrivacySettingSwitch(
            title = "Enviar tick azul al responder",
            description = "Muestra que lo leíste solo después de responder.",
            isPremium = true,
            hasEntitlement = hasEntitlement("send_blue_tick_on_reply"),
            isEnabled = isSettingEnabled("send_blue_tick_on_reply"),
            onCheckedChange = { viewModel.togglePrivacySetting("send_blue_tick_on_reply", it) }
        )

        PrivacySettingSwitch(
            title = "Botón para enviar tick azul",
            description = "Agrega un botón en el chat para enviar manualmente el tick azul.",
            isPremium = false,
            hasEntitlement = true,
            isEnabled = isSettingEnabled("show_send_blue_tick_button"),
            onCheckedChange = { viewModel.togglePrivacySetting("show_send_blue_tick_button", it) }
        )

        Divider(color = Color(0xFF2A3942))

        // --- Anti Delete ---
        PrivacySettingSwitch(
            title = "Anti-Eliminar Mensajes 🚫🗑️",
            description = "Evita que otras personas eliminen mensajes para ti.",
            isPremium = true,
            hasEntitlement = hasEntitlement("anti_message_delete"),
            isEnabled = isSettingEnabled("anti_message_delete"),
            onCheckedChange = { viewModel.togglePrivacySetting("anti_message_delete", it) }
        )

        PrivacySettingSwitch(
            title = "Anti-Eliminar Estados",
            description = "Podrás ver los estados de tus contactos incluso si los eliminan.",
            isPremium = true,
            hasEntitlement = hasEntitlement("anti_state_delete"),
            isEnabled = isSettingEnabled("anti_state_delete"),
            onCheckedChange = { viewModel.togglePrivacySetting("anti_state_delete", it) }
        )

        PrivacySettingSwitch(
            title = "Anti-Desaparición Temporales ⏳",
            description = "Conserva los mensajes configurados para desaparecer.",
            isPremium = true,
            hasEntitlement = hasEntitlement("anti_temp_message_disappearance"),
            isEnabled = isSettingEnabled("anti_temp_message_disappearance"),
            onCheckedChange = { viewModel.togglePrivacySetting("anti_temp_message_disappearance", it) }
        )

        Divider(color = Color(0xFF2A3942))

        // --- View Once & Status ---
        PrivacySettingSwitch(
            title = "Ocultar vista en estados 👁️",
            description = "Mira los estados de tus panas sin que ellos se enteren.",
            isPremium = true,
            hasEntitlement = hasEntitlement("hide_status_view"),
            isEnabled = isSettingEnabled("hide_status_view"),
            onCheckedChange = { viewModel.togglePrivacySetting("hide_status_view", it) }
        )
        
        PrivacySettingSwitch(
            title = "Ocultar visto 'Ver una vez'",
            description = "El remitente no sabrá que abriste una foto de 'Ver una vez'.",
            isPremium = true,
            hasEntitlement = hasEntitlement("hide_view_once"),
            isEnabled = isSettingEnabled("hide_view_once"),
            onCheckedChange = { viewModel.togglePrivacySetting("hide_view_once", it) }
        )

        PrivacySettingSwitch(
            title = "Ver una vez ilimitadamente ♾️",
            description = "Abre fotos o videos de 'Ver una vez' todas las veces que quieras.",
            isPremium = true,
            hasEntitlement = hasEntitlement("view_once_unlimited"),
            isEnabled = isSettingEnabled("view_once_unlimited"),
            onCheckedChange = { viewModel.togglePrivacySetting("view_once_unlimited", it) }
        )
        
        Divider(color = Color(0xFF2A3942))

        PrivacySettingSwitch(
            title = "Bloquear llamadas 📞",
            description = "Bloquea o filtra llamadas entrantes para mayor privacidad.",
            isPremium = true,
            hasEntitlement = hasEntitlement("block_calls"),
            isEnabled = isSettingEnabled("block_calls"),
            onCheckedChange = { viewModel.togglePrivacySetting("block_calls", it) }
        )

        PrivacySettingSwitch(
            title = "Chats Bloqueados Mejorados 🔐",
            description = "Oculta contactos y notificaciones de chats bloqueados por completo.",
            isPremium = true,
            hasEntitlement = hasEntitlement("blocked_chats_enhanced"),
            isEnabled = isSettingEnabled("blocked_chats_enhanced"),
            onCheckedChange = { viewModel.togglePrivacySetting("blocked_chats_enhanced", it) }
        )
    }
}
