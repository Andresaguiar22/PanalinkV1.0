# DOMAIN_MODEL_PANALINK.md
## Especificación Oficial del Modelo de Dominio y Arquitectura Consolidada (Fase 2)

---

## 1. INFORMACIÓN DEL DOCUMENTO Y ENFOQUE ARQUITECTÓNICO

- **Proyecto**: Panalink Mobile Platform
- **Documento**: Especificación Oficial del Modelo de Dominio (Domain-Driven Design & Clean Architecture)
- **Versión**: 2.0-CONSOLIDATED
- **Estado**: Aprobado Definitivamente para la Fase 3 (Implementación)
- **Alcance Tecnológico**:
  - **Android**: Kotlin, Jetpack Compose, Room (SSOT Local), WorkManager, Kotlin Coroutines & Flow.
  - **BaaS**: Supabase (PostgreSQL 15+, PostgREST, Realtime WebSockets, GoTrue Auth).
  - **Node.js**: Express HTTP (Media Ingestion), Socket.io (WebRTC Signaling & Presence).

---

## 2. REGLAS DE IDENTIDAD Y UNIFICACIÓN DE IDENTIFICADORES

### 2.1 Identificador Único Universal Inmutable (`MessageId`)
1. **Nacimiento en Android**: Todo mensaje es creado en la capa de dominio de Android asignándole un `MessageId` (UUID v4 estándar).
2. **Ciclo de Vida Único**: El `MessageId` se guarda en Room, se transmite a Supabase mediante REST/RPC, se distribuye por Realtime, llega al destinatario y se guarda en su base de datos local **sin sufrir modificaciones ni reconciliaciones**.
3. **Eliminación Total de `temp_id` y `server_id`**: Se elimina la duplicidad de identidades. Ya no existen mapeos temporales ni reemplazos de claves primarias en Room o Supabase, eliminando los riesgos de duplicación e inconsistencia en UI.

### 2.2 Secuenciamiento Canónico Servidor (`sequenceNumber`)
1. **Ordenamiento Canónico**: El reloj del cliente local (Android) nunca se utiliza para ordenar mensajes ni determinar la precedencia de eventos.
2. **Generación en PostgreSQL**: Al insertarse en la tabla `messages` de Supabase, un trigger o secuencia atómica atribuye un `sequenceNumber` (BIGINT autoincremental por conversación).
3. **Paginación y Sync Incremental**: El `sequenceNumber` se utiliza para paginación basada en cursores (`WHERE sequence_number > :last_sequence ORDER BY sequence_number ASC LIMIT 50`) y para la recuperación exacta de mensajes omitidos tras periodos de desconexión.

---

## 3. DOMAIN AGGREGATES & ENTIDADES DEL DOMINIO

### 3.1 Aggregate Root: Message
Representa la unidad básica de interacción de mensajería.
- **Campos**:
  - `id`: `MessageId` (UUID v4 Inmutable)
  - `conversationId`: `ConversationId`
  - `senderId`: `ParticipantId`
  - `senderDeviceId`: `DeviceId`
  - `sequenceNumber`: `SequenceNumber?` (Asignado tras respuesta del servidor)
  - `content`: `MessageContent` (Texto plano o Payload E2EE cifrado)
  - `type`: `MessageType` (`TEXT`, `IMAGE`, `VIDEO`, `AUDIO`, `DOCUMENT`, `SYSTEM`)
  - `status`: `MessageStatus` (FSM del mensaje)
  - `replyToId`: `MessageId?`
  - `createdAt`: `Timestamp` (Hora local de creación)
  - `serverTimestamp`: `Timestamp?` (Hora oficial del servidor)
  - `attachments`: `List<Attachment>`
  - `reactions`: `List<Reaction>`

### 3.2 Aggregate Root: Conversation
Gobernador de la consistencia de la sala de chat (Directa o Grupal).
- **Campos**:
  - `id`: `ConversationId`
  - `type`: `ConversationType` (`DIRECT`, `GROUP`, `CHANNEL`)
  - `title`: `String?`
  - `avatarUrl`: `String?`
  - `conversationVersion`: `Long` (Contador de versión que se incrementa en modificaciones de configuración o miembros)
  - `capabilities`: `ConversationCapabilities` (Permisos del usuario activo: `canSendMessages`, `canAttachMedia`, `canEdit`, `canDelete`)
  - `sequenceSnapshot`: `SequenceSnapshot` (`lastSequence`, `lastSyncSequence`, `lastReadSequence`)
  - `participants`: `List<Participant>`
  - `createdAt`: `Timestamp`

