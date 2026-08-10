package com.example.call

import org.webrtc.PeerConnection

/**
 * IceConfig represents a configuration structure for ICE Servers.
 */
data class IceConfig(
    val urls: String,
    val username: String? = null,
    val credential: String? = null
)

/**
 * IceServerProvider provides a list of STUN/TURN servers to be used by WebRTC client
 * to gather ICE Candidates and establish the P2P connection.
 */
object IceServerProvider {
    private var customIceServers: List<IceConfig>? = null

    /**
     * Allows dynamic injection of ice servers (e.g. loaded from Supabase or an Edge function).
     */
    fun setCustomIceServers(configs: List<IceConfig>) {
        customIceServers = configs
    }

    /**
     * Clears custom configuration to fallback to default servers.
     */
    fun clearCustomIceServers() {
        customIceServers = null
    }

    fun getIceServers(): List<PeerConnection.IceServer> {
        val customs = customIceServers
        if (!customs.isNullOrEmpty()) {
            return customs.map { config ->
                val builder = PeerConnection.IceServer.builder(config.urls)
                if (!config.username.isNullOrEmpty()) {
                    builder.setUsername(config.username)
                }
                if (!config.credential.isNullOrEmpty()) {
                    builder.setPassword(config.credential)
                }
                builder.createIceServer()
            }
        }

        // Fallback default servers (STUN + TURN UDP/TCP over strict ports)
        return listOf(
            // 1. STUN Público de Google (Para conexiones directas Wi-Fi)
            PeerConnection.IceServer.builder("stun:stun.l.google.com:19302")
                .createIceServer(),

            // 2. OpenRelay TURN sobre UDP (Puerto 80)
            PeerConnection.IceServer.builder("turn:openrelay.metered.ca:80")
                .setUsername("openrelayproject")
                .setPassword("openrelayproject")
                .createIceServer(),

            // 3. OpenRelay TURN sobre TCP/TLS (Puerto 443 - Para saltar firewalls estrictos en 4G/5G)
            PeerConnection.IceServer.builder("turn:openrelay.metered.ca:443?transport=tcp")
                .setUsername("openrelayproject")
                .setPassword("openrelayproject")
                .createIceServer()
        )
    }
}
