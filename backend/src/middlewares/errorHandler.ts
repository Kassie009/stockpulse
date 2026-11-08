import { Request, Response, NextFunction } from "express";

/** Middleware de manejo de errores centralizado (seccion 12 del prompt). */
export function errorHandler(
  err: unknown,
  _req: Request,
  res: Response,
  _next: NextFunction
): void {
  // eslint-disable-next-line no-console
  console.error("[error]", err instanceof Error ? err.stack : err);
  const message = err instanceof Error ? err.message : "Error interno";
  res.status(500).json({ ok: false, error: message });
}

export function notFoundHandler(
  _req: Request,
  res: Response
): void {
  res.status(404).json({ ok: false, error: "Ruta no encontrada" });
}
