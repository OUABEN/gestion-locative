import { Request, Response, NextFunction } from "express";
import { AppError } from "../utils/app-error";
import { logger } from "../utils/logger";

export const errorHandler = (
  err: Error,
  _req: Request,
  res: Response,
  _next: NextFunction
): void => {
  if (err instanceof AppError) {
    res.status(err.statusCode).json({
      message: err.message,
      ...(err.errors && err.errors.length > 0 ? { errors: err.errors } : {}),
    });
    return;
  }

  // Erreur de parsing JSON d'Express (SyntaxError levée par body-parser)
  const bodyErr = err as { type?: string; status?: number; statusCode?: number };
  if (bodyErr.type === "entity.parse.failed" || (err instanceof SyntaxError && "body" in err)) {
    res.status(400).json({
      message: "Format du corps de requête JSON invalide.",
    });
    return;
  }

  // Erreurs PostgreSQL courantes (ex: échec contrainte check ou format)
  const pgErr = err as { code?: string; detail?: string; constraint?: string };
  if (pgErr.code === "23514") {
    // Check constraint violation
    res.status(400).json({
      message: "Violation d'une contrainte de validation en base de données.",
      errors: [pgErr.constraint || "Contrainte non respectée"],
    });
    return;
  }

  if (pgErr.code === "22P02") {
    // Invalid text representation (ex: uuid)
    res.status(400).json({
      message: "Format de données invalide pour la base de données.",
    });
    return;
  }

  logger.error("Erreur serveur non gérée :", err);

  res.status(500).json({
    message: "Une erreur interne est survenue sur le serveur.",
  });
};

