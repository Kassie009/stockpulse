# 📈 STOCKPULSE

**Simulador bursátil educativo para Android + Wear OS.**
Aprende a invertir con **créditos virtuales** usando **precios reales** del mercado de EE.UU. (Finnhub), alertas de precio con **notificaciones push** (teléfono y reloj a la vez), motor de trading en el servidor y pagos de créditos con **Stripe** (modo test).

> Regla de la casa: **el precio SIEMPRE lo fija el servidor** — la app solo envía símbolo y cantidad. Nunca se simula un pago aprobado ni se otorgan créditos desde el cliente.

---

## 🧭 Qué incluye

| Módulo | Estado |
|---|---|
| Autenticación real (registro/login con correo) | ✅ |
| Mercado en vivo (cotizaciones Finnhub, búsqueda por símbolo) | ✅ |
| Motor de trading (comprar/vender, portafolio, P&L en vivo) | ✅ |
| Alertas de precio/variación + **notificaciones FCM** a teléfono y reloj | ✅ |
| Compra de créditos con Stripe Checkout (test) acreditada de forma **idempotente** | ✅ |
| App Wear OS (portafolio de un vistazo + alertas en la muñeca) | ✅ |
| Sincronización **instantánea** teléfono↔reloj↔backend (FCM data + polling de reserva) | ✅ |
| Pantallas: Dashboard, Mercado, Portafolio, Perfil, Alertas, Dispositivos, Pagos | ✅ |

---

## 🏗️ Arquitectura

```
┌──────────────┐     ┌──────────────┐
│ App teléfono │     │  App reloj   │      Android · Kotlin · Compose
│   (Compose)  │     │  (Wear OS)   │      MVVM + Clean · Hilt · Retrofit
└──────┬───────┘     └──────┬───────┘
       │  HTTPS + JWT        │  HTTPS + JWT
       ▼                     ▼
        ┌─────────────────────────┐
        │    BACKEND Node.js      │  Express + TypeScript
        │  - auth (JWT Supabase)  │
        │  - motor de trading     │  precio SIEMPRE del servidor
        │  - motor de alertas     │  ciclo cada 20s + cooldown 10min
        │  - créditos (Stripe)    │  fulfillment idempotente
        └───┬─────────┬────────┬──┘
            ▼         ▼        ▼
     ┌─────────┐ ┌─────────┐ ┌──────────────────┐
     │ Supabase│ │ Finnhub │ │ Stripe (test) +  │
     │ Postgres│ │ mercado │ │ Firebase FCM     │
     │ + Auth  │ │ en vivo │ │ (push + sync)    │
     └─────────┘ └─────────┘ └──────────────────┘
```

### Estructura del repositorio

```
stockpulse/
├── android-app/            # Gradle raíz de ambas apps Android
│   ├── app/                # App de teléfono (com.stockpulse.app)
│   └── wear/               # App de reloj (namespace com.stockpulse.wear, mismo applicationId → comparte Firebase)
├── backend/                # API Node + TS
│   ├── src/{routes,controllers,services,integrations,validators,middlewares,config,utils}
│   ├── secrets/            # Credenciales del servicio Firebase (NUNCA en Git)
│   └── .env / .env.example
├── database/               # SQL para Supabase (pegar en orden)
│   ├── schema.sql          # Tablas, RLS, triggers, datos bases
│   ├── trading.sql         # Motor SQL del trading (transacciones atómicas)
│   └── payments.sql        # credit_wallet (acreditar créditos, idempotente)
└── docs/
    ├── demo-guia-clase.md  # Guion de demostración en vivo (12 min)
    └── production-checklist.md
```

---

## 🚀 Puesta en marcha (desarrollo)

### 0) Requisitos
- Node.js 18+ (se usa `tsx` para TS directo)
- Android Studio con JDK 17 (JBR interno) y SDK 35
- Emuladores: teléfono (API 29+, ej. `Kassie_API_29`) y **Wear OS** (recomendado: imagen `android-wear` API 34, pantalla circular 320×320)

