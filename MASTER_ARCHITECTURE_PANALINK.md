# MASTER_ARCHITECTURE_PANALINK.md
## Documento Oficial de Referencia e Ingeniería Inversa de Panalink

---

## 1. VISIÓN GENERAL

### Objetivo del Proyecto
Panalink es una plataforma móvil multiplataforma de comunicación social e intercambio de medios en tiempo real, diseñada bajo un paradigma **Offline-First**. Combina mensajería instantánea cifrada de extremo a extremo (E2EE), llamadas de audio y video mediante WebRTC, feeds sociales, historias efímeras (*States*), videos cortos (*Reels*), televisión por IP (*PanaTV*), y un canal de almacenamiento en la nube (*Bore/Cloudflare CDN*).

### Estado de Desarrollo y Nivel de Madurez
- **Estado Actual**: Fase Beta Avanzada / Prototipo de Producción Funcional.
- **Nivel de Madurez**: **6.5 / 10** (Inestable en componentes de tiempo real y concurrencia; arquitectura base madura con MVVM y Clean Architecture parcial).
- **Filosofía de Arquitectura**: 
  - **Client-Side**: Android nativo en Kotlin con Jetpack Compose, MVVM + Repository Pattern, reactivo mediante Kotlin Flows y StateFlows.
  - **Data Layer**: Reactividad local con Room Database (SQLite) como fuente única de verdad local (SSOT), sincronizado asíncronamente con Supabase (PostgreSQL + PostgREST) y servicios Node.js.
  - **Backend Layer**: Enfoque híbrido BaaS (Supabase para autenticación, base de datos y canales Realtime) + servidor de señalización WebRTC e ingesta de archivos pesados en Node.js Express + Socket.io.

### Componentes Principales y Dependencias
```
┌────────────────────────────────────────────────────────────────────────┐
│                        PANALINK CLIENT (ANDROID)                       │
│  ┌───────────────────────┐  ┌──────────────────────┐  ┌─────────────┐  │
│  │  UI (Jetpack Compose) │  │ ViewModels / State   │  │   Room DB   │  │
│  └───────────┬───────────┘  └──────────┬───────────┘  └──────┬──────┘  │
│              └─────────────────────────┼─────────────────────┘         │
│                                        ▼                               │
│                         REPOSITORIES & WORKERS                         │
└──────────────┬─────────────────────────┬──────────────────────┬────────┘
               │                         │                      │
               ▼                         ▼                      ▼
┌───────────────────────────┐ ┌────────────────────┐ ┌────────────────────┐
│   SUPABASE (BaaS/Postgres) │ │  NODE.JS BACKEND   │ │ WEBRTC SIGNALING   │
│  - Auth (GoTrue)          │ │  - Express Uploads │ │  - Socket.io       │
│  - Database (PostgREST)   │ │  - Cloudflare CDN  │ │  - STUN/TURN       │
│  - Realtime WebSockets    │ │  - Bore Tunnel     │ │                    │
└───────────────────────────┘ └────────────────────┘ └────────────────────┘
```

---

## 2. ARQUITECTURA GENERAL Y COMUNICACIÓN ENTRE COMPONENTES

### Diagrama Textual de Comunicaciones e Interacciones
```
+----------------------------------------------------------------------------------------------------+
|                                           CLIENTE ANDROID                                          |
|                                                                                                    |
|  [Compose UI] ---> [ViewModel] ---> [Repository] ──(Lee/Escribe)──> [Room DB (SSOT)]               |
|                                         │                                │                         |
|                                         ├───(Sync Asíncrono / WorkManager)─┘                       |
|                                         │                                                          |
|          ┌──────────────────────────────┼──────────────────────────────┐                           |
|          │ HTTP / REST                  │ WebSocket (Realtime)         │ Socket.io (Signaling)     |
+──────────┼──────────────────────────────┼──────────────────────────────┼───────────────────────────+
           │                              │                              │
           ▼                              ▼                              ▼
+────────────────────+         +────────────────────+         +────────────────────+
| Supabase PostgREST |         | Supabase Realtime  |         | Node.js / Express  |
|  & GoTrue Auth     |         |  (Postgres Change) |         |  & Socket.io Server|
+──────────┬─────────+         +────────────────────+         +──────────┬─────────+
           │                                                             │
           ▼                                                             ▼
+────────────────────+                                        +────────────────────+
| PostgreSQL Engine  |                                        | Cloudflare CDN /   |
| (Tables, RPC, RLS) |                                        | Local Uploads      |
+────────────────────+                                        +────────────────────+
```

