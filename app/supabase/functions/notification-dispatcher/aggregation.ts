import { NotificationEventPayload, NotificationV2Record } from './types.ts';

export function prepareNotificationRecord(event: NotificationEventPayload): NotificationV2Record {
  const domain = event.domain || getDomainFromType(event.event_type);
  const groupingKey = event.grouping_key || `${event.event_type}_${event.entity_id || 'general'}`;
  const priority = event.priority || 'NORMAL';

  // Calculate 30-day expiration date
  const expiresAt = new Date(Date.now() + 30 * 24 * 60 * 60 * 1000).toISOString();

  return {
    recipient_id: event.target_user_id,
    actor_id: event.actor_id || 'system',
    domain: domain,
    type: event.event_type,
    grouping_key: groupingKey,
    priority: priority,
    payload: {
      title: event.title || 'PanaLink',
      body: event.body || 'Nueva notificación',
      entity_id: event.entity_id,
      ...(event.payload || {})
    },
    source_event_id: event.id,
    expires_at: expiresAt,
    read: false
  };
}

function getDomainFromType(type: string): string {
  if (type.startsWith('CHAT_')) return 'CHAT';
  if (type.startsWith('CALL_')) return 'CALLS';
  if (type.startsWith('STORY_') || type.startsWith('REEL_') || type.startsWith('POST_')) return 'SOCIAL';
  if (type.startsWith('SYSTEM_') || type.startsWith('SECURITY_')) return 'SYSTEM';
  return 'SOCIAL';
}
