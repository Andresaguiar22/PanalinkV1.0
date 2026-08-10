-- SUPABASE SCHEMA FOR PANALINK 🇻🇪

-- 1. Enable UUID Extension
create extension if not exists "uuid-ossp";

-- 2. Create Profiles Table (linked to auth.users)
create table public.profiles (
    id uuid references auth.users on delete cascade primary key,
    display_name text not null,
    avatar_url text null,
    privacy_level text default 'public' check (privacy_level in ('public', 'friends_only', 'private')) not null,
    created_at timestamp with time zone default now() not null
);

-- Enable RLS on Profiles
alter table public.profiles enable row level security;

-- Profiles Policies
create policy "Allow authenticated users to read all profiles"
on public.profiles for select
to authenticated
using (true); -- Required to search users by name

create policy "Allow users to update their own profile"
on public.profiles for update
to authenticated
using (auth.uid() = id)
with check (auth.uid() = id);

-- 3. Create Chats Table
create table public.chats (
    id uuid default gen_random_uuid() primary key,
    created_at timestamp with time zone default now() not null,
    type text not null default 'dm'
);

-- Enable RLS on Chats
alter table public.chats enable row level security;

-- 4. Create Chat Members Table
create table public.chat_members (
    chat_id uuid references public.chats(id) on delete cascade,
    user_id uuid references public.profiles(id) on delete cascade,
    role text default 'member' not null,
    joined_at timestamp with time zone default now() not null,
    primary key (chat_id, user_id)
);

-- Enable RLS on Chat Members
alter table public.chat_members enable row level security;

-- Chats Policies (A user only sees chats they are a member of)
create policy "Allow users to select chats they are member of"
on public.chats for select
to authenticated
using (
    exists (
        select 1 from public.chat_members
        where chat_members.chat_id = public.chats.id
        and chat_members.user_id = auth.uid()
    )
);

create policy "Allow users to create chats"
on public.chats for insert
to authenticated
with check (true);

-- Chat Members Policies
create policy "Allow members to see memberships"
on public.chat_members for select
to authenticated
using (
    exists (
        select 1 from public.chat_members as cm
        where cm.chat_id = public.chat_members.chat_id
        and cm.user_id = auth.uid()
    ) or (user_id = auth.uid())
);

create policy "Allow authenticated users to insert chat memberships"
on public.chat_members for insert
to authenticated
with check (true); -- Needed for creating chats and adding participants

-- 5. Create Messages Table
create table public.messages (
    id uuid default gen_random_uuid() primary key,
    chat_id uuid references public.chats(id) on delete cascade not null,
    sender_id uuid references public.profiles(id) on delete cascade not null,
    content text not null,
    created_at timestamp with time zone default now() not null,
    status text default 'sent' not null, -- 'sent', 'delivered', 'seen'
    reply_to_message_id uuid references public.messages(id) on delete set null
);

-- Enable RLS on Messages
alter table public.messages enable row level security;

-- Messages Policies (Only chat members can read/write messages)
create policy "Allow members to read messages"
on public.messages for select
to authenticated
using (
    exists (
        select 1 from public.chat_members
        where chat_members.chat_id = public.messages.chat_id
        and chat_members.user_id = auth.uid()
    )
);

create policy "Allow members to insert messages"
on public.messages for insert
to authenticated
with check (
    auth.uid() = sender_id
    and exists (
        select 1 from public.chat_members
        where chat_members.chat_id = public.messages.chat_id
        and chat_members.user_id = auth.uid()
    )
);

-- 6. Create User States (Stories/Estados) Table
create table public.user_media_statuses (
    id uuid default gen_random_uuid() primary key,
    user_id uuid references public.profiles(id) on delete cascade not null,
    media_url text null,
    media_type text not null, -- 'text' | 'image' | 'video'
    caption text null,
    visibility text default 'contacts' not null, -- 'public' | 'contacts' | 'private'
    duration_seconds numeric null, -- duration of video in seconds
    expires_at timestamp with time zone not null,
    created_at timestamp with time zone default now() not null,
    constraint chk_video_duration check (media_type <> 'video' or duration_seconds is null or duration_seconds <= 60)
);

-- Enable RLS on User States
alter table public.user_media_statuses enable row level security;

