# PanaLink — TURBO MODE

Rama de trabajo: `audit/full-app-2026-08-11`

## Objetivo
Convertir la rama de auditoría en una versión consolidada, robusta y medible de PanaLink sin tocar `main` hasta que los cambios estén verificados.

## Prioridades

### P0 — Integridad de datos
- Unificar la fuente de verdad de contadores e interacciones.
- Eliminar doble incremento/decremento entre Room, Realtime, RPC y triggers.
- Garantizar idempotencia de likes, favoritos, shares, comentarios y vistas.
- Reconciliar correctamente estados locales pendientes frente a Supabase confirmado.
- Revisar RLS de todos los dominios sociales.

### P0 — Contratos Android ↔ Supabase
- Comparar DTOs, Entities, DAOs, Retrofit y RPCs con las columnas reales.
- Identificar RPC duplicadas/legacy y dependencias reales antes de consolidar.
- Generar/actualizar SQL versionado sin destruir el estado actual.

### P1 — Realtime
- Verificar suscripciones y claves de INSERT/UPDATE/DELETE.
- Evitar eventos duplicados y eventos perdidos durante refresh/reconexión.
- Asegurar que Room sea la fuente reactiva para Compose.

### P1 — Rendimiento multimedia
- Auditar precarga, caché, MediaItem, CDN y health checks.
- Reducir latencia de primer frame de Reels/Stories.
- Evitar descargas duplicadas y trabajo innecesario en Compose.

### P1 — Feed/Muro
- Mantener separación real respecto a Reels.
- Consolidar tarjetas grandes y acciones sociales sin duplicar repositorios.
- Validar likes/comentarios/shares offline-first y Realtime.

### P2 — Arquitectura
- Detectar archivos/carpeta duplicados o históricos.
- No eliminar nada hasta demostrar que no participa en build, runtime o Supabase.
- Reducir complejidad accidental y conservar contratos públicos necesarios.

### P2 — Tests
- Añadir pruebas para invariantes de sincronización.
- Añadir regresiones para offline/online, Realtime duplicado, retry, 401, 409 y reconexión.
- Ejecutar build/tests antes de cada consolidación significativa.

## Reglas de ejecución

1. `main` permanece intacta.
2. Cambios únicamente en esta rama hasta revisión.
3. Preferir cambios pequeños, reversibles y verificables.
4. No sustituir una arquitectura existente por otra sin evidencia de mejora.
5. No modificar CDN, autenticación, mensajería o RLS sensible sin cruzar todas sus dependencias.
6. Toda corrección de backend debe tener contrato equivalente en Android.
7. Toda optimización de UI debe conservar Room/Repository como fuente de estado.
8. Antes de eliminar una duplicación, buscar todas las referencias en código y SQL.

## Primer objetivo técnico
Cerrar el circuito social completo:

`UI → Repository → Room/Optimistic State → PendingSocialAction → Worker → Supabase/RPC → Trigger → Realtime → Repository → Room → UI`

Debe ser idempotente, observable, recuperable tras reinicio y consistente después de reconexión.
