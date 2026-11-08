-- ============================================================
-- STOCKPULSE — Esquema de base de datos (Supabase / PostgreSQL)
-- FASE A — Ejecutar completo en el SQL Editor de Supabase.
-- Idempotente: puede re-ejecutarse sin duplicar objetos.
-- ============================================================

-- ------------------------------------------------------------
-- 0. Utilidades
-- ------------------------------------------------------------

-- Actualiza automaticamente updated_at en INSERT/UPDATE
create or replace function public.touch_updated_at()
returns trigger
language plpgsql
as $$
begin
  new.updated_at = now();
  return new;
end;
$$;

-- ------------------------------------------------------------
-- 1. users (extiende auth.users)
-- ------------------------------------------------------------
create table if not exists public.users (
  id uuid primary key references auth.users (id) on delete cascade,
  email text not null,
  full_name text,
  avatar_url text,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

drop trigger if exists users_updated_at on public.users;
create trigger users_updated_at
  before update on public.users
  for each row execute function public.touch_updated_at();

-- ------------------------------------------------------------
-- 2. stocks (catalogo de empresas)
-- ------------------------------------------------------------
create table if not exists public.stocks (
  symbol text primary key,
  name text not null,
  exchange text not null default 'US',
  currency text not null default 'USD',
  is_popular boolean not null default false,
  created_at timestamptz not null default now()
);

-- Seed inicial (7 empresas del prompt, seccion 4.1)
insert into public.stocks (symbol, name, is_popular) values
  ('AAPL',  'Apple Inc.',          true),
  ('MSFT',  'Microsoft Corp.',     true),
  ('NVDA',  'NVIDIA Corp.',        true),
  ('TSLA',  'Tesla Inc.',          true),
  ('AMZN',  'Amazon.com Inc.',     true),
  ('GOOGL', 'Alphabet Inc.',       true),
  ('META',  'Meta Platforms Inc.', true)
on conflict (symbol) do nothing;

-- ------------------------------------------------------------
-- 3. watchlists (listas de seguimiento por usuario)
-- ------------------------------------------------------------
create table if not exists public.watchlists (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references public.users (id) on delete cascade,
  name text not null default 'Mi lista',
  created_at timestamptz not null default now(),
  unique (user_id, name)
);

-- ------------------------------------------------------------
-- 4. stock_prices (historial/caché de precios, alimentado por backend)
-- ------------------------------------------------------------
create table if not exists public.stock_prices (
  id bigint generated always as identity primary key,
  symbol text not null references public.stocks (symbol) on delete cascade,
  price numeric(12, 4) not null,
  change_percent numeric(7, 4),
  is_delayed boolean not null default false,
  recorded_at timestamptz not null default now()
);
create index if not exists idx_stock_prices_symbol_time
  on public.stock_prices (symbol, recorded_at desc);

-- ------------------------------------------------------------
-- 5. portfolios (valor del portafolio por usuario; detalle en holdings)
-- ------------------------------------------------------------
create table if not exists public.portfolios (
  user_id uuid primary key references public.users (id) on delete cascade,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

drop trigger if exists portfolios_updated_at on public.portfolios;
create trigger portfolios_updated_at
  before update on public.portfolios
  for each row execute function public.touch_updated_at();

-- ------------------------------------------------------------
-- 6. virtual_wallets (saldo virtual; NADIE puede modificarlo directo)
-- ------------------------------------------------------------
create table if not exists public.virtual_wallets (
  user_id uuid primary key references public.users (id) on delete cascade,
  balance numeric(14, 2) not null default 10000 check (balance >= 0),
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

drop trigger if exists wallets_updated_at on public.virtual_wallets;
create trigger wallets_updated_at
  before update on public.virtual_wallets
  for each row execute function public.touch_updated_at();

-- ------------------------------------------------------------
-- 7. holdings (acciones virtuales poseidas)
-- ------------------------------------------------------------
create table if not exists public.holdings (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references public.users (id) on delete cascade,
  symbol text not null references public.stocks (symbol),
  quantity integer not null check (quantity > 0),
  avg_price numeric(12, 4) not null check (avg_price > 0),
  updated_at timestamptz not null default now(),
  unique (user_id, symbol)
);

drop trigger if exists holdings_updated_at on public.holdings;
create trigger holdings_updated_at
  before update on public.holdings
  for each row execute function public.touch_updated_at();

-- ------------------------------------------------------------
-- 8. transactions (historial de compras/ventas simuladas)
-- ------------------------------------------------------------
create table if not exists public.transactions (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references public.users (id) on delete cascade,
  symbol text not null references public.stocks (symbol),
  type text not null check (type in ('buy', 'sell')),
  quantity integer not null check (quantity > 0),
  unit_price numeric(12, 4) not null check (unit_price > 0),
  total numeric(14, 2) not null,
  profit_loss numeric(14, 2),          -- solo lleno en 'sell'
  price_source text not null default 'finnhub_quote',
  created_at timestamptz not null default now()
);
create index if not exists idx_transactions_user_time
  on public.transactions (user_id, created_at desc);

-- ------------------------------------------------------------
-- 9. alerts (reglas de alerta configurables)
-- ------------------------------------------------------------
create table if not exists public.alerts (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references public.users (id) on delete cascade,
  symbol text not null references public.stocks (symbol),
  -- price_above | price_below | change_pos | change_neg | volatility_high
  type text not null check (type in
    ('price_above', 'price_below', 'change_pos', 'change_neg', 'volatility_high')),
  threshold numeric(12, 4) not null,
  active boolean not null default true,
  last_triggered_at timestamptz,
  created_at timestamptz not null default now()
);
create index if not exists idx_alerts_active
  on public.alerts (active) where active;

-- ------------------------------------------------------------
-- 10. alert_history (registro de disparos de alertas)
-- ------------------------------------------------------------
create table if not exists public.alert_history (
  id uuid primary key default gen_random_uuid(),
  alert_id uuid not null references public.alerts (id) on delete cascade,
  user_id uuid not null references public.users (id) on delete cascade,
  message text not null,
  triggered_at timestamptz not null default now()
);
create index if not exists idx_alert_history_user_time
  on public.alert_history (user_id, triggered_at desc);

-- ------------------------------------------------------------
-- 11. devices (tokens FCM / dispositivos y reloje vinculado)
-- ------------------------------------------------------------
create table if not exists public.devices (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references public.users (id) on delete cascade,
  fcm_token text,
  platform text not null default 'android' check (platform in
    ('android', 'wearos')),
  device_name text,
  watch_connected boolean not null default false,
  last_seen_at timestamptz not null default now(),
  created_at timestamptz not null default now()
);
-- Evita tokens duplicados por usuario/plataforma (indice de expresion)
create unique index if not exists idx_devices_unique_token
  on public.devices (user_id, platform, coalesce(fcm_token, ''));

-- ------------------------------------------------------------
-- 12. packages (paquetes de creditos; modificable desde backend/admin)
-- ------------------------------------------------------------
create table if not exists public.packages (
  id uuid primary key default gen_random_uuid(),
  code text not null unique,
  name text not null,
  price_mxn numeric(10, 2) not null check (price_mxn > 0),
  credits integer not null check (credits > 0),
  active boolean not null default true,
  created_at timestamptz not null default now()
);

-- Paquetes del prompt (seccion 4.3); precios en MXN
insert into public.packages (code, name, price_mxn, credits) values
  ('basic',    'Basico',      49.00,  10000),
  ('standard', 'Intermedio',  99.00,  50000),
  ('premium',  'Premium',    199.00, 100000)
on conflict (code) do nothing;

-- ------------------------------------------------------------
-- 13. payments (pagos Stripe; los creditos SOLO se acreditan en 'paid')
-- ------------------------------------------------------------
create table if not exists public.payments (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references public.users (id) on delete cascade,
  package_id uuid not null references public.packages (id),
  stripe_session_id text unique,                 -- checkout.session.id
  stripe_payment_intent text,
  amount numeric(10, 2) not null,
  currency text not null default 'mxn',
  status text not null default 'pending'
    check (status in ('pending', 'paid', 'failed', 'canceled')),
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

drop trigger if exists payments_updated_at on public.payments;
create trigger payments_updated_at
  before update on public.payments
  for each row execute function public.touch_updated_at();

-- ------------------------------------------------------------
-- 14. credit_movements (trazabilidad de todo cambio de saldo)
-- ------------------------------------------------------------
create table if not exists public.credit_movements (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references public.users (id) on delete cascade,
  -- initial_grant | package_purchase | buy_stocks | sell_stocks
  type text not null check (type in
    ('initial_grant', 'package_purchase', 'buy_stocks', 'sell_stocks')),
  amount numeric(14, 2) not null,   -- positivo = entrada, negativo = salida
  balance_after numeric(14, 2) not null,
  reference text,                   -- p.ej. id de transaccion o payment
  created_at timestamptz not null default now()
);
create index if not exists idx_credit_movements_user_time
  on public.credit_movements (user_id, created_at desc);

-- ============================================================
-- RLS — Un usuario SOLO puede ver/modificar sus propias filas
-- ============================================================
alter table public.users            enable row level security;
alter table public.watchlists       enable row level security;
alter table public.stock_prices     enable row level security;
alter table public.portfolios       enable row level security;
alter table public.virtual_wallets  enable row level security;
alter table public.holdings         enable row level security;
alter table public.transactions     enable row level security;
alter table public.alerts           enable row level security;
alter table public.alert_history    enable row level security;
alter table public.devices          enable row level security;
alter table public.payments         enable row level security;
alter table public.credit_movements enable row level security;

-- users: proprio
create policy "users_select_own" on public.users
  for select using (auth.uid() = id);
create policy "users_update_own" on public.users
  for update using (auth.uid() = id) with check (auth.uid() = id);

-- Catalogo publico: lectura para cualquiera autenticado
create policy "stocks_select_all" on public.stocks
  for select using (true);
create policy "stock_prices_select_all" on public.stock_prices
  for select using (true);
create policy "packages_select_active" on public.packages
  for select using (true);

-- Datos privados por usuario
create policy "watchlists_all_own" on public.watchlists
  for all using (auth.uid() = user_id) with check (auth.uid() = user_id);
create policy "portfolios_select_own" on public.portfolios
  for select using (auth.uid() = user_id);
create policy "wallets_select_own" on public.virtual_wallets
  for select using (auth.uid() = user_id);   -- escritura SOLO backend (service)
create policy "holdings_all_own" on public.holdings
  for all using (auth.uid() = user_id) with check (auth.uid() = user_id);
create policy "transactions_all_own" on public.transactions
  for all using (auth.uid() = user_id) with check (auth.uid() = user_id);
create policy "alerts_all_own" on public.alerts
  for all using (auth.uid() = user_id) with check (auth.uid() = user_id);
create policy "alert_history_select_own" on public.alert_history
  for select using (auth.uid() = user_id);   -- escritura SOLO backend
create policy "devices_all_own" on public.devices
  for all using (auth.uid() = user_id) with check (auth.uid() = user_id);
create policy "payments_select_own" on public.payments
  for select using (auth.uid() = user_id);   -- escritura SOLO backend
create policy "credit_movements_select_own" on public.credit_movements
  for select using (auth.uid() = user_id);   -- escritura SOLO backend

-- ============================================================
-- Provision automatica al registrarse:
-- wallet 10,000 creditos + portafolio + movimiento inicial
-- ============================================================
create or replace function public.handle_new_user()
returns trigger
language plpgsql
security definer
set search_path = public
as $$
begin
  insert into public.users (id, email, full_name)
  values (new.id, new.email, new.raw_user_meta_data ->> 'full_name')
  on conflict (id) do nothing;

  insert into public.virtual_wallets (user_id, balance)
  values (new.id, 10000)
  on conflict (user_id) do nothing;

  insert into public.portfolios (user_id)
  values (new.id)
  on conflict (user_id) do nothing;

  insert into public.credit_movements (user_id, type, amount, balance_after, reference)
  values (new.id, 'initial_grant', 10000, 10000, 'registro inicial');

  return new;
end;
$$;

drop trigger if exists on_auth_user_created on auth.users;
create trigger on_auth_user_created
  after insert on auth.users
  for each row execute function public.handle_new_user();

-- ============================================================
-- FIN — 14 tablas del prompt + RLS + provision de creditos
-- ============================================================
