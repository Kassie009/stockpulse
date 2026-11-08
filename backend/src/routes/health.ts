import { Router } from "express";
import { integrationsStatus, isDev } from "../config/env";
import { finnhubClient } from "../integrations/finnhub";
import { getSupabasePublishable } from "../integrations/supabase";
import { firebaseStatus } from "../integrations/firebase";

export const healthRouter = Router();

/** GET /api/health - estado del servidor y configuracion (sin exponer secretos) */
healthRouter.get("/", (_req, res) => {
  res.json({
    ok: true,
    service: "stockpulse-backend",
    environment: isDev ? "development" : "production",
    time: new Date().toISOString(),
    integrationsConfigured: integrationsStatus,
  });
});

/** GET /api/health/integrations - prueba de conectividad real (diagnostico) */
healthRouter.get("/integrations", async (_req, res, next) => {
  try {
    const results: Record<string, unknown> = {};

    // Finnhub: cotizacion de AAPL
    try {
      const q = await finnhubClient.quote("AAPL");
      results.finnhub = {
        ok: true,
        aaplPrice: q.c,
        changePercent: q.dp,
        timestamp: q.t,
      };
    } catch (err) {
      results.finnhub = {
        ok: false,
        error: err instanceof Error ? err.message : String(err),
      };
    }

    // Supabase: consultar la tabla de prueba de autenticacion (sin RLS)
    try {
      const supabase = getSupabasePublishable();
      const { error } = await supabase.auth.getSession();
      results.supabase = { ok: !error, error: error?.message ?? null };
    } catch (err) {
      results.supabase = {
        ok: false,
        error: err instanceof Error ? err.message : String(err),
      };
    }

    // Firebase: solo configuracion (inicializacion real en FASE 7)
    results.firebase = firebaseStatus();

    res.json({ ok: true, time: new Date().toISOString(), results });
  } catch (err) {
    next(err);
  }
});
