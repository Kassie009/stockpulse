import express from "express";
import cors from "cors";
import { env } from "./config/env";
import { healthRouter } from "./routes/health";
import { authRouter } from "./routes/auth";
import { stocksRouter } from "./routes/stocks";
import { tradingRouter } from "./routes/trading";
import { alertsRouter } from "./routes/alerts";
import { paymentsRouter } from "./routes/payments";
import { startAlertEngine } from "./services/alertEngine";
import { errorHandler, notFoundHandler } from "./middlewares/errorHandler";

const app = express();

// Middlewares base
app.use(cors());
app.use(express.json({ limit: "1mb" }));

// Registre de rutas (FASE 2: solo health; se agregaran auth, stocks, simulator, etc.)
app.use("/api/health", healthRouter);
app.use("/api/auth", authRouter);
app.use("/api/stocks", stocksRouter);
app.use("/api/trading", tradingRouter);
app.use("/api/alerts", alertsRouter);
app.use("/api/payments", paymentsRouter);

// Utilidades
app.get("/", (_req, res) => {
  res.json({ ok: true, service: "stockpulse-backend", docs: "/api/health" });
});
app.use(notFoundHandler);
app.use(errorHandler);

app.listen(env.PORT, () => {
  // eslint-disable-next-line no-console
  console.log(
    `[stockpulse-backend] escuchando en http://localhost:${env.PORT} (${env.NODE_ENV})`
  );
  startAlertEngine(); // FASE 7: motor de alertas (precio -> FCM)
});

export default app;
