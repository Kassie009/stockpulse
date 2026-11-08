import * as fs from "fs";
import * as path from "path";
import { env } from "../config/env";

/**
 * Firebase Admin (FASE 7):
 * - Carga perezosa del JSON de cuenta de servicio.
 * - Nunca se empaqueta en la app Android.
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
    // Import dinamico para no cargar firebase-admin si aun no se usa (FASE 7).
    // eslint-disable-next-line @typescript-eslint/no-var-requires
    const admin = require("firebase-admin");
    if (admin.apps.length === 0) {
      admin.initializeApp({
        credential: admin.credential.cert(absolute),
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
