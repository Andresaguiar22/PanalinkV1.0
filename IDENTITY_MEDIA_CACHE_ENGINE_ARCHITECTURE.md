# IDENTITY & MEDIA CACHE ENGINE (IMCE) ARCHITECTURE

## Visión General
El **Identity & Media Cache Engine (IMCE)** es la infraestructura responsable de convertir a **Room** en la única fuente de la verdad para toda la información pública de usuarios (Profile) y sus recursos multimedia asociados (Avatares y Portadas).

Este documento describe la **Fase P1** de IMCE, que establece la infraestructura offline-first.

## Principios
1. **Offline-First:** La interfaz de usuario siempre lee de Room o de la memoria caché. Nunca espera una respuesta de red.
2. **Cero Regresiones:** El módulo `LegacyIdentityBridge` permite la coexistencia segura con el código preexistente durante el proceso de migración progresivo.
3. **Optimización Multimedia:** Las imágenes ya no se consultan indiscriminadamente por red. Se descargan y persisten de forma segura y se referencian localmente.

## Estructura de Componentes

### 1. `ProfileEntity` (Base de Datos Room)
La entidad local se ha evolucionado mediante una **Migración Explícita (V27 -> V28)** sin destruir datos:
* `avatarLocalPath`: Referencia a archivo físico.
* `coverLocalPath`: Referencia a archivo físico.
* `updatedAt`: Sello de tiempo de la última modificación local/remota.
* `lastSyncedAt`: Último momento de sincronización con Supabase.
* `syncVersion`: Versión incremental de la entidad (optimistic locking).
* `isDirty`: Bandera para entidades que necesitan subirse.
* `isDeleted`: Borrado lógico.

### 2. `IdentityRepository`
Único punto de acceso para la manipulación de perfiles.
* Exponer flujos reactivos con `observeProfile` y `observeProfiles`.
* Implementar validaciones y control de almacenamiento.

### 3. Caching en Memoria (`IdentityMemoryCache`)
* Cache LRU ultrarrápida.
* Almacena objetos de dominio `CachedProfile`.
* No guarda bitmaps de imágenes. Solo mantiene las rutas físicas (Sting localPaths) para delegarle el renderizado a `Coil`.

### 4. Storage Managers (`AvatarStorageManager` & `CoverStorageManager`)
* Administran el ciclo de vida de los archivos físicos (descarga, actualización, lectura).
* Guardan los archivos bajo el almacenamiento privado de la app (`filesDir/avatars/users/` y `filesDir/covers/users/`).
* Regla: "Room únicamente almacenará el local path. Nunca ByteArrays".

### 5. `IdentitySyncManager`
El motor en segundo plano que coordina los datos:
1. Lee de **Supabase**.
2. Almacena binarios en **Files**.
3. Guarda la entidad estructurada en **Room**.

### 6. `IdentityAnalytics`
Monitoriza el rendimiento del motor IMCE, calculando métricas críticas como:
* Eficiencia de la memoria/Room vs uso de red (`Room Hits` vs `Network Hits`).
* Tiempo promedio de sincronizaciones.
* Registro de fallos.

## Flujo de Sincronización
1. **Trigger:** Inicio de la app, apertura de un chat, o mensaje entrante que contiene información de un usuario no guardado.
2. **SyncManager** verifica el estado en Room. Si es necesario, descarga los JSONs y archivos desde Supabase.
3. **StorageManagers** descargan de manera asincrónica el contenido multimedia.
4. **Repository** actualiza Room.
5. Room emite los flujos reactivos que son capturados automáticamente por la UI en los consumidores refactorizados.

## Estado P1
La base tecnológica IMCE se encuentra construida, evaluada, con migraciones correctas en base de datos.
*(No se han impactado las vistas ni los módulos de Supabase previos para asegurar el principio de Cero Regresiones).*
