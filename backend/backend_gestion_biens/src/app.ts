import express, { Express, Request, Response, NextFunction } from "express";
import cors from "cors";
import swaggerUi from "swagger-ui-express";
import { openapiSpec } from "./docs/openapi";
import { authRouter } from "./routes/auth.routes";
import { bienRouter } from "./routes/bien.routes";
import { errorHandler } from "./middlewares/error-handler";
import { AppError } from "./utils/app-error";

export const createApp = (): Express => {
  const app = express();

  // Middlewares généraux
  app.use(cors());
  app.use(express.json({ limit: "10mb" }));

  // Healthcheck sans authentification
  app.get("/health", (_req: Request, res: Response) => {
    res.status(200).json({ service: "biens", status: "UP" });
  });

  // Documentation Swagger UI sur /api-docs (spécification OpenAPI complète)
  app.use("/api-docs", swaggerUi.serve, swaggerUi.setup(openapiSpec));

  // Routes d'authentification (Login / Register / Profil sur table PostgreSQL users)
  app.use("/api/auth", authRouter);

  // Routes métier Biens
  app.use("/api/biens", bienRouter);

  // Gestion des routes inexistantes (404)
  app.use((req: Request, _res: Response, next: NextFunction) => {
    next(new AppError(`Route introuvable : ${req.method} ${req.originalUrl}`, 404));
  });

  // Middleware centralisé de gestion des erreurs
  app.use(errorHandler);

  return app;
};
