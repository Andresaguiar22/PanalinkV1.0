package com.example.notification.engine.badge

import androidx.annotation.Keep
import kotlinx.coroutines.flow.Flow

@Keep
interface BadgeSource {
    val category: BadgeCategory
    fun observeCount(): Flow<Int>
}
