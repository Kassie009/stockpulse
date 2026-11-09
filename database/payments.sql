-- ============================================================
-- STOCKPULSE - FASE 8: Acreditacion de creditos tras pago Stripe
--
-- Ejecutar en: Supabase Dashboard -> SQL Editor -> Run
-- (CREATE OR REPLACE: seguro de re-ejecutar)
--
-- Reglas:
-- - Atomica: lock de la wallet (FOR UPDATE), actualiza saldo y
--   registra el movement en la misma transaccion.
-- - Idempotente: si la referencia ya existe, devuelve el saldo
--   sin duplicar (protege contra retrys de webhook/status).
-- ============================================================

create or replace function public.credit_wallet(
  p_user_id uuid,
  p_credits numeric,
  p_reference text
)
returns json
language plpgsql
security definer
set search_path = public
as $$
declare
  v_balance numeric(14, 2);
  v_balance_after numeric(14, 2);
  v_amount numeric(14, 2) := p_credits;  -- entrada positiva
begin
  if p_user_id is null or p_credits is null or p_credits <= 0 then
    raise exception 'Datos de acreditacion invalidos';
  end if;

  -- Idempotencia: referencia ya acreditada -> no duplicar
  if exists (
    select 1 from public.credit_movements
    where reference = p_reference and type = 'package_purchase'
  ) then
    select balance into v_balance from public.virtual_wallets
    where user_id = p_user_id;
    return json_build_object(
      'ok', true,
      'already', true,
      'balance', coalesce(v_balance, 0)
    );
  end if;

  select balance into v_balance
  from public.virtual_wallets
  where user_id = p_user_id
  for update;

  if v_balance is null then
    raise exception 'No existe billetera para esa cuenta';
  end if;

  update public.virtual_wallets
  set balance = balance + v_amount
  where user_id = p_user_id;

  v_balance_after := v_balance + v_amount;

  insert into public.credit_movements
    (user_id, type, amount, balance_after, reference)
  values
    (p_user_id, 'package_purchase', v_amount, v_balance_after, p_reference);

  return json_build_object(
    'ok', true,
    'already', false,
    'balance', v_balance_after
  );
end;
$$;

grant execute on function public.credit_wallet(uuid, numeric, text) to service_role;
