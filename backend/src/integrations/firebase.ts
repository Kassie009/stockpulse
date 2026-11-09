import * as fs from "fs";
import * as path from "path";
import { cert, getApps, initializeApp } from "firebase-admin/app";
import { getMessaging } from "firebase-admin/messaging";
import { env } from "../config/env";

/**
 * Firebase Admin (FASE 7):
 * - Carga Perezosa del JSON de cuenta de servicio.
 * - Nunca se empaqueta en la app Android.
 * - API unica de envio: sendFcmNotifications (evita interop require/ESM).
 */

let initialized = false;
let initError: string | null = null;

export function initFirebaseAdmin(): void {
  if (initialized) return;
  try {
    const credPath = env.FIREBASE_SERVICE_ACCOUNT_PATH;
    if (!credPath) {
      initError = "FIREBASE_SERVICE_ACCOUNT_PATH no configurada";
      return;
    }
    const absolute = path.resolve(process.cwd(), credPath);
    if (!fs.existsSync(absolute)) {
      initError = `No se encontro el archivo de credenciales: ${absolute}`;
      return;
    }
    if (getApps().length === 0) {
      initializeApp({
        credential: cert(absolute),
      });
    }
    initialized = true;
  } catch (err) {
    initError = err instanceof Error ? err.message : String(err);
  }
}

export function firebaseStatus(): { ready: boolean; error: string | null } {
  return { ready: initialized, error: initError };
}

/** Envia una notificacion a N tokens. Devuelve resumen legible. */
export async function sendFcmNotifications(
  tokens: string[],
  title: string,
  body: string
): Promise<string> {
  try {
    initFirebaseAdmin();
    if (!initialized) {
      return `Firebase no disponible: ${initError ?? "sin configurar"}`;
    }
    const response = await getMessaging().sendEachForMulticast({
      tokens,
      notification: { title, body },
    });
    if (response.failureCount > 0) {
      const detalles = response.responses
        .filter((r) => !r.success)
        .map((r) => r.error?.code ?? "desconocido")
        .slice(0, 3)
        .join(", ");
      return `${response.successCount} enviados, ${response.failureCount} fallidos (${detalles})`;
    }
    return `${response.successCount} enviados, ${response.failureCount} fallidos`;
  } catch (err) {
    return `error de push: ${err instanceof Error ? err.message : String(err)}`;
  }
}
