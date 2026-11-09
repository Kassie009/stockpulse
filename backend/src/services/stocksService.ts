import { getSupabaseService } from "../integrations/supabase";
import { finnhubClient, FinnhubQuote, FinnhubSearchResponse } from "../integrations/finnhub";

/**
 * Servicio del mercado bursatil (FASE 5).
 * - Catalogo de empresas: Supabase (RLS-safe, service client).
 * - Precios: Finnhub via Cliente unico con caché (regla 4.1).
 * - En cada respuesta se incluye la fecha/hora del dato (regla 12).
 */

export interface StockQuoteView {
  symbol: string;
  name: string | null;
  price: number | null;
  changePercent: number | null;
  dayHigh: number | null;
  dayLow: number | null;
  recordedAt: string;        // ISO UTC del momento de la consulta
  provider: "finnhub";
  available: boolean;        // false si Finnhub no respondio (fallback demo, regla 13)
}

export const stocksService = {
  /** Empresas populares (is_popular=true) con cotizacion del momento. */
  async getPopularWithQuotes(): Promise<StockQuoteView[]> {
    const supabase = getSupabaseService();
    const { data, error } = await supabase
      .from("stocks")
      .select("symbol, name, is_popular")
      .eq("is_popular", true)
      .order("symbol");

    if (error) {
      throw new Error(`No se pudo leer el catalogo de empresas: ${error.message}`);
    }

    const rows = (data ?? []) as Array<{ symbol: string; name: string }>;
    // Cotizaciones en serie (no paralelo) para respetar el throttle de Finnhub
    const quotes: StockQuoteView[] = [];
    for (const row of rows) {
      quotes.push(await this.toQuoteView(row.symbol, row.name));
    }
    return quotes;
  },

  /** Cotizacion de un simbolo concreto. */
  async getQuote(symbol: string): Promise<StockQuoteView> {
    return this.toQuoteView(symbol.toUpperCase(), null);
  },

  /** Busqueda de empresas por nombre o simbolo (Finnhub). */
  search(query: string): Promise<FinnhubSearchResponse> {
    return finnhubClient.search(query);
  },

  async toQuoteView(symbol: string, name: string | null): Promise<StockQuoteView> {
    let quote: FinnhubQuote | null = null;
    try {
      quote = await finnhubClient.quote(symbol);
    } catch {
      quote = null; // regla 13: proveedor caido -> respuesta marcada no disponible
    }

    const valid = Boolean(quote && quote.c > 0); // c=0 significa "sin dato"
    return {
      symbol,
      name,
      price: valid ? quote!.c : null,
      changePercent: valid ? quote!.dp : null,
      dayHigh: valid ? quote!.h : null,
      dayLow: valid ? quote!.l : null,
      recordedAt: new Date().toISOString(),
      provider: "finnhub",
      available: valid,
    };
  },
};
