package com.example.media.model

sealed class MediaResource {
    data class Local(val path: String) : MediaResource()
    data class Remote(val url: String) : MediaResource()
    object Loading : MediaResource()
    object Missing : MediaResource()
}
