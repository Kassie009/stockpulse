import { Router } from "express";
import { paymentsController } from "../controllers/paymentsController";
import { requireAuth } from "../middlewares/authMiddleware";

export const paymentsRouter = Router();

paymentsRouter.get("/packages", (req, res, next) => paymentsController.packages(req, res, next));
paymentsRouter.post("/checkout", requireAuth, (req, res, next) => paymentsController.checkout(req, res, next));
paymentsRouter.get("/status", requireAuth, (req, res, next) => paymentsController.status(req, res, next));

// Retornos de Stripe (viven en el WebView; sin token Bearer)
paymentsRouter.get("/success", (req, res) => paymentsController.success(req, res));
paymentsRouter.get("/cancel", (req, res) => paymentsController.cancel(req, res));
