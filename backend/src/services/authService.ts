import { getSupabasePublishable } from "../integrations/supabase";
import { LoginInput, RegisterInput } from "../validators/authValidators";

/**
 * Servicio de autenticacion con Supabase Auth.
 * Al registrarse, el trigger de base de datos (handle_new_user)
 * crea automaticamente: users + virtual_wallets (10,000 creditos)
 * + portfolios + credit_movements inicial.
 */

export interface AuthSession {
  accessToken: string;
  refreshToken: string;
  user: {
    id: string;
    email: string;
  };
}

export class AuthError extends Error {
  status: number;
  constructor(message: string, status = 400) {
    super(message);
    this.status = status;
  }
}

export const authService = {
  async register(input: RegisterInput): Promise<
    { session: AuthSession } | { requiresEmailConfirmation: true }
  > {
    const supabase = getSupabasePublishable();
    const { data, error } = await supabase.auth.signUp({
      email: input.email,
      password: input.password,
      options: { data: { full_name: input.fullName } },
    });

    if (error) {
      throw mapSupabaseError(error.message);
    }

    if (!data.session) {
      // Supabase pide confirmar el correo antes de entregar sesion
      return { requiresEmailConfirmation: true };
    }

    return { session: toSession(data.session) };
  },

  async login(input: LoginInput): Promise<AuthSession> {
    const supabase = getSupabasePublishable();
    const { data, error } = await supabase.auth.signInWithPassword({
      email: input.email,
      password: input.password,
    });

    if (error) {
      throw mapSupabaseError(error.message);
    }

    if (!data.session) {
      throw new AuthError("Credenciales invalidas", 401);
    }

    return toSession(data.session);
  },
};

function toSession(session: {
  access_token: string;
  refresh_token: string;
  user: { id: string; email?: string | null };
}): AuthSession {
  return {
    accessToken: session.access_token,
    refreshToken: session.refresh_token,
    user: {
      id: session.user.id,
      email: session.user.email ?? "",
    },
  };
}

function mapSupabaseError(message: string): AuthError {
  const lower = message.toLowerCase();
  if (lower.includes("already")) {
    return new AuthError("Ese correo ya esta registrado", 409);
  }
  if (lower.includes("not confirmed")) {
    return new AuthError("Debes confirmar tu correo antes de entrar", 403);
  }
  if (lower.includes("invalid login credentials")) {
    return new AuthError("Correo o contrasena incorrectos", 401);
  }
  if (lower.includes("rate limit") || lower.includes("too many")) {
    return new AuthError("Demasiados intentos. Espera unos minutos.", 429);
  }
  if (lower.includes("email") && lower.includes("invalid")) {
    return new AuthError("Correo invalido", 400);
  }
  if (lower.includes("password") || lower.includes("contrasena")) {
    return new AuthError(message, 400);
  }
  // Fallback: mensaje real del proveedor para depuracion (seccion 14 del prompt)
  return new AuthError(`Error de autenticacion: ${message}`, 400);
}
