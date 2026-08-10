package com.example.data.repository

import com.example.data.model.Profile
import com.example.data.model.PublicProfile

/**
 * Centralized resolver for public user identities.
 * Ensures consistent display name and avatar resolution across all features (Contacts, Chats, Feed, Stories, etc.)
 * Strict Policy: Never exposes raw UUIDs or fabricates persistent fake names ("Usuario", "Pana", "Usuario Desconocido") into DB/Cache.
 */
object PublicProfileResolver {

    private val UUID_REGEX = Regex("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$")

    /**
     * Checks if a string is a raw UUID or a generic fake name placeholder.
     */
    fun isGenericOrUuid(input: String?): Boolean {
        if (input.isNullOrBlank()) return true
        val trimmed = input.trim()
        if (UUID_REGEX.matches(trimmed)) return true

        val lower = trimmed.lowercase()
        return lower == "usuario" ||
               lower == "usuario desconocido" ||
               lower == "unknown user" ||
               lower == "pana" ||
               lower == "pana de panalink" ||
               lower == "usuario pana" ||
               lower == "pana de la comunidad"
    }

    /**
     * Resolves a clean display name given a [PublicProfile] or optional fallback name or raw userId.
     * Guaranteed NOT to return a raw UUID or generic fake name.
     */
    fun resolveDisplayName(
        publicProfile: PublicProfile?,
        fallbackName: String? = null,
        userId: String? = null
    ): String {
        // 1. Check PublicProfile.displayName
        if (!publicProfile?.displayName.isNullOrBlank()) {
            val name = publicProfile!!.displayName!!.trim()
            if (!isGenericOrUuid(name)) return name
        }

        // 2. Check PublicProfile.firstName / lastName
        if (!publicProfile?.firstName.isNullOrBlank()) {
            val full = listOfNotNull(publicProfile!!.firstName, publicProfile.lastName)
                .filter { it.isNotBlank() }
                .joinToString(" ").trim()
            if (full.isNotBlank() && !isGenericOrUuid(full)) return full
        }

        // 3. Check fallbackName if provided
        if (!fallbackName.isNullOrBlank()) {
            val cleanFallback = fallbackName.trim()
            if (!isGenericOrUuid(cleanFallback)) return cleanFallback
        }

        // 4. Return empty string as data level display name if no clean name is available
        return ""
    }

    /**
     * UI helper: formats a display name for visual presentation.
     * If the resolved name is blank, provides a clean UI fallback (e.g. "Contacto" or "Pana")
     * without persisting this fallback into Room or Supabase.
     */
    fun formatForUi(displayName: String?, defaultPlaceholder: String = "Contacto"): String {
        return if (!isGenericOrUuid(displayName)) {
            displayName!!.trim()
        } else {
            defaultPlaceholder
        }
    }

    /**
     * Converts a [PublicProfile] to a clean [Profile] domain model.
     */
    fun toProfile(pub: PublicProfile, fallbackName: String? = null): Profile {
        val resolvedName = resolveDisplayName(pub, fallbackName, pub.id)
        val resolvedAvatar = CdnManager.resolveAvatarUrl(pub.avatarUrl)

        return Profile(
            id = pub.id,
            displayName = resolvedName,
            firstName = pub.firstName,
            lastName = pub.lastName,
            avatarUrl = resolvedAvatar
        )
    }
}
