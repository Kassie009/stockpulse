import { z } from "zod";

/**
 * Validadores de alertas (FASE 7).
 * Tipos soportados por la tabla alerts del schema.
 */
export const ALERT_TYPES = [
  "price_above",
  "price_below",
  "change_pos",
  "change_neg",
  "volatility_high",
] as const;

export const createAlertSchema = z.object({
  symbol: z
    .string()
    .trim()
    .regex(/^[A-Za-z0-9.\-]{1,12}$/, "Simbolo invalido"),
  type: z.enum(ALERT_TYPES, { message: "Tipo de alerta no soportado" }),
  threshold: z
    .number({ message: "El umbral debe ser un numero" })
    .min(0.01, "El umbral debe ser mayor que 0")
    .max(1_000_000, "El umbral es demasiado grande"),
});

export type CreateAlertInput = z.infer<typeof createAlertSchema>;

export const registerDeviceSchema = z.object({
  token: z.string().trim().min(10).max(4096),
  platform: z.enum(["android", "wearos"]).default("android"),
  deviceName: z.string().trim().max(80).optional(),
});

export type RegisterDeviceInput = z.infer<typeof registerDeviceSchema>;
