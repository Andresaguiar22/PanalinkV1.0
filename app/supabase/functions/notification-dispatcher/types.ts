export interface NotificationEventPayload {
  id: string;
  event_type: string;
  actor_id: string;
  target_user_id: string;
  entity_id: string;
  domain?: string;
  title?: string;
  body?: string;
  grouping_key?: string;
  priority?: string;
  payload?: Record<string, any>;
  created_at?: string;
}

export interface NotificationV2Record {
  id?: string;
  recipient_id: string;
  actor_id: string;
  domain: string;
  type: string;
  grouping_key?: string;
  priority: string;
  payload: Record<string, any>;
  source_event_id: string;
  expires_at?: string;
  read?: boolean;
  created_at?: string;
}

export interface UserDevice {
  id: string;
  user_id: string;
  fcm_token: string;
  device_type: string;
  is_active: boolean;
  updated_at?: string;
}

export type EventStatus = 'PENDING' | 'PROCESSING' | 'PROCESSED' | 'FAILED' | 'IGNORED' | 'RETRYING';

export interface NotificationEventPayload {
  id: string;
  event_type: string;
  actor_id: string;
  target_user_id: string;
  entity_id: string;
  domain?: string;
  title?: string;
  body?: string;
  grouping_key?: string;
  priority?: string;
  payload?: Record<string, any>;
  attempt_count?: number;
  created_at?: string;
}
