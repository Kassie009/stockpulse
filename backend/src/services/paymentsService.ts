import Stripe from "stripe";
import { stripe } from "../integrations/stripe";
import { getSupabaseService } from "../integrations/supabase";
import { notifyDataRefresh } from "../integrations/firebase";

/**
 * Servicio de pagos (FASE 8).
 * - El precio NUNCA viene del cliente: se lee de la tabla packages.
 * - Los creditos se acreditan SOLO cuando Stripe confirma el pago
 *   (session.payment_status === 'paid'), verificado en el servidor.
 * - Fulfillment idempotente: si ya estaba 'paid', no duplica creditos.
 *
 * NOTA DEV: el "success_url" apunta al backend (10.0.2.2 desde el emulador).
 * En produccion se reemplaza por URL https y webhooks firmados (FASE 10).
 */

const DEV_BASE = "http://10.0.2.2:3000";

export class PaymentsError extends Error {
  status: number;
  constructor(message: string, status = 400) {
    super(message);
    this.status = status;
  }
}

export interface PackageView {
  code: string;
  name: string;
  priceMxn: number;
  credits: number;
}

export const paymentsService = {
  async listPackages(): Promise<PackageView[]> {
    const supabase = getSupabaseService();
    const { data, error } = await supabase
      .from("packages")
      .select("code, name, price_mxn, credits")
      .eq("active", true)
      .order("price_mxn");
    if (error) {
      throw new PaymentsError(`No se pudo leer los paquetes: ${error.message}`, 500);
    }
    return (data ?? []).map((p) => ({
      code: p.code,
      name: p.name,
      priceMxn: Number(p.price_mxn),
      credits: p.credits,
    }));
  },

  async createCheckout(
    userId: string,
    packageCode: string
  ): Promise<{ sessionId: string; checkoutUrl: string; amount: number; credits: number }> {
    const supabase = getSupabaseService();
    const { data: pkg, error } = await supabase
      .from("packages")
      .select("id, code, name, price_mxn, credits")
      .eq("code", packageCode)
      .eq("active", true)
      .maybeSingle();
    if (error) {
      throw new PaymentsError(`No se pudo validar el paquete: ${error.message}`, 500);
    }
    if (!pkg) {
      throw new PaymentsError("El paquete solicitado no existe");
    }

    const amount = Number(pkg.price_mxn);

    let session: Stripe.Checkout.Session;
    try {
      session = await stripe.checkout.sessions.create({
        mode: "payment",
        line_items: [
          {
            quantity: 1,
            price_data: {
              currency: "mxn",
              unit_amount: amount * 100, // Stripe usa centavos
              product_data: { name: `STOCKPULSE · ${pkg.name} (${pkg.credits} creditos)` },
            },
          },
        ],
        // Emulador: al terminar, Stripe redirige al WebView hacia ESTA URL,
        // que llega al propio backend y dispara el fulfillment verificado.
        success_url: `${DEV_BASE}/api/payments/success?session_id={CHECKOUT_SESSION_ID}`,
        cancel_url: `${DEV_BASE}/api/payments/cancel`,
        client_reference_id: userId,
        metadata: { userId, packageCode: pkg.code, credits: String(pkg.credits) },
      });
    } catch (err) {
      throw new PaymentsError(
        `No se pudo iniciar el pago en Stripe: ${err instanceof Error ? err.message : String(err)}`,
        502
      );
    }

    if (!session.url) {
      throw new PaymentsError("Stripe no devolvio la pagina de pago", 502);
    }

    const { error: pErr } = await supabase.from("payments").insert({
      user_id: userId,
      package_id: pkg.id,
      stripe_session_id: session.id,
      amount,
      currency: "mxn",
      status: "pending",
    });
    if (pErr) {
      throw new PaymentsError(`No se pudo registrar el intento de pago: ${pErr.message}`, 500);
    }

    return {
      sessionId: session.id,
      checkoutUrl: session.url,
      amount,
      credits: pkg.credits,
    };
  },

  /**
   * Fulfillment: verifica el pago con Stripe y acredita creditos via RPC
   * `credit_wallet` (atomica e idempotente; database/payments.sql).
   */
  async fulfillCheckout(sessionId: string): Promise<{
    status: "paid" | "pending" | "not_found";
    message: string;
    creditsAdded?: number;
    newBalance?: number;
  }> {
    let session: Stripe.Checkout.Session;
    try {
      session = await stripe.checkout.sessions.retrieve(sessionId);
    } catch (err) {
      throw new PaymentsError(
        `La sesion de pago no es valida: ${err instanceof Error ? err.message : String(err)}`,
        400
      );
    }

    const userId = session.metadata?.userId;
    const credits = Number(session.metadata?.credits ?? 0);
    if (!userId || !credits) {
      throw new PaymentsError("La sesion de pago no corresponde a STOCKPULSE", 400);
    }

    const supabase = getSupabaseService();

    if (session.payment_status !== "paid") {
      return {
        status: "pending",
        message: "El pago aun no se confirma. Si ya pagaste, reintenta en un minuto.",
      };
    }

    const { data: payment } = await supabase
      .from("payments")
      .select("id, status, amount")
      .eq("stripe_session_id", sessionId)
      .maybeSingle();

    if (!payment) {
      return { status: "not_found", message: "No se encontro el registro del pago" };
    }

    // Idempotencia: ya procesado -> solo reportar saldo
    if (payment.status === "paid") {
      const { data: w } = await supabase
        .from("virtual_wallets")
        .select("balance")
        .eq("user_id", userId)
        .maybeSingle();
      return {
        status: "paid",
        message: `El pago ya estaba procesado. Tienes ${Math.round(Number(w?.balance ?? 0))} creditos.`,
        newBalance: Number(w?.balance ?? 0),
      };
    }

    const { data: credited, error: rpcErr } = await supabase.rpc("credit_wallet", {
      p_user_id: userId,
      p_credits: credits,
      p_reference: sessionId,
    });

    if (rpcErr) {
      throw new PaymentsError(
        `El pago esta confirmado en Stripe pero no se pudo acreditar: ${rpcErr.message}. ` +
          "Ejecuta database/payments.sql en Supabase y vuelve a verificar.",
        500
      );
    }

    await supabase
      .from("payments")
      .update({
        status: "paid",
        stripe_payment_intent: typeof session.payment_intent === "string" ? session.payment_intent : null,
      })
      .eq("id", payment.id);

    void notifyDataRefresh(userId, "portfolio"); // el balance nuevo, al instante en reloj y telefono

    const newBalance = Number(credited?.balance ?? 0);

    return {
      status: "paid",
      message: `Pago confirmado: +${credits} creditos. Saldo: ${Math.round(newBalance)}`,
      creditsAdded: credits,
      newBalance,
    };
  },

  async statusForUser(userId: string, sessionId: string) {
    const supabase = getSupabaseService();
    const { data: payment } = await supabase
      .from("payments")
      .select("id")
      .eq("user_id", userId)
      .eq("stripe_session_id", sessionId)
      .maybeSingle();
    if (!payment) {
      throw new PaymentsError("Ese pago no es tuyo");
    }
    return this.fulfillCheckout(sessionId); // re-verifica y acredita si falta
  },
};
