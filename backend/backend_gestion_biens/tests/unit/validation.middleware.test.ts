import { Request, Response, NextFunction } from "express";
import {
  validateCreateBien,
  validatePatchStatut,
  validateUpdateBien,
  validateUuidParam,
} from "../../src/middlewares/validation.middleware";
import { AppError } from "../../src/utils/app-error";

describe("ValidationMiddleware - Tests Unitaires (Règles Métier)", () => {
  let mockReq: Partial<Request>;
  let mockRes: Partial<Response>;
  let nextFn: jest.MockedFunction<NextFunction>;

  beforeEach(() => {
    mockReq = {
      body: {},
      params: {},
    };
    mockRes = {};
    nextFn = jest.fn();
  });

  // =========================================================================
  // 1. VALIDATION DE L'ID (UUID)
  // =========================================================================
  describe("validateUuidParam", () => {
    it("[Cas normal] accepte un UUID v4 valide et appelle next()", () => {
      mockReq.params = { id: "3fa85f64-5717-4562-b3fc-2c963f66afa6" };
      const middleware = validateUuidParam();

      middleware(mockReq as Request, mockRes as Response, nextFn);

      expect(nextFn).toHaveBeenCalledTimes(1);
      expect(nextFn).toHaveBeenCalledWith();
    });

    it("[Cas limite] refuse un id au mauvais format (non UUID) avec une AppError 400", () => {
      mockReq.params = { id: "invalid-uuid-12345" };
      const middleware = validateUuidParam("id");

      expect(() => {
        middleware(mockReq as Request, mockRes as Response, nextFn);
      }).toThrow(AppError);

      try {
        middleware(mockReq as Request, mockRes as Response, nextFn);
      } catch (err: unknown) {
        const appErr = err as AppError;
        expect(appErr.statusCode).toBe(400);
        expect(appErr.message).toContain("n'est pas un UUID valide");
      }
      expect(nextFn).not.toHaveBeenCalled();
    });
  });

  // =========================================================================
  // 2. VALIDATION CRÉATION D'UN BIEN (validateCreateBien)
  // =========================================================================
  describe("validateCreateBien", () => {
    it("[Cas normal] valide un payload complet et correct", () => {
      mockReq.body = {
        adresse: "10 Avenue Foch",
        ville: "Paris",
        type: "Appartement",
        surface: 55.0,
        prixMensuel: 1200.0,
        charges: 100.0,
      };

      validateCreateBien(mockReq as Request, mockRes as Response, nextFn);

      expect(nextFn).toHaveBeenCalledTimes(1);
      expect(nextFn).toHaveBeenCalledWith();
    });

    it("[Cas limite] refuse surface = 0 avec AppError 400 (règle surface > 0)", () => {
      mockReq.body = {
        adresse: "10 Avenue Foch",
        ville: "Paris",
        type: "Appartement",
        surface: 0,
        prixMensuel: 1200.0,
      };

      expect(() => {
        validateCreateBien(mockReq as Request, mockRes as Response, nextFn);
      }).toThrow(AppError);

      try {
        validateCreateBien(mockReq as Request, mockRes as Response, nextFn);
      } catch (err: unknown) {
        const appErr = err as AppError;
        expect(appErr.statusCode).toBe(400);
        expect(appErr.errors).toContain(
          "Le champ 'surface' doit être un nombre strictement supérieur à 0."
        );
      }
    });

    it("[Cas limite] refuse une adresse vide ou composée uniquement d'espaces avec AppError 400", () => {
      mockReq.body = {
        adresse: "   ",
        ville: "Marseille",
        type: "Studio",
        surface: 25.0,
        prixMensuel: 500.0,
      };

      expect(() => {
        validateCreateBien(mockReq as Request, mockRes as Response, nextFn);
      }).toThrow(AppError);

      try {
        validateCreateBien(mockReq as Request, mockRes as Response, nextFn);
      } catch (err: unknown) {
        const appErr = err as AppError;
        expect(appErr.statusCode).toBe(400);
        expect(appErr.errors).toContain(
          "Le champ 'adresse' est obligatoire et doit être une chaîne non vide."
        );
      }
    });

    it("[Cas d'erreur] refuse une ville vide et un type manquant avec AppError 400", () => {
      mockReq.body = {
        adresse: "10 Avenue Mohammed V",
        ville: "   ",
        type: "",
        surface: 50,
        prixMensuel: 4000,
      };

      try {
        validateCreateBien(mockReq as Request, mockRes as Response, nextFn);
      } catch (err: unknown) {
        const appErr = err as AppError;
        expect(appErr.statusCode).toBe(400);
        expect(appErr.errors).toContain(
          "Le champ 'ville' est obligatoire et doit être une chaîne non vide."
        );
        expect(appErr.errors).toContain(
          "Le champ 'type' est obligatoire et doit être une chaîne non vide."
        );
      }
      expect(nextFn).not.toHaveBeenCalled();
    });

    it("[Cas d'erreur] refuse un corps de requête undefined avec toutes les erreurs de champs obligatoires", () => {
      mockReq.body = undefined;

      expect(() => {
        validateCreateBien(mockReq as Request, mockRes as Response, nextFn);
      }).toThrow(AppError);
      expect(nextFn).not.toHaveBeenCalled();
    });

    it("[Cas d'erreur] refuse un prixMensuel négatif avec AppError 400 (règle prixMensuel >= 0)", () => {
      mockReq.body = {
        adresse: "12 rue Centrale",
        ville: "Toulouse",
        type: "T2",
        surface: 40.0,
        prixMensuel: -150.0,
      };

      expect(() => {
        validateCreateBien(mockReq as Request, mockRes as Response, nextFn);
      }).toThrow(AppError);

      try {
        validateCreateBien(mockReq as Request, mockRes as Response, nextFn);
      } catch (err: unknown) {
        const appErr = err as AppError;
        expect(appErr.statusCode).toBe(400);
        expect(appErr.errors).toContain(
          "Le champ 'prixMensuel' doit être un nombre supérieur ou égal à 0."
        );
      }
    });

    it("[Cas d'erreur] refuse des charges négatives avec AppError 400 (règle charges >= 0)", () => {
      mockReq.body = {
        adresse: "12 rue Centrale",
        ville: "Toulouse",
        type: "T2",
        surface: 40.0,
        prixMensuel: 600.0,
        charges: -20.0,
      };

      expect(() => {
        validateCreateBien(mockReq as Request, mockRes as Response, nextFn);
      }).toThrow(AppError);

      try {
        validateCreateBien(mockReq as Request, mockRes as Response, nextFn);
      } catch (err: unknown) {
        const appErr = err as AppError;
        expect(appErr.statusCode).toBe(400);
        expect(appErr.errors).toContain(
          "Le champ 'charges' doit être un nombre supérieur ou égal à 0."
        );
      }
    });
  });

  // =========================================================================
  // 3. VALIDATION MISE À JOUR COMPLÈTE (validateUpdateBien)
  // =========================================================================
  describe("validateUpdateBien", () => {
    it("[Cas normal] valide une mise à jour complète avec statut LIBRE ou OCCUPE", () => {
      mockReq.body = {
        adresse: "14 Boulevard Anfa",
        ville: "Casablanca",
        type: "Appartement",
        surface: 90,
        prixMensuel: 7500,
        charges: 500,
        statut: "OCCUPE",
      };

      validateUpdateBien(mockReq as Request, mockRes as Response, nextFn);

      expect(nextFn).toHaveBeenCalledTimes(1);
      expect(nextFn).toHaveBeenCalledWith();
    });

    it("[Cas d'erreur] cumule les erreurs lorsque adresse, ville, type, surface, prixMensuel, charges et statut sont invalides", () => {
      mockReq.body = {
        adresse: "",
        ville: "  ",
        type: 123,
        surface: -10,
        prixMensuel: -5,
        charges: -1,
        statut: "ARCHIVED",
      };

      try {
        validateUpdateBien(mockReq as Request, mockRes as Response, nextFn);
      } catch (err: unknown) {
        const appErr = err as AppError;
        expect(appErr.statusCode).toBe(400);
        expect(appErr.message).toBe("Données de mise à jour du bien invalides.");
        expect(appErr.errors).toHaveLength(7);
      }
      expect(nextFn).not.toHaveBeenCalled();
    });

    it("[Cas d'erreur] refuse un corps undefined sur validateUpdateBien", () => {
      mockReq.body = undefined;

      expect(() => {
        validateUpdateBien(mockReq as Request, mockRes as Response, nextFn);
      }).toThrow(AppError);
      expect(nextFn).not.toHaveBeenCalled();
    });
  });

  // =========================================================================
  // 4. VALIDATION DU CHANGEMENT DE STATUT (validatePatchStatut)
  // =========================================================================
  describe("validatePatchStatut", () => {
    it("[Cas normal] accepte les statuts valides 'LIBRE' et 'OCCUPE'", () => {
      mockReq.body = { statut: "LIBRE" };
      validatePatchStatut(mockReq as Request, mockRes as Response, nextFn);
      expect(nextFn).toHaveBeenCalledWith();

      mockReq.body = { statut: "OCCUPE" };
      validatePatchStatut(mockReq as Request, mockRes as Response, nextFn);
      expect(nextFn).toHaveBeenCalledTimes(2);
    });

    it("[Cas d'erreur] refuse un statut autre que LIBRE ou OCCUPE avec AppError 400", () => {
      mockReq.body = { statut: "EN_TRAVAUX" };

      expect(() => {
        validatePatchStatut(mockReq as Request, mockRes as Response, nextFn);
      }).toThrow(AppError);

      try {
        validatePatchStatut(mockReq as Request, mockRes as Response, nextFn);
      } catch (err: unknown) {
        const appErr = err as AppError;
        expect(appErr.statusCode).toBe(400);
        expect(appErr.message).toContain("soit 'LIBRE', soit 'OCCUPE'");
      }
    });

    it("[Cas d'erreur] refuse un body undefined sur validatePatchStatut", () => {
      mockReq.body = undefined;

      expect(() => {
        validatePatchStatut(mockReq as Request, mockRes as Response, nextFn);
      }).toThrow(AppError);
    });
  });
});
