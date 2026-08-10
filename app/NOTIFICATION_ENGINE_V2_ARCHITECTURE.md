# PanaLink V2.0 — Notification Engine V2 Architectural Documentation

## 1. Context & Architecture Overview

The PanaLink V2.0 Notification Engine V2 is an asynchronous, decoupled, multi-tiered event processing pipeline designed to handle real-time social interactions, direct messages, voice/video calls, stories, reels, and system alerts at scale without blocking the main Android thread.

```
+-----------------------------------------------------------------------------------+
|                                EVENT PRODUCERS                                    |
| (WallEventPublisher, ChatEventPublisher, StoriesReelsEventPublisher, CallEvent)  |
+-----------------------------------------------------------------------------------+
                                          |
                                          v
                         +---------------------------------+
                         |   NotificationEventPublisher    |
                         +---------------------------------+
                                    /           \
                                   /             \
                                  v               v
                +-------------------+          +----------------------------------+
                | Notification Engine|          | Supabase notification_events     |
                | Local V2 Pipeline |          +----------------------------------+
                +-------------------+                         |
                          |                                   v
                          v                       +-----------------------+
                +-------------------+             | notification-dispatcher|
                | Room Database V2  |             |     (Edge Function)   |
                +-------------------+             +-----------------------+
                                                              |
                                                              v
                                                  +-----------------------+
                                                  |    notifications_v2   |
                                                  +-----------------------+
                                                              |
                                                              v
                                                  +-----------------------+
                                                  |   Supabase Realtime   |
                                                  | (notifications:<uuid>)|
                                                  +-----------------------+
                                                              |
                                                              v
                                                  +-----------------------+
                                                  | Realtime Subscriber   |
                                                  |   (Android Client)    |
                                                  +-----------------------+
```

---

## 2. Core Architectural Pillars

### A. Non-Blocking Event Dispatching
- **`NotificationEventPublisher`**: Single entry point for event producers across Feed, Chat, Stories, Reels, and Calls.
- **Self-Notification Guard**: Rejects events where `actor_id == target_user_id`.
- **LEGACY + V2 Bridge**: Dual-publishing maintains full backward compatibility with `NotificationHelper` and legacy presenters while feeding the new V2 engine pipeline.

### B. Intelligent Rules Engine & Aggregation
- **Rule Resolution**: Dynamic rules per `NotificationTypeV2` (priority, deduplication, quiet hours, spam detection).
- **Social Aggregation (`SmartAggregationEngine`)**: Merges multiple rapid interactions (e.g., "A, B and 15 others liked your post") into unified notification items based on sliding time windows.
- **Quiet Hours & Deduplication Engine**: Mutes non-critical alerts during user quiet hours; suppresses identical repeated event IDs (`deduplicationKey`).

### C. Backend Dispatcher Edge Function (`notification-dispatcher`)
- **Event Validation**: Ensures existing target users, applies user block checks, drops self-notifications.
- **Idempotency Constraint**: Uses `notifications_v2.source_event_id UNIQUE` constraint to guarantee 1:1 event processing even on retries.
- **Retry Mechanism & Status Pipeline**: Events transition through `PENDING` -> `PROCESSING` -> `PROCESSED` (or `IGNORED` / `FAILED` / `RETRYING`).
- **FCM Push Notification Hardening**: Sends FCM pushes to active devices and automatically deactivates obsolete tokens on 400/404/NotRegistered error codes.

### D. Private Supabase Realtime Delivery
- **Private User Channels**: Devices subscribe strictly to `notifications:<user_uuid>`.
- **Security Check**: `NotificationRealtimeSubscriber` verifies that the `targetUserId` matches the currently authenticated user session (`currentUser.id`) before routing events to `NotificationEngine.publish()`.
- **Strict Pipeline Processing**: Realtime events are never saved directly to the database by the WebSocket listener; they pass through the complete `NotificationEngine` pipeline for deduplication, rules evaluation, priority calculation, and analytics tracking.

---

## 3. Database Schema Reference

### `notification_events` (Queue Table)
| Column | Type | Description |
|---|---|---|
| `id` | UUID (PK) | Event identifier |
| `event_type` | VARCHAR | Notification type string |
| `actor_id` | UUID | User triggering the event |
| `target_user_id` | UUID | Recipient user |
| `entity_id` | VARCHAR | Target entity ID (post, story, call, etc.) |
| `domain` | VARCHAR | Domain (SOCIAL, CHAT, CALLS, SYSTEM) |
| `title` | VARCHAR | Notification title |
| `body` | TEXT | Notification content |
| `grouping_key` | VARCHAR | Aggregation grouping key |
| `priority` | VARCHAR | Priority level (LOW, NORMAL, HIGH, CRITICAL) |
| `payload` | JSONB | Additional metadata |
| `status` | VARCHAR | Processing status (PENDING, PROCESSING, PROCESSED, FAILED, IGNORED) |
| `attempt_count` | INT | Retry count |
| `created_at` | TIMESTAMPTZ | Event creation timestamp |
| `processed_at` | TIMESTAMPTZ | Processing completion timestamp |

### `notifications_v2` (Delivery Table)
| Column | Type | Description |
|---|---|---|
| `id` | UUID (PK) | Notification record ID |
| `recipient_id` | UUID | Recipient user ID |
| `actor_id` | UUID | Actor user ID |
| `domain` | VARCHAR | Domain name |
| `type` | VARCHAR | Notification type |
| `grouping_key` | VARCHAR | Grouping key |
| `priority` | VARCHAR | Priority level |
| `payload` | JSONB | Render payload |
| `source_event_id` | UUID (UNIQUE)| Idempotency source event link |
| `read` | BOOLEAN | Read status |
| `expires_at` | TIMESTAMPTZ | Retention limit (30 days) |
| `created_at` | TIMESTAMPTZ | Record creation timestamp |

---

## 4. Production Security Policies (RLS)

1. **`notification_events`**:
   - `INSERT`: Restricted to system actors or authenticated session users.
   - `SELECT`: Restricted strictly to `target_user_id = auth.uid()`.
2. **`notifications_v2`**:
   - `SELECT`: Restricted strictly to `recipient_id = auth.uid()`.
   - `UPDATE`: Restricted strictly to `recipient_id = auth.uid()` for updating `read` state.
3. **`user_devices`**:
   - Device tokens restricted to owned `user_id = auth.uid()`. Inactive devices (> 90 days without activity) automatically cleaned up.

---

## 5. Performance Metrics & Stress Testing

- **Throughput**: Validated for **>3,300 events/second** in high-concurrency memory benchmarks without UI thread blocking.
- **Latency**: Sub-15ms local pipeline processing latency.
- **Memory Footprint**: Strict LruCache bounds (max 10% heap allocation for notification media and avatar caches).
