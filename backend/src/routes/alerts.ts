import { Router } from "express";
import { alertsController } from "../controllers/alertsController";
import { requireAuth } from "../middlewares/authMiddleware";

export const alertsRouter = Router();

alertsRouter.post("/", requireAuth, (req, res, next) => alertsController.create(req, res, next));
alertsRouter.get("/", requireAuth, (req, res, next) => alertsController.list(req, res, next));
alertsRouter.delete("/", requireAuth, (req, res, next) => alertsController.remove(req, res, next));
alertsRouter.patch("/", requireAuth, (req, res, next) => alertsController.setActive(req, res, next));

// Registro de token FCM (dispositivo notificable) + listado (FASE 9.5)
alertsRouter.post("/devices", requireAuth, (req, res, next) => alertsController.registerDevice(req, res, next));
alertsRouter.get("/devices", requireAuth, (req, res, next) => alertsController.listDevices(req, res, next));
