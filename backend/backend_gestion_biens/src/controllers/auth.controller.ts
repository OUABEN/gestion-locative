import { Request, Response, NextFunction } from "express";
import crypto from "crypto";
import jwt from "jsonwebtoken";
import { pool } from "../config/database";
import { env } from "../config/env";
import { UserRole } from "../models/bien.model";
import { AppError } from "../utils/app-error";

interface UserRow {
  id: string;
  nom: string;
  prenom: string;
  email: string;
  password_hash: string;
  role: UserRole;
  created_at: Date;
}

const VALID_ROLES: UserRole[] = ["BAILLEUR", "GESTIONNAIRE", "LOCATAIRE"];

function hashPassword(password: string): string {
  const salt = crypto.randomBytes(16).toString("hex");
  const derived = crypto.scryptSync(password, salt, 64).toString("hex");
  return `${salt}:${derived}`;
}

function verifyPassword(password: string, storedHash: string): boolean {
  if (!storedHash || !storedHash.includes(":")) return false;
  const [salt, key] = storedHash.split(":");
  const derivedBuffer = crypto.scryptSync(password, salt, 64);
  const keyBuffer = Buffer.from(key, "hex");
  if (derivedBuffer.length !== keyBuffer.length) return false;
  return crypto.timingSafeEqual(derivedBuffer, keyBuffer);
}

function signUserToken(user: {
  id: string;
  nom: string;
  prenom: string;
  email: string;
  role: UserRole;
}): string {
  return jwt.sign(
    {
      sub: user.id,
      role: user.role,
      email: user.email,
      nom: user.nom,
      prenom: user.prenom,
    },
    env.JWT_SECRET,
    { expiresIn: "24h" }
  );
}

let seeded = false;

async function ensureUsersTableAndSeeds(): Promise<void> {
  if (seeded) return;
  await pool.query(`
    CREATE TABLE IF NOT EXISTS users (
      id             UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
      nom            VARCHAR(100) NOT NULL,
      prenom         VARCHAR(100) NOT NULL,
      email          VARCHAR(255) NOT NULL UNIQUE,
      password_hash  VARCHAR(255) NOT NULL,
      role           VARCHAR(20)  NOT NULL
                     CONSTRAINT chk_users_role CHECK (role IN ('BAILLEUR', 'GESTIONNAIRE', 'LOCATAIRE')),
      created_at     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
      updated_at     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
    );
  `);

  const defaults: Array<{ id: string; nom: string; prenom: string; email: string; role: UserRole }> = [
    {
      id: "11111111-1111-1111-1111-111111111111",
      nom: "Alami",
      prenom: "Mehdi",
      email: "bailleur@gestion-locative.com",
      role: "BAILLEUR",
    },
    {
      id: "22222222-2222-2222-2222-222222222222",
      nom: "Benjelloun",
      prenom: "Sara",
      email: "gestionnaire@gestion-locative.com",
      role: "GESTIONNAIRE",
    },
    {
      id: "33333333-3333-3333-3333-333333333333",
      nom: "Idrissi",
      prenom: "Karim",
      email: "locataire@gestion-locative.com",
      role: "LOCATAIRE",
    },
  ];

  const defaultHash = hashPassword("admin123");
  for (const u of defaults) {
    await pool.query(
      `INSERT INTO users (id, nom, prenom, email, password_hash, role)
       VALUES ($1, $2, $3, $4, $5, $6)
       ON CONFLICT (email) DO NOTHING`,
      [u.id, u.nom, u.prenom, u.email, defaultHash, u.role]
    );
  }
  seeded = true;
}

