import Stripe from "stripe";
import { env } from "../config/env";

/**
 * Cliente Stripe (modo test).
 * FASE 2: solo construccion y validacion de conectividad basica.
 * FASE 8: sesiones de pago + webhooks (verificacion de firma + idempotencia).
 */
export const stripe = new Stripe(env.STRIPE_SECRET_KEY, {
  typescript: true,
});

export const stripePublishableKey = env.STRIPE_PUBLISHABLE_KEY;