-- User States Policies
create policy "Allow users to see non-expired public, contact, or own statuses"
on public.user_media_statuses for select
to authenticated
using (
    user_id = auth.uid() or (
        expires_at > now() and (
            visibility = 'public' or 
            (visibility = 'contacts' and (
                exists (
                    select 1 from public.contacts
                    where (owner_user_id = auth.uid() and contact_user_id = user_media_statuses.user_id)
                       or (owner_user_id = user_media_statuses.user_id and contact_user_id = auth.uid())
                )
            ))
        )
    )
);

create policy "Allow users to insert their own states"
on public.user_media_statuses for insert
to authenticated
with check (
    auth.uid() = user_id
);

create policy "Allow users to delete their own states"
on public.user_media_statuses for delete
to authenticated
using (
    auth.uid() = user_id
);

-- Indexes to accelerate Feed (Stories & Reels)
create index if not exists idx_user_statuses_visibility_created_at_desc
  on public.user_media_statuses (visibility, created_at desc);

create index if not exists idx_user_statuses_expires_at
  on public.user_media_statuses (expires_at);

create index if not exists idx_user_statuses_author_created_at_desc
  on public.user_media_statuses (user_id, created_at desc);


-- 7. Automated Profile Trigger (upon auth.users creation)
create or replace function public.handle_new_user()
returns trigger as $$
begin
    insert into public.profiles (id, display_name, avatar_url)
    values (
        new.id,
        coalesce(new.raw_user_meta_data->>'display_name', split_part(new.email, '@', 1)),
        coalesce(new.raw_user_meta_data->>'avatar_url', null)
    );
    return new;
end;
$$ language plpgsql security definer;

create trigger on_auth_user_created
    after insert on auth.users
    for each row execute procedure public.handle_new_user();

-- 8. Storage Buckets configuration (To run in Supabase SQL editor if needed)
-- Note: Buckets can be created in the Supabase Dashboard, but these SQL queries will pre-configure them.

-- Insert buckets if they don't exist
insert into storage.buckets (id, name, public)
values 
  ('avatars', 'avatars', true),
  ('user-states', 'user-states', true)
on conflict (id) do nothing;

-- RLS for avatars bucket
create policy "Allow public avatar select"
on storage.objects for select
to authenticated
using (bucket_id = 'avatars');

create policy "Allow user avatar upload"
on storage.objects for insert
to authenticated
with check (bucket_id = 'avatars' and (storage.foldername(name))[1] = auth.uid()::text);

create policy "Allow user avatar update"
on storage.objects for update
to authenticated
using (bucket_id = 'avatars' and (storage.foldername(name))[1] = auth.uid()::text);

create policy "Allow user avatar delete"
on storage.objects for delete
to authenticated
using (bucket_id = 'avatars' and (storage.foldername(name))[1] = auth.uid()::text);

-- RLS for user-states bucket
create policy "Allow public user-states select"
on storage.objects for select
to authenticated
using (bucket_id = 'user-states');

create policy "Allow user state upload"
on storage.objects for insert
to authenticated
with check (bucket_id = 'user-states' and (storage.foldername(name))[1] = auth.uid()::text);

-- 9. One-to-One Threads Table (if not exists)
create table if not exists public.one_to_one_threads (
    id uuid default gen_random_uuid() primary key,
    user_a uuid references public.profiles(id) on delete cascade not null,
    user_b uuid references public.profiles(id) on delete cascade not null,
    created_at timestamp with time zone default now() not null,
    unique (user_a, user_b)
);

-- Enable RLS
alter table public.one_to_one_threads enable row level security;

create policy "Allow users to see their own threads"
on public.one_to_one_threads for select
to authenticated
using (user_a = auth.uid() or user_b = auth.uid());

create policy "Allow users to insert threads"
on public.one_to_one_threads for insert
to authenticated
with check (user_a = auth.uid() or user_b = auth.uid());

-- 10. Thread Messages Table (if not exists)
create table if not exists public.thread_messages (
    id uuid default gen_random_uuid() primary key,
    thread_id uuid references public.one_to_one_threads(id) on delete cascade not null,
    sender_id uuid references public.profiles(id) on delete cascade not null,
    content text not null,
    created_at timestamp with time zone default now() not null,
    status text default 'sent' not null,
    client_message_uuid text null
);

