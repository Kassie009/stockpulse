import { z } from "zod";

/**
 * Validadores de trading (FASE 6).
 */
export const tradeSchema = z.object({
  symbol: z
    .string()
    .trim()
    .regex(/^[A-Za-z0-9.\-]{1,12}$/, "Simbolo invalido"),
  quantity: z
    .number({ message: "La cantidad debe ser un numero" })
    .int("La cantidad debe ser un numero entero")
    .min(1, "La cantidad debe ser al menos 1 accion")
    .max(100000, "La cantidad maxima por operacion es 100,000"),
});

export type TradeInput = z.infer<typeof tradeSchema>;
