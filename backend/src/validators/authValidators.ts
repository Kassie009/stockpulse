import { z } from "zod";

/**
 * Validadores de autenticacion (seccion 4.4 y 12 del prompt).
 */
export const registerSchema = z.object({
  fullName: z.string().trim().min(2).max(80),
  email: z.string().trim().email().max(120),
  password: z.string().min(8, "La contrasena debe tener al menos 8 caracteres").max(72),
});

export const loginSchema = z.object({
  email: z.string().trim().email().max(120),
  password: z.string().min(1).max(72),
});

export type RegisterInput = z.infer<typeof registerSchema>;
export type LoginInput = z.infer<typeof loginSchema>;