-- Enable RLS
alter table public.thread_messages enable row level security;

create policy "Allow users to see messages in their threads"
on public.thread_messages for select
to authenticated
using (
    sender_id = auth.uid()
    or exists (
        select 1 from public.one_to_one_threads
        where id = thread_id and (user_a = auth.uid() or user_b = auth.uid())
    )
);

create policy "Allow users to insert messages in their threads"
on public.thread_messages for insert
to authenticated
with check (
    sender_id = auth.uid() and
    exists (
        select 1 from public.one_to_one_threads
        where id = thread_id and (user_a = auth.uid() or user_b = auth.uid())
    )
);

-- 11. Contacts Table
create table if not exists public.contacts (
    id uuid default gen_random_uuid() primary key,
    owner_user_id uuid references public.profiles(id) on delete cascade not null,
    contact_user_id uuid references public.profiles(id) on delete cascade not null,
    created_at timestamp with time zone default now() not null,
    unique (owner_user_id, contact_user_id)
);

-- Enable RLS on Contacts
alter table public.contacts enable row level security;

create policy "Allow users to see their own contacts"
on public.contacts for select
to authenticated
using (owner_user_id = auth.uid());

create policy "Allow users to insert their own contacts"
on public.contacts for insert
to authenticated
with check (owner_user_id = auth.uid());

-- 12. Add pin_hash to profiles
alter table public.profiles add column if not exists pin_hash text null;

-- Enable pgcrypto extension for secure SHA-256 digest hashing
create extension if not exists pgcrypto;

-- 13. Ensure One to One Thread Function
create or replace function public.ensure_one_to_one_thread(p_user_a uuid, p_user_b uuid)
returns uuid as $$
declare
    v_thread_id uuid;
begin
    -- Check if thread exists in either user_a/user_b ordering
    select id into v_thread_id
    from public.one_to_one_threads
    where (user_a = p_user_a and user_b = p_user_b) 
       or (user_a = p_user_b and user_b = p_user_a)
    limit 1;
    
    if v_thread_id is null then
        -- Create new thread
        insert into public.one_to_one_threads (id, user_a, user_b, created_at)
        values (gen_random_uuid(), p_user_a, p_user_b, now())
        returning id into v_thread_id;
    end if;
    
    return v_thread_id;
end;
$$ language plpgsql security definer;

-- 14. Add Contact by PIN Function (Legacy wrapper delegating to add_contact_by_identifier)
create or replace function public.add_contact_by_pin(p_pin text)
returns json as $$
begin
    return public.add_contact_by_identifier(p_pin);
end;
$$ language plpgsql security definer;

-- 15. Private Contact Identifiers Table (PIN and QR tokens decoupled from public profiles)
create table if not exists public.contact_identifiers (
    id uuid default gen_random_uuid() primary key,
    user_id uuid references public.profiles(id) on delete cascade not null unique,
    pin_raw text not null,
    pin_hash text not null unique,
    qr_token text not null unique,
    created_at timestamp with time zone default now() not null,
    updated_at timestamp with time zone default now() not null,
    revoked_at timestamp with time zone null
);

-- Ensure check constraint on contacts table
alter table public.contacts drop constraint if exists chk_contacts_not_self;
alter table public.contacts add constraint chk_contacts_not_self check (owner_user_id <> contact_user_id);

-- Enable RLS on contact_identifiers
alter table public.contact_identifiers enable row level security;

-- Policy: Only authenticated user can view their own contact identifier
create policy "Allow users to see only their own contact identifier"
on public.contact_identifiers for select
to authenticated
using (user_id = auth.uid());

-- RPC 1: Get or generate current authenticated user's contact identifier (PIN & QR Payload)
create or replace function public.get_my_contact_identifier()
returns json as $$
declare
    v_user_id uuid;
    v_row public.contact_identifiers%rowtype;
    v_pin text;
    v_pin_hash text;
    v_qr_token text;
    v_attempts int := 0;