### Mecanismos de Comunicación Internos y Externos
1. **Android - Room**: Room actua como fuente de verdad (`Single Source of Truth`). La UI observa `Flow<List<MessageEntity>>` o `Flow<List<ChatEntity>>`. Las modificaciones locales generan escrituras directas en Room con estado `sending`, disparando reconexión reactiva en Compose.
2. **Android - Supabase (HTTP/PostgREST)**: `SupabaseApiService` realiza llamadas REST a PostgREST de Supabase mediante Retrofit. Se utiliza token Bearer JWT recuperado desde `SessionManager`.
3. **Android - Supabase (Realtime)**: `MessagesRepository` y `PanalinkRealtimeService` suscriben canales Postgres via `SupabaseClient.realtime` para cambios en tablas `messages`, `reactions`, `states` y `profiles`.
4. **Android - Node.js Express (Uploads)**: Para la subida de archivos multimedia grandes, `UploadRepository` y `MediaUploadWorker` llaman al endpoint `/upload` del servidor Express en Node.js, enviando peticiones `multipart/form-data`.
5. **Android - Node.js Socket.io (WebRTC)**: `SignalingClient` mantiene un WebSocket con Socket.io en Node.js para transmitir ofertas SDP, respuestas SDP y candidatos ICE hacia `WebRTCClient`.
6. **WorkManager**: Administra tareas persistentes fuera de línea (`MediaUploadWorker`, `PostUploadWorker`, `SyncMessagesWorker`).

---

## 3. MAPA COMPLETO DEL CÓDIGO

```
/ (Root Workspace)
├── AGENTS.md                               <- Reglas de desarrollo y Checklist de Cero Regresiones.
├── supabase_schema.sql                     <- Esquema completo de BD PostgreSQL, Triggers, RLS y RPC.
├── server/                                 <- Servidor Node.js Express + Socket.io.
│   ├── index.js                            <- Express app, servidor HTTP, multer upload e integraciones CDN.
│   └── socket/
│       ├── events.js                       <- Manejadores de eventos de señalización de llamadas y presencia.
│       └── users.js                        <- Gestión en memoria de sockets de usuarios conectados.
└── app/
    ├── build.gradle.kts                    <- Configuración Gradle del módulo Android (Dependencies, Plugins).
    └── src/main/java/com/example/
        ├── MainActivity.kt                 <- Punto de entrada principal, NavigationHost y Compose Scaffolding.
        ├── PanaApplication.kt              <- Clase Application, inicializador global de WorkManager, Room y Supabase.
        ├── call/                           <- Subsistema WebRTC y Llamadas de Audio/Video.
        │   ├── WebRTCClient.kt             <- Wrapper de PeerConnectionFactory, SDP e ICE.
        │   ├── SignalingClient.kt          <- Cliente Socket.io de señalización.
        │   ├── CallManager.kt              <- Máquina de estados de llamadas y audio routing.
        │   ├── CallRepository.kt           <- Persistencia e historial de llamadas.
        │   └── IceServerProvider.kt        <- Proveedor de servidores STUN/TURN.
        ├── data/
        │   ├── database/                   <- Persistencia Local con Room.
        │   │   ├── PanalinkDatabase.kt     <- RoomDatabase principal (v9).
        │   │   ├── ChatEntity.kt / ChatDao.kt
        │   │   ├── MessageEntity.kt / MessageDao.kt
        │   │   ├── ProfileEntity.kt / ProfileDao.kt
        │   │   ├── ReactionEntity.kt / ReactionDao.kt
        │   │   ├── DraftEntity.kt / DraftDao.kt
        │   │   ├── PendingUploadEntity.kt / PendingUploadDao.kt
        │   │   └── PendingPostEntity.kt / PendingPostDao.kt
        │   ├── model/                      <- Modelos DTOs e interfaces de dominio.
        │   │   ├── Models.kt, FeedModels.kt, SocialModels.kt, Notification.kt
        │   ├── repository/                 <- Repositorios de Datos.
        │   │   ├── MessagesRepository.kt   <- Engine principal de chat y sync offline.
        │   │   ├── ChatsRepository.kt      <- Gestión de chats y usuarios.
        │   │   ├── UploadRepository.kt     <- Carga de archivos multipart a Node/Supabase.
        │   │   ├── SocialRepositoryImpl.kt <- Feed social, publicaciones y comentarios.
        │   │   ├── FeedRepositoryImpl.kt   <- Carga de noticias y reels.
        │   │   ├── ProfilesRepository.kt   <- Perfiles de usuario y presencias.
        │   │   ├── StatesRepository.kt     <- Historias de 24 horas.
        │   │   ├── DraftsRepository.kt     <- Borradores de texto de chat.
        │   │   └── UserKeysRepository.kt   <- Distribución de llaves E2EE.
        │   ├── supabase/                   <- Integración BaaS Supabase.
        │   │   ├── SupabaseClient.kt       <- Singleton e inicializador de Supabase Kotlin SDK.
        │   │   ├── SupabaseApiService.kt   <- Cliente Retrofit/PostgREST.
        │   │   ├── AuthManager.kt          <- Autenticación y recuperación de tokens.
        │   │   └── SessionManager.kt       <- Persistencia de credenciales en EncryptedSharedPreferences.
        │   └── video/                      <- Caché de Video con Media3 / SimpleCache.
        │       ├── VideoCacheManager.kt
        │       └── CacheDataSourceFactory.kt
        ├── panatv/                         <- Módulo PanaTV (IPTV / Canales).
        │   ├── PanaTVActivity.kt, PanaTVScreen.kt, PanaTVViewModel.kt, PanaTVRepository.kt
        ├── service/                        <- Servicios Android en Segundo Plano.
        │   ├── PanalinkRealtimeService.kt  <- Foreground Service para WebSockets persistentes.
        │   ├── PanalinkFirebaseMessagingService.kt <- Handlers de Push FCM.
        │   └── NotificationHelper.kt       <- Canales de Notificación y Heads-Up UI.
        ├── ui/                             <- Capa de Presentación (Jetpack Compose).
        │   ├── screen/                     <- Pantallas de la aplicación.
        │   │   ├── ChatScreen.kt, ChatsListScreen.kt, FeedScreen.kt, TikTokVideoFeedScreen.kt,
        │   │   ├── StoryViewerScreen.kt, StoryEditorScreen.kt, ProfileScreen.kt, LoginScreen.kt...
        │   ├── components/                 <- Componentes UI reutilizables y burbujas de chat.
        │   │   └── chat/bubble/            <- MessageBubbleEngine, TextBubbleContent, VoiceBubbleContent...
        │   └── theme/                      <- Color.kt, Theme.kt, Type.kt (Design System M3).
        ├── util/                           <- Criptografía, Utilidades y Helper Managers.
        │   ├── CryptoManager.kt            <- Generación e Intercambio de Llaves ECDH/AES-GCM.
        │   ├── PanalinkMediaManager.kt     <- Procesamiento de imágenes y thumbnails.
        │   ├── NetworkMonitor.kt           <- Monitoreo de conectividad a Internet.
        │   └── VideoCompressorHelper.kt    <- Compresión local de videos.
        └── worker/                         <- Tareas Programadas de WorkManager.
            ├── MediaUploadWorker.kt        <- Subida en diferido de archivos adjuntos.
            ├── PostUploadWorker.kt         <- Subida en diferido de publicaciones social feed.
            └── SyncMessagesWorker.kt       <- Sincronización en segundo plano de mensajes pendientes.
```

