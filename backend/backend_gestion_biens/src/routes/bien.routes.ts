import { Router } from "express";
import { BienController } from "../controllers/bien.controller";
import { authenticate, requireRole } from "../middlewares/auth.middleware";
import {
  validateCreateBien,
  validatePatchStatut,
  validateUpdateBien,
  validateUuidParam,
} from "../middlewares/validation.middleware";

export const bienRouter = Router();
const controller = new BienController();

// Toutes les routes /api/biens requièrent d'être authentifié
bienRouter.use(authenticate);

// Liste filtrée - accessible à tout utilisateur authentifié (BAILLEUR, GESTIONNAIRE, LOCATAIRE)
bienRouter.get("/", controller.getAll);

// Création d'un bien - BAILLEUR ou GESTIONNAIRE uniquement
bienRouter.post("/", requireRole("BAILLEUR", "GESTIONNAIRE"), validateCreateBien, controller.create);

// Disponibilité d'un bien (appelé par exemple par le module Loyers)
bienRouter.get("/:id/disponibilite", validateUuidParam("id"), controller.getDisponibilite);

// Modification atomique du statut - BAILLEUR ou GESTIONNAIRE
bienRouter.patch(
  "/:id/statut",
  requireRole("BAILLEUR", "GESTIONNAIRE"),
  validateUuidParam("id"),
  validatePatchStatut,
  controller.updateStatut
);

// Détail d'un bien
bienRouter.get("/:id", validateUuidParam("id"), controller.getById);

// Modification complète - BAILLEUR ou GESTIONNAIRE
bienRouter.put(
  "/:id",
  requireRole("BAILLEUR", "GESTIONNAIRE"),
  validateUuidParam("id"),
  validateUpdateBien,
  controller.update
);

// Suppression d'un bien - BAILLEUR ou GESTIONNAIRE
bienRouter.delete(
  "/:id",
  requireRole("BAILLEUR", "GESTIONNAIRE"),
  validateUuidParam("id"),
  controller.delete
);

