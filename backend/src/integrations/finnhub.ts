import { env } from "../config/env";

/**
 * Cliente Finnhub (FASE 2: construccion minima).
 * Reglas del prompt (seccion 4.1):
 * - Clave solo en backend (viene de env).
 * - Caché de cotizaciones (TTL corto) para respetar límites del plan.
 * - Respaldo de frecuencia: max ~1 req/seg hacia Finnhub (free tier: 60/min).
 */

const BASE_URL = "https://finnhub.io/api/v1";

interface CacheEntry {
  value: unknown;
  expiresAt: number;
}

const quoteCacheTtlMs = 15_000; // 15 s de caché por símbolo
const minIntervalMs = 1_100; // separación mínima entre llamadas

const cache = new Map<string, CacheEntry>();
let lastCallAt = 0;

async function throttle(): Promise<void> {
  const wait = lastCallAt + minIntervalMs - Date.now();
  if (wait > 0) await new Promise((r) => setTimeout(r, wait));
  lastCallAt = Date.now();
}

async function get<T>(path: string, params: URLSearchParams): Promise<T> {
  const url = `${BASE_URL}${path}?${params.toString()}`;
  const res = await fetch(url, {
    headers: { "X-Finnhub-Token": env.FINNHUB_API_KEY },
  });
  if (!res.ok) {
    throw new Error(`Finnhub HTTP ${res.status} en ${path}`);
  }
  return (await res.json()) as T;
}

export const finnhubClient = {
  /** Cotización simple (precio, variación, etc.) */
  quote(symbol: string): Promise<FinnhubQuote> {
    return this.cached<FinnhubQuote>(`quote:${symbol}`, () =>
      get<FinnhubQuote>("/quote", new URLSearchParams({ symbol }))
    );
  },

  /** Búsqueda de empresas por nombre o símbolo */
  search(query: string): Promise<FinnhubSearchResponse> {
    return this.cached<FinnhubSearchResponse>(`search:${query}`, () =>
      get<FinnhubSearchResponse>("/search", new URLSearchParams({ q: query }))
    );
  },

  async cached<T>(key: string, loader: () => Promise<T>): Promise<T> {
    const entry = cache.get(key);
    if (entry && entry.expiresAt > Date.now()) {
      return entry.value as T;
    }
    await throttle();
    const value = await loader();
    cache.set(key, { value, expiresAt: Date.now() + quoteCacheTtlMs });
    return value;
  },
};

export interface FinnhubQuote {
  c: number; // precio actual
  d: number; // variacion absoluta
  dp: number; // variacion porcentual
  h: number; // maximo del dia
  l: number; // minimo del dia
  o: number; // apertura
  pc: number; // cierre previo
  t: number; // timestamp
}

export interface FinnhubSearchResult {
  description: string;
  displaySymbol: string;
  symbol: string;
  type: string;
}

export interface FinnhubSearchResponse {
  count: number;
  result: FinnhubSearchResult[];
}