---

## 4. INVENTARIO DE CLASES Y COMPONENTES

| Clase | Archivo | Responsabilidad Principal | Métodos Clave | Dependencias | Criticidad |
| :--- | :--- | :--- | :--- | :--- | :--- |
| `MessagesRepository` | `data/repository/MessagesRepository.kt` | Coordinación de mensajes, persistencia Room, sincronización Supabase, estado optimista. | `sendMessage()`, `syncOfflineMessages()`, `handleIncomingRealtimeMessage()` | `MessageDao`, `SupabaseApiService`, `CryptoManager`, `WorkManager` | **CRÍTICA** |
| `PanalinkDatabase` | `data/database/PanalinkDatabase.kt` | Definición de Room Database, versión 9, DAOs y migraciones. | `messageDao()`, `chatDao()`, `profileDao()` | Room, SQLite | **CRÍTICA** |
| `CryptoManager` | `util/CryptoManager.kt` | Cifrado/Descifrado E2EE (ECDH + AES-GCM) y gestión en AndroidKeyStore. | `encryptMessage()`, `decryptMessage()`, `ensureKeyPairExists()` | AndroidKeyStore, Cipher | **CRÍTICA** |
| `WebRTCClient` | `call/WebRTCClient.kt` | Control de flujos de audio/video WebRTC, PeerConnection y SDP. | `createOffer()`, `createAnswer()`, `setRemoteDescription()`, `addIceCandidate()` | `org.webrtc.*`, `EglBase` | **CRÍTICA** |
| `SignalingClient` | `call/SignalingClient.kt` | Cliente Socket.io para intercambio de candidatos e invitaciones WebRTC. | `connect()`, `emitOffer()`, `emitAnswer()`, `emitIceCandidate()` | `io.socket:socket.io-client` | **ALTA** |
| `CallManager` | `call/CallManager.kt` | Máquina de estados para gestión de llamadas activas, tonos de timbre y audio. | `startCall()`, `acceptCall()`, `rejectCall()`, `endCall()` | `WebRTCClient`, `SignalingClient`, `AudioController` | **ALTA** |
| `UploadRepository` | `data/repository/UploadRepository.kt` | Subida de archivos mediante Multipart a Express / Supabase Storage. | `uploadFile()`, `uploadMediaToNode()` | `OkHttpClient`, `SupabaseApiService` | **ALTA** |
| `MediaUploadWorker` | `worker/MediaUploadWorker.kt` | Tarea en segundo plano con WorkManager para subida diferida de imágenes/videos. | `doWork()` | `PanalinkMediaManager`, `MessageDao`, `UploadRepository` | **ALTA** |
| `SupabaseApiService` | `data/supabase/SupabaseApiService.kt` | Cliente REST para llamadas PostgREST a Supabase. | `getMessages()`, `sendMessage()`, `syncMessagesRPC()` | Retrofit / OkHttp | **ALTA** |
| `PanalinkRealtimeService` | `service/PanalinkRealtimeService.kt` | Foreground Service para mantener WebSockets activos en background. | `onStartCommand()`, `startForeground()` | `SupabaseClient`, `NotificationHelper` | **MEDIA** |
| `ChatScreen` | `ui/screen/ChatScreen.kt` | UI principal de conversación en Compose con lista interactiva y controles. | `ChatScreen()`, `MessageList()`, `ChatInputBar()` | `ChatViewModel`, Compose runtime | **ALTA** |
| `MessageBubbleEngine` | `ui/components/chat/bubble/MessageBubbleEngine.kt` | Motor de renderizado dinámico según el tipo de mensaje (Texto, Imagen, Audio, Video, Doc). | `MessageBubbleEngine()` | Material3, Coil, Media3 | **MEDIA** |

