import { Request, Response, NextFunction } from "express";
import jwt from "jsonwebtoken";
import { env } from "../config/env";
import { JwtPayload, UserRole } from "../models/bien.model";
import { AppError } from "../utils/app-error";

declare global {
  namespace Express {
    interface Request {
      user?: JwtPayload;
    }
  }
}

export const authenticate = (req: Request, _res: Response, next: NextFunction): void => {
  const authHeader = req.headers.authorization;
  if (!authHeader) {
    throw new AppError("Jeton d'authentification manquant dans l'en-tête Authorization.", 401);
  }

  const parts = authHeader.split(" ");
  if (parts.length !== 2 || parts[0] !== "Bearer") {
    throw new AppError("Format du jeton invalide. Format attendu : Bearer <token>", 401);
  }

  const token = parts[1];
  try {
    const decoded = jwt.verify(token, env.JWT_SECRET) as JwtPayload;
    if (!decoded || !decoded.sub || !decoded.role) {
      throw new AppError("Payload du jeton JWT incomplet (sub et role requis).", 401);
    }
    req.user = decoded;
    next();
  } catch (error) {
    if (error instanceof AppError) {
      next(error);
      return;
    }
    next(new AppError("Jeton d'authentification invalide ou expiré.", 401));
  }
};

export const requireRole = (...allowedRoles: UserRole[]) => {
  return (req: Request, _res: Response, next: NextFunction): void => {
    if (!req.user) {
      throw new AppError("Utilisateur non authentifié.", 401);
    }

    if (!allowedRoles.includes(req.user.role)) {
      throw new AppError(
        `Accès refusé : rôle '${req.user.role}' insuffisant. Rôles autorisés : ${allowedRoles.join(", ")}.`,
        403
      );
    }

    next();
  };
};

