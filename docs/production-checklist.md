# 🛫 STOCKPULSE — Checklist para llevar a producción

Pasos y verificaciones para operar con usuarios reales. Marca cada ítem al completarlo.

---

## 1 · Infraestructura y dominio

- [ ] Contratar hosting del backend con ** HTTPS obligatorio** (p. ej. Railway/Render/Fly/VPS + Nginx). `http://` jamás en producción
- [ ] Dominio propio apuntando al servidor (ej. `api.stockpulse.app`)
- [ ] Backend corriendo con **gestor de procesos** (`pm2`, Docker o similar) — nunca una ventana de Node abierta
- [ ] Variables de entorno definidas en el panel del hosting (NO en archivos `.env` versionados)

## 2 · Base de datos (Supabase)

- [ ] Proyecto Supabase de producción (o plan pago), con **backups automáticos** activados
- [ ] Esquema cargado: `schema.sql` + `trading.sql` + `payments.sql` (revisar que `/_dev fix` no exista)
- [ ] **Auth → Email → "Confirm email" ON** (en dev está OFF a propósito)
- [ ] Reglas **RLS** verificadas manualmente: con el SQL Editor simular dos usuarios distintos y confirmar que no se vean filas entre sí
- [ ] `credit_wallet` probada con pagos duplicados (idempotencia: no acredita dos veces el mismo `stripe_session_id`)

## 3 · Stripe (producción)

- [ ] Cambiar llaves `sk_test_…`/`pk_test_…` por `sk_live_…`/`pk_live_…` en el `.env` del hosting
- [ ] Activar **Stripe Webhooks** firmados: evento `checkout.session.completed` → endpoint nuevo del backend en `/api/payments/webhook` con verificación de firma (`STRIPE_WEBHOOK_SECRET`)
- [ ] El webhook reusa `fulfillCheckout(sessionId)` — el flujo de verificación del WebView queda como **segunda vía**, no como única
- [ ] Probar el flujo con una tarjeta real de bajo valor y verificar `credit_movements` + `payments.status='paid'` + saldo
- [ ] Definir **impuestos/facturación** si el país lo requiere y actualizar textos del checkout (lenguaje, moneda MXN)

## 4 · Finnhub (mercado)

- [ ] Validar el **plan contratado** (free: 60 llamadas/min; evaluar upgrade si el volumen de usuarios lo exige)
- [ ] Confirmar límites configurados en el cliente (caché 15 s + throttle) soportan el número de usuarios esperado
- [ ] Definir qué tickers van en "Destacar" (tabla `stocks.is_popular`) y sembrar símbolos válidos (solo mercado EE.UU. en este plan)

## 5 · Firebase / FCM

- [ ] Proyecto Firebase en **plan con FCM escalable**; mantener el `google-services.json` actualizado en ambos módulos
- [ ] `FIREBASE_SERVICE_ACCOUNT_PATH` apuntando al JSON de producción en el hosting (fuera de Git)
- [ ] Probar push a → teléfono, → reloj, → ambos a la vez (registrar en `devices` con plataforma `android`/`wearos`)
- [ ] Mensajes silenciosos `sync` verificados (SyncBus instantáneo) con app en primer plano y en segundo plano

## 6 · App Android (teléfono y reloj)

- [ ] **URL del backend con HTTPS** en los clientes Retrofit (quitar `10.0.2.2` y el permiso/información cleartext de desarrollo)
  - teléfono: `app/.../di/AppModule.kt` (base URL del Retrofit)
  - reloj: `wear/.../data/WearRepository.kt` (baseUrl)
  - eliminar `wear/src/main/res/xml/network_security_config.xml` de "10.0.2.2" (o apuntarlo al dominio real con HTTPS)
- [ ] `versionCode`/`versionName` incrementados; **minify/proguard** evaluado; firma de release con keystore privado
- [ ] Config de ejecución `wear` probada en un reloj real (o emulador Wear final) con login completo del flujo E2E
- [ ] Permisos: `POST_NOTIFICATIONS` pedido en el primer arranque (ya implementado); Android 13+ verificado
- [ ] Publicar en Play Store (app + app del reloj como dependiente o independiente según estrategia)

## 7 · El backend: límites, monitoreo, resiliencia

- [ ] **Rate limiting** por IP/token (ej. `express-rate-limit`) en auth, trading y pagos
- [ ] **Logging estructurado** + alertas de errores (ej. Sentry) — hoy `console.log/warn`
- [ ] Uptime/monitoring del endpoint `/api/health` (ej. UptimeRobot)
- [ ] CORS configurado para el dominio real (no `*`)
- [ ] Confirmar que **ningún endpoint de crédito responde sin verificación de Stripe**
- [ ] Respaldo de credenciales: rotación planificada de llaves (Stripe, Finnhub, service account)

## 8 · Pruebas finales E2E (obligatorio antes de abrir al público)

- [ ] Registro → 10,000 créditos → compra 1 acción → portafolio matcheado con precio real
- [ ] Venta → saldo exacto, `credit_movements` y `transactions` consistentes
- [ ] Crear alerta que dispare → notificación en TELÉFONO y RELOJ + fila en `alert_history`
- [ ] Borrar alerta en el teléfono → desaparece en el reloj (push instantáneo)
- [ ] Compra Stripe (live) → consultado en la base (`payments.paid`) + créditos + push de sincronización
- [ ] Cerrar sesión → reingreso → sesión persistida; error de conexión → mensajes reales y no crasheos
- [ ] Dispositivos duplicados: el mismo usuario con reloj y teléfono recibe en ambos el aviso, no duplicado en pantallas

---

**Doña final:** apagar "modo dev" = (HTTPS + real keys + webhooks + confirmación de correo + límites + monitoreo). Todo lo demás ya está listo para producción.
