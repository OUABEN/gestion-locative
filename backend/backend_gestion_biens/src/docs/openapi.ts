export const openapiSpec = {
  openapi: "3.0.3",
  info: {
    title: "API Gestion des Biens",
    description: "Microservice de gestion des biens immobiliers (Node.js 20, TypeScript, Express, PostgreSQL).",
    version: "1.0.0",
  },
  servers: [
    { url: "http://localhost:8081", description: "Serveur local" },
    { url: "/api/biens", description: "Via reverse-proxy nginx frontend" },
  ],
  components: {
    securitySchemes: {
      BearerAuth: {
        type: "http",
        scheme: "bearer",
        bearerFormat: "JWT",
        description: "Jeton JWT contenant les champs { sub, role } avec role = BAILLEUR | GESTIONNAIRE | LOCATAIRE.",
      },
    },
    schemas: {
      Bien: {
        type: "object",
        required: [
          "id",
          "adresse",
          "ville",
          "type",
          "surface",
          "prixMensuel",
          "charges",
          "statut",
          "createdAt",
          "updatedAt",
        ],
        properties: {
          id: { type: "string", format: "uuid", example: "3fa85f64-5717-4562-b3fc-2c963f66afa6" },
          adresse: { type: "string", example: "12 rue de la Paix" },
          ville: { type: "string", example: "Paris" },
          type: { type: "string", example: "Appartement T3" },
          surface: { type: "number", example: 65.5 },
          prixMensuel: { type: "number", example: 950.0 },
          charges: { type: "number", example: 80.0 },
          statut: { type: "string", enum: ["LIBRE", "OCCUPE"], example: "LIBRE" },
          createdAt: { type: "string", format: "date-time" },
          updatedAt: { type: "string", format: "date-time" },
        },
      },
      CreateBienDto: {
        type: "object",
        required: ["adresse", "ville", "type", "surface", "prixMensuel"],
        properties: {
          adresse: { type: "string", example: "12 rue de la Paix" },
          ville: { type: "string", example: "Paris" },
          type: { type: "string", example: "Appartement T3" },
          surface: { type: "number", description: "Strictement supérieur à 0", example: 65.5 },
          prixMensuel: { type: "number", description: "Supérieur ou égal à 0", example: 950.0 },
          charges: { type: "number", default: 0, description: "Supérieur ou égal à 0", example: 80.0 },
        },
      },
      UpdateBienDto: {
        type: "object",
        required: ["adresse", "ville", "type", "surface", "prixMensuel"],
        properties: {
          adresse: { type: "string" },
          ville: { type: "string" },
          type: { type: "string" },
          surface: { type: "number" },
          prixMensuel: { type: "number" },
          charges: { type: "number" },
          statut: { type: "string", enum: ["LIBRE", "OCCUPE"] },
        },
      },
      PatchStatutDto: {
        type: "object",
        required: ["statut"],
        properties: {
          statut: { type: "string", enum: ["LIBRE", "OCCUPE"], example: "OCCUPE" },
        },
      },
      DisponibiliteResponse: {
        type: "object",
        required: ["id", "disponible", "statut"],
        properties: {
          id: { type: "string", format: "uuid" },
          disponible: { type: "boolean", example: true },
          statut: { type: "string", enum: ["LIBRE", "OCCUPE"], example: "LIBRE" },
        },
      },
      ErrorResponse: {
        type: "object",
        required: ["message"],
        properties: {
          message: { type: "string", example: "Description de l'erreur en français" },
          errors: {
            type: "array",
            items: { type: "string" },
            example: ["Le champ 'surface' doit être un nombre strictement supérieur à 0."],
          },
        },
      },
    },
    responses: {
      "400Error": {
        description: "Requête invalide",
        content: { "application/json": { schema: { $ref: "#/components/schemas/ErrorResponse" } } },
      },
      "401Error": {
        description: "Non authentifié",
        content: { "application/json": { schema: { $ref: "#/components/schemas/ErrorResponse" } } },
      },
      "403Error": {
        description: "Accès refusé",
        content: { "application/json": { schema: { $ref: "#/components/schemas/ErrorResponse" } } },
      },
      "404Error": {
        description: "Bien introuvable",
        content: { "application/json": { schema: { $ref: "#/components/schemas/ErrorResponse" } } },
      },
      "409Error": {
        description: "Conflit",
        content: { "application/json": { schema: { $ref: "#/components/schemas/ErrorResponse" } } },
      },
    },
  },
  paths: {
    "/health": {
      get: {
        summary: "Healthcheck du service",
        security: [],
        responses: {
          "200": {
            description: "Service opérationnel",
            content: {
              "application/json": {
                schema: {
                  type: "object",
                  properties: { service: { type: "string", example: "biens" }, status: { type: "string", example: "UP" } },
                },
              },
            },
          },
        },
      },
    },
    "/api/biens": {
      get: {
        summary: "Liste filtrée des biens",
        security: [{ BearerAuth: [] }],
        parameters: [
          { in: "query", name: "ville", schema: { type: "string" }, description: "Filtre insensible à la casse" },
          { in: "query", name: "statut", schema: { type: "string", enum: ["LIBRE", "OCCUPE"] } },
          { in: "query", name: "type", schema: { type: "string" } },
        ],
        responses: {
          "200": {
            description: "Liste des biens",
            content: { "application/json": { schema: { type: "array", items: { $ref: "#/components/schemas/Bien" } } } },
          },
          "401": { $ref: "#/components/responses/401Error" },
        },
      },
      post: {
        summary: "Création d'un bien immobilier (statut initial LIBRE)",
        description: "Réservé aux rôles BAILLEUR et GESTIONNAIRE.",
        security: [{ BearerAuth: [] }],
        requestBody: {
          required: true,
          content: { "application/json": { schema: { $ref: "#/components/schemas/CreateBienDto" } } },
        },
        responses: {
          "201": {
            description: "Bien créé avec succès",
            content: { "application/json": { schema: { $ref: "#/components/schemas/Bien" } } },
          },
          "400": { $ref: "#/components/responses/400Error" },
          "401": { $ref: "#/components/responses/401Error" },
          "403": { $ref: "#/components/responses/403Error" },
        },
      },
    },
    "/api/biens/{id}": {
      get: {
        summary: "Détail d'un bien",
        security: [{ BearerAuth: [] }],
        parameters: [{ in: "path", name: "id", required: true, schema: { type: "string", format: "uuid" } }],
        responses: {
          "200": {
            description: "Détail du bien",
            content: { "application/json": { schema: { $ref: "#/components/schemas/Bien" } } },
          },
          "400": { $ref: "#/components/responses/400Error" },
          "401": { $ref: "#/components/responses/401Error" },
          "404": { $ref: "#/components/responses/404Error" },
        },
      },
      put: {
        summary: "Modification complète d'un bien",
        description: "Réservé aux rôles BAILLEUR et GESTIONNAIRE.",
        security: [{ BearerAuth: [] }],
        parameters: [{ in: "path", name: "id", required: true, schema: { type: "string", format: "uuid" } }],
        requestBody: {
          required: true,
          content: { "application/json": { schema: { $ref: "#/components/schemas/UpdateBienDto" } } },
        },
        responses: {
          "200": {
            description: "Bien mis à jour",
            content: { "application/json": { schema: { $ref: "#/components/schemas/Bien" } } },
          },
          "400": { $ref: "#/components/responses/400Error" },
          "401": { $ref: "#/components/responses/401Error" },
          "403": { $ref: "#/components/responses/403Error" },
          "404": { $ref: "#/components/responses/404Error" },
        },
      },
      delete: {
        summary: "Suppression d'un bien",
        description: "Réservé aux rôles BAILLEUR et GESTIONNAIRE. Échoue avec 409 si le bien est OCCUPE.",
        security: [{ BearerAuth: [] }],
        parameters: [{ in: "path", name: "id", required: true, schema: { type: "string", format: "uuid" } }],
        responses: {
          "204": { description: "Bien supprimé avec succès" },
          "400": { $ref: "#/components/responses/400Error" },
          "401": { $ref: "#/components/responses/401Error" },
          "403": { $ref: "#/components/responses/403Error" },
          "404": { $ref: "#/components/responses/404Error" },
          "409": { $ref: "#/components/responses/409Error" },
        },
      },
    },
    "/api/biens/{id}/disponibilite": {
      get: {
        summary: "Vérification de disponibilité d'un bien",
        security: [{ BearerAuth: [] }],
        parameters: [{ in: "path", name: "id", required: true, schema: { type: "string", format: "uuid" } }],
        responses: {
          "200": {
            description: "Disponibilité du bien",
            content: { "application/json": { schema: { $ref: "#/components/schemas/DisponibiliteResponse" } } },
          },
          "400": { $ref: "#/components/responses/400Error" },
          "401": { $ref: "#/components/responses/401Error" },
          "404": { $ref: "#/components/responses/404Error" },
        },
      },
    },
    "/api/biens/{id}/statut": {
      patch: {
        summary: "Modification atomique du statut du bien",
        description: "Réservé aux rôles BAILLEUR et GESTIONNAIRE. Échoue avec 409 si déjà dans le statut demandé.",
        security: [{ BearerAuth: [] }],
        parameters: [{ in: "path", name: "id", required: true, schema: { type: "string", format: "uuid" } }],
        requestBody: {
          required: true,
          content: { "application/json": { schema: { $ref: "#/components/schemas/PatchStatutDto" } } },
        },
        responses: {
          "200": {
            description: "Statut mis à jour",
            content: { "application/json": { schema: { $ref: "#/components/schemas/Bien" } } },
          },
          "400": { $ref: "#/components/responses/400Error" },
          "401": { $ref: "#/components/responses/401Error" },
          "403": { $ref: "#/components/responses/403Error" },
          "404": { $ref: "#/components/responses/404Error" },
          "409": { $ref: "#/components/responses/409Error" },
        },
      },
    },
  },
};

