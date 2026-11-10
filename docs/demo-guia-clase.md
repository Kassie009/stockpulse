# STOCKPULSE — Demo guiada para clase (12 minutos)

> Guion paso a paso: qué hacer en la pantalla + qué decir en cada paso.
> Prepara todo ANTES de que te llamen y esta demo corre sola.

---

## 0 · Preparación (10 min antes de la clase)

| # | Tarea | Cómo |
|---|---|---|
| 1 | Backend encendido | `cd C:\Users\Administrator\Desktop\stockpulse\backend` → `node --import tsx src/index.ts` (deja la ventana abierta) |
| 2 | Emulador teléfono | Android Studio → Device Manager → **Kassie_API_29** ▶ |
| 3 | Emulador reloj | Device Manager → **WearStockPulse** ▶ (se ve redondo) |
| 4 | Apps abiertas y logueadas | teléfono: `kass@gmail.com` / reloj: **misma cuenta** |
| 5 | Estado limpio sugerido | 10 AAPL en portafolio, sin alertas viejas (borra las de pruebas) |
| 6 | Proyector listo | El emulador del TELÉFONO en grande; el reloj en ventana flotante pequeña |

**Frase de apertura (30 s):**
> "La inversión en bolsa es una habilidad que casi nadie enseña — solo dicen 'compra barato, vende caro'. Les presento STOCKPULSE: un simulador de inversión en Android con precios REALES del mercado en vivo, donde aprendes a invertir sin arriesgar ni un peso. Es una app de teléfono con su app compañera de reloj, un backend propio, y pagos con Stripe. La recorro en vivo."

---

## 1 · Registro y créditos (2 min) — la puerta de entrada

**Pantalla:** Bienvenida (STOCKPULSE)
**Toca:** "Crear cuenta" → llena nombre/correo/contraseña (ej. `demo@stockpulse.app`) → Registrar
**Qué pasa:** te loguea automáticamente y te regala **10,000 créditos**

**Dices mientras llenas el formulario:**
> "Todo lo que ven son créditos virtuales — practican sin tocar dinero real. El backend regala 10,000 créditos para practicar desde el minuto uno. La sesión queda guardada: si cierran la app y reabren, siguen donde lo dejaron — por eso el usuario de mi emulador ya está dentro."

💡 *Si se demora el registro: la contraseña viaja cifrada y los datos de cada usuario están protegidos con Row Level Security en la base (cada fila solo la ve su dueño).*

---

## 2 · Dashboard: la casa de todo (1 min)

**Ve a la pestaña "Inicio" (Dashboard).**
Muestra:
- **Saldo disponible**
- **Valor del portafolio** con el % verde/rojo
- **Movimientos recientes**

**Dices:**
> "'Saldo' es efectivo disponible para operar. 'Valor del portafolio' es qué valen mis acciones hoy. El porcentaje compara contra lo que PAGUÉ: verde = ganancia, rojo = pérdida. Y 'P&L' significa Profit and Loss: ganancia/pérdida."

💡 **Detalle técnico para clase:** "Estas pantallas se refrescan solas cada 30 segundos, y cuando ocurre algo importante (una compra, un pago) el backend manda una notificación push silenciosa y se refrescan AL INSTANTE — la misma arquitectura de las apps bancarias."

---

## 3 · Comprar la primera acción (3 min) — el momento "es real"

**Ve a "Mercado".**
- Se ven los tickers: AAPL (Apple), TSLA (Tesla), NVDA (Nvidia)… con **precio vivo y % del día**
- Abajo dice **"Actualizado: fecha, hora"** — señala la hora: se refresca cada 30s

**Toca AAPL →** se abre el cuadro de compra/venta → cantidad: **1** → **Comprar**

**Qué pasa:** mensaje real tipo *"Compra realizada: 1 de AAPL a $348.15"* — y el saldo baja exactamente ese monto.

**Dices:**
> "Fíjense: la app NO pide precio, pide CANTIDAD. El precio lo pone el servidor con la cotización REAL de Finnhub en ese instante. Es la regla de oro de las fintech: el cliente jamás decide montos. ¿Por qué cambió el precio que viste en clase de otro día? Porque el mercado en vivo se mueve — cada 30 segundos esta pantalla hace un refresco."

💡 Si preguntan "¿A quién le compras?": **"Al mercado. El sistema cruza mi orden con la mejor demanda disponible, como haría NYSE en milisegundos — la empresa no te vende sus acciones directamente."**

---

## 4 · Portafolio: tu dinero en dos mitades (2 min)

**Ve a "Portafolio".** Muestra la tarjeta grande:
- **Créditos disponibles** — el efectivo
- **Valor portafolio** — lo que valen tus acciones hoy (10 Apple × precio actual)
- **P&L no realizado** — la ganancia/pérdida "en el papel"

