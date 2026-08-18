-- Chat delivery root-cause repair.
-- The relationship-score helper is intentionally private. The AFTER INSERT
-- trigger must therefore execute as SECURITY DEFINER so a client INSERT is
-- not rolled back by EXECUTE privilege/RLS on user_relationship_scores.

create or replace function public.trg_thread_messages_after_insert()
returns trigger
language plpgsql
security definer
set search_path = ''
as $$
begin
  perform public.bump_relationship(
    new.sender_id,
    new.receiver_id,
    1,
    0,
    0,
    1,
    null
  );
  return new;
end;
$$;

revoke execute on function public.trg_thread_messages_after_insert() from public, anon, authenticated;
revoke execute on function public.bump_relationship(uuid, uuid, bigint, bigint, bigint, numeric, boolean) from public, anon, authenticated;
