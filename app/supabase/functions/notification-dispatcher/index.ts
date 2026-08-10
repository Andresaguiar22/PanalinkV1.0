import { serve } from 'https://deno.land/std@0.168.0/http/server.ts';
import { createClient } from 'https://esm.sh/@supabase/supabase-js@2';
import { NotificationEventPayload } from './types.ts';
import { validateEvent } from './rules.ts';
import { prepareNotificationRecord } from './aggregation.ts';
import { sendFcmNotification } from './fcm.ts';

serve(async (req) => {
  try {
    const supabaseUrl = Deno.env.get('SUPABASE_URL') ?? '';
    const supabaseServiceKey = Deno.env.get('SUPABASE_SERVICE_ROLE_KEY') ?? '';
    const supabase = createClient(supabaseUrl, supabaseServiceKey);

    const body = await req.json();
    const event: NotificationEventPayload = body.record || body;

    if (!event || !event.id) {
      return new Response(JSON.stringify({ error: 'Invalid event payload' }), {
        status: 400,
        headers: { 'Content-Type': 'application/json' }
      });
    }

    console.log(`Processing notification_event: ${event.id}, type: ${event.event_type}`);

    // Mark event as PROCESSING
    await supabase
      .from('notification_events')
      .update({ status: 'PROCESSING', processed_at: new Date().toISOString() })
      .eq('id', event.id);

    // 1. Validation
    const validation = await validateEvent(supabase, event);
    if (!validation.isValid) {
      console.log(`Event ${event.id} ignored: ${validation.reason}`);
      await supabase
        .from('notification_events')
        .update({ status: 'IGNORED', error_message: validation.reason })
        .eq('id', event.id);

      return new Response(JSON.stringify({ status: 'IGNORED', reason: validation.reason }), {
        status: 200,
        headers: { 'Content-Type': 'application/json' }
      });
    }

    // 2. Prepare Record for notifications_v2 with source_event_id idempotency constraint
    const record = prepareNotificationRecord(event);

    const { data: insertedNotification, error: insertError } = await supabase
      .from('notifications_v2')
      .insert(record)
      .select()
      .single();

    if (insertError) {
      // Check for duplicate source_event_id violation (idempotency check)
      if (insertError.code === '23505') {
        console.log(`Duplicate event ${event.id} detected via source_event_id constraint`);
        await supabase
          .from('notification_events')
          .update({ status: 'PROCESSED', error_message: 'Duplicate event skipped' })
          .eq('id', event.id);

        return new Response(JSON.stringify({ status: 'DUPLICATE_SKIPPED' }), {
          status: 200,
          headers: { 'Content-Type': 'application/json' }
        });
      }

      console.error(`Error inserting into notifications_v2 for event ${event.id}:`, insertError);
      await supabase
        .from('notification_events')
        .update({ status: 'FAILED', error_message: insertError.message })
        .eq('id', event.id);

      return new Response(JSON.stringify({ error: insertError.message }), {
        status: 500,
        headers: { 'Content-Type': 'application/json' }
      });
    }

    // 3. Send FCM Push Notification to target user's active devices
    const fcmResult = await sendFcmNotification(supabase, insertedNotification);

    // 4. Mark event as PROCESSED
    await supabase
      .from('notification_events')
      .update({ status: 'PROCESSED', processed_at: new Date().toISOString() })
      .eq('id', event.id);

    return new Response(
      JSON.stringify({
        status: 'PROCESSED',
        notification_id: insertedNotification.id,
        fcm: fcmResult
      }),
      { status: 200, headers: { 'Content-Type': 'application/json' } }
    );
  } catch (err: any) {
    console.error('Unhandled exception in notification-dispatcher:', err);
    return new Response(JSON.stringify({ error: err.message || 'Internal error' }), {
      status: 500,
      headers: { 'Content-Type': 'application/json' }
    });
  }
});
