package com.example.data.repository

import com.example.data.model.Comment
import kotlinx.coroutines.flow.Flow

interface SocialRepository {
    suspend fun toggleLike(stateId: String, isReel: Boolean = true)
    suspend fun getLikes(stateId: String, isReel: Boolean = true): Flow<Int>
    suspend fun getComments(stateId: String, isReel: Boolean = true): Flow<List<Comment>>
    suspend fun addComment(stateId: String, text: String, parentId: String? = null, isReel: Boolean = true)
    suspend fun getVideoUrl(stateId: String, isReel: Boolean = true): String
}
