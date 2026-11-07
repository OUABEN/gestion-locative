import {
  Bien,
  CreateBienDto,
  DisponibiliteResponse,
  FilterBiensDto,
  StatutBien,
  UpdateBienDto,
} from "../models/bien.model";
import { BienRepository } from "../repositories/bien.repository";
import { AppError } from "../utils/app-error";

export class BienService {
  constructor(private readonly bienRepository = new BienRepository()) {}

  async getAllBiens(filters: FilterBiensDto): Promise<Bien[]> {
    return this.bienRepository.findAll(filters);
  }

  async getBienById(id: string): Promise<Bien> {
    const bien = await this.bienRepository.findById(id);
    if (!bien) {
      throw new AppError(`Bien introuvable avec l'identifiant : ${id}`, 404);
    }
    return bien;
  }

  async createBien(dto: CreateBienDto): Promise<Bien> {
    return this.bienRepository.create(dto);
  }

  async updateBien(id: string, dto: UpdateBienDto): Promise<Bien> {
    const existing = await this.bienRepository.findById(id);
    if (!existing) {
      throw new AppError(`Bien introuvable avec l'identifiant : ${id}`, 404);
    }
    const updated = await this.bienRepository.update(id, dto);
    if (!updated) {
      throw new AppError(`Impossible de mettre à jour le bien : ${id}`, 500);
    }
    return updated;
  }

  /**
   * Mise à jour atomique du statut du bien.
   * Règle : un seul UPDATE conditionnel (statut <> nouveauStatut).
   * Si 0 ligne modifiée, on vérifie l'existence pour renvoyer 404 ou 409 (déjà dans ce statut).
   */
  async updateStatut(id: string, nouveauStatut: StatutBien): Promise<Bien> {
    const updated = await this.bienRepository.updateStatutAtomic(id, nouveauStatut);
    if (updated) {
      return updated;
    }

    // Soit le bien n'existe pas, soit il était déjà dans ce statut
    const current = await this.bienRepository.findById(id);
    if (!current) {
      throw new AppError(`Bien introuvable avec l'identifiant : ${id}`, 404);
    }
    if (current.statut === nouveauStatut) {
      throw new AppError(
        `Conflit : le bien ${id} est déjà dans le statut '${nouveauStatut}'.`,
        409
      );
    }

    throw new AppError(`Échec de la modification du statut du bien : ${id}`, 500);
  }

  async getDisponibilite(id: string): Promise<DisponibiliteResponse> {
    const bien = await this.getBienById(id);
    return {
      id: bien.id,
      disponible: bien.statut === StatutBien.LIBRE,
      statut: bien.statut,
    };
  }

  async deleteBien(id: string): Promise<void> {
    const existing = await this.bienRepository.findById(id);
    if (!existing) {
      throw new AppError(`Bien introuvable avec l'identifiant : ${id}`, 404);
    }

    if (existing.statut === StatutBien.OCCUPE) {
      throw new AppError(
        `Impossible de supprimer le bien ${id} : il est actuellement OCCUPE.`,
        409
      );
    }

    const deleted = await this.bienRepository.delete(id);
    if (!deleted) {
      throw new AppError(`Impossible de supprimer le bien : ${id}`, 500);
    }
  }
}

