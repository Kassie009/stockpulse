import { z } from "zod";

/** Validadores de pagos (FASE 8). */
export const checkoutSchema = z.object({
  packageCode: z
    .string()
    .trim()
    .regex(/^[a-z_]{2,20}$/, "Paquete invalido"),
});

export type CheckoutInput = z.infer<typeof checkoutSchema>;
