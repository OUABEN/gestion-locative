// Définir des variables d'environnement de test avant tout import
process.env.JWT_SECRET = "test_jwt_secret_for_unit_tests_32_chars_long";
process.env.DATABASE_URL = "postgresql://mock:mock@localhost:5432/mock_db";

import { BienService } from "../../src/services/bien.service";
import { BienRepository } from "../../src/repositories/bien.repository";
import {
  Bien,
  CreateBienDto,
  StatutBien,
  UpdateBienDto,
} from "../../src/models/bien.model";
import { AppError } from "../../src/utils/app-error";

describe("BienService - Tests Unitaires", () => {
  let mockBienRepository: jest.Mocked<BienRepository>;
  let bienService: BienService;

  const sampleBien: Bien = {
    id: "3fa85f64-5717-4562-b3fc-2c963f66afa6",
    adresse: "12 rue de la Paix",
    ville: "Paris",
    type: "T3",
    surface: 65.5,
    prixMensuel: 950.0,
    charges: 80.0,
    statut: StatutBien.LIBRE,
    createdAt: new Date("2026-01-01T10:00:00Z"),
    updatedAt: new Date("2026-01-01T10:00:00Z"),
  };

  beforeEach(() => {
    jest.clearAllMocks();
    mockBienRepository = {
      findAll: jest.fn(),
      findById: jest.fn(),
      create: jest.fn(),
      update: jest.fn(),
      updateStatutAtomic: jest.fn(),
      delete: jest.fn(),
    } as unknown as jest.Mocked<BienRepository>;

    bienService = new BienService(mockBienRepository);
  });

  // =========================================================================
  // 1. CRÉATION D'UN BIEN (createBien)
  // =========================================================================
  describe("createBien", () => {
    it("[Cas normal] crée un bien valide avec statut LIBRE et charges par défaut", async () => {
      // Arrange
      const dto: CreateBienDto = {
        adresse: "15 rue Victor Hugo",
        ville: "Lyon",
        type: "T2",
        surface: 45.0,
        prixMensuel: 650.0,
      };
      const createdEntity: Bien = {
        ...sampleBien,
        ...dto,
        charges: 0,
        statut: StatutBien.LIBRE,
      };
      mockBienRepository.create.mockResolvedValueOnce(createdEntity);

      // Act
      const result = await bienService.createBien(dto);

      // Assert
      expect(result).toEqual(createdEntity);
      expect(result.statut).toBe(StatutBien.LIBRE);
      expect(result.charges).toBe(0);
      expect(mockBienRepository.create).toHaveBeenCalledTimes(1);
      expect(mockBienRepository.create).toHaveBeenCalledWith(dto);
    });

    it("[Cas limite] accepte une surface très petite strictement positive (0.01 m²)", async () => {
      // Arrange
      const dto: CreateBienDto = {
        adresse: "Cave 4B",
        ville: "Paris",
        type: "Cave",
        surface: 0.01,
        prixMensuel: 50.0,
      };
      const createdEntity: Bien = {
        ...sampleBien,
        ...dto,
        charges: 0,
      };
      mockBienRepository.create.mockResolvedValueOnce(createdEntity);

      // Act
      const result = await bienService.createBien(dto);

      // Assert
      expect(result.surface).toBe(0.01);
      expect(mockBienRepository.create).toHaveBeenCalledTimes(1);
      expect(mockBienRepository.create).toHaveBeenCalledWith(dto);
    });

    it("[Cas limite] accepte un prix mensuel nul (0 €)", async () => {
      // Arrange
      const dto: CreateBienDto = {
        adresse: "Logement de fonction",
        ville: "Bordeaux",
        type: "Studio",
        surface: 20.0,
        prixMensuel: 0,
      };
      const createdEntity: Bien = {
        ...sampleBien,
        ...dto,
        charges: 0,
      };
      mockBienRepository.create.mockResolvedValueOnce(createdEntity);

      // Act
      const result = await bienService.createBien(dto);

      // Assert
      expect(result.prixMensuel).toBe(0);
      expect(mockBienRepository.create).toHaveBeenCalledWith(dto);
    });

    it("[Cas limite] charges absentes sont transmises et persistées par défaut à 0", async () => {
      // Arrange
      const dto: CreateBienDto = {
        adresse: "Rue sans charges",
        ville: "Lille",
        type: "T1",
        surface: 25.0,
        prixMensuel: 400.0,
      };
      mockBienRepository.create.mockImplementationOnce(async (input) => ({
        ...sampleBien,
        ...input,
        charges: input.charges ?? 0,
      }));

      // Act
      const result = await bienService.createBien(dto);

      // Assert
      expect(result.charges).toBe(0);
      expect(mockBienRepository.create).toHaveBeenCalledWith(dto);
    });
  });

  // =========================================================================
  // 2. RÉCUPÉRATION PAR ID (getBienById)
  // =========================================================================
  describe("getBienById", () => {
    it("[Cas normal] retourne le bien correspondant à un identifiant existant", async () => {
      // Arrange
      const id = "3fa85f64-5717-4562-b3fc-2c963f66afa6";
      mockBienRepository.findById.mockResolvedValueOnce(sampleBien);

      // Act
      const result = await bienService.getBienById(id);

      // Assert
      expect(result).toEqual(sampleBien);
      expect(mockBienRepository.findById).toHaveBeenCalledTimes(1);
      expect(mockBienRepository.findById).toHaveBeenCalledWith(id);
    });

    it("[Cas d'erreur] lève une AppError 404 lorsque le bien n'existe pas", async () => {
      // Arrange
      const idInexistant = "00000000-0000-0000-0000-000000000000";
      mockBienRepository.findById.mockResolvedValueOnce(null);

      // Act & Assert
      await expect(bienService.getBienById(idInexistant)).rejects.toThrow(AppError);

      try {
        await bienService.getBienById(idInexistant);
      } catch (err: unknown) {
        const appErr = err as AppError;
        expect(appErr.statusCode).toBe(404);
        expect(appErr.message).toContain("Bien introuvable");
        expect(appErr.message).toContain(idInexistant);
      }
      expect(mockBienRepository.findById).toHaveBeenCalledWith(idInexistant);
    });
  });

  // =========================================================================
  // 3. DISPONIBILITÉ (getDisponibilite)
  // =========================================================================
  describe("getDisponibilite", () => {
    it("[Cas normal] retourne disponible=true lorsque le statut est LIBRE", async () => {
      // Arrange
      const id = sampleBien.id;
      mockBienRepository.findById.mockResolvedValueOnce({
        ...sampleBien,
        statut: StatutBien.LIBRE,
      });

      // Act
      const result = await bienService.getDisponibilite(id);

      // Assert
      expect(result).toEqual({
        id,
        disponible: true,
        statut: StatutBien.LIBRE,
      });
      expect(mockBienRepository.findById).toHaveBeenCalledWith(id);
    });

    it("[Cas normal] retourne disponible=false lorsque le statut est OCCUPE", async () => {
      // Arrange
      const id = sampleBien.id;
      mockBienRepository.findById.mockResolvedValueOnce({
        ...sampleBien,
        statut: StatutBien.OCCUPE,
      });

      // Act
      const result = await bienService.getDisponibilite(id);

      // Assert
      expect(result).toEqual({
        id,
        disponible: false,
        statut: StatutBien.OCCUPE,
      });
      expect(mockBienRepository.findById).toHaveBeenCalledWith(id);
    });

    it("[Cas d'erreur] propage une AppError 404 si le bien est introuvable", async () => {
      // Arrange
      const id = "00000000-0000-0000-0000-000000000000";
      mockBienRepository.findById.mockResolvedValueOnce(null);

      // Act & Assert
      await expect(bienService.getDisponibilite(id)).rejects.toThrow(AppError);
      await expect(bienService.getDisponibilite(id)).rejects.toMatchObject({
        statusCode: 404,
      });
    });
  });

  // =========================================================================
  // 4. MODIFICATION DU STATUT (updateStatut)
  // =========================================================================
  describe("updateStatut", () => {
    it("[Cas normal] met à jour atomiquement le statut vers OCCUPE quand il était LIBRE", async () => {
      // Arrange
      const id = sampleBien.id;
      const updatedEntity: Bien = {
        ...sampleBien,
        statut: StatutBien.OCCUPE,
      };
      mockBienRepository.updateStatutAtomic.mockResolvedValueOnce(updatedEntity);

      // Act
      const result = await bienService.updateStatut(id, StatutBien.OCCUPE);

      // Assert
      expect(result.statut).toBe(StatutBien.OCCUPE);
      expect(mockBienRepository.updateStatutAtomic).toHaveBeenCalledTimes(1);
      expect(mockBienRepository.updateStatutAtomic).toHaveBeenCalledWith(id, StatutBien.OCCUPE);
      expect(mockBienRepository.findById).not.toHaveBeenCalled();
    });

    it("[Cas normal] met à jour atomiquement le statut vers LIBRE quand il était OCCUPE", async () => {
      // Arrange
      const id = sampleBien.id;
      const updatedEntity: Bien = {
        ...sampleBien,
        statut: StatutBien.LIBRE,
      };
      mockBienRepository.updateStatutAtomic.mockResolvedValueOnce(updatedEntity);

      // Act
      const result = await bienService.updateStatut(id, StatutBien.LIBRE);

      // Assert
      expect(result.statut).toBe(StatutBien.LIBRE);
      expect(mockBienRepository.updateStatutAtomic).toHaveBeenCalledWith(id, StatutBien.LIBRE);
    });

    it("[Cas d'erreur] lève une AppError 409 de conflit si le bien est déjà dans le statut demandé", async () => {
      // Arrange
      const id = sampleBien.id;
      // updateStatutAtomic renvoie null car statut == nouveauStatut
      mockBienRepository.updateStatutAtomic.mockResolvedValueOnce(null);
      // findById confirme que le bien existe et qu'il est déjà OCCUPE
      mockBienRepository.findById.mockResolvedValueOnce({
        ...sampleBien,
        statut: StatutBien.OCCUPE,
      });

      // Act & Assert
      await expect(bienService.updateStatut(id, StatutBien.OCCUPE)).rejects.toThrow(AppError);

      try {
        mockBienRepository.updateStatutAtomic.mockResolvedValueOnce(null);
        mockBienRepository.findById.mockResolvedValueOnce({
          ...sampleBien,
          statut: StatutBien.OCCUPE,
        });
        await bienService.updateStatut(id, StatutBien.OCCUPE);
      } catch (err: unknown) {
        const appErr = err as AppError;
        expect(appErr.statusCode).toBe(409);
        expect(appErr.message).toContain("Conflit");
        expect(appErr.message).toContain("déjà dans le statut 'OCCUPE'");
      }
    });

    it("[Cas d'erreur] lève une AppError 404 si le bien est introuvable lors de l'update de statut", async () => {
      // Arrange
      const id = "00000000-0000-0000-0000-000000000000";
      mockBienRepository.updateStatutAtomic.mockResolvedValueOnce(null);
      mockBienRepository.findById.mockResolvedValueOnce(null);

      // Act & Assert
      await expect(bienService.updateStatut(id, StatutBien.OCCUPE)).rejects.toThrow(AppError);

      try {
        mockBienRepository.updateStatutAtomic.mockResolvedValueOnce(null);
        mockBienRepository.findById.mockResolvedValueOnce(null);
        await bienService.updateStatut(id, StatutBien.OCCUPE);
      } catch (err: unknown) {
        const appErr = err as AppError;
        expect(appErr.statusCode).toBe(404);
        expect(appErr.message).toContain("Bien introuvable");
      }
    });
  });

  // =========================================================================
  // 5. SUPPRESSION D'UN BIEN (deleteBien)
  // =========================================================================
  describe("deleteBien", () => {
    it("[Cas normal] supprime avec succès un bien avec statut LIBRE", async () => {
      // Arrange
      const id = sampleBien.id;
      mockBienRepository.findById.mockResolvedValueOnce({
        ...sampleBien,
        statut: StatutBien.LIBRE,
      });
      mockBienRepository.delete.mockResolvedValueOnce(true);

      // Act
      await bienService.deleteBien(id);

      // Assert
      expect(mockBienRepository.findById).toHaveBeenCalledWith(id);
      expect(mockBienRepository.delete).toHaveBeenCalledWith(id);
      expect(mockBienRepository.delete).toHaveBeenCalledTimes(1);
    });

    it("[Cas d'erreur] lève une AppError 409 si le bien est OCCUPE et refuse la suppression", async () => {
      // Arrange
      const id = sampleBien.id;
      mockBienRepository.findById.mockResolvedValueOnce({
        ...sampleBien,
        statut: StatutBien.OCCUPE,
      });

      // Act & Assert
      await expect(bienService.deleteBien(id)).rejects.toThrow(AppError);

      try {
        mockBienRepository.findById.mockResolvedValueOnce({
          ...sampleBien,
          statut: StatutBien.OCCUPE,
        });
        await bienService.deleteBien(id);
      } catch (err: unknown) {
        const appErr = err as AppError;
        expect(appErr.statusCode).toBe(409);
        expect(appErr.message).toContain("Impossible de supprimer");
        expect(appErr.message).toContain("OCCUPE");
      }

      // La suppression SQL ne doit JAMAIS avoir été appelée
      expect(mockBienRepository.delete).not.toHaveBeenCalled();
    });

    it("[Cas d'erreur] lève une AppError 404 si le bien à supprimer n'existe pas", async () => {
      // Arrange
      const id = "00000000-0000-0000-0000-000000000000";
      mockBienRepository.findById.mockResolvedValueOnce(null);

      // Act & Assert
      await expect(bienService.deleteBien(id)).rejects.toThrow(AppError);

      try {
        mockBienRepository.findById.mockResolvedValueOnce(null);
        await bienService.deleteBien(id);
      } catch (err: unknown) {
        const appErr = err as AppError;
        expect(appErr.statusCode).toBe(404);
        expect(appErr.message).toContain("Bien introuvable");
      }

      expect(mockBienRepository.delete).not.toHaveBeenCalled();
    });
  });

  // =========================================================================
  // 6. MODIFICATION COMPLÈTE (updateBien) & LISTE (getAllBiens)
  // =========================================================================
  describe("updateBien", () => {
    it("[Cas normal] modifie un bien existant et retourne l'entité mise à jour", async () => {
      // Arrange
      const id = sampleBien.id;
      const updateDto: UpdateBienDto = {
        adresse: "12 rue de la Paix - Rénové",
        ville: "Paris",
        type: "T3 Bis",
        surface: 70.0,
        prixMensuel: 1050.0,
        charges: 90.0,
      };
      const updatedEntity: Bien = {
        ...sampleBien,
        ...updateDto,
        charges: updateDto.charges ?? 0,
      };
      mockBienRepository.findById.mockResolvedValueOnce(sampleBien);
      mockBienRepository.update.mockResolvedValueOnce(updatedEntity);

      // Act
      const result = await bienService.updateBien(id, updateDto);

      // Assert
      expect(result).toEqual(updatedEntity);
      expect(mockBienRepository.findById).toHaveBeenCalledWith(id);
      expect(mockBienRepository.update).toHaveBeenCalledWith(id, updateDto);
    });

    it("[Cas d'erreur] lève une AppError 404 si le bien à modifier n'existe pas", async () => {
      // Arrange
      const id = "00000000-0000-0000-0000-000000000000";
      const updateDto: UpdateBienDto = {
        adresse: "Inexistant",
        ville: "Paris",
        type: "T1",
        surface: 20,
        prixMensuel: 500,
      };
      mockBienRepository.findById.mockResolvedValueOnce(null);

      // Act & Assert
      await expect(bienService.updateBien(id, updateDto)).rejects.toThrow(AppError);
      await expect(bienService.updateBien(id, updateDto)).rejects.toMatchObject({
        statusCode: 404,
      });
      expect(mockBienRepository.update).not.toHaveBeenCalled();
    });
  });

  describe("getAllBiens", () => {
    it("[Cas normal] retourne la liste des biens correspondant aux filtres", async () => {
      // Arrange
      const filters = { ville: "Paris", statut: StatutBien.LIBRE };
      mockBienRepository.findAll.mockResolvedValueOnce([sampleBien]);

      // Act
      const result = await bienService.getAllBiens(filters);

      // Assert
      expect(result).toHaveLength(1);
      expect(result[0]).toEqual(sampleBien);
      expect(mockBienRepository.findAll).toHaveBeenCalledWith(filters);
    });
  });
});