### 3.3 Aggregate Root: Attachment (Ciclo de Vida Independiente)
Representa un recurso multimedia adjunto al mensaje.
- **Campos**:
  - `id`: `AttachmentId`
  - `messageId`: `MessageId`
  - `localUri`: `String?` (Ruta en almacenamiento interno Android)
  - `remoteUrl`: `String?` (URL final en CDN/Supabase Storage)
  - `thumbnailUrl`: `String?`
  - `mimeType`: `String`
  - `fileSizeBytes`: `Long`
  - `uploadState`: `AttachmentUploadState` (FSM del adjunto)
  - `mediaMetadata`: `MediaMetadata` (Ancho, Alto, Duración, Waveform audio)

### 3.4 Aggregate Root: Device (Soporte Multidispositivo & E2EE)
Representa un dispositivo autorizado vinculado a la cuenta del usuario.
- **Campos**:
  - `id`: `DeviceId`
  - `userId`: `ParticipantId`
  - `publicKey`: `String` (Llave pública ECDH para E2EE)
  - `platform`: `String` (`ANDROID`, `WEB`, `IOS`)
  - `pushToken`: `String?`
  - `lastActiveAt`: `Timestamp`

---

## 4. MÁQUINAS DE ESTADOS INDEPENDIENTES (FSM)

### 4.1 Máquina de Estados del Mensaje (`MessageStatus`)
```
[CREATED] ---> [LOCAL_STORED] ---> [WAITING_NETWORK] (si offline)
                     │
                     ▼ (si online)
                 [QUEUED] ---> [SYNCING] ---> [SERVER_ACCEPTED] ---> [DELIVERED] ---> [READ]
                     │            │
                     ▼            ▼
             [RETRY_SCHEDULED] [FAILED]
```

- **Estados**:
  - `CREATED`: Instanciado en memoria.
  - `LOCAL_STORED`: Persistido en Room con `MessageId`.
  - `WAITING_NETWORK`: En cola local aguardando restablecimiento de conectividad.
  - `QUEUED`: Encolado en la canalización de sincronización.
  - `SYNCING`: Petición HTTP/RPC activa hacia Supabase.
  - `RETRY_SCHEDULED`: Fallo transitorio registrado; WorkManager programó reintento con backoff exponencial.
  - `SERVER_ACCEPTED`: Confirmado e insertado en PostgreSQL (posee `sequenceNumber`).
  - `DELIVERED`: Confirmación recibida de que el dispositivo remoto recibió la carga útil.
  - `READ`: Confirmación recibida de que la conversación fue abierta en la pantalla remota.
  - `FAILED`: Fallo fatal no reintentable (ej. usuario bloqueado, conversación eliminada).

### 4.2 Máquina de Estados del Adjunto (`AttachmentUploadState`)
Completamente independiente del estado del mensaje.
```
[PENDING] ---> [COMPRESSING] ---> [UPLOADING] ---> [UPLOADED]
     │              │                  │
     └──────────────┴─────────┬────────┘
                              ▼
                          [FAILED] ---> [CANCELLED]
```

---

## 5. REGISTRO DE TRABAJO DE SINCRONIZACIÓN (`SyncJournal`)

Para evitar depender únicamente de los metadatos internos de WorkManager, el dominio define la entidad `SyncJournalEntry`:
- **Campos**:
  - `operationId`: `UUID`
  - `entityType`: `SyncEntityType` (`MESSAGE_SEND`, `MARK_READ`, `MESSAGE_EDIT`, `MESSAGE_DELETE`)
  - `entityId`: `String` (ID de la entidad afectada, ej. `MessageId`)
  - `status`: `SyncJournalStatus` (`SCHEDULED`, `EXECUTING`, `ACKNOWLEDGED`, `FAILED_RETRYABLE`, `FAILED_FATAL`)
  - `retryCount`: `Int`
  - `lastAttempt`: `Timestamp?`
  - `nextAttempt`: `Timestamp?`
  - `lastError`: `String?`

---

## 6. MODELO DE ERRORES DEL DOMINIO (`DomainError`)

Jerarquía de errores estricta y tipada (`sealed class DomainError`):
- `NetworkUnavailable`: Sin conexión a internet activa.
- `StorageFull`: Almacenamiento SQLite o espacio en disco agotado.
- `MessageTooLarge`: Archivo adjunto supera el límite de tamaño permitido.
- `UploadExpired`: Token o URL firmada de subida caducada.
- `Unauthorized`: Sesión JWT inválida o expirada.
- `InvalidConversation`: La conversación especificada no existe o fue eliminada.
- `UserBlocked`: Operación rechazada por bloqueo entre usuarios.
- `RateLimited`: Límite de tasa de envío alcanzado.
- `EncryptionFailed`: Error en la generación de claves o cifrado AES-GCM.

---

## 7. POLÍTICAS Y CAPACIDADES DEL DOMINIO

