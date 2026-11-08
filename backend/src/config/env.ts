import { z } from "zod";

/**
 * Configuracion central del backend.
 * - Carga .env
 * - Valida presencias minimas con zod
 * - Expone el estado de integraciones (sin exponer valores)
 */
import dotenv from "dotenv";
dotenv.config();

const schema = z.object({
  PORT: z.coerce.number().int().positive().default(3000),
  NODE_ENV: z
    .enum(["development", "test", "production"])
    .default("development"),
  FINNHUB_API_KEY: z.string().min(1),
  SUPABASE_URL: z.string().url(),
  SUPABASE_PUBLISHABLE_KEY: z.string().min(1),
  SUPABASE_SECRET_KEY: z.string().min(1),
  SUPABASE_DB_PASSWORD: z.string().min(1).optional(),
  STRIPE_SECRET_KEY: z.string().startsWith("sk_"),
  STRIPE_PUBLISHABLE_KEY: z.string().startsWith("pk_"),
  FIREBASE_SERVICE_ACCOUNT_PATH: z.string().optional(),
});

const parsed = schema.safeParse(process.env);

if (!parsed.success) {
  const missing = parsed.error.issues
    .map((i) => `${i.path.join(".")} (${i.message})`)
    .join("; ");
  // eslint-disable-next-line no-console
  console.error(
    `[config] Variables de entorno invalidas o faltantes: ${missing}`
  );
  process.exit(1);
}

export const env = parsed.data;
export const isDev = env.NODE_ENV !== "production";

export const integrationsStatus = {
  finnhub: Boolean(env.FINNHUB_API_KEY),
  supabase: Boolean(env.SUPABASE_URL && env.SUPABASE_PUBLISHABLE_KEY),
  supabaseAdmin: Boolean(env.SUPABASE_SECRET_KEY),
  stripe: Boolean(env.STRIPE_SECRET_KEY && env.STRIPE_PUBLISHABLE_KEY),
  firebase: Boolean(env.FIREBASE_SERVICE_ACCOUNT_PATH),
};
