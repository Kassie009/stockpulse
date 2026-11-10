import { Request, Response, NextFunction } from "express";
import { tradingService, TradingError } from "../services/tradingService";
import { notifyDataRefresh } from "../integrations/firebase";
import { TradeInput, tradeSchema } from "../validators/tradeValidators";
import { AuthedRequest } from "../middlewares/authMiddleware";

/**
 * Controlador de trading (FASE 6).
 * GET  /api/trading/summary       -> wallet + holdings en vivo
 * POST /api/trading/buy           -> comprar acciones
 * POST /api/trading/sell          -> vender acciones
 * GET  /api/trading/transactions  -> historial
 */
export const tradingController = {
  async summary(req: Request, res: Response, next: NextFunction) {
    try {
      const { userId } = req as AuthedRequest;
      if (!userId) {
        res.status(401).json({ ok: false, error: "Sesion no valida" });
        return;
      }
      const summary = await tradingService.summary(userId);
      res.json({ ok: true, summary, recordedAt: new Date().toISOString() });
    } catch (err) {
      handleError(err, res, next);
    }
  },

  async buy(req: Request, res: Response, next: NextFunction) {
    try {
      const { userJwt } = req as AuthedRequest;
      if (!userJwt) {
        res.status(401).json({ ok: false, error: "Sesion no valida" });
        return;
      }
      const input = parseTradeBody(req);
      if (typeof input === "string") {
        res.status(400).json({ ok: false, error: input });
        return;
      }
      const result = await tradingService.trade(userJwt, input, "buy");
      void notifyDataRefresh(userJwt, "portfolio"); // reloj/otras pantallas se sincronizan al instante
      res.json({ ok: true, trade: result, message: `Compra realizada: ${result.quantity} de ${result.symbol} a $${result.unitPrice}` });
    } catch (err) {
      handleError(err, res, next);
    }
  },

  async sell(req: Request, res: Response, next: NextFunction) {
    try {
      const { userJwt } = req as AuthedRequest;
      if (!userJwt) {
        res.status(401).json({ ok: false, error: "Sesion no valida" });
        return;
      }
      const input = parseTradeBody(req);
      if (typeof input === "string") {
        res.status(400).json({ ok: false, error: input });
        return;
      }
      const result = await tradingService.trade(userJwt, input, "sell");
      void notifyDataRefresh(userJwt, "portfolio");
      const pnl = result.profitLoss;
      res.json({
        ok: true,
        trade: result,
        message: `Venta realizada: ${result.quantity} de ${result.symbol}` +
          (pnl !== null ? ` (ganancia/perdida: ${pnl >= 0 ? "+" : ""}$${pnl})` : ""),
      });
    } catch (err) {
      handleError(err, res, next);
    }
  },

  async transactions(req: Request, res: Response, next: NextFunction) {
    try {
      const { userJwt } = req as AuthedRequest;
      if (!userJwt) {
        res.status(401).json({ ok: false, error: "Sesion no valida" });
        return;
      }
      const txs = await tradingService.transactions(userJwt);
      res.json({ ok: true, transactions: txs });
    } catch (err) {
      handleError(err, res, next);
    }
  },
};

function parseTradeBody(req: Request): TradeInput | string {
  const parsed = tradeSchema.safeParse(req.body);
  if (!parsed.success) {
    const issues = parsed.error.issues;
    if (!issues.length) {
      return "Datos de operacion invalidos";
    }
    return issues.map((i) => i.message).join("; ");
  }
  return parsed.data;
}

function handleError(err: unknown, res: Response, next: NextFunction) {
  if (err instanceof TradingError) {
    res.status(err.status).json({ ok: false, error: err.message });
    return;
  }
  next(err);
}
