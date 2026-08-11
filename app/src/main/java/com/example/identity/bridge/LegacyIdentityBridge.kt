package com.example.identity.bridge

import android.content.Context
import androidx.annotation.Keep
import com.example.identity.repository.IdentityRepository

@Keep
class LegacyIdentityBridge(context: Context) {
    val identityRepository = IdentityRepository(context)
}
