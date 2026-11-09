import { Request, Response, NextFunction } from "express";
import { getUserClientFromJwt } from "../integrations/supabase";

/**
 * Middleware de autenticacion (FASE 6 en adelante).
 * Verifica el JWT de Supabase que la app envia en "Authorization: Bearer ...".
 * Adjunta userId y jwt en la peticion o responde 401 con mensaje real.
 */
export const requireAuth = async (req: Request, res: Response, next: NextFunction) => {
  const header = req.headers.authorization;
  if (!header || !header.startsWith("Bearer ")) {
    res.status(401).json({ ok: false, error: "No enviaste tu sesion. Inicia sesion de nuevo." });
    return;
  }

  const jwt = header.slice("Bearer ".length).trim();
  if (jwt.length < 20) {
    res.status(401).json({ ok: false, error: "Sesion invalida. Inicia sesion de nuevo." });
    return;
  }

  try {
    const client = getUserClientFromJwt(jwt);
    const { data, error } = await client.auth.getUser();
    if (error || !data?.user) {
      res.status(401).json({ ok: false, error: "Tu sesion expiro. Inicia sesion de nuevo." });
      return;
    }
    (req as Request & { userId?: string; userJwt?: string }).userId = data.user.id;
    (req as Request & { userJwt?: string }).userJwt = jwt;
    next();
  } catch (err) {
    res.status(401).json({
      ok: false,
      error: `No se pudo validar tu sesion: ${err instanceof Error ? err.message : "error desconocido"}`,
    });
  }
};

export interface AuthedRequest extends Request {
  userId?: string;
  userJwt?: string;
}