---

## 5. FLUJO COMPLETO DE LA APLICACIÓN

### 1. Inicio y Bootstrap (`MainActivity.kt` & `PanaApplication.kt`)
1. `PanaApplication.onCreate()` se ejecuta, inicializando `PanalinkDatabase` (Room v9) y `SupabaseClient`.
2. Se registra el monitoreo de red en `NetworkMonitor.startMonitoring()`.
3. `MainActivity.onCreate()` invoca `enableEdgeToEdge()`.
4. Se verifica la sesión con `SessionManager.isLoggedIn()`. Si hay token JWT válido, se redirige a `ChatsListScreen`; de lo contrario, a `LoginScreen`.

### 2. Autenticación (`LoginScreen.kt` / `AuthManager.kt`)
1. El usuario ingresa credenciales. `AuthViewModel.login()` llama a `AuthManager.loginWithEmail()`.
2. Se consulta a Supabase GoTrue Auth `/auth/v1/token?grant_type=password`.
3. Se reciben JWT AccessToken, RefreshToken y UID del usuario.
4. Credenciales almacenadas en `SessionManager` con `EncryptedSharedPreferences`.
5. Se inicializan las llaves E2EE en `CryptoManager.ensureKeyPairExists()` y se publica la clave pública en la tabla `user_keys` de Supabase.

### 3. Carga Inicial y Sincronización de Chats (`ChatsListScreen.kt` / `ChatsRepository.kt`)
1. `ChatsViewModel` observa `ChatsRepository.getChatsFromRoom()`, emitiendo la lista local guardada inmediatamente (**Offline-First**).
2. Paralelamente, se ejecuta `ChatsRepository.syncChatsFromRemote()`, enviando una consulta PostgREST a Supabase para recuperar la lista actualizada de conversaciones.
3. Se cruzan datos remotos con locales y se insertan/actualizan en `ChatDao` via `Room`.

### 4. Envio de Mensajes de Texto y Multimedia (`ChatScreen.kt` / `MessagesRepository.kt`)
1. **Acción de Usuario**: Presiona enviar mensaje en `ChatScreen`.
2. **Generación Optimista**: `MessagesRepository.sendMessage()` crea un `MessageEntity` con un `id` temporal (`temp_<uuid>`), estado `"sending"`, y timestamp local. Se guarda inmediatamente en Room (`MessageDao.insertMessage()`).
3. **Actualización UI**: La interfaz de Compose reacciona al instante mostrando el mensaje con icono de reloj (estado `"sending"`).
4. **Procesamiento de Archivos (Multimedia)**:
   - Si contiene imágenes/videos/audios, se encola un `MediaUploadWorker` en `WorkManager`.
   - `MediaUploadWorker` ejecuta la compresión/reducción via `PanalinkMediaManager` y sube el archivo al servidor Node.js o Supabase Storage.
   - Una vez subido, reemplaza `localMediaUri` con `mediaUrl` remoto en Room.
