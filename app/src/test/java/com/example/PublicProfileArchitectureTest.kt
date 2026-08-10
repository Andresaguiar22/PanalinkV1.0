package com.example

import com.example.data.database.PublicProfileDao
import com.example.data.database.PublicProfileEntity
import com.example.data.mapper.PublicProfileMapper
import com.example.data.model.PublicProfile
import com.example.data.model.PublicProfileDto
import com.example.data.repository.PublicProfileFetchResult
import com.example.data.repository.PublicProfileRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

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
        assertEquals("avatars/juan.png", model.avatarUrl)
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
        assertEquals("avatars/carlos.png", model.avatarUrl)
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

            override suspend fun getByIds(ids: List<String>): List<PublicProfileEntity> {
                daoCalls.add(ids)
                return listOf(
                    PublicProfileEntity("usr_1", "User One", "User", "One", null, null)
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
}