begin
    v_user_id := auth.uid();
    if v_user_id is null then
        raise exception 'No autenticado';
    end if;

    -- Check if user already has an active identifier
    select * into v_row
    from public.contact_identifiers
    where user_id = v_user_id and revoked_at is null
    limit 1;

    if v_row.id is not null then
        return json_build_object(
            'pin', v_row.pin_raw,
            'qr_token', v_row.qr_token,
            'qr_payload', 'panalink:contact:' || v_row.qr_token
        );
    end if;

    -- Generate unique 6-digit PIN and unique QR token with collision check loop
    loop
        v_attempts := v_attempts + 1;
        if v_attempts > 50 then
            raise exception 'No se pudo generar un PIN único tras varios intentos';
        end if;

        v_pin := lpad(floor(random() * 1000000)::text, 6, '0');
        v_pin_hash := encode(digest(v_pin, 'sha256'), 'hex');

        if not exists (select 1 from public.contact_identifiers where pin_hash = v_pin_hash) then
            exit;
        end if;
    end loop;

    v_qr_token := encode(gen_random_bytes(16), 'hex');

    insert into public.contact_identifiers (user_id, pin_raw, pin_hash, qr_token)
    values (v_user_id, v_pin, v_pin_hash, v_qr_token)
    returning * into v_row;

    return json_build_object(
        'pin', v_row.pin_raw,
        'qr_token', v_row.qr_token,
        'qr_payload', 'panalink:contact:' || v_row.qr_token
    );
end;
$$ language plpgsql security definer;

-- RPC 2: Resolve identifier (PIN / QR payload) to target public profile safely
create or replace function public.resolve_contact_identifier(p_identifier text)
returns json as $$
declare
    v_clean text;
    v_target_id uuid;
    v_target_profile record;
    v_input_hash text;
begin
    if auth.uid() is null then
        raise exception 'No autenticado';
    end if;

    v_clean := trim(p_identifier);

    -- 1. Check if format is new QR token: panalink:contact:<qr_token> or raw hex qr_token (32 chars)
    if v_clean like 'panalink:contact:%' then
        v_clean := substring(v_clean from 18);
    end if;

    if length(v_clean) = 32 then
        select user_id into v_target_id
        from public.contact_identifiers
        where qr_token = v_clean and revoked_at is null
        limit 1;
    end if;

    -- 2. Check if format is PIN or PIN Hash (panalink:pin:<6digits> or 6 digits or 64 hex hash)
    if v_target_id is null then
        if v_clean like 'panalink:pin:%' then
            v_clean := substring(v_clean from 14);
        end if;

        if length(v_clean) = 64 then
            v_input_hash := v_clean;
        else
            v_clean := regexp_replace(v_clean, '\D', '', 'g');
            if length(v_clean) = 6 then
                v_input_hash := encode(digest(v_clean, 'sha256'), 'hex');
            end if;
        end if;

        if v_input_hash is not null then
            -- Query private contact_identifiers first
            select user_id into v_target_id
            from public.contact_identifiers
            where pin_hash = v_input_hash and revoked_at is null
            limit 1;

            -- Backward compatibility: check legacy profiles.pin_hash if not found in contact_identifiers
            if v_target_id is null then
                select id into v_target_id
                from public.profiles
                where pin_hash = v_input_hash
                limit 1;
            end if;
        end if;
    end if;

    if v_target_id is null then
        raise exception 'Identificador de Pana no encontrado o inválido';
    end if;

    select id, display_name, avatar_url into v_target_profile
    from public.profiles
    where id = v_target_id;

    if v_target_profile.id is null then
        raise exception 'Perfil de usuario no encontrado';
    end if;

    return json_build_object(
        'user_id', v_target_profile.id,
        'display_name', v_target_profile.display_name,
        'avatar_url', v_target_profile.avatar_url
    );
end;
$$ language plpgsql security definer;

-- RPC 3: Add contact by identifier (supports both PIN and QR tokens, creates bidirectional contact)
create or replace function public.add_contact_by_identifier(p_identifier text)
returns json as $$
declare
    v_current_user uuid;
    v_resolved json;
    v_target_id uuid;
    v_display_name text;
    v_avatar_url text;
    v_thread_id uuid;
