package com.example.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.database.PanalinkDatabase
import com.example.data.database.PostEntity
import com.example.data.database.PendingPostEntity
import com.example.data.model.PostDto
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class FeedLocalFirstTest {

    private lateinit var db: PanalinkDatabase

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, PanalinkDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun teardown() {
        db.close()
    }

    @Test
    fun test1_OfflineSincronizacion() = runBlocking {
        val pendingPostDao = db.pendingPostDao()
        val pending = PendingPostEntity(
            id = "pending-1",
            userId = "user-1",
            content = "Hello offline world",
            type = "TEXT",
            mediaUrisJson = "[]",
            privacy = "PUBLIC",
            status = "pending"
        )
        pendingPostDao.insertPost(pending)

        val retrieved = pendingPostDao.getPostById("pending-1")
        assertNotNull(retrieved)
        assertEquals("pending", retrieved?.status)
        assertEquals("Hello offline world", retrieved?.content)
    }

    @Test
    fun test2_FeedRoomPobladoYOffline() = runBlocking {
        val postDao = db.postDao()
        val post = PostEntity(
            id = "post-1",
            authorId = "user-1",
            type = "TEXT",
            content = "Cached post content",
            mediaUrlsJson = "[]",
            audioUrl = null,
            privacy = "PUBLIC",
            likesCount = 5,
            commentsCount = 2,
            currentUserLiked = false,
            createdAt = "2026-08-10T12:00:00Z"
        )
        postDao.upsert(post)

        // Read local posts flow
        val postsFlow = postDao.getAllPostsFlow().first()
        assertEquals(1, postsFlow.size)
        assertEquals("Cached post content", postsFlow[0].content)
    }

    @Test
    fun test3_FeedRoomPobladoYOnlineSWR() = runBlocking {
        val postDao = db.postDao()
        postDao.upsert(PostEntity(
            id = "post-1",
            authorId = "user-1",
            type = "TEXT",
            content = "Old Content",
            mediaUrlsJson = "[]",
            audioUrl = null,
            privacy = "PUBLIC",
            likesCount = 5,
            commentsCount = 2,
            currentUserLiked = false,
            createdAt = "2026-08-10T12:00:00Z"
        ))

        val refreshedPost = PostEntity(
            id = "post-1",
            authorId = "user-1",
            type = "TEXT",
            content = "New Refreshed Content",
            mediaUrlsJson = "[]",
            audioUrl = null,
            privacy = "PUBLIC",
            likesCount = 10,
            commentsCount = 3,
            currentUserLiked = true,
            createdAt = "2026-08-10T12:00:00Z"
        )
        postDao.upsert(refreshedPost)

        val finalPosts = postDao.getAllPostsFlow().first()
        assertEquals(1, finalPosts.size)
        assertEquals("New Refreshed Content", finalPosts[0].content)
        assertEquals(10, finalPosts[0].likesCount)
    }

    @Test
    fun test4_FeedVacioYOnline() = runBlocking {
        val postDao = db.postDao()
        val initialPosts = postDao.getAllPostsFlow().first()
        assertEquals(0, initialPosts.size)

        val networkPosts = listOf(
            PostEntity(
                id = "post-a",
                authorId = "user-a",
                type = "TEXT",
                content = "Content A",
                mediaUrlsJson = "[]",
                audioUrl = null,
                privacy = "PUBLIC",
                likesCount = 1,
                commentsCount = 0,
                currentUserLiked = false,
                createdAt = "2026-08-10T12:00:00Z"
            ),
            PostEntity(
                id = "post-b",
                authorId = "user-b",
                type = "TEXT",
                content = "Content B",
                mediaUrlsJson = "[]",
                audioUrl = null,
                privacy = "PUBLIC",
                likesCount = 10,
                commentsCount = 5,
                currentUserLiked = true,
                createdAt = "2026-08-10T12:01:00Z"
            )
        )
        postDao.upsertAll(networkPosts)

        val finalPosts = postDao.getAllPostsFlow().first()
        assertEquals(2, finalPosts.size)
    }

    @Test
    fun test5_PostActualizacion() = runBlocking {
        val postDao = db.postDao()
        val post = PostEntity(
            id = "post-update-id",
            authorId = "user-1",
            type = "TEXT",
            content = "Original Content",
            mediaUrlsJson = "[]",
            audioUrl = null,
            privacy = "PUBLIC",
            likesCount = 0,
            commentsCount = 0,
            currentUserLiked = false,
            createdAt = "2026-08-10T12:00:00Z"
        )
        postDao.upsert(post)

        val updatedPost = post.copy(content = "Edited Content")
        postDao.upsert(updatedPost)

        val postsFlow = postDao.getAllPostsFlow().first()
        assertEquals(1, postsFlow.size)
        assertEquals("Edited Content", postsFlow[0].content)
    }

    @Test
    fun test6_LikeOptimista() = runBlocking {
        val postDao = db.postDao()
        val post = PostEntity(
            id = "post-like",
            authorId = "user-1",
            type = "TEXT",
            content = "Like test post",
            mediaUrlsJson = "[]",
            audioUrl = null,
            privacy = "PUBLIC",
            likesCount = 4,
            commentsCount = 1,
            currentUserLiked = false,
            createdAt = "2026-08-10T12:00:00Z"
        )
        postDao.upsert(post)

        val likedPost = post.copy(currentUserLiked = true, likesCount = 5)
        postDao.upsert(likedPost)

        var postsFlow = postDao.getAllPostsFlow().first()
        assertEquals(true, postsFlow[0].currentUserLiked)
        assertEquals(5, postsFlow[0].likesCount)

        val unlikedPost = likedPost.copy(currentUserLiked = false, likesCount = 4)
        postDao.upsert(unlikedPost)

        postsFlow = postDao.getAllPostsFlow().first()
        assertEquals(false, postsFlow[0].currentUserLiked)
        assertEquals(4, postsFlow[0].likesCount)
    }

    @Test
    fun test7_ResolucionPerfiles() = runBlocking {
        val post = PostEntity(
            id = "post-profile-test",
            authorId = "user-profile-id",
            type = "TEXT",
            content = "Profile resolution test",
            mediaUrlsJson = "[]",
            audioUrl = null,
            privacy = "PUBLIC",
            likesCount = 0,
            commentsCount = 0,
            currentUserLiked = false,
            createdAt = "2026-08-10T12:00:00Z"
        )
        val dto = post.toPostDto()
        assertEquals("user-profile-id", dto.userId)
    }

    @Test
    fun test8_PostEliminacion() = runBlocking {
        val postDao = db.postDao()
        val post = PostEntity(
            id = "post-delete-me",
            authorId = "user-1",
            type = "TEXT",
            content = "Going to be deleted",
            mediaUrlsJson = "[]",
            audioUrl = null,
            privacy = "PUBLIC",
            likesCount = 0,
            commentsCount = 0,
            currentUserLiked = false,
            createdAt = "2026-08-10T12:00:00Z"
        )
        postDao.upsert(post)

        var postsFlow = postDao.getAllPostsFlow().first()
        assertEquals(1, postsFlow.size)

        postDao.deletePostById("post-delete-me")

        postsFlow = postDao.getAllPostsFlow().first()
        assertEquals(0, postsFlow.size)
    }
}
