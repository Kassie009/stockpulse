import { Router } from "express";
import { stocksController } from "../controllers/stocksController";

export const stocksRouter = Router();

stocksRouter.get("/popular", (req, res, next) => stocksController.popular(req, res, next));
stocksRouter.get("/quote", (req, res, next) => stocksController.quote(req, res, next));
stocksRouter.get("/search", (req, res, next) => stocksController.search(req, res, next));
