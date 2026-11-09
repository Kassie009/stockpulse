import express from "express";
import cors from "cors";
import { env } from "./config/env";
import { healthRouter } from "./routes/health";
import { authRouter } from "./routes/auth";
import { errorHandler, notFoundHandler } from "./middlewares/errorHandler";

const app = express();

// Middlewares base
app.use(cors());
app.use(express.json({ limit: "1mb" }));

// Registre de rutas (FASE 2: solo health; se agregaran auth, stocks, simulator, etc.)
app.use("/api/health", healthRouter);
app.use("/api/auth", authRouter);

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
});

export default app;
