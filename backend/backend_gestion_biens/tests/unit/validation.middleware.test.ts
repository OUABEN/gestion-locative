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
      // Arrange
      mockReq.params = { id: "3fa85f64-5717-4562-b3fc-2c963f66afa6" };
      const middleware = validateUuidParam("id");

      // Act
      middleware(mockReq as Request, mockRes as Response, nextFn);

      // Assert
      expect(nextFn).toHaveBeenCalledTimes(1);
      expect(nextFn).toHaveBeenCalledWith();
    });

    it("[Cas limite] refuse un id au mauvais format (non UUID) avec une AppError 400", () => {
      // Arrange
      mockReq.params = { id: "invalid-uuid-12345" };
      const middleware = validateUuidParam("id");

      // Act & Assert
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
      // Arrange
      mockReq.body = {
        adresse: "10 Avenue Foch",
        ville: "Paris",
        type: "Appartement",
        surface: 55.0,
        prixMensuel: 1200.0,
        charges: 100.0,
      };

      // Act
      validateCreateBien(mockReq as Request, mockRes as Response, nextFn);

      // Assert
      expect(nextFn).toHaveBeenCalledTimes(1);
      expect(nextFn).toHaveBeenCalledWith();
    });

    it("[Cas limite] refuse surface = 0 avec AppError 400 (règle surface > 0)", () => {
      // Arrange
      mockReq.body = {
        adresse: "10 Avenue Foch",
        ville: "Paris",
        type: "Appartement",
        surface: 0,
        prixMensuel: 1200.0,
      };

      // Act & Assert
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
      // Arrange
      mockReq.body = {
        adresse: "   ",
        ville: "Marseille",
        type: "Studio",
        surface: 25.0,
        prixMensuel: 500.0,
      };

      // Act & Assert
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

    it("[Cas d'erreur] refuse un prixMensuel négatif avec AppError 400 (règle prixMensuel >= 0)", () => {
      // Arrange
      mockReq.body = {
        adresse: "12 rue Centrale",
        ville: "Toulouse",
        type: "T2",
        surface: 40.0,
        prixMensuel: -150.0,
      };

      // Act & Assert
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
      // Arrange
      mockReq.body = {
        adresse: "12 rue Centrale",
        ville: "Toulouse",
        type: "T2",
        surface: 40.0,
        prixMensuel: 600.0,
        charges: -20.0,
      };

      // Act & Assert
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
  // 3. VALIDATION DU CHANGEMENT DE STATUT (validatePatchStatut)
  // =========================================================================
  describe("validatePatchStatut", () => {
    it("[Cas normal] accepte les statuts valides 'LIBRE' et 'OCCUPE'", () => {
      // Test LIBRE
      mockReq.body = { statut: "LIBRE" };
      validatePatchStatut(mockReq as Request, mockRes as Response, nextFn);
      expect(nextFn).toHaveBeenCalledWith();

      // Test OCCUPE
      mockReq.body = { statut: "OCCUPE" };
      validatePatchStatut(mockReq as Request, mockRes as Response, nextFn);
      expect(nextFn).toHaveBeenCalledTimes(2);
    });

    it("[Cas d'erreur] refuse un statut autre que LIBRE ou OCCUPE avec AppError 400", () => {
      // Arrange
      mockReq.body = { statut: "EN_TRAVAUX" };

      // Act & Assert
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
  });
});

