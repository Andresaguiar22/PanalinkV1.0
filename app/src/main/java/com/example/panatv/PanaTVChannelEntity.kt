package com.example.panatv

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "panatv_channels")
data class PanaTVChannelEntity(
    @PrimaryKey val id: String,
    val name: String,
    val streamUrl: String,
    val logoUrl: String,
    val country: String,
    val userAgent: String?,
    val referrer: String?
)
