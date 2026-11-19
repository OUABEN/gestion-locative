import { pool } from "../config/database";
import { Bien, CreateBienDto, FilterBiensDto, StatutBien, UpdateBienDto } from "../models/bien.model";

interface BienRow {
  id: string;
  adresse: string;
  ville: string;
  type: string;
  surface: string | number;
  prix_mensuel: string | number;
  charges: string | number;
  statut: StatutBien;
  image_url?: string | null;
  created_at: Date;
  updated_at: Date;
}

let imageColumnReady = false;

async function ensureImageColumn(): Promise<void> {
  if (imageColumnReady) return;
  try {
    await pool.query("ALTER TABLE biens ADD COLUMN IF NOT EXISTS image_url TEXT");
    imageColumnReady = true;
  } catch {
    // Ignore if table doesn't exist yet
  }
}

export class BienRepository {
  private mapRowToEntity(row: BienRow): Bien {
    return {
      id: row.id,
      adresse: row.adresse,
      ville: row.ville,
      type: row.type,
      surface: Number(row.surface),
      prixMensuel: Number(row.prix_mensuel),
      charges: Number(row.charges),
      statut: row.statut,
      ...(row.image_url ? { imageUrl: row.image_url } : {}),
      createdAt: row.created_at,
      updatedAt: row.updated_at,
    };
  }

  async findAll(filters: FilterBiensDto): Promise<Bien[]> {
    await ensureImageColumn();
    const conditions: string[] = [];
    const values: unknown[] = [];

    if (filters.ville) {
      values.push(filters.ville.trim());
      conditions.push(`LOWER(ville) = LOWER($${values.length})`);
    }

    if (filters.statut) {
      values.push(filters.statut);
      conditions.push(`statut = $${values.length}`);
    }

    if (filters.type) {
      values.push(filters.type.trim());
      conditions.push(`LOWER(type) = LOWER($${values.length})`);
    }

    const whereClause = conditions.length > 0 ? `WHERE ${conditions.join(" AND ")}` : "";
    const query = `
      SELECT id, adresse, ville, type, surface, prix_mensuel, charges, statut, image_url, created_at, updated_at
      FROM biens
      ${whereClause}
      ORDER BY created_at DESC
    `;

    const res = await pool.query<BienRow>(query, values);
    return res.rows.map((row) => this.mapRowToEntity(row));
  }

  async findById(id: string): Promise<Bien | null> {
    await ensureImageColumn();
    const query = `
      SELECT id, adresse, ville, type, surface, prix_mensuel, charges, statut, image_url, created_at, updated_at
      FROM biens
      WHERE id = $1
    `;
    const res = await pool.query<BienRow>(query, [id]);
    if (res.rows.length === 0) {
      return null;
    }
    return this.mapRowToEntity(res.rows[0]);
  }

  async create(dto: CreateBienDto): Promise<Bien> {
    await ensureImageColumn();
    const charges = dto.charges ?? 0;
    const imageUrl = dto.imageUrl ?? null;
    const query = `
      INSERT INTO biens (adresse, ville, type, surface, prix_mensuel, charges, statut, image_url)
      VALUES ($1, $2, $3, $4, $5, $6, $7, $8)
      RETURNING id, adresse, ville, type, surface, prix_mensuel, charges, statut, image_url, created_at, updated_at
    `;
    const values = [
      dto.adresse,
      dto.ville,
      dto.type,
      dto.surface,
      dto.prixMensuel,
      charges,
      StatutBien.LIBRE,
      imageUrl,
    ];
    const res = await pool.query<BienRow>(query, values);
    return this.mapRowToEntity(res.rows[0]);
  }

  async update(id: string, dto: UpdateBienDto): Promise<Bien | null> {
    await ensureImageColumn();
    const charges = dto.charges ?? 0;
    const statut = dto.statut ?? StatutBien.LIBRE;
    const imageUrl = dto.imageUrl !== undefined ? dto.imageUrl : null;
    const query = `
      UPDATE biens
      SET adresse = $1,
          ville = $2,
          type = $3,
          surface = $4,
          prix_mensuel = $5,
          charges = $6,
          statut = $7,
          image_url = COALESCE($8, image_url),
          updated_at = CURRENT_TIMESTAMP
      WHERE id = $9
      RETURNING id, adresse, ville, type, surface, prix_mensuel, charges, statut, image_url, created_at, updated_at
    `;
    const values = [
      dto.adresse,
      dto.ville,
      dto.type,
      dto.surface,
      dto.prixMensuel,
      charges,
      statut,
      imageUrl,
      id,
    ];
    const res = await pool.query<BienRow>(query, values);
    if (res.rows.length === 0) {
      return null;
    }
    return this.mapRowToEntity(res.rows[0]);
  }

  /**
   * Mise à jour atomique du statut :
   * L'UPDATE réussit uniquement si le bien existe ET que son statut actuel est différent du nouveau.
   */
  async updateStatutAtomic(id: string, nouveauStatut: StatutBien): Promise<Bien | null> {
    await ensureImageColumn();
    const query = `
      UPDATE biens
      SET statut = $1,
          updated_at = CURRENT_TIMESTAMP
      WHERE id = $2 AND statut <> $1
      RETURNING id, adresse, ville, type, surface, prix_mensuel, charges, statut, image_url, created_at, updated_at
    `;
    const res = await pool.query<BienRow>(query, [nouveauStatut, id]);
    if (res.rows.length === 0) {
      return null;
    }
    return this.mapRowToEntity(res.rows[0]);
  }

  async delete(id: string): Promise<boolean> {
    const query = `DELETE FROM biens WHERE id = $1`;
    const res = await pool.query(query, [id]);
    return (res.rowCount ?? 0) > 0;
  }
}
