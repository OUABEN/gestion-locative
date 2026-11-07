import { Request, Response, NextFunction } from "express";
import { AppError } from "../utils/app-error";
import { StatutBien } from "../models/bien.model";

const UUID_REGEX = /^[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i;

export const validateUuidParam = (paramName = "id") => {
  return (req: Request, _res: Response, next: NextFunction): void => {
    const value = req.params[paramName];
    if (!value || !UUID_REGEX.test(value)) {
      throw new AppError(
        `L'identifiant '${value}' n'est pas un UUID valide pour le paramètre '${paramName}'.`,
        400
      );
    }
    next();
  };
};

export const validateCreateBien = (req: Request, _res: Response, next: NextFunction): void => {
  const { adresse, ville, type, surface, prixMensuel, charges } = req.body || {};
  const errors: string[] = [];

  if (!adresse || typeof adresse !== "string" || adresse.trim() === "") {
    errors.push("Le champ 'adresse' est obligatoire et doit être une chaîne non vide.");
  }
  if (!ville || typeof ville !== "string" || ville.trim() === "") {
    errors.push("Le champ 'ville' est obligatoire et doit être une chaîne non vide.");
  }
  if (!type || typeof type !== "string" || type.trim() === "") {
    errors.push("Le champ 'type' est obligatoire et doit être une chaîne non vide.");
  }

  if (surface === undefined || typeof surface !== "number" || isNaN(surface) || surface <= 0) {
    errors.push("Le champ 'surface' doit être un nombre strictement supérieur à 0.");
  }

  if (
    prixMensuel === undefined ||
    typeof prixMensuel !== "number" ||
    isNaN(prixMensuel) ||
    prixMensuel < 0
  ) {
    errors.push("Le champ 'prixMensuel' doit être un nombre supérieur ou égal à 0.");
  }

  if (charges !== undefined) {
    if (typeof charges !== "number" || isNaN(charges) || charges < 0) {
      errors.push("Le champ 'charges' doit être un nombre supérieur ou égal à 0.");
    }
  }

  if (errors.length > 0) {
    throw new AppError("Données de création du bien invalides.", 400, errors);
  }

  next();
};

export const validateUpdateBien = (req: Request, _res: Response, next: NextFunction): void => {
  const { adresse, ville, type, surface, prixMensuel, charges, statut } = req.body || {};
  const errors: string[] = [];

  if (!adresse || typeof adresse !== "string" || adresse.trim() === "") {
    errors.push("Le champ 'adresse' est obligatoire et doit être une chaîne non vide.");
  }
  if (!ville || typeof ville !== "string" || ville.trim() === "") {
    errors.push("Le champ 'ville' est obligatoire et doit être une chaîne non vide.");
  }
  if (!type || typeof type !== "string" || type.trim() === "") {
    errors.push("Le champ 'type' est obligatoire et doit être une chaîne non vide.");
  }

  if (surface === undefined || typeof surface !== "number" || isNaN(surface) || surface <= 0) {
    errors.push("Le champ 'surface' doit être un nombre strictement supérieur à 0.");
  }

  if (
    prixMensuel === undefined ||
    typeof prixMensuel !== "number" ||
    isNaN(prixMensuel) ||
    prixMensuel < 0
  ) {
    errors.push("Le champ 'prixMensuel' doit être un nombre supérieur ou égal à 0.");
  }

  if (charges !== undefined) {
    if (typeof charges !== "number" || isNaN(charges) || charges < 0) {
      errors.push("Le champ 'charges' doit être un nombre supérieur ou égal à 0.");
    }
  }

  if (statut !== undefined && !(statut in StatutBien)) {
    errors.push("Le champ 'statut' doit valoir soit 'LIBRE', soit 'OCCUPE'.");
  }

  if (errors.length > 0) {
    throw new AppError("Données de mise à jour du bien invalides.", 400, errors);
  }

  next();
};

export const validatePatchStatut = (req: Request, _res: Response, next: NextFunction): void => {
  const { statut } = req.body || {};

  if (!statut || typeof statut !== "string" || !(statut in StatutBien)) {
    throw new AppError(
      "Le champ 'statut' est obligatoire et doit valoir soit 'LIBRE', soit 'OCCUPE'.",
      400
    );
  }

  next();
};

