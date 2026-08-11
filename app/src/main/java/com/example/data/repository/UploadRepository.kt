package com.example.data.repository

import com.example.data.model.UploadMediaResult
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import com.example.data.supabase.SessionManager
import com.example.data.supabase.SupabaseClient
import org.json.JSONObject
import java.io.File
import java.util.UUID
import java.util.concurrent.TimeUnit

class UploadRepository {
    private val TAG = "UploadRepository"

    companion object {
        private val _globalUploadProgress = MutableStateFlow<Float?>(null)
        val globalUploadProgress: StateFlow<Float?> = _globalUploadProgress.asStateFlow()
        
        private val _uploadSuccessEvent = kotlinx.coroutines.flow.MutableSharedFlow<Unit>(extraBufferCapacity = 1)
        val uploadSuccessEvent: kotlinx.coroutines.flow.SharedFlow<Unit> = _uploadSuccessEvent

        fun setGlobalProgress(progress: Float?) {
            _globalUploadProgress.value = progress
        }
        
        fun triggerUploadSuccess() {
            _uploadSuccessEvent.tryEmit(Unit)
        }
    }

    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(120, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            .writeTimeout(120, TimeUnit.SECONDS)
            .pingInterval(20, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()
    }

    suspend fun uploadVideo(
        mediaFile: java.io.File,
        mediaMimeType: String,
        caption: String,
        userId: String,
        fileNamePrefix: String? = null,
        type: String? = null,
        onProgress: ((bytesWritten: Long, totalBytes: Long) -> Unit)? = null
    ): Result<UploadMediaResult> {
        return uploadVideoWithRetry(mediaFile, mediaMimeType, caption, userId, fileNamePrefix, type, isRetry = false, onProgress = onProgress)
    }

    suspend fun uploadVideo(
        mediaBytes: ByteArray,
        mediaMimeType: String,
        caption: String,
        userId: String,
        fileNamePrefix: String? = null,
        type: String? = null
    ): Result<UploadMediaResult> = withContext(Dispatchers.IO) {
        if (mediaBytes.isEmpty()) {
            return@withContext Result.failure(Exception("ByteArray is empty"))
        }
        // Write bytes to a temporary file to use the robust file-based upload logic
        val tempFile = File.createTempFile("upload_wrap_", ".tmp")
        try {
            tempFile.writeBytes(mediaBytes)
            uploadVideoWithRetry(tempFile, mediaMimeType, caption, userId, fileNamePrefix, type, isRetry = false)
        } finally {
            tempFile.delete()
        }
    }

    private suspend fun uploadVideoWithRetry(
        mediaFile: java.io.File,
        mediaMimeType: String,
        caption: String,
        userId: String,
        fileNamePrefix: String?,
        type: String?,
        isRetry: Boolean,
        onProgress: ((bytesWritten: Long, totalBytes: Long) -> Unit)? = null
    ): Result<UploadMediaResult> = withContext(Dispatchers.IO) {
        val extensionFromFileName = if (mediaFile.name.contains(".")) mediaFile.name.substringAfterLast(".").lowercase() else ""
        val extensionFromMime = when {
            mediaMimeType.contains("pdf", ignoreCase = true) -> "pdf"
            mediaMimeType.contains("word", ignoreCase = true) || mediaMimeType.contains("msword", ignoreCase = true) -> "docx"
            mediaMimeType.contains("excel", ignoreCase = true) || mediaMimeType.contains("sheet", ignoreCase = true) -> "xlsx"
            mediaMimeType.contains("zip", ignoreCase = true) -> "zip"
            mediaMimeType.contains("audio", ignoreCase = true) || mediaMimeType.contains("m4a", ignoreCase = true) -> "m4a"
            mediaMimeType.contains("png", ignoreCase = true) -> "png"
            mediaMimeType.contains("jpeg", ignoreCase = true) || mediaMimeType.contains("jpg", ignoreCase = true) -> "jpg"
            else -> mediaMimeType.split("/").lastOrNull()?.takeIf { it.length <= 8 && it.matches(Regex("^[a-zA-Z0-9]+$")) } ?: "bin"
        }
        val rawExt = if (extensionFromFileName.isNotEmpty() && extensionFromFileName.length <= 8 && extensionFromFileName.matches(Regex("^[a-zA-Z0-9]+$"))) extensionFromFileName else extensionFromMime
        val extension = rawExt.ifEmpty { "bin" }

        val prefix = fileNamePrefix ?: ""
        val fileName = if (prefix.isNotEmpty()) {
            if (prefix.endsWith("_") || prefix.endsWith("-")) {
                "$prefix${UUID.randomUUID()}.$extension"
            } else {
                "${prefix}_${UUID.randomUUID()}.$extension"
            }
        } else {
            "${UUID.randomUUID()}.$extension"
        }

        if (!mediaFile.exists() || mediaFile.length() == 0L) {
            val errMsg = "Error: El archivo que se intenta subir no existe o está vacío."
            Log.e(TAG, "❌ $errMsg")
            return@withContext Result.failure(Exception(errMsg))
        }

        val fileSize = mediaFile.length()
        Log.i(TAG, "=== AUDITORÍA DE ARCHIVO PARA SUBIDA ===")
        Log.i(TAG, "- Nombre del archivo generado: $fileName")
        Log.i(TAG, "- Tipo de medio (MIME type): $mediaMimeType")
        Log.i(TAG, "- Tamaño real verificado en disco: $fileSize bytes (${String.format("%.2f", fileSize.toDouble() / (1024 * 1024))} MB)")
        Log.i(TAG, "=========================================")

        // 1. Obtener URL del CDN actual (usando caché temporal si está disponible para evitar consultas redundantes)
        val cdnUrl = CdnManager.getCDNUrl(forceRefresh = isRetry).trim()
        Log.d(TAG, "URL obtenida desde Supabase para subida: '$cdnUrl'")

        if (cdnUrl.isEmpty()) {
            val errMsg = "La URL del CDN obtenida desde Supabase está vacía. No se puede proceder con la subida."
            Log.e(TAG, "❌ $errMsg")
            return@withContext Result.failure(Exception(errMsg))
        }

        val uploadEndpoint = if (cdnUrl.endsWith("/")) "${cdnUrl}upload" else "$cdnUrl/upload"

        // 2. Imprimir logs obligatorios antes del envío
        Log.d(TAG, "=== PREPARANDO PETICIÓN DE SUBIDA MULTIPART ===")
        Log.d(TAG, "1. URL completa final utilizada para el POST: $uploadEndpoint")
        Log.d(TAG, "2. Método HTTP: POST")
        Log.d(TAG, "3. Nombre del campo multipart: mediaFile")
        Log.d(TAG, "4. Nombre del archivo: $fileName, Tamaño: $fileSize bytes, Tipo: $mediaMimeType")

        try {
            val reqBody = ProgressRequestBody(mediaMimeType.toMediaTypeOrNull(), mediaFile, onProgress)

            // Incluimos "mediaFile" que es el que espera el Multer backend de Node.
            val multipartBuilder = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("mediaFile", fileName, reqBody)
                .addFormDataPart("caption", caption)
                .addFormDataPart("userId", userId)

            if (type != null) {
                multipartBuilder.addFormDataPart("type", type)
            }

            val multipartBody = multipartBuilder.build()

            // Get Supabase token for secure authentication
            val token = SessionManager.getUserAuthToken()
            if (token == null) {
                Log.e(TAG, "❌ No se pudo obtener el token de sesión. El usuario debe estar autenticado.")
                return@withContext Result.failure(Exception("Usuario no autenticado"))
            }

            val requestBuilder = Request.Builder()
                .url(uploadEndpoint)
                .addHeader("Authorization", "Bearer $token")
                .post(multipartBody)

            val request = requestBuilder.build()

            Log.d(TAG, "Enviando petición a través de OkHttp...")

            client.newCall(request).execute().use { response ->
                val responseCode = response.code
                val responseHeaders = response.headers
                val requestHeaders = response.request.headers

                Log.d(TAG, "=== REGISTRO DE RESPUESTA CDN ===")
                Log.d(TAG, "5. Código HTTP recibido: $responseCode")
                Log.d(TAG, "Headers de la petición:\n$requestHeaders")
                Log.d(TAG, "Headers de la respuesta recibidos:\n$responseHeaders")

                if (response.isSuccessful) {
                    val bodyStr = response.body?.string()?.trim() ?: ""
                    Log.d(TAG, "Respuesta completa del servidor: $bodyStr")
                    Log.d(TAG, "=== BODY COMPLETO DE RESPUESTA CDN ===")
                    Log.d(TAG, bodyStr)

                    if (bodyStr.isNotEmpty()) {
                        if (bodyStr.startsWith("{") && bodyStr.endsWith("}")) {
                            val json = JSONObject(bodyStr)
                            
                            // Extraction logic for different backend response formats
                            val targetJson = if (json.has("data")) json.optJSONObject("data") ?: json else json
                            
                            val rawUrl = targetJson.optString("url", targetJson.optString("media_url", ""))
                            val rawThumb = targetJson.optString("thumbnail_url", targetJson.optString("thumbnail", ""))
                            val mime = targetJson.optString("mime", targetJson.optString("media_mime", mediaMimeType))
                            val size = if (targetJson.has("size")) targetJson.optLong("size") else if (targetJson.has("media_size")) targetJson.optLong("media_size") else fileSize
                            val duration = if (targetJson.has("duration")) targetJson.optLong("duration") else if (targetJson.has("media_duration")) targetJson.optLong("media_duration") else 0L
                            val width = if (targetJson.has("width")) targetJson.optInt("width") else if (targetJson.has("media_width")) targetJson.optInt("media_width") else 0
                            val height = if (targetJson.has("height")) targetJson.optInt("height") else if (targetJson.has("media_height")) targetJson.optInt("media_height") else 0

                            if (rawUrl.isNotEmpty()) {
                                Log.i(TAG, "🟢 URL del archivo obtenida exitosamente: $rawUrl")
                                val resolvedUrl = CdnManager.resolveMediaUrl(rawUrl)
                                val resolvedThumb = if (rawThumb.isNotEmpty()) CdnManager.resolveMediaUrl(rawThumb) else null
                                
                                Log.i(TAG, "MEDIA_UPLOAD: media_url=$resolvedUrl, thumbnail=$resolvedThumb, mime=$mime, size=$size")
                                
                                val result = UploadMediaResult(
                                    url = resolvedUrl,
                                    thumbnailUrl = resolvedThumb,
                                    mime = mime,
                                    size = size,
                                    duration = duration,
                                    width = width,
                                    height = height
                                )
                                return@withContext Result.success(result)
                            } else {
                                Log.e(TAG, "❌ No se encontraron claves url o media_url en: $bodyStr")
                                return@withContext Result.failure(Exception("La subida fue exitosa pero no se encontró la clave 'url', 'media_url' ni 'data' en la respuesta: $bodyStr"))
                            }
                        } else {
                            Log.e(TAG, "❌ La respuesta no es JSON válido: $bodyStr")
                            return@withContext Result.failure(Exception("La respuesta del servidor no es un objeto JSON válido (probablemente HTML o error del proxy): ${bodyStr.take(200)}"))
                        }
                    } else {
                        Log.e(TAG, "❌ Respuesta vacía del servidor de CDN")
                        return@withContext Result.failure(Exception("Respuesta vacía del servidor de CDN"))
                    }
                } else {
                    val errStr = response.body?.string() ?: ""
                    Log.d(TAG, "Respuesta completa del servidor (Error $responseCode): $errStr")
                    Log.e(TAG, "❌ Error en el servidor de CDN: Código $responseCode - $errStr")
                    if (!isRetry) {
                        Log.i(TAG, "Subida falló (Código $responseCode). Limpiando caché de CDN y reintentando una vez...")
                        CdnManager.clearCache()
                        return@withContext uploadVideoWithRetry(mediaFile, mediaMimeType, caption, userId, fileNamePrefix, type, isRetry = true)
                    }
                    return@withContext Result.failure(Exception("Error de servidor CDN ($responseCode): $errStr"))
                }
            }
        } catch (ioe: java.io.IOException) {
            Log.e(TAG, "🚨 [IOException] Excepción capturada durante la subida a CDN: ${ioe.message}", ioe)
            if (!isRetry) {
                Log.i(TAG, "Subida falló por IOException. Limpiando caché de CDN y reintentando...")
                CdnManager.clearCache()
                return@withContext uploadVideoWithRetry(mediaFile, mediaMimeType, caption, userId, fileNamePrefix, type, isRetry = true)
            }
            return@withContext Result.failure(ioe)
        } catch (je: org.json.JSONException) {
            Log.e(TAG, "🚨 [JSONException] Excepción capturada parseando respuesta JSON de CDN: ${je.message}", je)
            if (!isRetry) {
                Log.i(TAG, "Subida falló por JSONException. Limpiando caché de CDN y reintentando...")
                CdnManager.clearCache()
                return@withContext uploadVideoWithRetry(mediaFile, mediaMimeType, caption, userId, fileNamePrefix, type, isRetry = true, onProgress = onProgress)
            }
            return@withContext Result.failure(je)
        } catch (re: java.lang.RuntimeException) {
            Log.e(TAG, "🚨 [RuntimeException] Excepción capturada durante la subida a CDN: ${re.message}", re)
            if (!isRetry) {
                Log.i(TAG, "Subida falló por RuntimeException. Limpiando caché de CDN y reintentando...")
                CdnManager.clearCache()
                return@withContext uploadVideoWithRetry(mediaFile, mediaMimeType, caption, userId, fileNamePrefix, type, isRetry = true, onProgress = onProgress)
            }
            return@withContext Result.failure(re)
        } catch (t: Throwable) {
            Log.e(TAG, "🚨 [Throwable] Excepción fatal capturada durante la subida a CDN: ${t.message}", t)
            if (!isRetry) {
                Log.i(TAG, "Subida falló por Throwable. Limpiando caché de CDN y reintentando...")
                CdnManager.clearCache()
                return@withContext uploadVideoWithRetry(mediaFile, mediaMimeType, caption, userId, fileNamePrefix, type, isRetry = true, onProgress = onProgress)
            }
            val exc = if (t is Exception) t else Exception(t)
            return@withContext Result.failure(exc)
        }
    }
}

class ProgressRequestBody(
    private val contentType: okhttp3.MediaType?,
    private val file: java.io.File,
    private val onProgress: ((bytesWritten: Long, contentLength: Long) -> Unit)?
) : okhttp3.RequestBody() {
    override fun contentType(): okhttp3.MediaType? = contentType
    override fun contentLength(): Long = file.length()

    override fun writeTo(sink: okio.BufferedSink) {
        val totalLength = contentLength()
        if (totalLength <= 0L) return
        val buffer = ByteArray(8192)
        var uploaded = 0L
        file.inputStream().use { inputStream ->
            var read: Int
            var lastPercent = -1
            while (inputStream.read(buffer).also { read = it } != -1) {
                sink.write(buffer, 0, read)
                uploaded += read
                val percent = ((uploaded * 100) / totalLength).toInt()
                if (percent != lastPercent) {
                    lastPercent = percent
                    onProgress?.invoke(uploaded, totalLength)
                }
            }
        }
    }
}

