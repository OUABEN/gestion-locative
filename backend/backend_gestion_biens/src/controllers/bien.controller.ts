import { Request, Response, NextFunction } from "express";
import { BienService } from "../services/bien.service";
import { CreateBienDto, FilterBiensDto, StatutBien, UpdateBienDto } from "../models/bien.model";

export class BienController {
  constructor(private readonly bienService = new BienService()) {}

  getAll = async (req: Request, res: Response, next: NextFunction): Promise<void> => {
    try {
      const filters: FilterBiensDto = {
        ville: req.query.ville ? String(req.query.ville) : undefined,
        statut: req.query.statut ? (req.query.statut as StatutBien) : undefined,
        type: req.query.type ? String(req.query.type) : undefined,
      };

      const biens = await this.bienService.getAllBiens(filters);
      res.status(200).json(biens);
    } catch (error) {
      next(error);
    }
  };

  getById = async (req: Request, res: Response, next: NextFunction): Promise<void> => {
    try {
      const { id } = req.params;
      const bien = await this.bienService.getBienById(id);
      res.status(200).json(bien);
    } catch (error) {
      next(error);
    }
  };

  create = async (req: Request, res: Response, next: NextFunction): Promise<void> => {
    try {
      const dto: CreateBienDto = {
        adresse: req.body.adresse,
        ville: req.body.ville,
        type: req.body.type,
        surface: Number(req.body.surface),
        prixMensuel: Number(req.body.prixMensuel),
        charges: req.body.charges !== undefined ? Number(req.body.charges) : 0,
      };

      const created = await this.bienService.createBien(dto);
      res.status(201).json(created);
    } catch (error) {
      next(error);
    }
  };

  update = async (req: Request, res: Response, next: NextFunction): Promise<void> => {
    try {
      const { id } = req.params;
      const dto: UpdateBienDto = {
        adresse: req.body.adresse,
        ville: req.body.ville,
        type: req.body.type,
        surface: Number(req.body.surface),
        prixMensuel: Number(req.body.prixMensuel),
        charges: req.body.charges !== undefined ? Number(req.body.charges) : 0,
        statut: req.body.statut,
      };

      const updated = await this.bienService.updateBien(id, dto);
      res.status(200).json(updated);
    } catch (error) {
      next(error);
    }
  };

  updateStatut = async (req: Request, res: Response, next: NextFunction): Promise<void> => {
    try {
      const { id } = req.params;
      const { statut } = req.body;
      const updated = await this.bienService.updateStatut(id, statut as StatutBien);
      res.status(200).json(updated);
    } catch (error) {
      next(error);
    }
  };

  getDisponibilite = async (req: Request, res: Response, next: NextFunction): Promise<void> => {
    try {
      const { id } = req.params;
      const disponibilite = await this.bienService.getDisponibilite(id);
      res.status(200).json(disponibilite);
    } catch (error) {
      next(error);
    }
  };

  delete = async (req: Request, res: Response, next: NextFunction): Promise<void> => {
    try {
      const { id } = req.params;
      await this.bienService.deleteBien(id);
      res.status(204).send();
    } catch (error) {
      next(error);
    }
  };
}

