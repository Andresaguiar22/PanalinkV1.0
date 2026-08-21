# Rules for Panalink Development

## 🛡️ Mandate: Zero Regressions (Cero Regresiones)

Every modification to any class must follow a strict "Zero Regression" policy. Before any code is changed, the following functional checklist must be verified, and then re-verified after the change to guarantee no existing features are broken.

---

## 📋 Functional Checklist

### 💬 Mensajería
- [ ] **Envío de mensajes de texto**: Confirm text messages can be composed and sent.
- [ ] **Recepción en tiempo real**: Confirm real-time updates of incoming messages are functional via Supabase Realtime without manually leaving or reopening the screen.
- [ ] **Carga del historial existente**: Verify existing message history loads correctly when entering the chat screen.
- [ ] **Persistencia en Room**: Confirm that messages are persisted to Room database locally for offline-first support.
- [ ] **Sincronización con Supabase**: Verify background sync and database upload sync tasks correctly synchronize states with the Supabase backend.
- [ ] **Mensajes optimistas**: Verify that sending a message immediately places it in the UI with a "sending" state for instant user feedback.
- [ ] **Reemplazo del ID temporal por el definitivo**: Ensure that once Supabase saves the message, the temporary ID (`temp_...`) is replaced in the database and UI by the final remote ID.
- [ ] **Reconexión después de perder Internet**: Confirm that the app gracefully recovers and reconnects to Realtime channels after network loss.

### 📊 Estados del mensaje
- [ ] **Status transitions**: Check that messages move properly through statuses:
  - `sending` -> `sent` -> `delivered` -> `read` -> `failed`
- [ ] **Realtime + Room updates**: Ensure state changes arrive in real-time from the backend and instantly update Room.

### 📡 Señalización (Signaling)
- [ ] **Usuario escribiendo**: Real-time typing indicators are visible and update instantly.
- [ ] **Usuario grabando audio**: Real-time recording indicators are visible and update instantly.
- [ ] **Usuario subiendo archivo**: Real-time uploading indicators function as expected.
- [ ] **Doble tilde gris**: Indicates message was successfully delivered to the remote service.
- [ ] **Doble tilde azul**: Indicates message was read.
- [ ] **Instant update**: All signaling indicators update in real-time without leaving the chat.

### 📦 Multimedia
- [ ] **File type verification**: Check image, video, document, voice note, and stickers.
- [ ] **Performance testing matrix**:
  - Small file uploads and downloads.
  - Large file uploads and downloads (handling large files without memory spikes).
  - Offline mode (queueing files while disconnected).
  - Connection recovery (WorkManager automatically uploads queued media upon regaining internet).
  - App termination (closing the app while uploading, confirming WorkManager resumes successfully).
  - Device reboot (verifying scheduled jobs persist and resume).

### 🎬 Estados (Stories / Status)
- [ ] **Subidas (Uploads)**: Status/stories continue to upload successfully without regressions.
- [ ] **Descargas (Downloads)**: Status media downloads cleanly.
- [ ] **Reproducción (Playback)**: Video/image stories render and play correctly.
- [ ] **WorkManager Isolation**: Confirm that chat's WorkManager migration has absolutely no side-effects on Stories uploading.

### 👤 Perfil (Profile)
- [ ] **Foto de perfil**: Changing the profile picture works flawlessly and uses current local stream/upload logic.

### 📰 Publicaciones (Reels & Feed)
- [ ] **UploadRepository integration**: Verify that feed publications, comments, and other media-heavy screens continue to use `UploadRepository` unmodified and function correctly.

---

## 📊 Phase 1 Post-Implementation Metrics (Rendimiento)
To confirm Phase 1 success, the following metrics must be tracked and presented:
1. Max RAM usage during media upload (must avoid out-of-memory exceptions).
2. Average upload time for media files.
3. Recovery time after connection loss.
4. Resumption time after app force close.
5. Temp files generated vs. successfully cleaned up.
6. Number of full-read `readBytes()` calls eliminated from the main UI thread.

---

## 🧠 Repo Knowledge (learned 2026-08-21)

- **DB en vivo**: proyecto Supabase `tivqjfgjdxgzicrridaz`. `thread_messages.text_content` es la columna de texto (NO existe `content`). `social.user_stories` / `social.user_reels` viven en el esquema `social` (Realtime topics `realtime:social:*`); `audio_url` no existía (ver migración `20260821010000_social_stories_audio_url.sql`).
- **Probar columnas sin JWT**: `GET /rest/v1/<tabla>?select=<col>` con anon key: `42501` = columna existe (RLS bloquea), `42703` = columna NO existe. `PGRST205` = tabla no existe en ese schema (probar header `Accept-Profile: social`).
- **Room guarda contenido YA DESENCRIPTADO** de mensajes (collector realtime desencripta antes de merge). Nunca re-aplicar desencriptación al leer de Room: convierte texto plano en "[Mensaje cifrado]".
- **Merge realtime**: un evento con `content` vacío nunca debe pisar contenido local no vacío (`MessageDao.mergeEntities`). El mapeo realtime debe preferir `text_content` sobre `content` y tratar JSON null como ausente.
- **FCM app cerrada**: declarar `com.google.firebase.messaging.default_notification_channel_id` = `panalink_messages_v3` en el manifest y crear canales en `PanaApplication.onCreate`.
- **Compilación**: el sandbox no tiene JDK ni Android SDK; verificar sintaxis a mano o instalar toolchain antes de `./gradlew`.
- **Contactos por PIN/QR**: agregar contacto = enviar SOLICITUD (`send_friend_request_by_pin(p_to_pin_raw)` / `send_friend_request_by_qr(p_qr_token)`); el contacto mutuo solo se crea cuando el receptor acepta (`accept_friend_request(p_request_id)`). Firmas verificadas en vivo vía PGRST202/42501.
- **Push**: solo `thread_messages` tenía trigger FCM (`fcm_notify_on_new_message`); `friend_requests` no tenía ninguno → migración `20260821020000_friend_request_push.sql` agrega push al receptor (INSERT) y al emisor (UPDATE a accepted). El payload de send-push es `{user_id, title, body}` + header `x-internal-secret` = GUC `app.edge_secret`.
- **Realtime**: los canales se joinean a mano en `SupabaseClient.onOpen` (protocolo Phoenix sobre OkHttp WS). Tablas sin join propio caen al branch de mensajes de chat. `friend_requests` ahora tiene canal + flow `realtimeFriendRequests` consumido por `ChatsViewModel` para recargar solicitudes en vivo (requiere `alter publication supabase_realtime add table`, incluido en la migración).
