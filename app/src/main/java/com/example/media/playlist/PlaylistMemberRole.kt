package com.example.media.playlist

/**
 * P6.7.7 - Playlist Member Roles
 * Define los permisos para cada tipo de colaborador en una playlist.
 */
enum class PlaylistMemberRole {
    OWNER,
    EDITOR,
    VIEWER;

    /**
     * ¿Puede el usuario editar la metadata de la playlist (nombre, descripción, portada)?
     */
    fun canEditMetadata(): Boolean = this == OWNER || this == EDITOR

    /**
     * ¿Puede el usuario agregar o eliminar pistas?
     */
    fun canManageTracks(): Boolean = this == OWNER || this == EDITOR

    /**
     * ¿Puede el usuario eliminar la playlist completa?
     */
    fun canDeletePlaylist(): Boolean = this == OWNER

    /**
     * ¿Puede el usuario gestionar colaboradores (agregar/quitar)?
     */
    fun canManageCollaborators(): Boolean = this == OWNER

    /**
     * ¿Puede el usuario compartir la playlist?
     */
    fun canShare(): Boolean = true // Todos pueden compartir el enlace

    /**
     * ¿Puede el usuario generar portadas con IA?
     */
    fun canGenerateAI(): Boolean = this == OWNER || this == EDITOR
}
