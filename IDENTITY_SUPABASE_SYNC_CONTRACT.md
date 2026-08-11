# Identity Supabase Sync Contract (Phase P2.5)

## 📌 Objetivo de Sincronización
Este documento establece las reglas de arquitectura y sincronización entre el backend remoto (Supabase) y el caché local (Identity Management and Caching Engine - IMCE), asegurando el funcionamiento Offline First y rendimiento óptimo en producción.

## 🗃️ Campos Sincronizados
La arquitectura de identidad se divide en dos fuentes dependiendo del contexto:

### 1. Identidad Propia (Módulo IMCE - Private)
El usuario autenticado sincroniza su propio perfil desde `public.profiles` hacia el caché local `local_profiles`.

| Remoto (Supabase `profiles`) | Local (Room `local_profiles`) | Descripción |
|-----------------------------|------------------------------|-------------|
| `id` (UUID)                 | `id` (String, PK)            | Identificador único del usuario. |
| `display_name` (Text)       | `displayName` (String)       | Nombre para mostrar. |
| `avatar_url` (Text)         | `avatarUrl` (String?)        | URL remota original del avatar. |

### 2. Identidad de Terceros (Public Identity)
Para resolver la identidad de contactos, miembros de grupos y otros usuarios, se utiliza la tabla `public.public_profiles` (Fuente de Verdad Única) sincronizada hacia Room `public_profiles`.

| Remoto (Supabase `public_profiles`) | Local (Room `public_profiles`) |
|-------------------------------------|-------------------------------|
| `id` (UUID)                         | `id` (String, PK)             |
| `display_name` (Text)               | `displayName` (String)        |
| `first_name` (Text)                 | `firstName` (String)          |
| `last_name` (Text)                  | `lastName` (String)           |
| `avatar_url` (Text)                 | `avatarUrl` (String)          |
| `updated_at` (Timestamp)            | `updatedAt` (String)          |

*Nota: Actualmente `coverUrl` no está en la base de datos principal, pero la arquitectura local lo soporta.*

## 📐 Reglas de Oro de Sincronización

1. **Supabase es la fuente de la verdad (remota).**
   - Todos los cambios de perfil (nombre, avatar) nacen de una petición a Supabase, luego se reflejan localmente.

2. **Room es la fuente de datos operacional (local).**
   - La interfaz de usuario (Compose) **NUNCA** lee perfiles directamente de Supabase.
   - Compose observa exclusivamente los Flow emitidos por Room vía `IdentityRepository`.

3. **Storage es la única fuente multimedia (UI).**
   - Coil / Jetpack Compose **NUNCA** descargan imágenes directamente de la red usando `avatarUrl`.
   - La UI utiliza `avatarLocalPath` (archivo físico). Si no existe, delega a IMCE para descargarlo en background y luego Coil renderiza el archivo físico.

4. **Sincronización Silenciosa y Desacoplada.**
   - Al iniciar la app (o en un Worker), `IdentitySyncManager` actualiza Room de manera incremental.
   - Room notifica a Compose. Si hay avatar nuevo, `IdentitySyncManager` baja el archivo y actualiza Room de nuevo. Compose reacciona al instante sin bloqueos de UI.

## 🧹 Reglas de Limpieza y Basura (Garbage Collection)
El componente `MediaStorageCleaner` se ejecuta periódicamente (ej. cada reinicio o por Worker semanal) y sigue estas reglas:
- Elimina avatares/portadas de `filesDir` **SI Y SOLO SI** el path del archivo no existe en la tabla `local_profiles`.
- Se conserva todo archivo multimedia de mensajes y estados (Reels/Stories).
- Elimina archivos corruptos (0 bytes).

---
*IMCE Hardening - Panalink V2.0*
