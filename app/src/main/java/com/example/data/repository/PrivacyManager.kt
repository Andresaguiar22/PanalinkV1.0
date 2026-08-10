package com.example.data.repository

import com.example.data.model.UserEntitlementDto
import com.example.data.model.UserPrivacySettingDto
import com.example.data.supabase.SupabaseClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

object PrivacyManager {
    private val repository = PrivacyRepository()
    
    private val _entitlements = MutableStateFlow<List<UserEntitlementDto>>(emptyList())
    val entitlements: StateFlow<List<UserEntitlementDto>> = _entitlements.asStateFlow()

    private val _settings = MutableStateFlow<List<UserPrivacySettingDto>>(emptyList())
    val settings: StateFlow<List<UserPrivacySettingDto>> = _settings.asStateFlow()
    
    fun refresh() {
        GlobalScope.launch {
            val entsResult = repository.getEntitlements()
            if (entsResult.isSuccess) {
                _entitlements.value = entsResult.getOrNull() ?: emptyList()
            }
            
            val settingsResult = repository.getPrivacySettings()
            if (settingsResult.isSuccess) {
                _settings.value = settingsResult.getOrNull() ?: emptyList()
            }
        }
    }
    
    fun clearSession() {
        _entitlements.value = emptyList()
        _settings.value = emptyList()
    }

    fun hasEntitlement(code: String): Boolean {
        val nowIso = SupabaseClient.getNowIsoString()
        return _entitlements.value.any { entitlement ->
            entitlement.featureCode == code &&
            entitlement.enabled &&
            (entitlement.expiresAt == null || entitlement.expiresAt > nowIso)
        }
    }
    
    fun isSettingEnabled(code: String): Boolean {
        val setting = _settings.value.firstOrNull { it.featureCode == code }
        return setting?.value?.get("enabled") as? Boolean ?: false
    }
    
    fun isPremiumFeatureActive(code: String): Boolean {
        return hasEntitlement(code) && isSettingEnabled(code)
    }
}
