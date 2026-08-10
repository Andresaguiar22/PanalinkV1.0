package com.example.identity.bridge

import android.content.Context
import com.example.identity.repository.IdentityRepository
import com.example.identity.storage.AvatarStorageManager
import com.example.identity.storage.CoverStorageManager
import com.example.identity.sync.IdentitySyncManager

/**
 * Bridge between IMCE and legacy code.
 * Ensures the rest of the application can keep working without knowing about IMCE directly,
 * until those parts are migrated.
 */
class LegacyIdentityBridge(private val context: Context) {

    val identityRepository by lazy { IdentityRepository(context) }
    val avatarStorageManager by lazy { AvatarStorageManager(context) }
    val coverStorageManager by lazy { CoverStorageManager(context) }
    val identitySyncManager by lazy { 
        IdentitySyncManager(context, identityRepository, avatarStorageManager, coverStorageManager) 
    }

    // Helper functions for legacy modules can be added here
}