5. **Cifrado E2EE (Si aplica)**: Si `CryptoManager.ENABLE_E2EE` es `true`, el contenido se cifra usando AES-256-GCM y la clave compartida generada via ECDH.
6. **Sincronización a Backend**: Se llama a `SupabaseApiService.sendMessage()` o al RPC `sync_offline_messages`.
7. **Reemplazo de ID**: Supabase responde con el `id` definitivo (UUID). `MessagesRepository` actualiza Room, eliminando el registro con `temp_...` e insertando el nuevo ID remoto, cambiando el estado a `"sent"`.

### 5. Recepción en Tiempo Real e Indicadores
1. `SupabaseClient.realtime` escucha la tabla `messages` con filtro `chat_id=eq.<id>`.
2. Al recibir un nuevo mensaje remoto, se inserta directamente en Room via `MessageDao.insertMessage()`.
3. La UI reacciona automáticamente refrescando la `LazyColumn`.
4. Si el mensaje es leído por el destinatario, un evento Realtime actualiza el estado a `"read"`, cambiando las tildes a azul en Compose.

---

## 6. BASE DE DATOS SUPABASE (POSTGRESQL)

### Tablas y Esquema (`supabase_schema.sql`)
1. **`profiles`**:
   - Campos: `id` (UUID, PK, FK `auth.users`), `username`, `full_name`, `avatar_url`, `status`, `last_seen`, `updated_at`.
   - Indice: `idx_profiles_username` en `username`.
2. **`chats`**:
   - Campos: `id` (UUID, PK), `type` (text, e.g. 'direct', 'group'), `name`, `created_at`, `updated_at`.
3. **`chat_members`**:
   - Campos: `chat_id` (FK `chats`), `user_id` (FK `profiles`), `role`, `joined_at`. PK compuesta (`chat_id`, `user_id`).
4. **`messages`**:
   - Campos: `id` (UUID, PK), `chat_id` (FK `chats`), `sender_id` (FK `profiles`), `content`, `type`, `media_url`, `thumbnail_url`, `media_mime`, `media_size`, `media_duration`, `media_width`, `media_height`, `status`, `created_at`, `reply_to_id`.
   - Indices: `idx_messages_chat_id` en `chat_id`, `idx_messages_created_at` en `created_at`.
5. **`reactions`**:
   - Campos: `id` (UUID, PK), `message_id` (FK `messages`), `user_id` (FK `profiles`), `emoji`, `created_at`.
6. **`user_keys`**:
   - Campos: `user_id` (UUID, PK), `public_key` (TEXT), `updated_at`. Guardado de claves públicas E2EE.
7. **`states`** (Historias 24h):
   - Campos: `id`, `user_id`, `media_url`, `caption`, `created_at`, `expires_at`.
8. **`reels`** / **`feed_posts`** / **`feed_comments`** / **`feed_likes`**:
   - Soporte para feed social tipo TikTok/Instagram.

### Funciones RPC Destacadas
- **`sync_offline_messages(p_messages JSONB)`**: Permite el envío masivo de mensajes acumulados durante el periodo sin conexión en una sola transacción atómica PostgreSQL.
- **`mark_messages_as_read(p_chat_id UUID, p_user_id UUID)`**: Actualiza el estado de los mensajes no leídos a `'read'` y retorna el recuento.

---

## 7. PERSISTENCIA LOCAL EN ROOM

### Entidades (`app/src/main/java/com/example/data/database/`)
1. **`MessageEntity`** (`table_name = "messages"`):
   - PrimaryKey: `id` (String). Soporta UUID remotos y `temp_<uuid>` locales.
   - Indexado en `chatId` y `createdAt`.
   - Campos de estado local: `localMediaUri`, `isPendingSync`, `status` (`sending`, `sent`, `delivered`, `read`, `failed`).
2. **`ChatEntity`** (`table_name = "chats"`):
   - PrimaryKey: `id` (String). Almacena resumen del último mensaje (`lastMessageText`, `lastMessageTime`), avatar de contacto y contador de no leídos (`unreadCount`).
