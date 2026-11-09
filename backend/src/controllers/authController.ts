import { Request, Response, NextFunction } from "express";
import { authService, AuthError } from "../services/authService";
import { loginSchema, registerSchema } from "../validators/authValidators";

/**
 * Controlador de autenticacion.
 * POST /api/auth/register  -> crea usuario + 10,000 creditos automaticos
 * POST /api/auth/login     -> entrega tokens de sesion
 */
export const authController = {
  async register(req: Request, res: Response, next: NextFunction) {
    try {
      const parsed = registerSchema.safeParse(req.body);
      if (!parsed.success) {
        const msg = parsed.error.issues.map((i) => `${i.path.join(".")}: ${i.message}`).join("; ");
        res.status(400).json({ ok: false, error: `Datos invalidos: ${msg}` });
        return;
      }
      const result = await authService.register(parsed.data);
      if ("requiresEmailConfirmation" in result) {
        res.json({
          ok: true,
          requiresEmailConfirmation: true,
          message: "Cuenta creada. Revisa tu correo para confirmar antes de iniciar sesion.",
        });
        return;
      }
      res.json({
        ok: true,
        session: {
          accessToken: result.session.accessToken,
          refreshToken: result.session.refreshToken,
          user: result.session.user,
        },
      });
    } catch (err) {
      if (err instanceof AuthError) {
        res.status(err.status).json({ ok: false, error: err.message });
        return;
      }
      next(err);
    }
  },

  async login(req: Request, res: Response, next: NextFunction) {
    try {
      const parsed = loginSchema.safeParse(req.body);
      if (!parsed.success) {
        res.status(400).json({ ok: false, error: "Correo o contrasena invalidos" });
        return;
      }
      const session = await authService.login(parsed.data);
      res.json({
        ok: true,
        session: {
          accessToken: session.accessToken,
          refreshToken: session.refreshToken,
          user: session.user,
        },
      });
    } catch (err) {
      if (err instanceof AuthError) {
        res.status(err.status).json({ ok: false, error: err.message });
        return;
      }
      next(err);
    }
  },
};
