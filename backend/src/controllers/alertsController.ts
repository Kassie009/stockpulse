import { Request, Response, NextFunction } from "express";
import { alertsService, AlertsError } from "../services/alertsService";
import { createAlertSchema, registerDeviceSchema } from "../validators/alertValidators";
import { AuthedRequest } from "../middlewares/authMiddleware";

/**
 * Controlador de alertas (FASE 7).
 * POST   /api/alerts         -> crear alerta
 * GET    /api/alerts         -> listar mis alertas
 * DELETE /api/alerts?id=     -> eliminar
 * PATCH  /api/alerts?id=&active= -> pausar/reanudar
 * POST   /api/devices        -> registrar token FCM del dispositivo
 */
export const alertsController = {
  async create(req: Request, res: Response, next: NextFunction) {
    try {
      const { userId } = req as AuthedRequest;
      if (!userId) {
        res.status(401).json({ ok: false, error: "Sesion no valida" });
        return;
      }
      const parsed = createAlertSchema.safeParse(req.body);
      if (!parsed.success) {
        res.status(400).json({
          ok: false,
          error: parsed.error.issues.map((i) => i.message).join("; ") || "Datos de alerta invalidos",
        });
        return;
      }
      const alert = await alertsService.create(userId, parsed.data);
      res.json({ ok: true, alert });
    } catch (err) {
      handle(err, res, next);
    }
  },

  async list(req: Request, res: Response, next: NextFunction) {
    try {
      const { userId } = req as AuthedRequest;
      if (!userId) {
        res.status(401).json({ ok: false, error: "Sesion no valida" });
        return;
      }
      const alerts = await alertsService.list(userId);
      res.json({ ok: true, alerts });
    } catch (err) {
      handle(err, res, next);
    }
  },

  async remove(req: Request, res: Response, next: NextFunction) {
    try {
      const { userId } = req as AuthedRequest;
      if (!userId) {
        res.status(401).json({ ok: false, error: "Sesion no valida" });
        return;
      }
      const id = String(req.query.id ?? "").trim();
      if (!/^[0-9a-f\-]{36}$/i.test(id)) {
        res.status(400).json({ ok: false, error: "Identificador de alerta invalido" });
        return;
      }
      await alertsService.remove(userId, id);
      res.json({ ok: true, message: "Alerta eliminada" });
    } catch (err) {
      handle(err, res, next);
    }
  },

  async setActive(req: Request, res: Response, next: NextFunction) {
    try {
      const { userId } = req as AuthedRequest;
      if (!userId) {
        res.status(401).json({ ok: false, error: "Sesion no valida" });
        return;
      }
      const id = String(req.query.id ?? "").trim();
      if (!/^[0-9a-f\-]{36}$/i.test(id)) {
        res.status(400).json({ ok: false, error: "Identificador de alerta invalido" });
        return;
      }
      const active = req.query.active === "true";
      await alertsService.setActive(userId, id, active);
      res.json({ ok: true, message: active ? "Alerta reactivada" : "Alerta pausada" });
    } catch (err) {
      handle(err, res, next);
    }
  },

  /** Lista los dispositivos vinculados de la sesion (FASE 9.5). */
  async listDevices(req: Request, res: Response, next: NextFunction) {
    try {
      const { userId } = req as AuthedRequest;
      if (!userId) {
        res.status(401).json({ ok: false, error: "Sesion no valida" });
        return;
      }
      const { getSupabaseService } = await import("../integrations/supabase");
      const service = getSupabaseService();
      const { data, error } = await service
        .from("devices")
        .select("id, platform, device_name, last_seen_at")
        .eq("user_id", userId)
        .order("last_seen_at", { ascending: false });
      if (error) {
        res.status(400).json({ ok: false, error: `No se pudo leer tus dispositivos: ${error.message}` });
        return;
      }
      res.json({ ok: true, devices: data ?? [] });
    } catch (err) {
      next(err);
    }
  },

  /** Registro del token FCM (la app Android lo manda al encender). */  async registerDevice(req: Request, res: Response, next: NextFunction) {
    try {
      const { userId, userJwt } = req as AuthedRequest;
      if (!userId || !userJwt) {
        res.status(401).json({ ok: false, error: "Sesion no valida" });
        return;
      }
      const parsed = registerDeviceSchema.safeParse(req.body);
      if (!parsed.success) {
        res.status(400).json({
          ok: false,
          error: parsed.error.issues.map((i) => i.message).join("; ") || "Datos de dispositivo invalidos",
        });
        return;
      }

      // El indice unique del schema es de expresion: upsert manual
      const { getSupabaseService } = await import("../integrations/supabase");
      const service = getSupabaseService();
      const { data: existing } = await service
        .from("devices")
        .select("id")
        .eq("user_id", userId)
        .eq("platform", parsed.data.platform)
        .eq("fcm_token", parsed.data.token)
        .maybeSingle();

      if (existing) {
        await service.from("devices").update({ last_seen_at: new Date().toISOString() }).eq("id", (existing as { id: string }).id);
        res.json({ ok: true, message: "Dispositivo actualizado" });
        return;
      }

      const { error } = await service.from("devices").insert({
        user_id: userId,
        fcm_token: parsed.data.token,
        platform: parsed.data.platform,
        device_name: parsed.data.deviceName ?? null,
      });
      if (error) {
        res.status(400).json({ ok: false, error: `No se pudo registrar el dispositivo: ${error.message}` });
        return;
      }
      res.json({ ok: true, message: "Dispositivo registrado" });
    } catch (err) {
      handle(err, res, next);
    }
  },
};

function handle(err: unknown, res: Response, next: NextFunction) {
  if (err instanceof AlertsError) {
    res.status(err.status).json({ ok: false, error: err.message });
    return;
  }
  next(err);
}
