const swaggerSpec = {
  openapi: '3.0.0',
  info: {
    title: 'Module 02 - Gestion des Locataires & Baux',
    version: '1.0.0',
    description: "API REST du microservice Gestion des Locataires et Baux (Ahmed LAARIF) - Master d'Excellence Web Services. Port 8082.",
    contact: {
      name: 'Ahmed LAARIF',
      email: 'ahmed.laarif@gestion-locative.ma'
    }
  },
  servers: [
    {
      url: 'http://localhost:8082',
      description: 'Serveur local direct'
    },
    {
      url: 'http://localhost:3000/api/locataires',
      description: 'Reverse proxy frontend / Nginx'
    }
  ],
  components: {
    securitySchemes: {
      bearerAuth: {
        type: 'http',
        scheme: 'bearer',
        bearerFormat: 'JWT',
        description: 'Jeton JWT partagé émis par backend_gestion_biens (sub, role).'
      }
    },
    schemas: {
      Locataire: {
        type: 'object',
        properties: {
          id: { type: 'string', format: 'uuid', example: 'a1111111-1111-1111-1111-111111111111' },
          nom: { type: 'string', example: 'El Amrani' },
          prenom: { type: 'string', example: 'Yassine' },
          cin: { type: 'string', example: 'AB123456' },
          email: { type: 'string', format: 'email', example: 'yassine@mail.ma' },
          telephone: { type: 'string', example: '0661001122' },
          piecesJustificatives: { type: 'array', items: { type: 'string' } },
          createdAt: { type: 'string', format: 'date-time' },
          updatedAt: { type: 'string', format: 'date-time' }
        }
      },
      Bail: {
        type: 'object',
        properties: {
          id: { type: 'string', format: 'uuid', example: 'b1111111-1111-1111-1111-111111111111' },
          bienId: { type: 'string', format: 'uuid', example: 'fe365469-6d12-4311-922a-eb6f3a0820fe' },
          locataireId: { type: 'string', format: 'uuid', example: 'a1111111-1111-1111-1111-111111111111' },
          dateDebut: { type: 'string', format: 'date', example: '2026-01-01' },
          dateFin: { type: 'string', format: 'date', example: '2026-12-31' },
          montantLoyer: { type: 'number', example: 3400 },
          depotGarantie: { type: 'number', example: 6800 },
          statut: { type: 'string', enum: ['ACTIF', 'TERMINE'], example: 'ACTIF' }
        }
      },
      ErrorResponse: {
        type: 'object',
        properties: {
          status: { type: 'integer', example: 400 },
          error: { type: 'string', example: 'Bad Request' },
          message: { type: 'string', example: 'Description explicite de l\'erreur' }
        }
      }
    }
  },
  security: [
    {
      bearerAuth: []
    }
  ],
  paths: {
    '/health': {
      get: {
        summary: 'Vérification de l\'état de santé du service',
        security: [],
        responses: {
          200: { description: 'Service opérationnel' }
        }
      }
    },
    '/api/locataires': {
      get: {
        summary: 'Lister les locataires (filtrage automatique selon le rôle)',
        responses: {
          200: { description: 'Liste des fiches locataires' },
          401: { description: 'Non authentifié' }
        }
      },
      post: {
        summary: 'Créer une fiche locataire (contrôle d\'unicité absolue du CIN)',
        responses: {
          201: { description: 'Locataire créé' },
          409: { description: 'Fiche refusée : CIN déjà utilisé' }
        }
      }
    },
    '/api/locataires/{id}': {
      get: {
        summary: 'Consulter un locataire par identifiant UUID',
        parameters: [{ name: 'id', in: 'path', required: true, schema: { type: 'string' } }],
        responses: {
          200: { description: 'Fiche locataire' },
          403: { description: 'Accès refusé' },
          404: { description: 'Locataire introuvable' }
        }
      },
      put: {
        summary: 'Modifier les coordonnées d\'un locataire',
        parameters: [{ name: 'id', in: 'path', required: true, schema: { type: 'string' } }],
        responses: {
          200: { description: 'Fiche mise à jour' },
          404: { description: 'Locataire introuvable' }
        }
      },
      delete: {
        summary: 'Supprimer un locataire',
        parameters: [{ name: 'id', in: 'path', required: true, schema: { type: 'string' } }],
        responses: {
          200: { description: 'Locataire supprimé' },
          404: { description: 'Locataire introuvable' }
        }
      }
    },
    '/api/baux': {
      get: {
        summary: 'Lister les baux (filtrage par statut=ACTIF, locataireId, bienId)',
        parameters: [
          { name: 'statut', in: 'query', schema: { type: 'string' } },
          { name: 'locataireId', in: 'query', schema: { type: 'string' } }
        ],
        responses: {
          200: { description: 'Liste des baux' }
        }
      },
      post: {
        summary: 'Créer un contrat de bail (contrôle de disponibilité auprès de Biens)',
        responses: {
          201: { description: 'Bail créé' },
          409: { description: 'Bien déjà loué' }
        }
      }
    },
    '/api/baux/{id}': {
      get: {
        summary: 'Lecture d\'un bail par ID (utilisé par Loyers et Quittances)',
        parameters: [{ name: 'id', in: 'path', required: true, schema: { type: 'string' } }],
        responses: {
          200: { description: 'Détail du bail (BailDto)' },
          404: { description: 'Bail introuvable' }
        }
      }
    }
  }
};

module.exports = swaggerSpec;