begin
    v_current_user := auth.uid();
    if v_current_user is null then
        raise exception 'No autenticado';
    end if;

    -- Resolve identifier
    v_resolved := public.resolve_contact_identifier(p_identifier);
    v_target_id := (v_resolved->>'user_id')::uuid;
    v_display_name := v_resolved->>'display_name';
    v_avatar_url := v_resolved->>'avatar_url';

    if v_target_id = v_current_user then
        raise exception 'No puedes agregarte a ti mismo como contacto';
    end if;

    -- Bidirectional contact insertion in atomic transaction
    insert into public.contacts (owner_user_id, contact_user_id)
    values
        (v_current_user, v_target_id),
        (v_target_id, v_current_user)
    on conflict (owner_user_id, contact_user_id) do nothing;

    -- Ensure 1-to-1 thread exists
    v_thread_id := public.ensure_one_to_one_thread(v_current_user, v_target_id);

    return json_build_object(
        'success', true,
        'contact_id', v_target_id,
        'display_name', v_display_name,
        'avatar_url', v_avatar_url,
        'thread_id', v_thread_id
    );
end;
$$ language plpgsql security definer;

-- Set permissions explicitly
revoke execute on function public.get_my_contact_identifier() from anon;
grant execute on function public.get_my_contact_identifier() to authenticated;

revoke execute on function public.resolve_contact_identifier(text) from anon;
grant execute on function public.resolve_contact_identifier(text) to authenticated;

revoke execute on function public.add_contact_by_identifier(text) from anon;
grant execute on function public.add_contact_by_identifier(text) to authenticated;

revoke execute on function public.add_contact_by_pin(text) from anon;
grant execute on function public.add_contact_by_pin(text) to authenticated;

-- =========================
-- A) Estados persistentes en thread_messages
-- =========================
alter table public.thread_messages
  add column if not exists delivered_at timestamp with time zone null,
  add column if not exists seen_at timestamp with time zone null,
  add column if not exists updated_at timestamp with time zone not null default now();

-- Trigger updated_at para thread_messages
create or replace function public.set_updated_at()
returns trigger
language plpgsql
as $$
begin
  new.updated_at = now();
  return new;
end;
$$;

drop trigger if exists trg_thread_messages_updated_at on public.thread_messages;

create trigger trg_thread_messages_updated_at
before update on public.thread_messages
for each row execute function public.set_updated_at();

-- =========================
-- B) RLS UPDATE en thread_messages
-- =========================
alter table public.thread_messages enable row level security;

drop policy if exists "tm: update delivered/seen (recipient only)" on public.thread_messages;

create policy "tm: update delivered/seen (recipient only)"
on public.thread_messages
for update
to authenticated
using (
  exists (
    select 1
    from public.one_to_one_threads t
    where t.id = thread_messages.thread_id
      and (t.user_a = auth.uid() or t.user_b = auth.uid())
  )
  and thread_messages.sender_id <> auth.uid()
)
with check (
  exists (
    select 1
    from public.one_to_one_threads t
    where t.id = thread_messages.thread_id
      and (t.user_a = auth.uid() or t.user_b = auth.uid())
  )
  and thread_messages.sender_id <> auth.uid()
  and (
    seen_at is null
    or delivered_at is not null
  )
);

-- =========================
-- C) Tabla reacciones (message_reactions)
-- =========================
create table if not exists public.message_reactions (
  thread_message_id uuid not null,
  user_id uuid not null,
  emoji text not null,
  created_at timestamp with time zone not null default now(),
  updated_at timestamp with time zone not null default now(),
  primary key (thread_message_id, user_id),
  constraint fk_message_reactions_thread_message
    foreign key (thread_message_id) references public.thread_messages(id) on delete cascade,
  constraint fk_message_reactions_user
    foreign key (user_id) references public.profiles(id) on delete cascade
);

-- Trigger updated_at para message_reactions
create or replace function public.set_message_reactions_updated_at()
returns trigger
language plpgsql
as $$
begin
  new.updated_at = now();
  return new;
end;
$$;

drop trigger if exists trg_message_reactions_updated_at on public.message_reactions;

create trigger trg_message_reactions_updated_at
before update on public.message_reactions
for each row execute function public.set_message_reactions_updated_at();

-- =========================
-- D) RLS message_reactions
-- =========================
alter table public.message_reactions enable row level security;