3. **`ProfileEntity`** (`table_name = "profiles"`):
   - Caché local de información de usuario y claves públicas E2EE.
4. **`PendingUploadEntity`**:
   - Registro de cola de archivos pendientes por subir cuando no hay conexión.

---

## 8. BACKEND NODE.JS & SIGNALING

### Módulos y Rutas (`server/index.js`)
- **Express App**: Corre por defecto en el puerto `3000`.
- **Ruta POST `/upload`**:
  - Utiliza `multer` con almacenamiento en disco en la carpeta `server/uploads/`.
  - Genera URLs de acceso público o delega la redistribución hacia Cloudflare CDN / Bore Tunnel.
  - Soporta imágenes, videos, documentos y notas de voz en formato OGG/MP4/JPEG.
- **Servidor Socket.io (`server/socket/events.js`)**:
  - `register-user`: Asocia un `userId` con el `socket.id`.
  - `call-user`: Reenvía oferta SDP de llamada entrante hacia el destinatario.
  - `make-answer`: Reenvía respuesta SDP.
  - `ice-candidate`: Transmite candidatos ICE para penetración NAT.
  - `typing` / `recording-audio`: Eventos de presencia en tiempo real.

---

## 9. SEGURIDAD AUDITADA

| Dominio | Implementación Actual | Hallazgos / Vulnerabilidades Identificadas | Nivel de Riesgo |
| :--- | :--- | :--- | :--- |
| **Cifrado E2EE** | `CryptoManager.kt` usa ECDH (secp256r1) + AES-256-GCM. | **`const val ENABLE_E2EE = false`** está desactivado por omisión en el código fuente actual. Los mensajes viajan en texto plano en la BD de Supabase. | **CRÍTICO** |
| **Almacenamiento de Tokens** | `SessionManager.kt` usa `EncryptedSharedPreferences`. | Correctamente implementado usando Android KeyStore MasterKey. | **BAJO** |
| **Backend Express (`server/index.js`)** | Ingesta de archivos via `/upload`. | Falta autenticación JWT middleware en el endpoint `/upload` de Node. Cualesquiera usuarios pueden subir archivos sin token. | **ALTA** |
| **Filtro de Archivos** | `multer` en Node. | No realiza validación de tipo MIME real en el buffer del servidor (solo confía en extensión declarada). | **MEDIA** |
| **Supabase RLS** | Políticas en `supabase_schema.sql`. | Habilitado en `messages` y `chats`, verificando membresía en `chat_members`. Correcto. | **BAJO** |

---

## 10. RENDIMIENTO Y RECURSOS

1. **Recomposiciones en Compose**: `ChatScreen.kt` utiliza optimizadamente `key` dentro de `LazyColumn(items = ...)` referenciando `message.id`. Sin embargo, las burbujas que reproducen voz (`VoiceBubbleContent`) recomponen frecuentemente al actualizar la posición de reproducción del `ExoPlayer` sin aislar el estado con `derivedStateOf`.
2. **Uso de Memoria en Carga de Imágenes**: `Coil` se utiliza para la carga diferida de imágenes. Los thumbnails se reducen localmente con `PanalinkMediaManager.createThumbnail()`, lo cual mitiga picos de RAM OOM (Out of Memory).
3. **Caché de Video**: Implementado en `VideoCacheManager` mediante `androidx.media3.datasource.cache.SimpleCache`, limitando el espacio máximo en disco a 500 MB.

---

## 11. MATRIZ DE RIESGOS DE REGRESIÓN

| Módulo | Componente Relevante | Posible Falla al Modificar | Forma de Verificación |
| :--- | :--- | :--- | :--- |
| **Mensajería Offline** | `MessagesRepository.kt` | Duplicación de mensajes al reemplazar `temp_id` con el `id` final remoto. | Verificar `MessageDao.insertMessage()` con `OnConflictStrategy.REPLACE`. |
| **Realtime Sync** | `PanalinkRealtimeService.kt` | Pérdida de conexión en segundo plano e interrupción de notificaciones. | Inspeccionar reconexión automática en el cliente Realtime. |
| **WebRTC Calls** | `WebRTCClient.kt` | Congelamiento de video local o audio mudo por mala gestión de `EglBase.Context`. | Ejecutar llamada de prueba y verificar logs de IceConnectionState. |
| **Uploads Multimedia** | `MediaUploadWorker.kt` | Archivos huérfanos o inconsistencia de URLs en Room al fallar la red. | Simular corte de red durante la subida y comprobar reintentos de WorkManager. |

---

## 12. DEUDA TÉCNICA DETECTADA

