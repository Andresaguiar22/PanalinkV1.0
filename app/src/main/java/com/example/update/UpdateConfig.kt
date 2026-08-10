package com.example.update

import com.example.BuildConfig

interface UpdateDistributionConfig {
    val manifestUrl: String
    val providerType: UpdateConfig.ProviderType
    val isProduction: Boolean
    val githubOwner: String
    val githubRepo: String
    val githubReleaseTag: String
}

data class UpdateConfig(
    override val manifestUrl: String,
    override val providerType: ProviderType = ProviderType.CUSTOM_SERVER,
    override val isProduction: Boolean = !BuildConfig.DEBUG,
    override val githubOwner: String = "",
    override val githubRepo: String = "",
    override val githubReleaseTag: String = ""
) : UpdateDistributionConfig {
    enum class ProviderType {
        CUSTOM_SERVER,
        GITHUB_RELEASES
    }
}
