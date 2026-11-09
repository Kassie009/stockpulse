import * as fs from "fs";
import * as path from "path";
import { env } from "../config/env";

/**
 * Motor de alertas (FASE 7).
 * - Corre en ciclo: lee alertas activas (service), evalua contra la
 *   cotizacion real (cliente Finnhub con su propia caché/throttle),
 *   dispara notificacion FCM y escribe historial.
 * - Cooldown de 10 min por alerta para no volver a disparar seguido.
 * - Si Firebase no esta listo, degrada con transparencia (historial lo
 *   anota), sin tumbar el ciclo.
 */

import { getSupabaseService } from "../integrations/supabase";
import { finnhubClient } from "../integrations/finnhub";

const CYCLE_MS = 20_000;          // cada 20 s (desarrollo)
const COOLDOWN_MS = 10 * 60_000;  // 10 min entre disparos de la misma alerta

interface ActiveAlert {
  id: string;
  user_id: string;
  symbol: string;
  type: string;
  threshold: number;
  last_triggered_at: string | null;
}

let running = false;

function describeAlert(alert: ActiveAlert): { title: string; body: string } {
  switch (alert.type) {
    case "price_above":
      return { title: `Alerta de precio: ${alert.symbol}`, body: `El precio subio a $${alert.threshold} o mas` };
    case "price_below":
      return { title: `Alerta de precio: ${alert.symbol}`, body: `El precio cayo a $${alert.threshold} o menos` };
    case "change_pos":
      return { title: `Alerta de variacion: ${alert.symbol}`, body: `El precio subio mas del ${alert.threshold}%` };
    case "change_neg":
      return { title: `Alerta de variacion: ${alert.symbol}`, body: `El precio cayo mas del ${alert.threshold}%` };
    case "volatility_high":
      return { title: `Alerta de volatilidad: ${alert.symbol}`, body: `Volatilidad alta en el dia (> ${alert.threshold}%)` };
    default:
      return { title: `Alerta de ${alert.symbol}`, body: "Condicion cumplida" };
  }
}

function isTriggered(alert: ActiveAlert, price: number, changePercent: number): boolean {
  switch (alert.type) {
    case "price_above": return price >= alert.threshold;
    case "price_below": return price <= alert.threshold;
    case "change_pos": return changePercent >= alert.threshold;
    case "change_neg": return changePercent <= -alert.threshold;
    case "volatility_high": return Math.abs(changePercent) >= alert.threshold;
    default: return false;
  }
}

async function cycle(): Promise<void> {
  const firebaseConfigured = Boolean(
    env.FIREBASE_SERVICE_ACCOUNT_PATH &&
    fs.existsSync(path.resolve(process.cwd(), env.FIREBASE_SERVICE_ACCOUNT_PATH))
  );

  const supabase = getSupabaseService();

  const { data: alerts, error } = await supabase
    .from("alerts")
    .select("id, user_id, symbol, type, threshold, last_triggered_at")
    .eq("active", true);

  if (error) {
    console.warn(`[alertEngine] no se pudieron leer alertas: ${error.message}`);
    return;
  }
  if (!alerts?.length) return;

  // Cotizaciones unicas por simbolo (el cliente ya throttling+caché)
  const quotes = new Map<string, { price: number; changePercent: number }>();
  for (const alert of alerts as ActiveAlert[]) {
    if (quotes.has(alert.symbol)) continue;
    try {
      const q = await finnhubClient.quote(alert.symbol);
      if (q && q.c > 0) {
        quotes.set(alert.symbol, { price: q.c, changePercent: q.dp });
      }
    } catch {
      // sin dato -> alerta no evaluada este ciclo (regla 13)
    }
  }

  const now = Date.now();
  for (const alert of alerts as ActiveAlert[]) {
    const quote = quotes.get(alert.symbol);
    if (!quote) continue;

    // cooldown: no disparar de nuevo pronto
    if (alert.last_triggered_at && now - Date.parse(alert.last_triggered_at) < COOLDOWN_MS) {
      continue;
    }

    if (!isTriggered(alert, quote.price, quote.changePercent)) continue;

    const { title, body } = describeAlert(alert);
    let pushResult = "sin envio (Firebase no esta configurado)";

    // Tokens FCM del usuario
    const { data: devices } = await supabase
      .from("devices")
      .select("fcm_token")
      .eq("user_id", alert.user_id)
      .not("fcm_token", "is", null);

    const tokens = (devices ?? []).map((d) => d.fcm_token).filter((t): t is string => Boolean(t));

    if (tokens.length > 0 && firebaseConfigured) {
      pushResult = await sendPush(tokens, title, quote.price);
    } else if (tokens.length === 0) {
      pushResult = "sin envio: el usuario no tiene dispositivos registrados";
    }

    const fullMessage = `${body} | precio $${quote.price.toFixed(2)} (${quote.changePercent >= 0 ? "+" : ""}${quote.changePercent.toFixed(2)}%)`;

    // Historial + marca de disparo (service: es del motor, no del usuario)
    await supabase.from("alert_history").insert({
      alert_id: alert.id,
      user_id: alert.user_id,
      message: fullMessage,
    });
    await supabase
      .from("alerts")
      .update({ last_triggered_at: new Date().toISOString() })
      .eq("id", alert.id);

    console.log(`[alertEngine] disparada alerta ${alert.symbol} para ${alert.user_id}: ${fullMessage} | push=${pushResult}`);
  }
}

async function sendPush(tokens: string[], title: string, price: number): Promise<string> {
  try {
    // Module require perezoso (mismo patron que integrations/firebase.ts)
    // eslint-disable-next-line @typescript-eslint/no-var-requires
    const { initFirebaseAdmin } = await import("../integrations/firebase");
    initFirebaseAdmin();
    // eslint-disable-next-line @typescript-eslint/no-var-requires
    const admin = require("firebase-admin");
    const res = await admin.messaging().sendEachForMulticast({
      tokens,
      notification: { title, body: `Precio actual: $${price.toFixed(2)}` },
    });
    return `${res.successCount} enviados, ${res.failureCount} fallidos`;
  } catch (err) {
    return `error de push: ${err instanceof Error ? err.message : String(err)}`;
  }
}

export function startAlertEngine(): void {
  if (running) return;
  running = true;
  console.log("[alertEngine] iniciado: revisando alertas cada 20 s");
  const tick = async () => {
    try {
      await cycle();
    } catch (err) {
      console.warn(`[alertEngine] ciclo con error: ${err instanceof Error ? err.message : String(err)}`);
    }
  };
  void tick();
  setInterval(tick, CYCLE_MS);
}