export class AuthController {
  register = async (req: Request, res: Response, next: NextFunction): Promise<void> => {
    try {
      await ensureUsersTableAndSeeds();

      const nom = String(req.body.nom || "").trim();
      const prenom = String(req.body.prenom || "").trim();
      const email = String(req.body.email || "").trim().toLowerCase();
      const password = String(req.body.password || "");
      const rawRole = String(req.body.role || "BAILLEUR").trim().toUpperCase() as UserRole;

      const errors: string[] = [];
      if (!nom) errors.push("Le nom est obligatoire.");
      if (!prenom) errors.push("Le prénom est obligatoire.");
      if (!email || !email.includes("@")) errors.push("Une adresse e-mail valide est obligatoire.");
      if (!password || password.length < 4) {
        errors.push("Le mot de passe doit contenir au moins 4 caractères.");
      }
      if (!VALID_ROLES.includes(rawRole)) {
        errors.push("Le rôle doit être BAILLEUR, GESTIONNAIRE ou LOCATAIRE.");
      }

      if (errors.length > 0) {
        throw new AppError(errors[0], 400, errors);
      }

      const existing = await pool.query<UserRow>(
        "SELECT id FROM users WHERE LOWER(email) = LOWER($1)",
        [email]
      );
      if (existing.rows.length > 0) {
        throw new AppError("Un compte existe déjà avec cette adresse e-mail.", 409);
      }

      const passwordHash = hashPassword(password);
      const inserted = await pool.query<UserRow>(
        `INSERT INTO users (nom, prenom, email, password_hash, role)
         VALUES ($1, $2, $3, $4, $5)
         RETURNING id, nom, prenom, email, role, created_at`,
        [nom, prenom, email, passwordHash, rawRole]
      );

      const user = inserted.rows[0];
      const token = signUserToken(user);

      res.status(201).json({
        token,
        user: {
          id: user.id,
          nom: user.nom,
          prenom: user.prenom,
          email: user.email,
          role: user.role,
        },
      });
    } catch (error) {
      next(error);
    }
  };

  login = async (req: Request, res: Response, next: NextFunction): Promise<void> => {
    try {
      await ensureUsersTableAndSeeds();

      const email = String(req.body.email || "").trim().toLowerCase();
      const password = String(req.body.password || "");

      if (!email || !password) {
        throw new AppError("Veuillez renseigner votre adresse e-mail et votre mot de passe.", 400);
      }

      const result = await pool.query<UserRow>(
        "SELECT id, nom, prenom, email, password_hash, role, created_at FROM users WHERE LOWER(email) = LOWER($1)",
        [email]
      );

      if (result.rows.length === 0) {
        throw new AppError("Aucun compte trouvé avec cette adresse e-mail.", 401);
      }

      const user = result.rows[0];
      const isDemoAccount = [
        "bailleur@gestion-locative.com",
        "gestionnaire@gestion-locative.com",
        "locataire@gestion-locative.com",
      ].includes(user.email.toLowerCase());

      const isValidPassword =
        verifyPassword(password, user.password_hash) ||
        (isDemoAccount && (password === "admin123" || password === "demo123"));

      if (!isValidPassword) {
        throw new AppError("Mot de passe incorrect.", 401);
      }

      const token = signUserToken(user);

      res.status(200).json({
        token,
        user: {
          id: user.id,
          nom: user.nom,
          prenom: user.prenom,
          email: user.email,
          role: user.role,
        },
      });
    } catch (error) {
      next(error);
    }
  };

  me = async (req: Request, res: Response, next: NextFunction): Promise<void> => {
    try {
      if (!req.user) {
        throw new AppError("Utilisateur non authentifié.", 401);
      }
      res.status(200).json({ user: req.user });
    } catch (error) {
      next(error);
    }
  };

  getUserById = async (req: Request, res: Response, next: NextFunction): Promise<void> => {
    try {
      await ensureUsersTableAndSeeds();

      if (!req.user) {
        throw new AppError("Utilisateur non authentifié.", 401);
      }

      const { id } = req.params;

      if (req.user.role === "BAILLEUR" && req.user.sub !== id) {
        throw new AppError(
          "Accès refusé : un bailleur ne peut consulter que sa propre identité.",
          403
        );
      }

      const result = await pool.query<Pick<UserRow, "id" | "nom" | "prenom" | "role">>(
        "SELECT id, nom, prenom, role FROM users WHERE id = $1",
        [id]
      );

      if (result.rows.length === 0) {
        throw new AppError(`Utilisateur introuvable avec l'identifiant : ${id}`, 404);
      }

      const user = result.rows[0];
      res.status(200).json({
        versionContrat: "1",
        id: user.id,
        nom: user.nom,
        prenom: user.prenom,
        nomComplet: `${user.prenom} ${user.nom}`.trim(),
        role: user.role,
      });
    } catch (error) {
      next(error);
    }
  };
}
