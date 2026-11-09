import { Request, Response, NextFunction } from "express";
import { paymentsService, PaymentsError } from "../services/paymentsService";
import { checkoutSchema } from "../validators/paymentValidators";
import { AuthedRequest } from "../middlewares/authMiddleware";

/**
 * Controlador de pagos (FASE 8).
 * GET  /api/payments/packages             -> paquetes activos
 * POST /api/payments/checkout             -> crea checkout Stripe
 * GET  /api/payments/status?session_id=   -> verifica y acredita si falta
 * GET  /api/payments/success?session_id=  -> retorno de Stripe (fulfillment + HTML)
 * GET  /api/payments/cancel               -> cancelacion
 */
export const paymentsController = {
  async packages(_req: Request, res: Response, next: NextFunction) {
    try {
      const pkgs = await paymentsService.listPackages();
      res.json({ ok: true, packages: pkgs });
    } catch (err) {
      handle(err, res, next);
    }
  },

  async checkout(req: Request, res: Response, next: NextFunction) {
    try {
      const { userId } = req as AuthedRequest;
      if (!userId) {
        res.status(401).json({ ok: false, error: "Sesion no valida" });
        return;
      }
      const parsed = checkoutSchema.safeParse(req.body);
      if (!parsed.success) {
        res.status(400).json({ ok: false, error: parsed.error.issues[0]?.message ?? "Paquete invalido" });
        return;
      }
      const checkout = await paymentsService.createCheckout(userId, parsed.data.packageCode);
      res.json({ ok: true, ...checkout });
    } catch (err) {
      handle(err, res, next);
    }
  },

  /** La app consulta (con su sesion) en cuanto vuelve del WebView. */
  async status(req: Request, res: Response, next: NextFunction) {
    try {
      const { userId } = req as AuthedRequest;
      if (!userId) {
        res.status(401).json({ ok: false, error: "Sesion no valida" });
        return;
      }
      const sessionId = String(req.query.session_id ?? "").trim();
      if (!/^cs_[A-Za-z0-9_]{10,}$/.test(sessionId)) {
        res.status(400).json({ ok: false, error: "Sesion de pago invalida" });
        return;
      }
      const result = await paymentsService.statusForUser(userId, sessionId);
      res.json({ ok: true, ...result });
    } catch (err) {
      handle(err, res, next);
    }
  },

  /** Pagina de retorno de Stripe (sin auth: valida contra el session_id). */
  async success(req: Request, res: Response) {
    const sessionId = String(req.query.session_id ?? "").trim();
    if (!sessionId) {
      res.status(400).send(page("Falta el identificador de pago", false));
      return;
    }
    try {
      const result = await paymentsService.fulfillCheckout(sessionId);
      const paid = result.status === "paid";
      res.status(paid ? 200 : 202).send(page(result.message, paid));
    } catch (err) {
      const msg = err instanceof Error ? err.message : "Error al verificar el pago";
      res.status(500).send(page(msg, false));
    }
  },

  async cancel(_req: Request, res: Response) {
    res.send(page("Pago cancelado. No se cobro nada y no se acreditaron creditos.", false, true));
  },
};

function page(message: string, success: boolean, neutral = false): string {
  const color = neutral ? "#555555" : success ? "#0a7d38" : "#c62828";
  return `<!doctype html><html lang="es"><head><meta charset="utf-8"/>
<meta name="viewport" content="width=device-width, initial-scale=1"/>
<title>StockPulse · Pago</title></head>
<body style="margin:0;font-family:system-ui,-apple-system,sans-serif;background:#0b1729;color:#eaf1f9;display:flex;min-height:100vh;align-items:center;justify-content:center;padding:24px;box-sizing:border-box">
<div style="max-width:420px;text-align:center">
<div style="font-size:28px;font-weight:700;color:#5cc98c;margin-bottom:16px">STOCKPULSE</div>
<div style="font-size:18px;font-weight:600;color:${color};line-height:1.5">${escapeHtml(message)}</div>
<p style="color:#8aa0b8;font-size:14px;margin-top:20px">Vuelve a la app para ver tu saldo actualizado.</p>
</div></body></html>`;
}

function escapeHtml(s: string): string {
  return s.replace(/[&<>"']/g, (c) =>
    ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" }[c] as string)
  );
}

function handle(err: unknown, res: Response, next: NextFunction) {
  if (err instanceof PaymentsError) {
    res.status(err.status).json({ ok: false, error: err.message });
    return;
  }
  next(err);
}
