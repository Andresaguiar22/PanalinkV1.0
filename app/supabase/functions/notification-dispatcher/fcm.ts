import { NotificationV2Record, UserDevice } from './types.ts';

export async function sendFcmNotification(
  supabaseClient: any,
  notification: NotificationV2Record
): Promise<{ sentCount: number; failedCount: number }> {
  // 1. Fetch active devices for recipient
  const { data: devices, error } = await supabaseClient
    .from('user_devices')
    .select('*')
    .eq('user_id', notification.recipient_id)
    .eq('is_active', true);

  if (error || !devices || devices.length === 0) {
    return { sentCount: 0, failedCount: 0 };
  }

  const fcmServerKey = Deno.env.get('FCM_SERVER_KEY');
  if (!fcmServerKey) {
    console.warn('FCM_SERVER_KEY env var not set, skipping remote Push delivery');
    return { sentCount: 0, failedCount: devices.length };
  }

  let sentCount = 0;
  let failedCount = 0;

  for (const device of devices as UserDevice[]) {
    try {
      const token = device.fcm_token || (device as any).push_token || '';
      if (token.startsWith('device_fallback_') || (device as any).token_type === 'FALLBACK') {
        console.log(`Fallback device token detected (${token}) for device ${device.id}. Skipping FCM delivery.`);
        
        // Audit push_skipped_reason in notifications_v2 payload for traceability
        if (notification.id && notification.payload) {
          const updatedPayload = {
            ...notification.payload,
            push_skipped_reason: 'fallback_device'
          };
          await supabaseClient
            .from('notifications_v2')
            .update({ payload: updatedPayload })
            .eq('id', notification.id);
        }
        continue;
      }

      const response = await fetch('https://fcm.googleapis.com/fcm/send', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `key=${fcmServerKey}`
        },
        body: JSON.stringify({
          to: device.fcm_token,
          priority: notification.priority === 'CRITICAL' || notification.priority === 'HIGH' ? 'high' : 'normal',
          notification: {
            title: notification.payload.title,
            body: notification.payload.body,
            sound: 'default'
          },
          data: {
            notification_id: notification.id,
            domain: notification.domain,
            type: notification.type,
            grouping_key: notification.grouping_key,
            entity_id: notification.payload.entity_id || ''
          }
        })
      });

      if (response.ok) {
        sentCount++;
      } else {
        failedCount++;
        console.error(`FCM push failed for device ${device.id}, status: ${response.status}`);
        if (response.status === 400 || response.status === 404) {
          console.log(`Deactivating obsolete device token for device ${device.id}`);
          await supabaseClient
            .from('user_devices')
            .update({ is_active: false, updated_at: new Date().toISOString() })
            .eq('id', device.id);
        }
      }
    } catch (e) {
      failedCount++;
      console.error(`Error sending FCM push to device ${device.id}`, e);
    }
  }

  return { sentCount, failedCount };
}
