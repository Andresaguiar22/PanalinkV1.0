import { NotificationEventPayload } from './types.ts';

export interface ValidationResult {
  isValid: boolean;
  reason?: string;
}

export async function validateEvent(
  supabaseClient: any,
  event: NotificationEventPayload
): Promise<ValidationResult> {
  // 1. Basic field validation
  if (!event.id || !event.event_type || !event.target_user_id) {
    return { isValid: false, reason: 'Missing required event fields' };
  }

  // 2. Reject self-notifications
  if (event.actor_id && event.actor_id === event.target_user_id) {
    return { isValid: false, reason: 'Self-notification discarded (actor_id equals target_user_id)' };
  }

  // 3. Verify target_user_id exists
  const { data: targetUser, error: targetErr } = await supabaseClient
    .from('profiles')
    .select('id')
    .eq('id', event.target_user_id)
    .single();

  if (targetErr || !targetUser) {
    return { isValid: false, reason: `Target user ${event.target_user_id} does not exist` };
  }

  // 4. Check user block relationship if actor_id is present
  if (event.actor_id) {
    const { data: blocks, error: blockErr } = await supabaseClient
      .from('user_blocks')
      .select('id')
      .or(`and(blocker_id.eq.${event.target_user_id},blocked_id.eq.${event.actor_id}),and(blocker_id.eq.${event.actor_id},blocked_id.eq.${event.target_user_id})`)
      .limit(1);

    if (!blockErr && blocks && blocks.length > 0) {
      return { isValid: false, reason: 'Blocked user interaction' };
    }
  }

  return { isValid: true };
}
