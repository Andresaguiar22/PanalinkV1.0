package com.example.notification.engine.storage.converter

import androidx.room.TypeConverter
import com.example.notification.engine.model.InterruptivenessLevel
import com.example.notification.engine.model.NotificationDomain
import com.example.notification.engine.model.NotificationPriority
import com.example.notification.engine.model.NotificationTypeV2

class NotificationConverters {

    @TypeConverter
    fun fromDomain(domain: NotificationDomain): String = domain.name

    @TypeConverter
    fun toDomain(value: String): NotificationDomain {
        return try {
            NotificationDomain.valueOf(value)
        } catch (e: Exception) {
            NotificationDomain.SYSTEM
        }
    }

    @TypeConverter
    fun fromType(type: NotificationTypeV2): String = type.name

    @TypeConverter
    fun toType(value: String): NotificationTypeV2 {
        return NotificationTypeV2.fromString(value)
    }

    @TypeConverter
    fun fromPriority(priority: NotificationPriority): String = priority.name

    @TypeConverter
    fun toPriority(value: String): NotificationPriority {
        return try {
            NotificationPriority.valueOf(value)
        } catch (e: Exception) {
            NotificationPriority.NORMAL
        }
    }

    @TypeConverter
    fun fromInterruptiveness(level: InterruptivenessLevel): String = level.name

    @TypeConverter
    fun toInterruptiveness(value: String): InterruptivenessLevel {
        return try {
            InterruptivenessLevel.valueOf(value)
        } catch (e: Exception) {
            InterruptivenessLevel.STATUS_BAR_ONLY
        }
    }
}
