-- ============================================================
-- STOCKPULSE - FASE 6: Motor de trading (funcion atomica)
--
-- Ejecutar en: Supabase Dashboard -> SQL Editor -> New query
-- (es un CREATE OR REPLACE: se puede re-ejecutar con seguridad)
--
-- Reglas implementadas:
-- - El usuario solo opera SOBRE SI MISMO (auth.uid() forzado dentro).
-- - Ni antics ni frontend pueden tocar la wallet fuera de aqui
--   (SECURITY DEFINER con controles internos, RLS desk al usuario).
-- - Compra: valida fondos, debita wallet, actualiza holdings con
--   precio promedio ponderado, registra transaction + credit_movement.
-- - Venta: valida acciones, acredita wallet, calcula P&L real.
-- - Todo atomico: si algo falla, no se mueve ningun credito.
-- ============================================================

create or replace function public.execute_trade(
  p_symbol text,
  p_action text,           -- 'buy' | 'sell'
  p_quantity integer,
  p_unit_price numeric
)
returns json
language plpgsql
security definer
set search_path = public
as $$
declare
  v_user uuid := nullif(auth.uid()::text, '')::uuid;
  v_total numeric(14, 2);
  v_balance numeric(14, 2);
  v_qty_current integer;
  v_avg_current numeric(12, 4);
  v_new_qty integer;
  v_new_avg numeric(12, 4);
  v_pnl numeric(14, 2);
  v_tx public.transactions.id%type;
  v_balance_after numeric(14, 2);
begin
  -- 1. Debe haber sesion valida (el backend retransmite el JWT del usuario)
  if v_user is null then
    raise exception 'Sesion no valida: inicia sesion de nuevo';
  end if;

  -- 2. Validaciones de entrada
  p_symbol := upper(btrim(p_symbol));
  p_action := lower(btrim(p_action));

  if p_quantity is null or p_quantity <= 0 then
    raise exception 'La cantidad debe ser al menos 1 accion';
  end if;
  if p_quantity > 100000 then
    raise exception 'La cantidad maxima por operacion es 100,000 acciones';
  end if;
  if p_unit_price is null or p_unit_price <= 0 then
    raise exception 'Precio invalido: no hay cotizacion disponible';
  end if;
  if p_action not in ('buy', 'sell') then
    raise exception 'Operacion desconocida: usa buy o sell';
  end if;
  if not exists (select 1 from public.stocks s where s.symbol = p_symbol) then
    raise exception 'El simbolo % no esta en el catalogo', p_symbol;
  end if;

  v_total := round(p_quantity * p_unit_price, 2);

  if p_action = 'buy' then
    -- ---------- COMPRA ----------
    -- Bloquea la wallet para evitar carreras (doble compra simultanea)
    select balance into v_balance
    from public.virtual_wallets
    where user_id = v_user
    for update;

    if v_balance is null then
      raise exception 'No existe billetera para tu cuenta. Contacta soporte';
    end if;
    if v_balance < v_total then
      raise exception 'Fondos insuficientes: necesitas % creditos y tienes %',
        to_char(v_total, 'FM999,999.00'), to_char(v_balance, 'FM999,999.00');
    end if;

    update public.virtual_wallets
    set balance = balance - v_total
    where user_id = v_user;

    v_balance_after := v_balance - v_total;

    -- Holding: acumular con promedio ponderado
    select quantity, avg_price into v_qty_current, v_avg_current
    from public.holdings
    where user_id = v_user and symbol = p_symbol
    for update;

    v_new_qty := coalesce(v_qty_current, 0) + p_quantity;
    v_new_avg := round(((coalesce(v_avg_current, 0) * coalesce(v_qty_current, 0)) + v_total) / v_new_qty, 4);

    insert into public.holdings (user_id, symbol, quantity, avg_price)
    values (v_user, p_symbol, v_new_qty, v_new_avg)
    on conflict (user_id, symbol) do update
      set quantity = v_new_qty,
          avg_price = v_new_avg;

    insert into public.transactions (user_id, symbol, type, quantity, unit_price, total, profit_loss)
    values (v_user, p_symbol, 'buy', p_quantity, p_unit_price, v_total, null)
    returning id into v_tx;

    insert into public.credit_movements (user_id, type, amount, balance_after, reference)
    values (v_user, 'buy_stocks', -v_total, v_balance_after, v_tx::text);

  else
    -- ---------- VENTA ----------
    select quantity, avg_price into v_qty_current, v_avg_current
    from public.holdings
    where user_id = v_user and symbol = p_symbol
    for update;

    if v_qty_current is null then
      raise exception 'No tienes acciones de % para vender', p_symbol;
    end if;
    if v_qty_current < p_quantity then
      raise exception 'No tienes suficientes acciones de %: tienes %, pediste %',
        p_symbol, v_qty_current, p_quantity;
    end if;

    v_pnl := round((p_unit_price - v_avg_current) * p_quantity, 2);

    -- Bloquea wallet y acredita la venta
    select balance into v_balance
    from public.virtual_wallets
    where user_id = v_user
    for update;
    if v_balance is null then
      raise exception 'No existe billetera para tu cuenta';
    end if;

    update public.virtual_wallets
    set balance = balance + v_total
    where user_id = v_user;

    v_balance_after := v_balance + v_total;

    -- Holding: reducir (o eliminar si queda en 0)
    v_new_qty := v_qty_current - p_quantity;
    if v_new_qty = 0 then
      delete from public.holdings
      where user_id = v_user and symbol = p_symbol;
    else
      update public.holdings
      set quantity = v_new_qty  -- el avg_price queda intacto (costo de lo restante)
      where user_id = v_user and symbol = p_symbol;
    end if;

    insert into public.transactions (user_id, symbol, type, quantity, unit_price, total, profit_loss)
    values (v_user, p_symbol, 'sell', p_quantity, p_unit_price, v_total, v_pnl)
    returning id into v_tx;

    insert into public.credit_movements (user_id, type, amount, balance_after, reference)
    values (v_user, 'sell_stocks', v_total, v_balance_after, v_tx::text);
  end if;

  return json_build_object(
    'ok', true,
    'transactionId', v_tx,
    'symbol', p_symbol,
    'action', p_action,
    'quantity', p_quantity,
    'unitPrice', p_unit_price,
    'total', v_total,
    'profitLoss', v_pnl,
    'walletBalance', v_balance_after
  );
end;
$$;

-- Empoderar ejecucion a usuarios autenticados (los checks internos
-- garantizan que solo operen sobre su propia data)
grant execute on function public.execute_trade(text, text, integer, numeric) to authenticated;
-- Nota: el backend la usa retransmitiendo el JWT del usuario (no con service key),
-- asi que RLS esta fuera del camino y las validaciones internas mandan.
