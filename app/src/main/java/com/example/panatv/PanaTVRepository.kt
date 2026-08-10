package com.example.panatv

import android.content.Context
import android.util.Log
import com.squareup.moshi.Json
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.flow.Flow
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class IptvChannel(
    @Json(name = "id") val id: String? = null,
    @Json(name = "name") val name: String? = null,
    @Json(name = "country") val country: String? = null,
    @Json(name = "languages") val languages: List<String>? = null,
    @Json(name = "logo") val logo: String? = null
)

data class IptvStream(
    @Json(name = "channel") val channel: String? = null,
    @Json(name = "url") val url: String? = null,
    @Json(name = "http_referrer") val http_referrer: String? = null,
    @Json(name = "user_agent") val user_agent: String? = null
)

data class IptvBlocklist(
    @Json(name = "channel") val channel: String? = null,
    @Json(name = "reason") val reason: String? = null
)

data class IptvLogo(
    @Json(name = "channel") val channel: String? = null,
    @Json(name = "url") val url: String? = null
)

interface PanaTVApiService {
    @GET("api/channels.json")
    suspend fun getChannels(): retrofit2.Response<List<IptvChannel>>

    @GET("api/streams.json")
    suspend fun getStreams(): retrofit2.Response<List<IptvStream>>

    @GET("api/blocklist.json")
    suspend fun getBlocklist(): retrofit2.Response<List<IptvBlocklist>>

    @GET("api/logos.json")
    suspend fun getLogos(): retrofit2.Response<List<IptvLogo>>
}

class PanaTVRepository(private val context: Context) {
    private val database = PanaTVDatabase.getDatabase(context)
    private val dao = database.channelDao()
    private val favDao = database.favoriteDao()
    private val prefs = context.getSharedPreferences("panatv_prefs", Context.MODE_PRIVATE)
    private val TAG = "PanaTVRepository"

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val apiService: PanaTVApiService by lazy {
        Retrofit.Builder()
            .baseUrl("https://iptv-org.github.io/")
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(PanaTVApiService::class.java)
    }

    fun getChannels(query: String = "", country: String = ""): Flow<List<PanaTVChannelEntity>> {
        return dao.searchChannels(query, country)
    }
    
    fun getFavorites(): Flow<List<PanaTVFavoriteEntity>> = favDao.getFavorites()
    
    suspend fun addFavorite(id: String) = favDao.addFavorite(PanaTVFavoriteEntity(id))
    
    suspend fun removeFavorite(id: String) = favDao.removeFavorite(id)

    suspend fun fetchChannelsIfNeeded(onDebug: (String) -> Unit = {}) {
        val lastSync = prefs.getLong("last_sync_time", 0)
        val currentTime = System.currentTimeMillis()
        val oneDayInMillis = 24 * 60 * 60 * 1000L
        val channelCount = dao.getChannelCount()

        if (channelCount == 0 || currentTime - lastSync > oneDayInMillis || lastSync == 0L) {
            Log.d(TAG, "Iniciando sincronización de canales...")
            onDebug("Sincronizando canales LATAM...")
            forceSyncChannels(onDebug)
        } else {
            Log.d(TAG, "Los canales ya están sincronizados (caché local).")
            onDebug("")
        }
    }

