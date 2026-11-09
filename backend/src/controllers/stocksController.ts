import { Request, Response, NextFunction } from "express";
import { stocksService } from "../services/stocksService";

/**
 * Controlador del mercado (FASE 5).
 * GET /api/stocks/popular        -> empresas populares + cotizacion actual
 * GET /api/stocks/quote?symbol=  -> cotizacion de un simbolo
 * GET /api/stocks/search?q=      -> busqueda de empresas (Finnhub)
 */
export const stocksController = {
  async popular(_req: Request, res: Response, next: NextFunction) {
    try {
      const stocks = await stocksService.getPopularWithQuotes();
      res.json({ ok: true, stocks, recordedAt: new Date().toISOString() });
    } catch (err) {
      next(err);
    }
  },

  async quote(req: Request, res: Response, next: NextFunction) {
    try {
      const symbol = String(req.query.symbol ?? "").trim();
      if (!symbol) {
        res.status(400).json({ ok: false, error: "Falta el parametro symbol" });
        return;
      }
      if (!/^[A-Za-z0-9.\-]{1,12}$/.test(symbol)) {
        res.status(400).json({ ok: false, error: "Simbolo invalido" });
        return;
      }
      const quote = await stocksService.getQuote(symbol);
      if (!quote.available) {
        res.status(404).json({
          ok: false,
          error: `Sin cotizacion disponible para ${symbol} en este momento`,
        });
        return;
      }
      res.json({ ok: true, quote });
    } catch (err) {
      next(err);
    }
  },

  async search(req: Request, res: Response, next: NextFunction) {
    try {
      const query = String(req.query.q ?? "").trim();
      if (query.length < 1) {
        res.status(400).json({ ok: false, error: "Falta el parametro q" });
        return;
      }
      const result = await stocksService.search(query);
      res.json({ ok: true, count: result.count, results: result.result.slice(0, 15) });
    } catch (err) {
      next(err);
    }
  },
};