**Dices:**
> "Mi patrimonio total = los dos números sumados. El P&L es 'no realizado' porque solo cuando VENDO se vuelve créditos reales en mi saldo. Verde: gano. Rojo: pierdo en el papel. ¿Y qué hago si está en rojo? Esa pregunta NO se responde con nervios — se responde con la herramienta que vengo a mostrar..."

---

## 5 · Alertas: tu copiloto (3 min) — la pieza estrella ⭐

**Ve a Perfil → Mis alertas de precio → +**

**Crea el STOP (protección de pérdida):**
```
Símbolo:  AAPL
Tipo:     Precio por debajo
Umbral:   325   (≈ 7% abajo de tu precio de compra)
```
> "Este es mi STOP-LOSS: si Apple cae a $325, la app me avisa para VENDER y limitar el daño. El stop es la disciplina #1 de inversión: pierde pequeño, deja correr las ganancias — porque recuperar un −50% requiere ganar +100%, mientras un −7% se recupera con un +7.5%."

**Crea el OBJETIVO (take profit):**
```
Símbolo:  AAPL
Tipo:     Precio por encima
Umbral:   370   (≈ +6% sobre tu compra)
```
> "Y este es mi objetivo: si Apple sube a $370, cumplí mi plan y vendo. La lección: se define el plan ANTES de emocionarse, y la herramienta lo vigila 24/7 — el motor del backend revisa todas las alertas de todos los usuarios CADA 20 SEGUNDOS con precios en vivo."

**Cambia al emulador del RELOJ y desliza hasta la Página 2 "Tus alertas":**
> "Y miren esto: las MISMAS alertas y el MISMO portafolio se ven en el reloj — misma cuenta, mismo backend. Cuando algo cambia en el teléfono, el reloj se sincroniza AL INSTANTE por notificación push silenciosa de Firebase; y si borro una alerta acá, en segundos desaparece de allá. El reloj es para consultar y recibir avisos; operar es del teléfono — nadie opera bolsa en una pantalla de 320 píxeles."

---

## 6 · Pagos con Stripe (1.5 min)

**Ve a Perfil → Obtener más créditos → "Comprar" en Básico ($49 / +10,000 créditos)**
- Se abre la página REAL de Stripe dentro de la app (número de tarjeta, CVC, país…)
- Muestra el formulario unos segundos y toca **"← Regresar"** (no llenes la tarjeta en clase)

**Dices:**
> "Si me quedo sin créditos, compro un paquete. Es la página de pago REAL de Stripe, en modo de pruebas: el precio lo arma el servidor, el pago viaja por el checkout seguro de Stripe, y los créditos SOLO se acreditan cuando Stripe le confirma al backend que el pago existe. La app jamás puede otorgarse créditos gratis — por diseño."

💡 *(Si alguien levanta la mano sobre la missing de "Sandbox": le explicas que en modo test Stripe no cobra dinero real — es la misma pasarela que en producción, con tarjetas de prueba.)*

---

## 7 · Cierre (30 s) 🎓
> "El viaje completo: registrarse → 10,000 créditos → compré una Apple a precio vivo → vi mi portafolio con P&L → definí mi plan (stop y objetivo) con alertas → las alertas me vigilan en el teléfono y en el reloj al mismo tiempo → y si necesito más créditos, pago con Stripe con la seguridad de un e-commerce real. Todo el cerebro del negocio vive en un backend Node con TypeScript sobre Supabase/Postgres, con las reglas de oro fintech: el servidor fija precios, verifica pagos, protege los datos por usuario y hace las operaciones financieras atómicas. Gracias — ¿preguntas?"

---

## Emergencias de demo (qué decir si falla algo)

| Si pasa... | Dices... |
|---|---|
| Backend no responde | "Es entorno dev: inicio el servidor y sigo" → reabre la pantalla |
| La alerta no dispara en clase | "La condición depende del precio real de hoy; el motor la vigila 24/7 y avisa apenas cruce. Las dejo configuradas" |
| La compra la rechaza | Muestra el mensaje real (ej. saldo insuficiente) — eso ES la demo: validaciones honestas del servidor |
| Se deslogueó algo | Demuestra el RE-LOGIN en vivo: otra pieza del sistema (tokens + persistencia segura) |
| Se acaba el tiempo | Salta directo a paso 4 (Portafolio) + paso 5 (Alertas + reloj) — son los 5 min esenciales |

## Tiempos
| Min | Paso |
|---|---|
| 0–0.5 | Apertura |
| 0.5–2.5 | Registro (1) |
| 2.5–3.5 | Dashboard (2) |
| 3.5–6.5 | Compra (3) |
| 6.5–8.5 | Portafolio (4) |
| 8.5–11.5 | Alertas + reloj (5) |
| 11.5–12 | Stripe (6) + cierre (7) |

*(Si no alcanza: corta Registro y Stripe, dásele el aire extra a Alertas + reloj — son tu diferencial.)*
