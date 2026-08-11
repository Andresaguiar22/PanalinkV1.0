package com.example

import com.example.data.database.PublicProfileDao
import com.example.data.database.PublicProfileEntity
import com.example.data.mapper.PublicProfileMapper
import com.example.data.model.PublicProfile
import com.example.data.model.PublicProfileDto
import com.example.data.repository.PublicProfileFetchResult
import com.example.data.repository.PublicProfileRepository
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.async
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PublicProfileArchitectureTest {

    @Test
    fun testDtoToPublicProfileMapping() {
        val dto = PublicProfileDto(
            id = "user_123",
            displayName = "Juan Perez",
            firstName = "Juan",
            lastName = "Perez",
            avatarUrl = "avatars/juan.png",
            updatedAt = "2026-08-10T10:00:00Z"
        )

        val model = PublicProfileMapper.dtoToModel(dto)

        assertEquals("user_123", model.id)
        assertEquals("Juan Perez", model.displayName)
        assertEquals("Juan", model.firstName)
        assertEquals("Perez", model.lastName)
        assertEquals("https://tivqjfgjdxgzicrridaz.supabase.co/storage/v1/object/public/avatars/juan.png", model.avatarUrl)
        assertEquals("2026-08-10T10:00:00Z", model.updatedAt)
    }

    @Test
    fun testPublicProfileToEntityMapping() {
        val model = PublicProfile(
            id = "user_456",
            displayName = "Maria Gomez",
            firstName = "Maria",
            lastName = "Gomez",
            avatarUrl = "https://cdn.example.com/avatar.jpg",
            updatedAt = "2026-08-10T11:00:00Z"
        )

        val entity = PublicProfileMapper.modelToEntity(model, lastSyncedAt = 123456789L)

        assertEquals("user_456", entity.id)
        assertEquals("Maria Gomez", entity.displayName)
        assertEquals("Maria", entity.firstName)
        assertEquals("Gomez", entity.lastName)
        assertEquals("https://cdn.example.com/avatar.jpg", entity.avatarUrl)
        assertEquals("2026-08-10T11:00:00Z", entity.updatedAt)
        assertEquals(123456789L, entity.lastSyncedAt)
    }

    @Test
    fun testEntityToPublicProfileMapping() {
        val entity = PublicProfileEntity(
            id = "user_789",
            displayName = "Carlos Lopez",
            firstName = "Carlos",
            lastName = "Lopez",
            avatarUrl = "avatars/carlos.png",
            updatedAt = "2026-08-10T12:00:00Z",
            lastSyncedAt = 987654321L
        )

        val model = PublicProfileMapper.entityToModel(entity)

        assertEquals("user_789", model.id)
        assertEquals("Carlos Lopez", model.displayName)
        assertEquals("Carlos", model.firstName)
        assertEquals("Lopez", model.lastName)
        assertEquals("https://tivqjfgjdxgzicrridaz.supabase.co/storage/v1/object/public/avatars/carlos.png", model.avatarUrl)
        assertEquals("2026-08-10T12:00:00Z", model.updatedAt)
    }

    @Test
    fun testNullFieldsMapping() {
        val dtoWithNulls = PublicProfileDto(
            id = "user_nulls",
            displayName = null,
            firstName = null,
            lastName = null,
            avatarUrl = null,
            updatedAt = null
        )

        val model = PublicProfileMapper.dtoToModel(dtoWithNulls)

        assertEquals("user_nulls", model.id)
        assertNull(model.displayName)
        assertNull(model.firstName)
        assertNull(model.lastName)
        assertNull(model.avatarUrl)
        assertNull(model.updatedAt)

        val entity = PublicProfileMapper.modelToEntity(model)
        assertNull(entity.displayName)
        assertNull(entity.avatarUrl)
        assertNull(entity.updatedAt)

        val reconstructedModel = PublicProfileMapper.entityToModel(entity)
        assertNull(reconstructedModel.displayName)
        assertNull(reconstructedModel.avatarUrl)
        assertNull(reconstructedModel.updatedAt)
    }

    @Test
    fun testDuplicateIdsHandlingInRepository() = runBlocking {
        val daoCalls = mutableListOf<List<String>>()

        val fakeDao = object : PublicProfileDao {
            override suspend fun getById(id: String): PublicProfileEntity? = null

            override suspend fun searchLocal(query: String): List<PublicProfileEntity> = emptyList()
            override suspend fun getByIds(ids: List<String>): List<PublicProfileEntity> {
                daoCalls.add(ids)
                return listOf(
                    PublicProfileEntity("usr_1", "User One", "User", "One", null, null),
                    PublicProfileEntity("usr_2", "User Two", "User", "Two", null, null)
                )
            }

            override suspend fun upsert(entity: PublicProfileEntity) {}
            override suspend fun upsertAll(entities: List<PublicProfileEntity>) {}
            override suspend fun delete(id: String) {}
            override suspend fun deleteAll() {}
        }

        val repository = PublicProfileRepository(
            publicProfileDao = fakeDao,
            apiServiceSupplier = { null }
        )

        val inputDuplicateIds = listOf("usr_1", "usr_1", "usr_2", "usr_1", "  usr_2  ")
        val result = repository.getPublicProfiles(inputDuplicateIds, forceRefresh = false)

        assertTrue(result is PublicProfileFetchResult.Success)
        val queriedIdsInDao = daoCalls.firstOrNull() ?: emptyList()

        // Verify IDs were deduplicated and trimmed before querying DAO
        assertEquals(listOf("usr_1", "usr_2"), queriedIdsInDao)
    }

    @Test
    fun testFullProfileResolution() {
        val pub = PublicProfile(
            id = "usr_100",
            displayName = "Ana Martinez",
            firstName = "Ana",
            lastName = "Martinez",
            avatarUrl = "avatars/ana.png"
        )
        val profile = com.example.data.repository.PublicProfileResolver.toProfile(pub)
        assertEquals("usr_100", profile.id)
        assertEquals("Ana Martinez", profile.displayName)
        assertEquals("https://tivqjfgjdxgzicrridaz.supabase.co/storage/v1/object/public/avatars/ana.png", profile.avatarUrl)
    }

    @Test
    fun testProfileWithoutAvatarResolution() {
        val pub = PublicProfile(
            id = "usr_101",
            displayName = "Pedro Sanchez",
            firstName = "Pedro",
            lastName = "Sanchez",
            avatarUrl = null
        )
        val profile = com.example.data.repository.PublicProfileResolver.toProfile(pub)
        assertEquals("usr_101", profile.id)
        assertEquals("Pedro Sanchez", profile.displayName)
        assertNull(profile.avatarUrl)
    }

    @Test
    fun testProfileWithoutDisplayNameResolutionDoesNotExposeUuid() {
        val rawUuid = "123e4567-e89b-12d3-a456-426614174000"
        val pub = PublicProfile(
            id = rawUuid,
            displayName = null,
            firstName = null,
            lastName = null,
            avatarUrl = null
        )
        val resolvedName = com.example.data.repository.PublicProfileResolver.resolveDisplayName(pub, fallbackName = rawUuid, userId = rawUuid)
        assertEquals("", resolvedName)

        val uiFormatted = com.example.data.repository.PublicProfileResolver.formatForUi(resolvedName, "Contacto")
        assertEquals("Contacto", uiFormatted)
    }

    @Test
    fun testGenericPlaceholderFiltering() {
        assertTrue(com.example.data.repository.PublicProfileResolver.isGenericOrUuid("Usuario"))
        assertTrue(com.example.data.repository.PublicProfileResolver.isGenericOrUuid("Usuario Desconocido"))
        assertTrue(com.example.data.repository.PublicProfileResolver.isGenericOrUuid("Pana"))
        assertTrue(com.example.data.repository.PublicProfileResolver.isGenericOrUuid("123e4567-e89b-12d3-a456-426614174000"))
        assertTrue(!com.example.data.repository.PublicProfileResolver.isGenericOrUuid("Carlos Ruiz"))
    }

    @Test
    fun testAvatarUrlResolutionVariants() {
        val cdn = com.example.data.repository.CdnManager
        assertEquals("https://cdn.example.com/pic.jpg", cdn.resolveAvatarUrl("https://cdn.example.com/pic.jpg"))
        assertEquals("content://media/external/images/1", cdn.resolveAvatarUrl("content://media/external/images/1"))
        assertEquals("https://tivqjfgjdxgzicrridaz.supabase.co/storage/v1/object/public/avatars/test.jpg", cdn.resolveAvatarUrl("avatars/test.jpg"))
        assertNull(cdn.resolveAvatarUrl(null))
        assertNull(cdn.resolveAvatarUrl(""))
        assertNull(cdn.resolveAvatarUrl("null"))
        assertNull(cdn.resolveAvatarUrl("undefined"))
    }

    @Test
    fun testAuthErrorWhenNoSessionToken() = runBlocking {
        com.example.data.supabase.SupabaseClient.currentToken = null
        val fakeDao = object : PublicProfileDao {
            override suspend fun getById(id: String): PublicProfileEntity? = null
            override suspend fun searchLocal(query: String): List<PublicProfileEntity> = emptyList()
            override suspend fun getByIds(ids: List<String>): List<PublicProfileEntity> = emptyList()
            override suspend fun upsert(entity: PublicProfileEntity) {}
            override suspend fun upsertAll(entities: List<PublicProfileEntity>) {}
            override suspend fun delete(id: String) {}
            override suspend fun deleteAll() {}
        }

        val repository = PublicProfileRepository(
            publicProfileDao = fakeDao,
            apiServiceSupplier = { null }
        )

        val result = repository.getPublicProfiles(listOf("user_test"), forceRefresh = true)
        assertTrue(result is PublicProfileFetchResult.AuthError)
    }

    @Test
    fun testSearchPublicProfilesWithBlankQueryReturnsEmptyList() = runBlocking {
        val fakeDao = object : PublicProfileDao {
            override suspend fun getById(id: String): PublicProfileEntity? = null
            override suspend fun searchLocal(query: String): List<PublicProfileEntity> = emptyList()
            override suspend fun getByIds(ids: List<String>): List<PublicProfileEntity> = emptyList()
            override suspend fun upsert(entity: PublicProfileEntity) {}
            override suspend fun upsertAll(entities: List<PublicProfileEntity>) {}
            override suspend fun delete(id: String) {}
            override suspend fun deleteAll() {}
        }

        val repository = PublicProfileRepository(
            publicProfileDao = fakeDao,
            apiServiceSupplier = { null }
        )

        val result = repository.searchPublicProfiles("   ")
        assertTrue(result is PublicProfileFetchResult.Success)
        assertEquals(emptyList<PublicProfile>(), (result as PublicProfileFetchResult.Success).data)
    }

    @Test
    fun testCacheFirstRetrievalFromPublicProfileDao() = runBlocking {
        val cachedEntity = PublicProfileEntity(
            id = "cached_101",
            displayName = "Elena Rostova",
            firstName = "Elena",
            lastName = "Rostova",
            avatarUrl = "avatars/elena.jpg",
            updatedAt = "2026-08-10T09:00:00Z"
        )

        val fakeDao = object : PublicProfileDao {
            override suspend fun getById(id: String): PublicProfileEntity? = if (id == "cached_101") cachedEntity else null
            override suspend fun searchLocal(query: String): List<PublicProfileEntity> = emptyList()
            override suspend fun getByIds(ids: List<String>): List<PublicProfileEntity> = if (ids.contains("cached_101")) listOf(cachedEntity) else emptyList()
            override suspend fun upsert(entity: PublicProfileEntity) {}
            override suspend fun upsertAll(entities: List<PublicProfileEntity>) {}
            override suspend fun delete(id: String) {}
            override suspend fun deleteAll() {}
        }

        val repository = PublicProfileRepository(
            publicProfileDao = fakeDao,
            apiServiceSupplier = { null }
        )

        val result = repository.getPublicProfile("cached_101", forceRefresh = false)
        assertTrue(result is PublicProfileFetchResult.Success)
        val profile = (result as PublicProfileFetchResult.Success).data
        assertEquals("cached_101", profile.id)
        assertEquals("Elena Rostova", profile.displayName)
    }

    @Test
    fun testNotFoundForBlankUserId() = runBlocking {
        val fakeDao = object : PublicProfileDao {
            override suspend fun getById(id: String): PublicProfileEntity? = null
            override suspend fun searchLocal(query: String): List<PublicProfileEntity> = emptyList()
            override suspend fun getByIds(ids: List<String>): List<PublicProfileEntity> = emptyList()
            override suspend fun upsert(entity: PublicProfileEntity) {}
            override suspend fun upsertAll(entities: List<PublicProfileEntity>) {}
            override suspend fun delete(id: String) {}
            override suspend fun deleteAll() {}
        }

        val repository = PublicProfileRepository(
            publicProfileDao = fakeDao,
            apiServiceSupplier = { null }
        )

        val result = repository.getPublicProfile("")
        assertTrue(result is PublicProfileFetchResult.NotFound)
    }

    @Test
    fun testConcurrentSingleFlightDeduplication() = runBlocking {
        try {
            val httpCallCount = java.util.concurrent.atomic.AtomicInteger(0)
            com.example.data.supabase.SupabaseClient.currentToken = "valid_test_token"

            val fakeDao = object : PublicProfileDao {
                override suspend fun getById(id: String): PublicProfileEntity? = null
                override suspend fun searchLocal(query: String): List<PublicProfileEntity> = emptyList()
                override suspend fun getByIds(ids: List<String>): List<PublicProfileEntity> = emptyList()
                override suspend fun upsert(entity: PublicProfileEntity) {}
                override suspend fun upsertAll(entities: List<PublicProfileEntity>) {}
                override suspend fun delete(id: String) {}
                override suspend fun deleteAll() {}
            }

            val fakeApiService = java.lang.reflect.Proxy.newProxyInstance(
                com.example.data.supabase.SupabaseApiService::class.java.classLoader,
                arrayOf(com.example.data.supabase.SupabaseApiService::class.java),
                object : java.lang.reflect.InvocationHandler {
                    override fun invoke(proxy: Any, method: java.lang.reflect.Method, args: Array<out Any>?): Any {
                        if (method.name == "getPublicProfiles") {
                            httpCallCount.incrementAndGet()
                            Thread.sleep(100) // Simulate network delay
                            val dto = PublicProfileDto(
                                id = "concurrent_user",
                                displayName = "Concurrent Test",
                                firstName = "Concurrent",
                                lastName = "Test",
                                avatarUrl = "avatar.jpg",
                                updatedAt = "2026-08-10T12:00:00Z"
                            )
                            return retrofit2.Response.success(listOf(dto))
                        } else if (method.name == "getProfiles") {
                            throw IllegalStateException("getProfiles should NOT be called directly for third-party profiles in Realtime!")
                        } else {
                            throw UnsupportedOperationException("Method ${method.name} not mocked")
                        }
                    }
                }
            ) as com.example.data.supabase.SupabaseApiService

            val repository = PublicProfileRepository(
                publicProfileDao = fakeDao,
                apiServiceSupplier = { fakeApiService }
            )

            val deferreds = (1..10).map {
                this@runBlocking.async(kotlinx.coroutines.Dispatchers.IO) {
                    repository.getPublicProfile("concurrent_user", forceRefresh = true)
                }
            }

            val results = deferreds.map { it.await() }

            assertEquals(1, httpCallCount.get())
            assertEquals(10, results.size)
            results.forEach { res ->
                assertTrue(res is PublicProfileFetchResult.Success<*>)
                val successRes = res as PublicProfileFetchResult.Success<PublicProfile>
                assertEquals("concurrent_user", successRes.data.id)
                assertEquals("Concurrent Test", successRes.data.displayName)
            }
        } finally {
            com.example.data.supabase.SupabaseClient.currentToken = null
        }
    }

    @Test
    fun testBatchRequestFiresExactlyOneRemoteCall() = runBlocking {
        val httpCallCount = java.util.concurrent.atomic.AtomicInteger(0)
        com.example.data.supabase.SupabaseClient.currentToken = "valid_test_token"
        
        val fakeDao = object : PublicProfileDao {
            override suspend fun getById(id: String): PublicProfileEntity? = null
            override suspend fun searchLocal(query: String): List<PublicProfileEntity> = emptyList()
            override suspend fun getByIds(ids: List<String>): List<PublicProfileEntity> = emptyList()
            override suspend fun upsert(entity: PublicProfileEntity) {}
            override suspend fun upsertAll(entities: List<PublicProfileEntity>) {}
            override suspend fun delete(id: String) {}
            override suspend fun deleteAll() {}
        }
        
        val fakeApiService = java.lang.reflect.Proxy.newProxyInstance(
            com.example.data.supabase.SupabaseApiService::class.java.classLoader,
            arrayOf(com.example.data.supabase.SupabaseApiService::class.java),
            object : java.lang.reflect.InvocationHandler {
                override fun invoke(proxy: Any, method: java.lang.reflect.Method, args: Array<out Any>?): Any {
                    if (method.name == "getPublicProfiles") {
                        httpCallCount.incrementAndGet()
                        val dtos = (1..10).map { i ->
                            PublicProfileDto("user_$i", "User $i", "User", "$i", null, null)
                        }
                        return retrofit2.Response.success(dtos)
                    }
                    throw UnsupportedOperationException()
                }
            }
        ) as com.example.data.supabase.SupabaseApiService

        val repository = PublicProfileRepository(fakeDao, { fakeApiService })
        val ids = (1..10).map { "user_$it" }
        val result = repository.getPublicProfiles(ids, forceRefresh = true)
        
        assertEquals(1, httpCallCount.get())
        assertTrue(result is PublicProfileFetchResult.Success)
        val dataMap = (result as PublicProfileFetchResult.Success).data
        assertEquals(10, dataMap.size)
        ids.forEach { id ->
            val userResult = dataMap[id]
            assertTrue(userResult is PublicProfileFetchResult.Success)
            assertEquals(id, (userResult as PublicProfileFetchResult.Success).data.id)
        }
    }

    @Test
    fun testPartialBatchRequestSuccessAndNotFound() = runBlocking {
        val httpCallCount = java.util.concurrent.atomic.AtomicInteger(0)
        com.example.data.supabase.SupabaseClient.currentToken = "valid_test_token"
        
        val fakeDao = object : PublicProfileDao {
            override suspend fun getById(id: String): PublicProfileEntity? = null
            override suspend fun searchLocal(query: String): List<PublicProfileEntity> = emptyList()
            override suspend fun getByIds(ids: List<String>): List<PublicProfileEntity> = emptyList()
            override suspend fun upsert(entity: PublicProfileEntity) {}
            override suspend fun upsertAll(entities: List<PublicProfileEntity>) {}
            override suspend fun delete(id: String) {}
            override suspend fun deleteAll() {}
        }
        
        val fakeApiService = java.lang.reflect.Proxy.newProxyInstance(
            com.example.data.supabase.SupabaseApiService::class.java.classLoader,
            arrayOf(com.example.data.supabase.SupabaseApiService::class.java),
            object : java.lang.reflect.InvocationHandler {
                override fun invoke(proxy: Any, method: java.lang.reflect.Method, args: Array<out Any>?): Any {
                    if (method.name == "getPublicProfiles") {
                        httpCallCount.incrementAndGet()
                        val dtos = (1..8).map { i ->
                            PublicProfileDto("user_$i", "User $i", "User", "$i", null, null)
                        }
                        return retrofit2.Response.success(dtos)
                    }
                    throw UnsupportedOperationException()
                }
            }
        ) as com.example.data.supabase.SupabaseApiService

        val repository = PublicProfileRepository(fakeDao, { fakeApiService })
        val ids = (1..10).map { "user_$it" }
        val result = repository.getPublicProfiles(ids, forceRefresh = true)
        
        assertEquals(1, httpCallCount.get())
        assertTrue(result is PublicProfileFetchResult.Success)
        val dataMap = (result as PublicProfileFetchResult.Success).data
        assertEquals(10, dataMap.size)
        
        (1..8).forEach { i ->
            val userResult = dataMap["user_$i"]
            assertTrue(userResult is PublicProfileFetchResult.Success)
            assertEquals("user_$i", (userResult as PublicProfileFetchResult.Success).data.id)
        }
        
        (9..10).forEach { i ->
            val userResult = dataMap["user_$i"]
            assertTrue(userResult is PublicProfileFetchResult.NotFound)
        }
    }

    @Test
    fun testNetworkErrorPropagatesOnTimeout() = runBlocking {
        com.example.data.supabase.SupabaseClient.currentToken = "valid_test_token"
        
        val fakeDao = object : PublicProfileDao {
            override suspend fun getById(id: String): PublicProfileEntity? = null
            override suspend fun searchLocal(query: String): List<PublicProfileEntity> = emptyList()
            override suspend fun getByIds(ids: List<String>): List<PublicProfileEntity> = emptyList()
            override suspend fun upsert(entity: PublicProfileEntity) {}
            override suspend fun upsertAll(entities: List<PublicProfileEntity>) {}
            override suspend fun delete(id: String) {}
            override suspend fun deleteAll() {}
        }
        
        val fakeApiService = java.lang.reflect.Proxy.newProxyInstance(
            com.example.data.supabase.SupabaseApiService::class.java.classLoader,
            arrayOf(com.example.data.supabase.SupabaseApiService::class.java),
            object : java.lang.reflect.InvocationHandler {
                override fun invoke(proxy: Any, method: java.lang.reflect.Method, args: Array<out Any>?): Any {
                    if (method.name == "getPublicProfiles") {
                        throw java.io.IOException("Connection timeout")
                    }
                    throw UnsupportedOperationException()
                }
            }
        ) as com.example.data.supabase.SupabaseApiService

        val repository = PublicProfileRepository(fakeDao, { fakeApiService })
        val result = repository.getPublicProfiles(listOf("user_1"), forceRefresh = true)
        
        assertTrue(result is PublicProfileFetchResult.NetworkError)
        val error = result as PublicProfileFetchResult.NetworkError
        val actualException = if (error.exception is java.lang.reflect.UndeclaredThrowableException) error.exception.cause else error.exception
        assertTrue(actualException is java.io.IOException)
        assertEquals("Connection timeout", actualException?.message)
    }

    @Test
    fun testAuthErrorOnUnauthorizedStatusCode() = runBlocking {
        com.example.data.supabase.SupabaseClient.currentToken = "valid_test_token"
        
        val fakeDao = object : PublicProfileDao {
            override suspend fun getById(id: String): PublicProfileEntity? = null
            override suspend fun searchLocal(query: String): List<PublicProfileEntity> = emptyList()
            override suspend fun getByIds(ids: List<String>): List<PublicProfileEntity> = emptyList()
            override suspend fun upsert(entity: PublicProfileEntity) {}
            override suspend fun upsertAll(entities: List<PublicProfileEntity>) {}
            override suspend fun delete(id: String) {}
            override suspend fun deleteAll() {}
        }
        
        val fakeApiService = java.lang.reflect.Proxy.newProxyInstance(
            com.example.data.supabase.SupabaseApiService::class.java.classLoader,
            arrayOf(com.example.data.supabase.SupabaseApiService::class.java),
            object : java.lang.reflect.InvocationHandler {
                override fun invoke(proxy: Any, method: java.lang.reflect.Method, args: Array<out Any>?): Any {
                    if (method.name == "getPublicProfiles") {
                        val responseBody = okhttp3.ResponseBody.create(
                            "application/json".toMediaTypeOrNull(),
                            "{\"message\":\"Invalid JWT\"}"
                        )
                        return retrofit2.Response.error<List<PublicProfileDto>>(401, responseBody)
                    }
                    throw UnsupportedOperationException()
                }
            }
        ) as com.example.data.supabase.SupabaseApiService

        val repository = PublicProfileRepository(fakeDao, { fakeApiService })
        val result = repository.getPublicProfiles(listOf("user_1"), forceRefresh = true)
        
        assertTrue(result is PublicProfileFetchResult.AuthError)
        val authError = result as PublicProfileFetchResult.AuthError
        assertEquals(401, authError.code)
    }

    @Test
    fun testCacheRetrievalWhenNoNetwork() = runBlocking {
        val cachedEntity = PublicProfileEntity(
            id = "cached_999",
            displayName = "Offline Cache User",
            firstName = "Offline",
            lastName = "Cache",
            avatarUrl = "avatars/cached.jpg",
            updatedAt = "2026-08-10T09:00:00Z"
        )
        
        val fakeDao = object : PublicProfileDao {
            override suspend fun getById(id: String): PublicProfileEntity? = cachedEntity
            override suspend fun searchLocal(query: String): List<PublicProfileEntity> = emptyList()
            override suspend fun getByIds(ids: List<String>): List<PublicProfileEntity> = listOf(cachedEntity)
            override suspend fun upsert(entity: PublicProfileEntity) {}
            override suspend fun upsertAll(entities: List<PublicProfileEntity>) {}
            override suspend fun delete(id: String) {}
            override suspend fun deleteAll() {}
        }
        
        val repository = PublicProfileRepository(fakeDao, { null })
        val result = repository.getPublicProfiles(listOf("cached_999"), forceRefresh = false)
        
        assertTrue(result is PublicProfileFetchResult.Success)
        val dataMap = (result as PublicProfileFetchResult.Success).data
        val userResult = dataMap["cached_999"]
        assertTrue(userResult is PublicProfileFetchResult.Success)
        val profile = (userResult as PublicProfileFetchResult.Success).data
        assertEquals("cached_999", profile.id)
        assertEquals("Offline Cache User", profile.displayName)
    }

    @Test
    fun testChatViewModelDoesNotUseProfileDaoOrProfilesRepository() {
        val clazz = com.example.ui.viewmodel.ChatViewModel::class.java
        val fields = clazz.declaredFields
        for (field in fields) {
            val type = field.type
            assertTrue(
                "ChatViewModel should not use ProfilesRepository: field name ${field.name}",
                !type.name.contains("ProfilesRepository")
            )
            assertTrue(
                "ChatViewModel should not use ProfileDao: field name ${field.name}",
                !type.name.contains("ProfileDao")
            )
        }
    }
}