drop policy if exists "mr: select participants" on public.message_reactions;
create policy "mr: select participants"
on public.message_reactions
for select
to authenticated
using (
  exists (
    select 1
    from public.thread_messages tm
    join public.one_to_one_threads t on t.id = tm.thread_id
    where tm.id = message_reactions.thread_message_id
      and (t.user_a = auth.uid() or t.user_b = auth.uid())
  )
);

drop policy if exists "mr: insert own reaction" on public.message_reactions;
create policy "mr: insert own reaction"
on public.message_reactions
for insert
to authenticated
with check (
  message_reactions.user_id = auth.uid()
);

drop policy if exists "mr: update own reaction" on public.message_reactions;
create policy "mr: update own reaction"
on public.message_reactions
for update
to authenticated
using (
  message_reactions.user_id = auth.uid()
)
with check (
  message_reactions.user_id = auth.uid()
);

drop policy if exists "mr: delete own reaction" on public.message_reactions;
create policy "mr: delete own reaction"
on public.message_reactions
for delete
to authenticated
using (
  message_reactions.user_id = auth.uid()
);


-- =========================
-- F) Friend Requests
-- =========================
create table if not exists public.friend_requests (
    id uuid default gen_random_uuid() primary key,
    sender_id uuid references public.profiles(id) on delete cascade not null,
    receiver_id uuid references public.profiles(id) on delete cascade not null,
    status text default 'pending' check (status in ('pending', 'accepted', 'declined')) not null,
    created_at timestamp with time zone default now() not null,
    updated_at timestamp with time zone default now() not null,
    unique (sender_id, receiver_id)
);

alter table public.friend_requests enable row level security;

create policy "Select requests" on public.friend_requests for select to authenticated using (sender_id = auth.uid() or receiver_id = auth.uid());
create policy "Insert requests" on public.friend_requests for insert to authenticated with check (sender_id = auth.uid());
create policy "Update requests" on public.friend_requests for update to authenticated using (receiver_id = auth.uid());

create or replace function public.accept_friend_request(req_id uuid)
returns void
language plpgsql
security definer
as $$
declare
    v_sender_id uuid;
    v_receiver_id uuid;
begin
    -- Lock and get request
    select sender_id, receiver_id into v_sender_id, v_receiver_id
    from public.friend_requests
    where id = req_id and receiver_id = auth.uid() and status = 'pending'
    for update;

    if not found then
        raise exception 'Request not found or not authorized';
    end if;

    -- Update status
    update public.friend_requests
    set status = 'accepted', updated_at = now()
    where id = req_id;

    -- Add to contacts
    insert into public.contacts (owner_user_id, contact_user_id)
    values (v_sender_id, v_receiver_id), (v_receiver_id, v_sender_id)
    on conflict do nothing;
end;
$$;

-- E) RPC Decline Friend Request
create or replace function public.decline_friend_request(req_id uuid)
returns void
language plpgsql
security definer
as $$
begin
    -- Validate that the request exists, is pending, and the user is the receiver
    if not exists (
        select 1 from public.friend_requests
        where id = req_id
        and receiver_id = auth.uid()
        and status = 'pending'
    ) then
        raise exception 'Request not found or not authorized';
    end if;

    -- Update status to declined
    update public.friend_requests
    set status = 'declined', updated_at = now()
    where id = req_id;
end;
$$;

-- F) RPC Send Friend Request
create or replace function public.send_friend_request(p_receiver_id uuid)
returns void
language plpgsql
security definer
as $$
declare
    v_privacy_level text;
    v_is_already_contact boolean;
begin
    -- Get receiver privacy level
    select privacy_level into v_privacy_level from public.profiles where id = p_receiver_id;
    
    if v_privacy_level = 'private' then
        raise exception 'Usuario no acepta solicitudes';
    end if;

    if v_privacy_level = 'friends_only' then
        -- Check if they are already friends (in contacts)
        select exists (
            select 1 from public.contacts 
            where owner_user_id = auth.uid() and contact_user_id = p_receiver_id
        ) into v_is_already_contact;
        
        if not v_is_already_contact then
             raise exception 'Solo amigos pueden enviar solicitudes';
        end if;
    end if;

    -- Insert request
    insert into public.friend_requests (sender_id, receiver_id)
    values (auth.uid(), p_receiver_id)
    on conflict do nothing;
end;
$$;

