-- Salas de Voz (Voice Rooms) — modulo completamente independiente del chat privado.
--
-- Decisiones de diseno:
-- * 4 tablas nuevas en public, con prefijo voice_room_ para no colisionar con nada.
-- * NO hay tabla persistente de senalizacion WebRTC: offer/answer/ICE son efimeros
--   y viajan por eventos broadcast de Supabase Realtime en el topic voice_room:{id}.
-- * Nunca se almacena audio en Supabase.
-- * La membresia se consulta via is_voice_room_member() (security definer) para
--   evitar recursividad infinita de RLS al consultar voice_room_members.

-- ============================================================
-- Tablas
-- ============================================================

create table if not exists public.voice_rooms (
  id          uuid primary key default gen_random_uuid(),
  name        text not null,
  owner_id    uuid not null references auth.users(id) on delete cascade,
  status      text not null default 'live' check (status in ('live', 'closed')),
  max_seats   int  not null default 7 check (max_seats = 7),
  created_at  timestamptz not null default now(),
  updated_at  timestamptz not null default now()
);

create table if not exists public.voice_room_members (
  id         uuid primary key default gen_random_uuid(),
  room_id    uuid not null references public.voice_rooms(id) on delete cascade,
  user_id    uuid not null references auth.users(id) on delete cascade,
  role       text not null default 'listener' check (role in ('owner', 'speaker', 'listener')),
  joined_at  timestamptz not null default now(),
  left_at    timestamptz
);

-- Un usuario no puede estar dos veces activo en la misma sala.
create unique index if not exists voice_room_members_active_unique
  on public.voice_room_members (room_id, user_id)
  where left_at is null;

create table if not exists public.voice_room_seats (
  id         uuid primary key default gen_random_uuid(),
  room_id    uuid not null references public.voice_rooms(id) on delete cascade,
  seat_index int  not null check (seat_index between 0 and 6),
  user_id    uuid not null references auth.users(id) on delete cascade,
  is_muted   boolean not null default false,
  joined_at  timestamptz not null default now(),
  unique (room_id, seat_index),   -- un sillon, una persona
  unique (room_id, user_id)       -- una persona, un sillon (regla anti doble-sillon a nivel DB)
);

create table if not exists public.voice_room_messages (
  id         uuid primary key default gen_random_uuid(),
  room_id    uuid not null references public.voice_rooms(id) on delete cascade,
  sender_id  uuid not null references auth.users(id) on delete cascade,
  content    text not null check (char_length(content) between 1 and 2000),
  created_at timestamptz not null default now()
);

create index if not exists voice_room_messages_room_idx on public.voice_room_messages (room_id, created_at);
create index if not exists voice_room_members_room_idx  on public.voice_room_members (room_id) where left_at is null;
create index if not exists voice_room_seats_room_idx    on public.voice_room_seats (room_id);

-- ============================================================
-- Funcion anti-recursividad para RLS
-- ============================================================

create or replace function public.is_voice_room_member(p_room_id uuid)
returns boolean
language sql
stable
security definer
set search_path = 'pg_catalog', 'public'
as $$
  select exists (
    select 1
      from public.voice_room_members m
     where m.room_id = p_room_id
       and m.user_id = auth.uid()
       and m.left_at is null
  );
$$;

revoke all on function public.is_voice_room_member(uuid) from public;
grant execute on function public.is_voice_room_member(uuid) to authenticated;

-- ============================================================
-- RLS
-- ============================================================

alter table public.voice_rooms         enable row level security;
alter table public.voice_room_members  enable row level security;
alter table public.voice_room_seats    enable row level security;
alter table public.voice_room_messages enable row level security;

-- voice_rooms: cualquier autenticado puede descubrir salas; solo el owner las crea/edita.
drop policy if exists voice_rooms_select on public.voice_rooms;
create policy voice_rooms_select on public.voice_rooms
  for select to authenticated
  using (status = 'live' or owner_id = auth.uid());

drop policy if exists voice_rooms_insert on public.voice_rooms;
create policy voice_rooms_insert on public.voice_rooms
  for insert to authenticated
  with check (owner_id = auth.uid());

drop policy if exists voice_rooms_update on public.voice_rooms;
create policy voice_rooms_update on public.voice_rooms
  for update to authenticated
  using (owner_id = auth.uid())
  with check (owner_id = auth.uid());

-- voice_room_members: leer solo si perteneces; entrar solo como tu mismo; salir solo tu.
drop policy if exists voice_room_members_select on public.voice_room_members;
create policy voice_room_members_select on public.voice_room_members
  for select to authenticated
  using (public.is_voice_room_member(room_id) or user_id = auth.uid());

drop policy if exists voice_room_members_insert on public.voice_room_members;
create policy voice_room_members_insert on public.voice_room_members
  for insert to authenticated
  with check (user_id = auth.uid());

drop policy if exists voice_room_members_update on public.voice_room_members;
create policy voice_room_members_update on public.voice_room_members
  for update to authenticated
  using (user_id = auth.uid())
  with check (user_id = auth.uid());

-- voice_room_seats: leer si perteneces; ocupar/editar/liberar solo tu propio sillon.
drop policy if exists voice_room_seats_select on public.voice_room_seats;
create policy voice_room_seats_select on public.voice_room_seats
  for select to authenticated
  using (public.is_voice_room_member(room_id));

drop policy if exists voice_room_seats_insert on public.voice_room_seats;
create policy voice_room_seats_insert on public.voice_room_seats
  for insert to authenticated
  with check (user_id = auth.uid() and public.is_voice_room_member(room_id));

drop policy if exists voice_room_seats_update on public.voice_room_seats;
create policy voice_room_seats_update on public.voice_room_seats
  for update to authenticated
  using (user_id = auth.uid())
  with check (user_id = auth.uid());

drop policy if exists voice_room_seats_delete on public.voice_room_seats;
create policy voice_room_seats_delete on public.voice_room_seats
  for delete to authenticated
  using (user_id = auth.uid());

-- voice_room_messages: leer si perteneces; enviar solo como miembro y como tu mismo.
drop policy if exists voice_room_messages_select on public.voice_room_messages;
create policy voice_room_messages_select on public.voice_room_messages
  for select to authenticated
  using (public.is_voice_room_member(room_id));

drop policy if exists voice_room_messages_insert on public.voice_room_messages;
create policy voice_room_messages_insert on public.voice_room_messages
  for insert to authenticated
  with check (sender_id = auth.uid() and public.is_voice_room_member(room_id));

-- ============================================================
-- Realtime
-- ============================================================

do $$
begin
  if not exists (select 1 from pg_publication_tables where pubname = 'supabase_realtime' and schemaname = 'public' and tablename = 'voice_rooms') then
    alter publication supabase_realtime add table public.voice_rooms;
  end if;
  if not exists (select 1 from pg_publication_tables where pubname = 'supabase_realtime' and schemaname = 'public' and tablename = 'voice_room_members') then
    alter publication supabase_realtime add table public.voice_room_members;
  end if;
  if not exists (select 1 from pg_publication_tables where pubname = 'supabase_realtime' and schemaname = 'public' and tablename = 'voice_room_seats') then
    alter publication supabase_realtime add table public.voice_room_seats;
  end if;
  if not exists (select 1 from pg_publication_tables where pubname = 'supabase_realtime' and schemaname = 'public' and tablename = 'voice_room_messages') then
    alter publication supabase_realtime add table public.voice_room_messages;
  end if;
end $$;
