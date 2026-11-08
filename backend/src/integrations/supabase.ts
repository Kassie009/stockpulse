import { createClient, SupabaseClient } from "@supabase/supabase-js";
import { env } from "../config/env";

/**
 * Clientes Supabase.
 * - publishable: operaciones en nombre de usuario (respeta RLS).
 * - secret: operaciones de servicio (backend: acreditar creditos, etc.)
 *   Regla del prompt: la clave privilegiada NUNCA sale del backend.
 */

let publishableClient: SupabaseClient | null = null;
let serviceClient: SupabaseClient | null = null;

export function getSupabasePublishable(): SupabaseClient {
  if (!publishableClient) {
    publishableClient = createClient(
      env.SUPABASE_URL,
      env.SUPABASE_PUBLISHABLE_KEY,
      { auth: { autoRefreshToken: false, persistSession: false } }
    );
  }
  return publishableClient;
}

/** Solo para operaciones del backend (escrituras criticas). */
export function getSupabaseService(): SupabaseClient {
  if (!serviceClient) {
    serviceClient = createClient(
      env.SUPABASE_URL,
      env.SUPABASE_SECRET_KEY,
      { auth: { autoRefreshToken: false, persistSession: false } }
    );
  }
  return serviceClient;
}

/** Valida un token de sesion filtrado por RLS (basico para proximas fases). */
export function getUserClientFromJwt(userJwt: string): SupabaseClient {
  return createClient(env.SUPABASE_URL, env.SUPABASE_PUBLISHABLE_KEY, {
    auth: { autoRefreshToken: false, persistSession: false },
    global: { headers: { Authorization: `Bearer ${userJwt}` } },
  });
}