1. **[CRÍTICA] E2EE Deshabilitado**: Variable global `CryptoManager.ENABLE_E2EE = false` en `CryptoManager.kt`. Es necesario activar y probar el intercambio de llaves públicas ECDH.
2. **[ALTA] Endpoint Express Desprotegido**: `/upload` en `server/index.js` carece de middleware de validación de JWT Supabase.
3. **[ALTA] Duplicación de Modelos de Onboarding**: Existen duplicados de pantallas de Onboarding en rutas del proyecto (`com.example.ui.screen.onboarding` vs `app.applet.app...`).
4. **[MEDIA] Servidor Node de Un Solo Hilo**: El servidor Express en `server/index.js` maneja WebSockets e ingesta de archivos en el mismo proceso sin clúster o cola Redis.
5. **[BAJA] Falta de Migraciones Automatizadas de Room**: `PanalinkDatabase` utiliza `fallbackToDestructiveMigration()`, lo cual provocaría pérdida de datos de chat local ante cambios de versión del esquema Room.

---

## 13. INVENTARIO DE FUNCIONALIDADES

| Funcionalidad | Estado | Archivos Involucrados | Observaciones |
| :--- | :--- | :--- | :--- |
| **Envío de Texto** | **Completa** | `ChatScreen.kt`, `MessagesRepository.kt`, `MessageDao.kt` | Soporta estado optimista y reemplazo de ID temporal. |
| **Envío de Imágenes** | **Completa** | `MediaUploadWorker.kt`, `PanalinkMediaManager.kt`, `UploadRepository.kt` | Genera thumbnails y comprime antes de enviar. |
| **Notas de Voz** | **Completa** | `VoiceBubbleContent.kt`, `AudioHelper.kt`, `Media3` | Grabación OGG/AAC y reproductor integrado. |
| **Llamadas WebRTC** | **Parcial / Inestable** | `WebRTCClient.kt`, `SignalingClient.kt`, `CallManager.kt` | Funciona bajo STUN público; falla en NATs simétricos sin TURN. |
| **Historias (States 24h)** | **Completa** | `StoryViewerScreen.kt`, `StatesRepository.kt`, `supabase_schema.sql` | Expira a las 24 horas vía políticas Postgres. |
| **Reels / Feed Short Video** | **Completa** | `TikTokVideoFeedScreen.kt`, `FeedRepositoryImpl.kt`, `VideoCacheManager.kt` | Renderizado con Media3 y caché en disco. |
| **E2EE (Cifrado)** | **Incompleta** | `CryptoManager.kt` | Lógica implementada pero bandera `ENABLE_E2EE` en `false`. |
| **PanaTV (IPTV)** | **Experimental** | `PanaTVScreen.kt`, `PanaTVDatabase.kt` | Lista de canales HLS/M3U8 integrada. |

---

## 14. ARQUITECTURA IDEAL DE EVOLUCIÓN (2 AÑOS)

1. **Modularización por Features (Gradle Multi-Module)**:
   - `:core:database`, `:core:network`, `:core:crypto`, `:feature:chat`, `:feature:calls`, `:feature:feed`.
2. **Servidor de Señalización Dedicado y Escalamiento**:
   - Separar el servidor de ingesta de archivos de la señalización WebRTC. Implementar Redis Pub/Sub para múltiples instancias de Socket.io.
3. **Turn Server Propio (Coturn)**:
   - Desplegar nodos Coturn propios con credenciales efímeras REST (Secret Time-Limited Tokens) para garantizar un 99.9% de éxito en la conexión de llamadas WebRTC.
4. **Migraciones Graduales de Room**:
   - Reemplazar `fallbackToDestructiveMigration()` por `Migration(old, new)` explícitas para salvaguardar el historial offline.

---

## 15. REGLAS DE ORO DE PANALINK

1. **CERO PÉRDIDA DE MENSAJES**: Todo mensaje enviado debe escribirse primero en Room (`MessageDao`) con un ID temporal antes de cualquier intento de red.
2. **OFLLINE-FIRST OBLIGATORIO**: Ninguna pantalla de la UI debe esperar a la red para renderizar datos existentes. Room es la fuente única de verdad.
3. **MANTENER EL REEMPLAZO DE ID ATÓMICO**: Al recibir confirmación del backend, el registro con `temp_<uuid>` debe ser reemplazado o actualizado limpiamente sin dejar duplicados.
4. **NO EJECUTAR TRABAJO HEAVY EN EL HILO UI**: La compresión de imágenes, encriptación y procesamiento de archivos deben realizarse estrictamente en `Dispatchers.IO` o via `WorkManager`.
5. **PRESERVAR CHECKS DE SEGURIDAD EN ROOM Y KEYSTORE**: Nunca almacenar claves privadas E2EE ni tokens JWT en texto plano sin cifrado.
6. **PROTEGER LA RETENCIÓN DE MEMORIA EN COMPOSE**: Recordar optimizar las listas con `key` explícito e aislar animaciones o tickers de tiempo usando `derivedStateOf`.