### 1) Base de datos (Supabase)
1. Crea un proyecto en [supabase.com](https://supabase.com)
2. En **SQL Editor**, ejecuta **en orden** y completo:
   - `database/schema.sql` (tablas + RLS + auth ↔ users trigger"
   - `database/trading.sql` (función transaccional de compra/venta)
   - `database/payments.sql` (función `credit_wallet` atómica e idempotente)
3. **Auth → Providers → Email**: para desarrollo deja **"Confirm email" OFF** (evita el límite de correos del sandbox). Para producción: ON.

### 2) Backend
```powershell
cd backend
copy .env.example .env    # y pega tus valores reales
npm install
node --import tsx src/index.ts   # sirve en http://localhost:3000
```
Variables del `.env` (ver `.env.example`): Supabase URL + claves, `FINNHUB_API_KEY`, `STRIPE_SECRET_KEY`/`STRIPE_PUBLISHABLE_KEY` (test), y `FIREBASE_SERVICE_ACCOUNT_PATH` apuntando al JSON de cuenta de servicio **en `backend/secrets/`** (jamás en Git).

### 3) Android
1. Abre **`android-app/`** en Android Studio
2. Coloca tu **`google-services.json`** en `app/` y en `wear/` (el módulo del reloj usa el **mismo applicationId `com.stockpulse.app`**, por eso comparte el archivo y los tokens FCM registran plataforma `wearos`)
3. `Run ▶` en el teléfono (y en el reloj con la configuración de ejecución `wear`)
- En los emuladores el backend se ve en **`http://10.0.2.2:3000/`** (localhost del PC); en un teléfono físico cambia la base URL al IP local y habilita tráfico claro solo para desarrollo.

### 4) Tarjeta de prueba Stripe
```
Número: 4242 4242 4242 4242   ·   Fecha: futura (ej. 12/34)   ·   CVC: 3 dígitos
```
Al completar, el backend **verifica la sesión con Stripe** y acredita vía `credit_wallet` (si la base no está desplegada, avisa con instrucción explícita).

---

## 🔌 API (resumen)

| Método | Ruta | ¿Auth? | Descripción |
|---|---|---|---|
| GET | `/api/health` | — | Estado del backend + integraciones |
| POST | `/api/auth/register` `/api/auth/login` | — | Registro/login (Supabase Auth) |
| GET | `/api/stocks/popular` — `/api/stocks/quote?symbol=` — `/api/stocks/search?q=` | JWT | Cotizaciones Finnhub (caché 15s) |
| GET | `/api/trading/summary` `/api/trading/transactions` | JWT | Portafolio en vivo + historial |
| POST | `/api/trading/buy` `/api/trading/sell` | JWT | Compra/venta (transaccional, precio del servidor) |
| GET/POST/DELETE/PATCH | `/api/alerts` | JWT | Alertas del usuario (CRUD) |
| GET/POST | `/api/alerts/devices` | JWT | Dispositivos vinculados (tokens FCM, plataforma `android`/`wearos`) |
| GET | `/api/payments/packages` | — | Paquetes de créditos |
| POST | `/api/payments/checkout` | JWT | Crea sesión Stripe Checkout |
| GET | `/api/payments/status?session_id=` | JWT | Verifica y acredita si falta |
| GET | `/api/payments/success` `/api/payments/cancel` | — | Retorno del WebView (página HTML de resultado) |

Mensajes de error **siempre reales y específicos** (p. ej. "Saldo insuficiente", "El precio subió a…"): nunca textos genéricos.

---

## 🔔 Notificaciones y sincronización

```
Motor de alertas (cada 20s)        CRUD alertas / trade / pago acreditado
        │                                    │
        ▼ notificación visible               ▼ push silencioso (data)
   FCM → teléfono + reloj              FCM → SyncBus (app) / WearSyncBus (reloj)
   (ambos dispositivos de               → las pantallas abiertas refrescan
    la cuenta, por token)                 AL INSTANTE (push)
                                        + refresco programado de respaldo
```
- Cada dispositivo (teléfono, reloj) registra su **token FCM** al iniciar sesión (`devices` tabla con plataforma)
- **Idempotencia**: una alerta repetida entra en *cooldown* de 10 min para no bombardear
- El reloj muestra: Página 1 (portafolio de un vistazo), Página 2 (tus alertas), Página 3 (cerrar sesión), con puntos indicadores; y redondeó intrínseco: contenido centrado para pantalla circular

---

## 🔐 Seguridad implementada

1. **JWT de Supabase** en cada endpoint privado (`requireAuth`); sin token → 401
2. **RLS en Postgres**: las filas del usuario solo la ve él (`auth.uid()` matching), incluso suplantando la API pública
3. **Precio y montos solo en el servidor**: la app manda `symbol` + `quantity`; nunca cifras
4. **Funciones `SECURITY DEFINER` transaccionales** para trading y acreditación (bloqueo `FOR UPDATE`, idempotencia por referencia del pago)
5. **OAuth nulo**: no se guardan llaves secretas en el APK; el `.env` y `backend/secrets/` **están excluidos de Git** (`.gitignore`)
6. **Validación de entrada** con Zod (símbolos, umbrales, paquetes con whitelist)
7. Credenciales de service-role **solo** en el backend

---

## 🧪 Operación para demo/clase
Ver `docs/demo-guia-clase.md` — guion de 12 minutos con improvisaciones de emergencia.

## 🛫 Llevar a producción
Ver `docs/production-checklist.md` — HTTPS, webhook firmado de Stripe, "Confirm email" ON, límites de tasa, monitoreo, etc.

---

## 📝 Notas de diseño (por qué tomamos ciertas decisiones)

- **El precio vive en el backend**: simplifica las apps (teléfono/reloj), evita manipulación, y centraliza el control de cuota de Finnhub (caché + throttle)
- **El reloj no opera**: una pantalla de 320 px no es para operar bolsa; su rol = consultar portafolio de un vistazo + alertas en la muñeca
- **`change_pos`/`change_neg` miden el movimiento del DÍA** (precio actual vs cierre de ayer, como lo reporta Finnhub `dp`); no varían desde la creación de la alerta — así lo entiende el usuario desde el Dashboard
- **Fulfillment de pagos con segunda vía**: además del webhook estandarizado de la FASE de producción, el backend verifica la sesión al volver del WebView (útil en desarrollo sin HTTPS)
- **El polling de 30s/45s es de reserva**: el canal primario de sincronización es el push silencioso
