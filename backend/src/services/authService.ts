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
  if (message.includes("already registered") || message.includes("already been registered")) {
    return new AuthError("Ese correo ya esta registrado", 409);
  }
  if (message.toLowerCase().includes("not confirmed")) {
    return new AuthError("Debes confirmar tu correo antes de entrar", 403);
  }
  if (message.toLowerCase().includes("invalid login credentials")) {
    return new AuthError("Correo o contrasena incorrectos", 401);
  }
  if (message.toLowerCase().includes("email address") || message.toLowerCase().includes("invalid")) {
    return new AuthError("Datos invalidos para el registro", 400);
  }
  return new AuthError("Error de autenticacion", 400);
}
