import fs from "fs";
import path from "path";
import crypto from "crypto";
import jwt from "jsonwebtoken";
import { Client } from "pg";
import request from "supertest";

// Configuration de l'environnement de test AVANT l'import de l'application Express
const TEST_JWT_SECRET = "integration_test_jwt_secret_key_32_characters_minimum";
const TEST_DB_NAME = "db_gestion_locative_test";
const PG_HOST = process.env.TEST_DB_HOST || "localhost";
const PG_PORT = Number(process.env.TEST_DB_PORT || "5432");
const PG_USER = process.env.TEST_DB_USER || "postgres";
const PG_PASSWORD = process.env.TEST_DB_PASSWORD || "1010";

process.env.NODE_ENV = "test";
process.env.JWT_SECRET = TEST_JWT_SECRET;
process.env.DATABASE_URL =
  process.env.TEST_DATABASE_URL ||
  `postgresql://${PG_USER}:${PG_PASSWORD}@${PG_HOST}:${PG_PORT}/${TEST_DB_NAME}`;

import { createApp } from "../../src/app";
import { pool } from "../../src/config/database";
import { UserRole } from "../../src/models/bien.model";

const app = createApp();

function signTestToken(
  role: UserRole,
  options?: { expiresIn?: jwt.SignOptions["expiresIn"]; sub?: string }
): string {
  return jwt.sign(
    {
      sub: options?.sub ?? "11111111-1111-1111-1111-111111111111",
      role,
      email: `${role.toLowerCase()}@test-integration.local`,
    },
    TEST_JWT_SECRET,
    { expiresIn: options?.expiresIn ?? "1h" }
  );
}

async function ensureTestDatabaseExists(): Promise<void> {
  if (process.env.TEST_DATABASE_URL) return;
  const adminClient = new Client({
    host: PG_HOST,
    port: PG_PORT,
    user: PG_USER,
    password: PG_PASSWORD,
    database: "postgres",
  });
  await adminClient.connect();
  try {
    const check = await adminClient.query(
      "SELECT 1 FROM pg_database WHERE datname = $1",
      [TEST_DB_NAME]
    );
    if (check.rowCount === 0) {
      await adminClient.query(`CREATE DATABASE ${TEST_DB_NAME}`);
    }
  } finally {
    await adminClient.end();
  }
}

