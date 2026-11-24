export enum StatutBien {
  LIBRE = "LIBRE",
  OCCUPE = "OCCUPE",
}

export type UserRole = "BAILLEUR" | "GESTIONNAIRE" | "LOCATAIRE";

export interface JwtPayload {
  sub: string;
  role: UserRole;
  email?: string;
  iat?: number;
  exp?: number;
}

export interface Bien {
  versionContrat?: string;
  id: string;
  bailleurId?: string;
  adresse: string;
  ville: string;
  type: string;
  surface: number;
  prixMensuel: number;
  charges: number;
  statut: StatutBien;
  imageUrl?: string | null;
  createdAt: Date;
  updatedAt: Date;
}

export interface CreateBienDto {
  adresse: string;
  ville: string;
  type: string;
  surface: number;
  prixMensuel: number;
  charges?: number;
  imageUrl?: string | null;
  bailleurId?: string;
}

export interface UpdateBienDto {
  adresse: string;
  ville: string;
  type: string;
  surface: number;
  prixMensuel: number;
  charges?: number;
  statut?: StatutBien;
  imageUrl?: string | null;
}

export interface PatchStatutDto {
  statut: StatutBien;
}

export interface FilterBiensDto {
  ville?: string;
  statut?: StatutBien;
  type?: string;
}

export interface DisponibiliteResponse {
  id: string;
  disponible: boolean;
  statut: StatutBien;
}