---

## 16. CONCLUSIÓN Y LISTAS DE CONTROL PRINCIPALES

### Las 20 Clases Más Importantes
1. `MessagesRepository`
2. `PanalinkDatabase`
3. `CryptoManager`
4. `WebRTCClient`
5. `SignalingClient`
6. `CallManager`
7. `UploadRepository`
8. `SupabaseApiService`
9. `SupabaseClient`
10. `MediaUploadWorker`
11. `MessageDao`
12. `ChatDao`
13. `AuthManager`
14. `SessionManager`
15. `PanalinkMediaManager`
16. `PanalinkRealtimeService`
17. `ChatViewModel`
18. `ChatsViewModel`
19. `VideoCacheManager`
20. `NotificationHelper`

### Los 20 Archivos Más Delicados
1. `/app/src/main/java/com/example/data/repository/MessagesRepository.kt`
2. `/app/src/main/java/com/example/data/database/PanalinkDatabase.kt`
3. `/app/src/main/java/com/example/util/CryptoManager.kt`
4. `/app/src/main/java/com/example/call/WebRTCClient.kt`
5. `/app/src/main/java/com/example/call/SignalingClient.kt`
6. `/app/src/main/java/com/example/call/CallManager.kt`
7. `/app/src/main/java/com/example/data/supabase/SupabaseApiService.kt`
8. `/app/src/main/java/com/example/data/supabase/SupabaseClient.kt`
9. `/app/src/main/java/com/example/data/database/MessageDao.kt`
10. `/app/src/main/java/com/example/data/database/MessageEntity.kt`
11. `/app/src/main/java/com/example/worker/MediaUploadWorker.kt`
12. `/app/src/main/java/com/example/service/PanalinkRealtimeService.kt`
13. `/app/src/main/java/com/example/ui/screen/ChatScreen.kt`
14. `/server/index.js`
15. `/server/socket/events.js`
16. `/supabase_schema.sql`
17. `/app/build.gradle.kts`
18. `/app/src/main/AndroidManifest.xml`
19. `/app/src/main/java/com/example/MainActivity.kt`
20. `/app/src/main/java/com/example/PanaApplication.kt`

### Las 20 Prioridades para Continuar el Desarrollo
1. Activar y verificar E2EE cambiando `ENABLE_E2EE = true` en `CryptoManager.kt`.
2. Agregar middleware de autenticación JWT en `/upload` (`server/index.js`).
3. Eliminar `fallbackToDestructiveMigration()` en `PanalinkDatabase.kt` e implementar migraciones de Room.
4. Desplegar un servidor TURN propio (Coturn) para garantizar la conectividad de llamadas WebRTC.
5. Limpiar los archivos/clases duplicadas en la ruta `app/applet/app/...`.
6. Optimizar recomposiciones en `VoiceBubbleContent.kt` usando `derivedStateOf`.
7. Implementar limitación de tasa (Rate Limiting) en el servidor Node.js.
8. Validar la magia de bytes de archivos en la subida en Node.js para prevenir ejecuciones Path Traversal / Malicious Uploads.
9. Configurar canal de sincronización en segundo plano con reintentos exponenciales en `SyncMessagesWorker`.
10. Implementar borrado de mensajes locales/remotos (Delete for Everyone) vía RPC de Supabase.
11. Completar la vista de editar mensajes enviados.
12. Unificar los manejadores de notificaciones push en `PanalinkFirebaseMessagingService`.
13. Añadir indicadores de presencia de usuario masivos optimizados con Redis en Node.js.
14. Integrar soporte para envío de documentos comprimidos (.zip, .pdf).
15. Implementar retransmisión de mensajes (Forward) conservando la cadena de custodia.
16. Aislar la capa de UI de `ExoPlayer` en un controlador único en `TikTokVideoFeedScreen.kt`.
17. Proporcionar fallback de calidad adaptativa (HLS) para los videos subidos a Reels.
18. Crear pruebas unitarias automatizadas para la cola de `MessagesRepository`.
19. Integrar pruebas de integración de base de datos local con Robolectric.
20. Configurar la canalización CI/CD en `.github/workflows/android.yml` con firmas de producción.
