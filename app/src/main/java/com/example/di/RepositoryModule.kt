package com.example.di

import android.content.Context
import com.example.ui.session.SessionRepository
import com.example.ui.settings.repository.PrivacyRepository
import com.example.ui.settings.repository.SecurityRepository
import com.example.ui.settings.repository.PresenceRepository
import com.example.ui.settings.repository.ActivityRepository
import com.example.ui.settings.repository.ChatsSettingsRepository
import com.example.ui.settings.repository.CustomizationRepository
import com.example.ui.settings.repository.NotificationSettingsRepository

object RepositoryModule {
    fun provideSessionRepository(context: Context): SessionRepository = SessionRepository(context)
    fun providePrivacyRepository(context: Context): PrivacyRepository = PrivacyRepository(context)
    fun provideSecurityRepository(context: Context): SecurityRepository = SecurityRepository(context)
    fun providePresenceRepository(context: Context): PresenceRepository = PresenceRepository(context)
    fun provideActivityRepository(context: Context): ActivityRepository = ActivityRepository(context)
    fun provideChatsSettingsRepository(context: Context): ChatsSettingsRepository = ChatsSettingsRepository(context)
    fun provideCustomizationRepository(context: Context): CustomizationRepository = CustomizationRepository(context)
    fun provideNotificationSettingsRepository(context: Context): NotificationSettingsRepository = NotificationSettingsRepository(context)
}
