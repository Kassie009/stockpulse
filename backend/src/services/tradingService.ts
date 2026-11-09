import { getUserClientFromJwt, getSupabaseService } from "../integrations/supabase";
import { finnhubClient } from "../integrations/finnhub";
import { TradeInput } from "../validators/tradeValidators";

/**
 * Servicio de trading (FASE 6).
 * - El PRECIO siempre lo define el backend con Finnhub (fuente unica):
 *   la app nunca manda el precio, evitando manipulacion.
 * - La operacion atomica (fondos, holdings, P&L) ocurre en el RPC
 *   execute_trade (database/trading.sql) validando auth.uid() dentro.
 */

export interface TradeResult {
  transactionId: string;
  symbol: string;
  action: string;
  quantity: number;
  unitPrice: number;
  total: number;
  profitLoss: number | null;
  walletBalance: number;
}

export class TradingError extends Error {
  status: number;
  constructor(message: string, status = 400) {
    super(message);
    this.status = status;
  }
}

export const tradingService = {
  async trade(userJwt: string, input: TradeInput, action: "buy" | "sell"): Promise<TradeResult> {
    const symbol = input.symbol.toUpperCase();

    // 1. Precio de mercado REAL desde Finnhub (con caché del cliente)
    let quote;
    try {
      quote = await finnhubClient.quote(symbol);
    } catch (err) {
      throw new TradingError(
        `No hay cotizacion disponible de ${symbol} en este momento. Reintenta en unos segundos.`,
        503
      );
    }
    if (!quote || quote.c <= 0) {
      throw new TradingError(`Sin precio de mercado para ${symbol}. Verifica el simbolo.`, 404);
    }

    // 2. Ejecutar la operacion atomica en la base de datos (con el JWT del usuario)
    const client = getUserClientFromJwt(userJwt);
    const { data, error } = await client.rpc("execute_trade", {
      p_symbol: symbol,
      p_action: action,
      p_quantity: input.quantity,
      p_unit_price: quote.c,
    });

    if (error) {
      throw new TradingError(mapPostgrestError(error.message), error.message.includes("Fondos") ? 422 : 400);
    }

    const result = (data ?? {}) as {
      transactionId?: string;
      unitPrice?: number;
      total?: number;
      profitLoss?: number | null;
      walletBalance?: number;
    };

    return {
      transactionId: result.transactionId ?? "",
      symbol,
      action,
      quantity: input.quantity,
      unitPrice: result.unitPrice ?? quote.c,
      total: result.total ?? 0,
      profitLoss: result.profitLoss ?? null,
      walletBalance: result.walletBalance ?? 0,
    };
  },

  /** Resumen del portafolio: wallet + holdings con precio en vivo. */
  async summary(userId: string): Promise<{
    balance: number;
    holdingsEnVivo: Array<{
      symbol: string;
      quantity: number;
      avgPrice: number;
      price: number | null;
      changePercent: number | null;
      marketValue: number | null;
      unrealizedPnl: number | null;
      available: boolean;
    }>;
    portfolioValue: number;
    investedCost: number;
  }> {
    const supabase = getSupabaseService();

    const { data: wallet, error: walletErr } = await supabase
      .from("virtual_wallets")
      .select("balance")
      .eq("user_id", userId)
      .maybeSingle();
    if (walletErr) {
      throw new TradingError(`No se pudo leer tu billetera: ${walletErr.message}`, 500);
    }

    const { data: holdings, error: hErr } = await supabase
      .from("holdings")
      .select("symbol, quantity, avg_price")
      .eq("user_id", userId)
      .order("symbol");
    if (hErr) {
      throw new TradingError(`No se pudo leer tu portafolio: ${hErr.message}`, 500);
    }

    let portfolioValue = 0;
    let investedCost = 0;
    const holdingsEnVivo = [] as Awaited<ReturnType<typeof tradingService.summary>>["holdingsEnVivo"];

    for (const h of holdings ?? []) {
      let price: number | null = null;
      let changePercent: number | null = null;
      try {
        const q = await finnhubClient.quote(h.symbol);
        if (q && q.c > 0) {
          price = q.c;
          changePercent = q.dp;
        }
      } catch {
        // regla 13: proveedor caido -> marcado no disponible
      }

      const marketValue = price !== null ? price * h.quantity : null;
      const unrealizedPnl = price !== null ? (price - Number(h.avg_price)) * h.quantity : null;
      if (marketValue !== null) portfolioValue += marketValue;
      investedCost += Number(h.avg_price) * h.quantity;

      holdingsEnVivo.push({
        symbol: h.symbol,
        quantity: h.quantity,
        avgPrice: Number(h.avg_price),
        price,
        changePercent,
        marketValue,
        unrealizedPnl,
        available: price !== null,
      });
    }

    return {
      balance: Number(wallet?.balance ?? 0),
      holdingsEnVivo,
      portfolioValue: Math.round(portfolioValue * 100) / 100,
      investedCost: Math.round(investedCost * 100) / 100,
    };
  },

  /** Historial de transacciones (RLS del usuario). */
  async transactions(userJwt: string, limit = 50) {
    const client = getUserClientFromJwt(userJwt);
    const { data, error } = await client
      .from("transactions")
      .select("id, symbol, type, quantity, unit_price, total, profit_loss, created_at")
      .order("created_at", { ascending: false })
      .limit(limit);
    if (error) {
      throw new TradingError(`No se pudo leer el historial: ${error.message}`, 500);
    }
    return data ?? [];
  },
};

function mapPostgrestError(message: string): string {
  // Los mensajes de execute_trade ya son claros para el usuario;
  // esta capa cuida el ruido de infraestructura.
  if (message.includes("Fondos insuficientes") || message.includes("acciones")) {
    return message;
  }
  if (message.includes("permission denied") || message.includes("42501")) {
    return "Sesion no valida: inicia sesion de nuevo";
  }
  return `La operacion no se completo: ${message}`;
}
