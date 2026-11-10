import { getSupabaseService } from "../integrations/supabase";
import { notifyDataRefresh } from "../integrations/firebase";
import { CreateAlertInput } from "../validators/alertValidators";

/**
 * Servicio de alertas (FASE 7).
 * Usa el cliente de servicio porque las escrituras SIEMPRE llevan el
 * user_id derivado del JWT ya verificado por el middleware (requireAuth).
 * El motor de disparo vive en alertEngine.ts.
 */

export class AlertsError extends Error {
  status: number;
  constructor(message: string, status = 400) {
    super(message);
    this.status = status;
  }
}

export interface AlertRow {
  id: string;
  symbol: string;
  type: string;
  threshold: number;
  active: boolean;
  last_triggered_at: string | null;
  created_at: string;
}

const MAX_ACTIVE_ALERTS = 20;

export const alertsService = {
  async create(userId: string, input: CreateAlertInput): Promise<AlertRow> {
    const symbol = input.symbol.toUpperCase();
    const service = getSupabaseService();

    // El simbolo debe existir en el catalogo real
    const { data: stock, error: stockErr } = await service
      .from("stocks")
      .select("symbol")
      .eq("symbol", symbol)
      .maybeSingle();
    if (stockErr) {
      throw new AlertsError(`No se pudo validar el simbolo: ${stockErr.message}`, 500);
    }
    if (!stock) {
      throw new AlertsError(`El simbolo ${symbol} no esta en el catalogo`);
    }

    // Regla del prompt: numero razonable de alertas activas por usuario
    const { count, error: countErr } = await service
      .from("alerts")
      .select("id", { count: "exact", head: true })
      .eq("user_id", userId)
      .eq("active", true);
    if (countErr) {
      throw new AlertsError(`No se pudo validar tus alertas: ${countErr.message}`, 500);
    }
    if ((count ?? 0) >= MAX_ACTIVE_ALERTS) {
      throw new AlertsError(`Tienes ${count} alertas activas. Elimina alguna antes de crear otra`, 422);
    }

    const { data, error } = await service
      .from("alerts")
      .insert({
        user_id: userId,
        symbol,
        type: input.type,
        threshold: input.threshold,
        active: true,
      })
      .select("id, symbol, type, threshold, active, last_triggered_at, created_at")
      .single();

    if (error) {
      throw new AlertsError(`No se pudo crear la alerta: ${error.message}`, 400);
    }
    void notifyDataRefresh(userId, "alerts"); // sincroniza reloj/otro telefono al instante
    return data as AlertRow;
  },

  async list(userId: string): Promise<AlertRow[]> {
    const service = getSupabaseService();
    const { data, error } = await service
      .from("alerts")
      .select("id, symbol, type, threshold, active, last_triggered_at, created_at")
      .eq("user_id", userId)
      .order("created_at", { ascending: false });
    if (error) {
      throw new AlertsError(`No se pudo leer tus alertas: ${error.message}`, 500);
    }
    return (data ?? []) as AlertRow[];
  },

  async remove(userId: string, id: string): Promise<void> {
    const service = getSupabaseService();
    const { error } = await service
      .from("alerts")
      .delete()
      .eq("id", id)
      .eq("user_id", userId); // solo se elimina SI es del usuario
    if (error) {
      throw new AlertsError(`No se pudo eliminar la alerta: ${error.message}`, 400);
    }
    void notifyDataRefresh(userId, "alerts");
  },

  async setActive(userId: string, id: string, active: boolean): Promise<void> {
    const service = getSupabaseService();
    const { error } = await service
      .from("alerts")
      .update({ active })
      .eq("id", id)
      .eq("user_id", userId);
    if (error) {
      throw new AlertsError(`No se pudo actualizar la alerta: ${error.message}`, 400);
    }
    void notifyDataRefresh(userId, "alerts");
  },
};