describe("Tests d'Intégration — Microservice backend_gestion_biens (Express + PostgreSQL)", () => {
  const bailleurToken = signTestToken("BAILLEUR");
  const gestionnaireToken = signTestToken("GESTIONNAIRE");
  const locataireToken = signTestToken("LOCATAIRE");

  beforeAll(async () => {
    await ensureTestDatabaseExists();

    // Exécution en lecture seule des scripts SQL d'initialisation 01_users.sql et 02_biens.sql
    const initDir = path.resolve(__dirname, "../../../../database/init");
    const sqlUsers = fs.readFileSync(path.join(initDir, "01_users.sql"), "utf8");
    const sqlBiens = fs.readFileSync(path.join(initDir, "02_biens.sql"), "utf8");

    await pool.query(sqlUsers);
    await pool.query(sqlBiens);
  });

  beforeEach(async () => {
    // Nettoyage strict de la table biens de la base de test uniquement
    await pool.query("TRUNCATE TABLE biens RESTART IDENTITY CASCADE");
  });

  afterAll(async () => {
    await pool.end();
  });

  // =========================================================================
  // a) AUTHENTIFICATION, SÉCURITÉ JWT & CONTRÔLE DES RÔLES (RBAC)
  // =========================================================================
  describe("a) Authentification JWT, Rôles et Endpoint /api/auth", () => {
    it("refuse l'accès sans en-tête Authorization avec HTTP 401", async () => {
      // Arrange & Act
      const res = await request(app).get("/api/biens");

      // Assert
      expect(res.status).toBe(401);
      expect(res.body.message).toContain("manquant");
    });

    it("refuse un en-tête Authorization mal formé (sans préfixe Bearer) avec HTTP 401", async () => {
      const res = await request(app)
        .get("/api/biens")
        .set("Authorization", `Token ${bailleurToken}`);

      expect(res.status).toBe(401);
      expect(res.body.message).toContain("Format du jeton invalide");
    });

    it("refuse un jeton JWT signé avec une mauvaise clé ou expiré avec HTTP 401", async () => {
      // Arrange
      const wrongSecretToken = jwt.sign(
        { sub: "11111111-1111-1111-1111-111111111111", role: "BAILLEUR" },
        "wrong_secret_key_completely_different"
      );
      const expiredToken = signTestToken("BAILLEUR", { expiresIn: "-10s" });

      // Act
      const resWrong = await request(app)
        .get("/api/biens")
        .set("Authorization", `Bearer ${wrongSecretToken}`);
      const resExpired = await request(app)
        .get("/api/biens")
        .set("Authorization", `Bearer ${expiredToken}`);

      // Assert
      expect(resWrong.status).toBe(401);
      expect(resExpired.status).toBe(401);
    });

    it("interdit au rôle LOCATAIRE les opérations d'écriture (POST, PUT, PATCH, DELETE) avec HTTP 403", async () => {
      // Arrange : création préalable par un BAILLEUR
      const created = await request(app)
        .post("/api/biens")
        .set("Authorization", `Bearer ${bailleurToken}`)
        .send({
          adresse: "10 Rue Test RBAC",
          ville: "Casablanca",
          type: "Appartement",
          surface: 70,
          prixMensuel: 5500,
        });
      const bienId = created.body.id;

      // Act
      const resPost = await request(app)
        .post("/api/biens")
        .set("Authorization", `Bearer ${locataireToken}`)
        .send({
          adresse: "Tentative Locataire",
          ville: "Rabat",
          type: "Studio",
          surface: 30,
          prixMensuel: 3000,
        });

      const resPut = await request(app)
        .put(`/api/biens/${bienId}`)
        .set("Authorization", `Bearer ${locataireToken}`)
        .send({
          adresse: "Modif Locataire",
          ville: "Casablanca",
          type: "Appartement",
          surface: 75,
          prixMensuel: 6000,
        });

      const resPatch = await request(app)
        .patch(`/api/biens/${bienId}/statut`)
        .set("Authorization", `Bearer ${locataireToken}`)
        .send({ statut: "OCCUPE" });

      const resDelete = await request(app)
        .delete(`/api/biens/${bienId}`)
        .set("Authorization", `Bearer ${locataireToken}`);

      // Assert
      expect(resPost.status).toBe(403);
      expect(resPut.status).toBe(403);
      expect(resPatch.status).toBe(403);
      expect(resDelete.status).toBe(403);
    });

    it("autorise le rôle LOCATAIRE à consulter la liste, le détail et la disponibilité (HTTP 200)", async () => {
      // Arrange
      const created = await request(app)
        .post("/api/biens")
        .set("Authorization", `Bearer ${gestionnaireToken}`)
        .send({
          adresse: "22 Avenue Hassan II",
          ville: "Rabat",
          type: "Appartement",
          surface: 85,
          prixMensuel: 6800,
        });
      const bienId = created.body.id;

      // Act
      const resList = await request(app)
        .get("/api/biens")
        .set("Authorization", `Bearer ${locataireToken}`);
      const resDetail = await request(app)
        .get(`/api/biens/${bienId}`)
        .set("Authorization", `Bearer ${locataireToken}`);
      const resDispo = await request(app)
        .get(`/api/biens/${bienId}/disponibilite`)
        .set("Authorization", `Bearer ${locataireToken}`);

      // Assert
      expect(resList.status).toBe(200);
      expect(resList.body).toHaveLength(1);
      expect(resDetail.status).toBe(200);
      expect(resDetail.body.id).toBe(bienId);
      expect(resDispo.status).toBe(200);
      expect(resDispo.body.disponible).toBe(true);
    });

    it("valide le flux complet /api/auth/register et /api/auth/login sur la base de test", async () => {
      // Arrange : mot de passe généré aléatoirement en mémoire pour le test (aucun secret en dur)
      const dynamicSecretValue = crypto.randomBytes(12).toString("hex");
      const testEmail = `qa_${Date.now()}@test-integration.local`;

      // Act 1 : Inscription
      const regRes = await request(app)
        .post("/api/auth/register")
        .send({
          nom: "Testeur",
          prenom: "QA",
          email: testEmail,
          password: dynamicSecretValue,
          role: "BAILLEUR",
        });

      expect(regRes.status).toBe(201);
      expect(regRes.body.token).toBeDefined();
      expect(regRes.body.user.email).toBe(testEmail);

      // Act 2 : Connexion réussie
      const loginOk = await request(app)
        .post("/api/auth/login")
        .send({ email: testEmail, password: dynamicSecretValue });
      expect(loginOk.status).toBe(200);
      expect(loginOk.body.token).toBeDefined();

      // Act 3 : Mauvais mot de passe -> 401
      const loginBad = await request(app)
        .post("/api/auth/login")
        .send({ email: testEmail, password: `${dynamicSecretValue}_wrong` });
      expect(loginBad.status).toBe(401);

      // Act 4 : Champs manquants -> 400
      const loginMissing = await request(app)
        .post("/api/auth/login")
        .send({ email: "" });
      expect(loginMissing.status).toBe(400);
    });
  });

  // =========================================================================
  // b) OPÉRATIONS CRUD COMPLÈTES & FILTRES
  // =========================================================================
  describe("b) Cycle CRUD complet sur les Biens", () => {
    it("crée un bien (201) avec statut LIBRE, charges à 0 par défaut, UUID valide et imageUrl optionnelle", async () => {
      // Arrange
      const payload = {
        adresse: "45 Boulevard d'Anfa",
        ville: "Casablanca",
        type: "Villa",
        surface: 280.5,
        prixMensuel: 22000,
        imageUrl: "https://example.com/villa-anfa.jpg",
      };

      // Act
      const res = await request(app)
        .post("/api/biens")
        .set("Authorization", `Bearer ${bailleurToken}`)
        .send(payload);

      // Assert
      expect(res.status).toBe(201);
      expect(res.body.id).toMatch(
        /^[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i
      );
      expect(res.body.statut).toBe("LIBRE");
      expect(res.body.charges).toBe(0);
      expect(res.body.surface).toBe(280.5);
      expect(res.body.prixMensuel).toBe(22000);
      expect(res.body.imageUrl).toBe("https://example.com/villa-anfa.jpg");
    });

    it("liste les biens et applique correctement les filtres ville, statut et type", async () => {
      // Arrange
      const b1 = await request(app)
        .post("/api/biens")
        .set("Authorization", `Bearer ${bailleurToken}`)
        .send({
          adresse: "Bien 1 Casa",
          ville: "Casablanca",
          type: "Appartement",
          surface: 80,
          prixMensuel: 6000,
        });

      await request(app)
        .post("/api/biens")
        .set("Authorization", `Bearer ${bailleurToken}`)
        .send({
          adresse: "Bien 2 Rabat",
          ville: "Rabat",
          type: "Studio",
          surface: 40,
          prixMensuel: 3500,
        });

      // Passer b1 à OCCUPE
      await request(app)
        .patch(`/api/biens/${b1.body.id}/statut`)
        .set("Authorization", `Bearer ${bailleurToken}`)
        .send({ statut: "OCCUPE" });

      // Act & Assert : Filtre par ville
      const resVille = await request(app)
        .get("/api/biens?ville=Rabat")
        .set("Authorization", `Bearer ${bailleurToken}`);
      expect(resVille.status).toBe(200);
      expect(resVille.body).toHaveLength(1);
      expect(resVille.body[0].ville).toBe("Rabat");

      // Act & Assert : Filtre par statut
      const resStatut = await request(app)
        .get("/api/biens?statut=OCCUPE")
        .set("Authorization", `Bearer ${bailleurToken}`);
      expect(resStatut.status).toBe(200);
      expect(resStatut.body).toHaveLength(1);
      expect(resStatut.body[0].id).toBe(b1.body.id);

      // Act & Assert : Filtre par type
      const resType = await request(app)
        .get("/api/biens?type=Studio")
        .set("Authorization", `Bearer ${bailleurToken}`);
      expect(resType.status).toBe(200);
      expect(resType.body).toHaveLength(1);
      expect(resType.body[0].type).toBe("Studio");
    });

    it("consulte le détail (GET /:id), modifie complètement (PUT /:id) et supprime un bien LIBRE (DELETE /:id -> 204)", async () => {
      // Arrange : Création
      const created = await request(app)
        .post("/api/biens")
        .set("Authorization", `Bearer ${bailleurToken}`)
        .send({
          adresse: "8 Rue Majorelle",
          ville: "Marrakech",
          type: "Studio",
          surface: 45,
          prixMensuel: 4200,
          charges: 250,
        });
      const id = created.body.id;

      // Act 1 : Détail
      const detail = await request(app)
        .get(`/api/biens/${id}`)
        .set("Authorization", `Bearer ${bailleurToken}`);
      expect(detail.status).toBe(200);
      expect(detail.body.adresse).toBe("8 Rue Majorelle");

      // Act 2 : Modification PUT
      const updated = await request(app)
        .put(`/api/biens/${id}`)
        .set("Authorization", `Bearer ${bailleurToken}`)
        .send({
          adresse: "8 Rue Majorelle - Rénové",
          ville: "Marrakech",
          type: "Appartement",
          surface: 52,
          prixMensuel: 4900,
          charges: 300,
          statut: "LIBRE",
        });
      expect(updated.status).toBe(200);
      expect(updated.body.adresse).toBe("8 Rue Majorelle - Rénové");
      expect(updated.body.prixMensuel).toBe(4900);

      // Act 3 : Suppression d'un bien LIBRE
      const deleted = await request(app)
        .delete(`/api/biens/${id}`)
        .set("Authorization", `Bearer ${bailleurToken}`);
      expect(deleted.status).toBe(204);

      // Vérification 404 après suppression
      const afterDel = await request(app)
        .get(`/api/biens/${id}`)
        .set("Authorization", `Bearer ${bailleurToken}`);
      expect(afterDel.status).toBe(404);
    });
  });

  // =========================================================================
  // c) RÈGLES MÉTIER & GESTION DES CONFLITS (409, 400, 404)
  // =========================================================================
  describe("c) Règles métier : Conflits 409, Disponibilité, Validations 400 et 404", () => {
    it("refuse la suppression d'un bien OCCUPE avec HTTP 409 Conflict", async () => {
      // Arrange
      const created = await request(app)
        .post("/api/biens")
        .set("Authorization", `Bearer ${bailleurToken}`)
        .send({
          adresse: "Bien Occupé Non Supprimable",
          ville: "Tanger",
          type: "Appartement",
          surface: 95,
          prixMensuel: 7000,
        });
      const id = created.body.id;

      await request(app)
        .patch(`/api/biens/${id}/statut`)
        .set("Authorization", `Bearer ${bailleurToken}`)
        .send({ statut: "OCCUPE" });

      // Act
      const delRes = await request(app)
        .delete(`/api/biens/${id}`)
        .set("Authorization", `Bearer ${bailleurToken}`);

      // Assert
      expect(delRes.status).toBe(409);
      expect(delRes.body.message).toContain("OCCUPE");
    });

    it("renvoie HTTP 409 lors d'un second PATCH /statut identique (un seul bail actif par bien)", async () => {
      // Arrange
      const created = await request(app)
        .post("/api/biens")
        .set("Authorization", `Bearer ${bailleurToken}`)
        .send({
          adresse: "Bien Transition Atomique",
          ville: "Fès",
          type: "Bureau",
          surface: 60,
          prixMensuel: 4500,
        });
      const id = created.body.id;

      // Act 1 : LIBRE -> OCCUPE (200)
      const firstPatch = await request(app)
        .patch(`/api/biens/${id}/statut`)
        .set("Authorization", `Bearer ${bailleurToken}`)
        .send({ statut: "OCCUPE" });
      expect(firstPatch.status).toBe(200);
      expect(firstPatch.body.statut).toBe("OCCUPE");

      // Act 2 : OCCUPE -> OCCUPE (409)
      const secondPatch = await request(app)
        .patch(`/api/biens/${id}/statut`)
        .set("Authorization", `Bearer ${bailleurToken}`)
        .send({ statut: "OCCUPE" });
      expect(secondPatch.status).toBe(409);
      expect(secondPatch.body.message).toContain("Conflit");
    });

    it("refuse un PATCH /statut avec une valeur hors LIBRE/OCCUPE (HTTP 400)", async () => {
      const created = await request(app)
        .post("/api/biens")
        .set("Authorization", `Bearer ${bailleurToken}`)
        .send({
          adresse: "Bien Statut Invalide",
          ville: "Rabat",
          type: "Studio",
          surface: 35,
          prixMensuel: 3200,
        });

      const res = await request(app)
        .patch(`/api/biens/${created.body.id}/statut`)
        .set("Authorization", `Bearer ${bailleurToken}`)
        .send({ statut: "RESERVE" });

      expect(res.status).toBe(400);
    });

    it("retourne disponible=true si LIBRE, disponible=false si OCCUPE, et 404 si UUID inconnu", async () => {
      const created = await request(app)
        .post("/api/biens")
        .set("Authorization", `Bearer ${bailleurToken}`)
        .send({
          adresse: "Bien Test Dispo",
          ville: "Casablanca",
          type: "Studio",
          surface: 40,
          prixMensuel: 4000,
        });
      const id = created.body.id;

      // LIBRE -> true
      const dispo1 = await request(app)
        .get(`/api/biens/${id}/disponibilite`)
        .set("Authorization", `Bearer ${bailleurToken}`);
      expect(dispo1.status).toBe(200);
      expect(dispo1.body).toEqual({ id, disponible: true, statut: "LIBRE" });

      // Passage à OCCUPE -> false
      await request(app)
        .patch(`/api/biens/${id}/statut`)
        .set("Authorization", `Bearer ${bailleurToken}`)
        .send({ statut: "OCCUPE" });

      const dispo2 = await request(app)
        .get(`/api/biens/${id}/disponibilite`)
        .set("Authorization", `Bearer ${bailleurToken}`);
      expect(dispo2.status).toBe(200);
      expect(dispo2.body).toEqual({ id, disponible: false, statut: "OCCUPE" });

      // UUID inconnu -> 404
      const unknownId = "3fa85f64-5717-4562-b3fc-2c963f66afa6";
      const dispo404 = await request(app)
        .get(`/api/biens/${unknownId}/disponibilite`)
        .set("Authorization", `Bearer ${bailleurToken}`);
      expect(dispo404.status).toBe(404);
    });

    it("renvoie HTTP 400 pour un identifiant non UUID et HTTP 404 pour un UUID inexistant", async () => {
      const resBadUuid = await request(app)
        .get("/api/biens/not-a-valid-uuid")
        .set("Authorization", `Bearer ${bailleurToken}`);
      expect(resBadUuid.status).toBe(400);

      const unknownUuid = "3fa85f64-5717-4562-b3fc-2c963f66afa6";
      const resNotFound = await request(app)
        .get(`/api/biens/${unknownUuid}`)
        .set("Authorization", `Bearer ${bailleurToken}`);
      expect(resNotFound.status).toBe(404);
    });
  });

  // =========================================================================
  // d) CAS LIMITES (BOUNDARY VALUES)
  // =========================================================================
  describe("d) Cas limites : surface, prixMensuel, adresse et charges", () => {
    it("refuse surface = 0 avec HTTP 400 et détail dans errors", async () => {
      const res = await request(app)
        .post("/api/biens")
        .set("Authorization", `Bearer ${bailleurToken}`)
        .send({
          adresse: "Surface Nulle",
          ville: "Casablanca",
          type: "Studio",
          surface: 0,
          prixMensuel: 3000,
        });

      expect(res.status).toBe(400);
      expect(Array.isArray(res.body.errors)).toBe(true);
    });

    it("accepte surface = 0.01 m² et prixMensuel = 0 DH (HTTP 201)", async () => {
      const res = await request(app)
        .post("/api/biens")
        .set("Authorization", `Bearer ${bailleurToken}`)
        .send({
          adresse: "Logement de fonction / Cave",
          ville: "Rabat",
          type: "Cave",
          surface: 0.01,
          prixMensuel: 0,
        });

      expect(res.status).toBe(201);
      expect(res.body.surface).toBe(0.01);
      expect(res.body.prixMensuel).toBe(0);
    });

    it("refuse une adresse vide et des charges négatives avec HTTP 400", async () => {
      const res = await request(app)
        .post("/api/biens")
        .set("Authorization", `Bearer ${bailleurToken}`)
        .send({
          adresse: "   ",
          ville: "Tanger",
          type: "Appartement",
          surface: 60,
          prixMensuel: 5000,
          charges: -50,
        });

      expect(res.status).toBe(400);
      expect(res.body.errors.length).toBeGreaterThanOrEqual(2);
    });
  });

  // =========================================================================
  // e) HEALTHCHECK & ROUTES INCONNUES
  // =========================================================================
  describe("e) Endpoint /health et gestion 404 globale", () => {
    it("retourne HTTP 200 avec { service: 'biens', status: 'UP' } sur GET /health", async () => {
      const res = await request(app).get("/health");

      expect(res.status).toBe(200);
      expect(res.body).toEqual({ service: "biens", status: "UP" });
    });

    it("retourne HTTP 404 sur une route inexistante", async () => {
      const res = await request(app).get("/api/route-inconnue");
      expect(res.status).toBe(404);
    });
  });
});
