package com.example.data.repository

import com.example.data.model.UserEntitlementDto
import com.example.data.model.UserPrivacySettingDto
import com.example.data.supabase.SupabaseClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class PrivacyRepository {
    private val apiService = SupabaseClient.apiService
    
    suspend fun getEntitlements(): Result<List<UserEntitlementDto>> = withContext(Dispatchers.IO) {
        try {
            val token = SupabaseClient.currentToken ?: return@withContext Result.failure(Exception("Not authenticated"))
            val userId = SupabaseClient.currentUser?.id ?: return@withContext Result.failure(Exception("Not authenticated"))
            val service = apiService ?: return@withContext Result.failure(Exception("API Service not initialized"))
            
            val response = service.getUserEntitlements(
                apiKey = SupabaseClient.supabaseAnonKey,
                authHeader = "Bearer $token",
                userIdFilter = "eq.$userId"
            )
            
            if (response.isSuccessful) {
                Result.success(response.body() ?: emptyList())
            } else {
                Result.failure(Exception("Failed to fetch entitlements: ${response.errorBody()?.string()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getPrivacySettings(): Result<List<UserPrivacySettingDto>> = withContext(Dispatchers.IO) {
        try {
            val token = SupabaseClient.currentToken ?: return@withContext Result.failure(Exception("Not authenticated"))
            val userId = SupabaseClient.currentUser?.id ?: return@withContext Result.failure(Exception("Not authenticated"))
            val service = apiService ?: return@withContext Result.failure(Exception("API Service not initialized"))
            
            val response = service.getUserPrivacySettings(
                apiKey = SupabaseClient.supabaseAnonKey,
                authHeader = "Bearer $token",
                userIdFilter = "eq.$userId"
            )
            
            if (response.isSuccessful) {
                Result.success(response.body() ?: emptyList())
            } else {
                Result.failure(Exception("Failed to fetch settings: ${response.errorBody()?.string()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun updatePrivacySetting(featureCode: String, value: Map<String, Any>? = null): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val token = SupabaseClient.currentToken ?: return@withContext Result.failure(Exception("Not authenticated"))
            val userId = SupabaseClient.currentUser?.id ?: return@withContext Result.failure(Exception("Not authenticated"))
            val service = apiService ?: return@withContext Result.failure(Exception("API Service not initialized"))
            
            val setting = UserPrivacySettingDto(
                userId = userId,
                featureCode = featureCode,
                value = value
            )
            
            val response = service.upsertUserPrivacySetting(
                apiKey = SupabaseClient.supabaseAnonKey,
                authHeader = "Bearer $token",
                setting = setting
            )
            
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to update setting: ${response.errorBody()?.string()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