### 7.1 Capa de Políticas (`Policy Layer`)
- **`DeletePolicy`**: Permite borrado universal ("Delete for Everyone") dentro de las 48 horas posteriores al envío. Transcurridas 48h, solo se permite borrado local.
- **`EditPolicy`**: Edición de texto permitida dentro de los 15 minutos posteriores al envío.
- **`RetryPolicy`**: Algoritmo de reintento de sincronización con Exponential Backoff (1s, 2s, 4s, 8s, 16s... máx 1 hora) con Jitter aleatorio para evitar estampidas de peticiones.
- **`RetentionPolicy`**: Limpieza automática de adjuntos temporales en caché local tras 30 días.

---

## 8. ACUSES DE RECIBO SEPARADOS (`DeliveryReceipt` & `ReadReceipt`)

Para escalar a chats grupales y escenarios multidispositivo, los acuses no modifican directamente un escalar en el mensaje, sino que se gestionan como entidades independientes:
- **`DeliveryReceipt`**: `messageId`, `userId`, `deviceId`, `deliveredAt`.
- **`ReadReceipt`**: `messageId`, `userId`, `deviceId`, `readAt`.

---

## 9. CASOS DE USO DEL DOMINIO (`Use Cases`)

Los Use Cases son el único punto de entrada para manipular la lógica de negocio desde la UI (ViewModels):
1. **`SendMessageUseCase`**: Valida capacidades, crea el `Message` con `MessageId`, guarda en Room local, registra entrada en `SyncJournal` y dispara el flujo de envío.
2. **`SyncOfflineMessagesUseCase`**: Procesa la cola de `SyncJournal` al recuperar la conexión, ejecutando batching de mensajes hacia el RPC de Supabase.
3. **`UploadAttachmentUseCase`**: Ejecuta la compresión de medios y delega la subida al repositorio de almacenamiento.
4. **`MarkMessageAsReadUseCase`**: Genera el `ReadReceipt` local y remoto, actualizando el marcador de mensajes sin leer.
5. **`EditMessageUseCase`**: Evalúa `EditPolicy` y aplica cambios de contenido emitiendo el evento correspondiente.
6. **`DeleteMessageUseCase`**: Evalúa `DeletePolicy` y ejecuta el marcado de borrado en Room y Supabase.
7. **`ProcessRealtimeEventUseCase`**: Deserializa eventos entrantes de Supabase Realtime, los reconcilia con Room mediante `MessageId` e incrementa la secuencia del chat.

---

## 10. BOUNDED CONTEXTS Y MATRIZ DE DEPENDENCIAS

```
┌────────────────────────────────────────────────────────────────────────┐
│                            IDENTITY CONTEXT                            │
│           (Auth, User Profiles, Devices, E2EE Key Distribution)        │
└───────────────────────────────────┬────────────────────────────────────┘
                                    │
                                    ▼
┌────────────────────────────────────────────────────────────────────────┐
│                              CHAT CONTEXT                              │
│       (Messages, Conversations, Receipts, Cursor Pagination)           │
└──────────────┬────────────────────┬────────────────────┬───────────────┘
               │                    │                    │
               ▼                    ▼                    ▼
┌─────────────────────┐    ┌──────────────────┐    ┌─────────────────────┐
│    MEDIA CONTEXT    │    │   SYNC CONTEXT   │    │  REALTIME CONTEXT   │
│ (Uploads, FSM Media,│    │ (SyncJournal,    │    │ (WebSockets,        │
│  Compress, CDN)     │    │  Offline Queue)  │    │  Presence, Typing)  │
└─────────────────────┘    └──────────────────┘    └─────────────────────┘
```

### Regla de Aislamiento
- **Prohibido**: Que `ChatContext` importe librerías de UI (Compose), dependencias de Room o llamadas de Retrofit directamente. Se comunica exclusivamente a través de interfaces de Repositorios de Dominio.

---

## 11. PLAN DE MIGRACIÓN Y ESCALABILIDAD EN FASE 3

1. **Sin Downtime**: Se añade la columna `sequence_number` BIGINT en Supabase PostgreSQL mediante migración idempotente.
2. **Sustitución de IDs**: Se actualizan las tablas de Room para usar `MessageId` universal desde Android, prescindiendo del patrón de reescritura `temp_<uuid>`.
3. **Paginación por Cursores**: Sustitución gradual de consultas `OFFSET` por cursores `sequenceNumber > :last_sequence` en `MessageDao` y PostgREST.

---

## 12. EVALUACIÓN Y CONCLUSIÓN

Este Modelo de Dominio Consolidado para la Fase 2 proporciona la especificación formal definitiva para Panalink. Elimina los puntos únicos de falla, garantiza la idempotencia end-to-end, establece la FSM independiente para mensajes y adjuntos, y provee una Clean Architecture basada en DDD lista para soportar millones de usuarios concurrentes sin pérdida ni duplicación de información.
