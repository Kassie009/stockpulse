import { Router } from "express";
import { tradingController } from "../controllers/tradingController";
import { requireAuth } from "../middlewares/authMiddleware";

export const tradingRouter = Router();

// Todas las rutas de trading exigen sesion valida
tradingRouter.get("/summary", requireAuth, (req, res, next) => tradingController.summary(req, res, next));
tradingRouter.post("/buy", requireAuth, (req, res, next) => tradingController.buy(req, res, next));
tradingRouter.post("/sell", requireAuth, (req, res, next) => tradingController.sell(req, res, next));
tradingRouter.get("/transactions", requireAuth, (req, res, next) => tradingController.transactions(req, res, next));