    suspend fun forceSyncChannels(onDebug: (String) -> Unit = {}) {
        withContext(Dispatchers.IO) {
            try {
                // CLEAR DATABASE BEFORE SYNC
                dao.clearChannels() 
                
                Log.d(TAG, "Descargando data de canales...")
                onDebug("Descargando data de IPTV-ORG...")
                
                val blocklistResponse = apiService.getBlocklist()
                if (!blocklistResponse.isSuccessful) {
                    onDebug("blocklist.json: HTTP ${blocklistResponse.code()}")
                    return@withContext
                }
                onDebug("blocklist.json OK")
                val blocklistRes = blocklistResponse.body()?.mapNotNull { it.channel }?.toSet() ?: emptySet()
                
                val logosResponse = apiService.getLogos()
                val logosMap = if (logosResponse.isSuccessful) {
                    logosResponse.body()?.associate { (it.channel ?: "") to (it.url ?: "") } ?: emptyMap()
                } else {
                    onDebug("logos.json: HTTP ${logosResponse.code()}")
                    emptyMap()
                }
                onDebug("logos.json OK (${logosMap.size} logos)")

                val channelsResponse = apiService.getChannels()
                if (!channelsResponse.isSuccessful) {
                    onDebug("channels.json: HTTP ${channelsResponse.code()}")
                    return@withContext
                }
                onDebug("channels.json OK")
                val channelsRes = channelsResponse.body() ?: emptyList()

                val streamsResponse = apiService.getStreams()
                if (!streamsResponse.isSuccessful) {
                    onDebug("streams.json: HTTP ${streamsResponse.code()}")
                    return@withContext
                }
                onDebug("streams.json OK")
                val streamsRes = streamsResponse.body() ?: emptyList()

                onDebug("Channels descargados: ${channelsRes.size}")

                // 2. Unir channels con streams (Cruce inicial)
                val allChannelsById = channelsRes.filter { it.id != null }.associateBy { it.id!! }
                val initialJoined = streamsRes.mapNotNull { stream ->
                    val channelId = stream.channel
                    val streamUrl = stream.url
                    if (channelId != null && streamUrl != null) {
                        val ch = allChannelsById[channelId]
                        if (ch != null) {
                            // Guardamos el par para filtrar progresivamente
                            Pair(ch, stream)
                        } else null
                    } else null
                }
                onDebug("Con stream asociado: ${initialJoined.size}")
                if (initialJoined.isEmpty()) {
                    onDebug("STOP: 0 canales después del Cruce.")
                    return@withContext
                }

                // 3. Eliminar duplicados (por URL de stream)
                val distinctJoined = initialJoined.distinctBy { it.second.url }
                onDebug("Sin duplicados: ${distinctJoined.size}")

                // 4. Filtro LATAM (Países específicos)
                val latamCountries = setOf(
                    "AR", "BO", "CL", "CO", "CR", "CU", "DO", "EC", "SV", 
                    "GT", "HN", "MX", "NI", "PA", "PY", "PE", "PR", "UY", "VE",
                    "ES", "US"
                )
                val latamList = distinctJoined.filter { pair ->
                    pair.first.country != null && latamCountries.contains(pair.first.country)
                }
                onDebug("Después del filtro LATAM: ${latamList.size}")
                if (latamList.isEmpty()) {
                    onDebug("STOP: 0 después del filtro LATAM.")
                    return@withContext
                }

                // 5. Filtro Idioma (DESACTIVADO temporalmente por petición del usuario)
                // val spaList = latamList.filter { it.first.languages?.contains("spa") == true }
                val spaList = latamList
                onDebug("Filtro idioma: DESACTIVADO (${spaList.size})")

                // 6. Eliminar Blocklist
                val finalFiltered = spaList.filter { pair ->
                    !blocklistRes.contains(pair.first.id)
                }
                onDebug("Después de Blocklist: ${finalFiltered.size}")

                // Convertir a entidades finales y limitar a 800
                var entitiesSoFar = 0
                val entities = finalFiltered.map { (ch, stream) ->
                    val rawLogo = ch.logo ?: ""
                    // Preference: 1. logos.json mapping, 2. channel.logo field if URL, 3. constructed URL, 4. empty
                    val logoFromMap = if (!ch.id.isNullOrBlank()) logosMap[ch.id] else null
                    
                    val finalLogo = when {
                        !logoFromMap.isNullOrBlank() -> logoFromMap
                        rawLogo.startsWith("http") -> rawLogo
                        rawLogo.isNotBlank() -> "https://iptv-org.github.io/images/channels/$rawLogo"
                        else -> ""
                    }
                    
                    if (finalLogo.isNotEmpty()) {
                        val logMsg = "Canal: ${ch.name} | Logo Map: $logoFromMap | Logo Final: $finalLogo"
                        Log.d(TAG, logMsg)
                        if (entitiesSoFar < 5) {
                            onDebug(logMsg)
                        }
                    }
                    entitiesSoFar++
 
                    PanaTVChannelEntity(
                        id = stream.url!!,
                        name = ch.name ?: "Sin nombre",
                        streamUrl = stream.url,
                        logoUrl = finalLogo,
                        country = ch.country ?: "",
                        userAgent = stream.user_agent,
                        referrer = stream.http_referrer
                    )
                }.take(1400)

                onDebug("Total final para Room: ${entities.size}")

                if (entities.isNotEmpty()) {
                    dao.clearChannels()
                    dao.insertChannels(entities)
                    prefs.edit().putLong("last_sync_time", System.currentTimeMillis()).apply()
                    Log.d(TAG, "Sincronización exitosa. ${entities.size} canales guardados en la BD local.")
                    onDebug("Sincronización exitosa: ${entities.size} canales.")
                } else {
                    Log.w(TAG, "No se encontraron canales válidos para guardar.")
                    onDebug("Error: No se encontraron canales válidos.")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error de red o sincronización al obtener los canales: ${e.message}", e)
                onDebug("Error: ${e.message}")
                e.printStackTrace()
            }
        }
    }
}
