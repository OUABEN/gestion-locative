const LocataireController = require('../src/controllers/locataire.controller');
const BailController = require('../src/controllers/bail.controller');
const LocataireService = require('../src/services/locataire.service');
const BailService = require('../src/services/bail.service');
const locataireRoutes = require('../src/routes/locataire.routes');
const bailRoutes = require('../src/routes/bail.routes');
const app = require('../src/app');

describe("Tests Unitaires & Routes - Locataires & Baux (Ahmed LAARIF)", () => {
  let locController;
  let locService;
  let bailController;
  let bailService;
  let req;
  let res;

  beforeEach(() => {
    locService = new LocataireService(true);
    locController = new LocataireController(locService);

    bailService = new BailService();
    bailController = new BailController(bailService);

    req = {
      params: {},
      query: {},
      body: {},
      headers: {},
      user: null
    };

    res = {
      status: jest.fn().mockReturnThis(),
      json: jest.fn().mockReturnThis()
    };
  });

  describe('1. Endpoint GET /api/locataires/me', () => {
    test('UT-ME-01 : Rejet 401 si aucun utilisateur authentifié dans req.user', () => {
      req.user = null;
      locController.getMe(req, res);
      expect(res.status).toHaveBeenCalledWith(401);
      expect(res.json).toHaveBeenCalledWith(expect.objectContaining({
        status: 401,
        error: 'Unauthorized'
      }));
    });

    test('UT-ME-02 : Rejet 404 si le jeton n\'a aucun claim email', () => {
      req.user = { sub: 'u-1', role: 'LOCATAIRE' };
      locController.getMe(req, res);
      expect(res.status).toHaveBeenCalledWith(404);
      expect(res.json).toHaveBeenCalledWith(expect.objectContaining({
        status: 404,
        error: 'Not Found'
      }));
    });

    test('UT-ME-03 : Rejet 404 si l\'email ne correspond à aucune fiche locataire', () => {
      req.user = { sub: 'u-1', role: 'LOCATAIRE', email: 'introuvable@mail.ma' };
      locController.getMe(req, res);
      expect(res.status).toHaveBeenCalledWith(404);
      expect(res.json).toHaveBeenCalledWith(expect.objectContaining({
        status: 404,
        error: 'Not Found',
        message: expect.stringContaining('introuvable@mail.ma')
      }));
    });

    test('UT-ME-04 : Succès 200 pour le rôle LOCATAIRE avec email correspondant (yassine@mail.ma)', () => {
      req.user = { sub: 'u-1', role: 'LOCATAIRE', email: 'yassine@mail.ma' };
      locController.getMe(req, res);
      expect(res.status).toHaveBeenCalledWith(200);
      expect(res.json).toHaveBeenCalledWith(expect.objectContaining({
        nom: 'El Amrani',
        prenom: 'Yassine',
        cin: 'AB123456',
        email: 'yassine@mail.ma'
      }));
    });

    test('UT-ME-05 : Succès 200 insensible à la casse de l\'email', () => {
      req.user = { sub: 'u-1', role: 'LOCATAIRE', email: 'YASSINE@MAIL.MA' };
      locController.getMe(req, res);
      expect(res.status).toHaveBeenCalledWith(200);
      expect(res.json).toHaveBeenCalledWith(expect.objectContaining({
        cin: 'AB123456'
      }));
    });
  });

  describe('2. Endpoint GET /api/locataires/:id (Contrôle RBAC et inter-services)', () => {
    const existingId = 'a1111111-1111-1111-1111-111111111111';
    const otherId = 'a2222222-2222-2222-2222-222222222222';

    test('UT-ID-01 : Succès 200 pour BAILLEUR (appels inter-services Loyers/Quittances)', () => {
      req.user = { sub: 'u-bailleur', role: 'BAILLEUR', email: 'bailleur@test.ma' };
      req.params = { id: existingId };

      locController.getById(req, res);
      expect(res.status).toHaveBeenCalledWith(200);
      expect(res.json).toHaveBeenCalledWith(expect.objectContaining({
        id: existingId,
        nom: 'El Amrani'
      }));
    });

    test('UT-ID-02 : Succès 200 pour GESTIONNAIRE (appels inter-services)', () => {
      req.user = { sub: 'u-gest', role: 'GESTIONNAIRE', email: 'gest@test.ma' };
      req.params = { id: existingId };

      locController.getById(req, res);
      expect(res.status).toHaveBeenCalledWith(200);
      expect(res.json).toHaveBeenCalledWith(expect.objectContaining({
        id: existingId
      }));
    });

    test('UT-ID-03 : Succès 200 pour LOCATAIRE consultant sa propre fiche (sub match)', () => {
      req.user = { sub: existingId, role: 'LOCATAIRE', email: 'yassine@mail.ma' };
      req.params = { id: existingId };

      locController.getById(req, res);
      expect(res.status).toHaveBeenCalledWith(200);
      expect(res.json).toHaveBeenCalledWith(expect.objectContaining({
        id: existingId
      }));
    });

    test('UT-ID-04 : Rejet 403 pour LOCATAIRE consultant la fiche d\'un autre locataire', () => {
      req.user = { sub: existingId, role: 'LOCATAIRE', email: 'yassine@mail.ma' };
      req.params = { id: otherId };

      locController.getById(req, res);
      expect(res.status).toHaveBeenCalledWith(403);
      expect(res.json).toHaveBeenCalledWith(expect.objectContaining({
        status: 403,
        error: 'Forbidden'
      }));
    });

    test('UT-ID-05 : Rejet 404 si l\'identifiant n\'existe pas (pour BAILLEUR)', () => {
      req.user = { sub: 'u-bailleur', role: 'BAILLEUR', email: 'bailleur@test.ma' };
      req.params = { id: 'non-existent-uuid' };

      locController.getById(req, res);
      expect(res.status).toHaveBeenCalledWith(404);
      expect(res.json).toHaveBeenCalledWith(expect.objectContaining({
        status: 404,
        error: 'Not Found'
      }));
    });
  });

  describe('3. Entité Bail en lecture seule & Absence de routes d\'écriture', () => {
    test('UT-BAIL-RO-01 : Vérification qu\'aucune méthode POST, PUT, PATCH, DELETE n\'est enregistrée sur le routeur Baux', () => {
      const writeMethods = ['post', 'put', 'patch', 'delete'];
      const routeLayers = bailRoutes.stack.filter(layer => layer.route);

      expect(routeLayers.length).toBeGreaterThan(0);

      routeLayers.forEach(layer => {
        const methods = Object.keys(layer.route.methods).filter(m => layer.route.methods[m]);
        writeMethods.forEach(wm => {
          expect(methods).not.toContain(wm);
        });
        // Seule la lecture (GET) est autorisée
        expect(methods).toContain('get');
      });
    });

    test('UT-BAIL-RO-02 : Les seules routes exposées sur /api/baux sont GET / et GET /:id', () => {
      const routeLayers = bailRoutes.stack.filter(layer => layer.route);
      const paths = routeLayers.map(l => l.route.path);

      expect(paths).toContain('/');
      expect(paths).toContain('/:id');
      expect(paths.length).toBe(2);
    });

    test('UT-BAIL-RO-03 : Consultation des baux (GET /) retourne les baux en lecture seule (200)', () => {
      req.user = { sub: 'u-bailleur', role: 'BAILLEUR' };
      bailController.lister(req, res);
      expect(res.status).toHaveBeenCalledWith(200);
      expect(res.json).toHaveBeenCalledWith(expect.any(Array));
    });

    test('UT-BAIL-RO-04 : Consultation d\'un bail par ID (GET /:id) retourne le contrat (200)', () => {
      req.user = { sub: 'u-bailleur', role: 'BAILLEUR' };
      req.params = { id: 'b1111111-1111-1111-1111-111111111111' };

      bailController.getById(req, res);
      expect(res.status).toHaveBeenCalledWith(200);
      expect(res.json).toHaveBeenCalledWith(expect.objectContaining({
        id: 'b1111111-1111-1111-1111-111111111111',
        montantLoyer: 3400
      }));
    });
  });

  describe('4. Vérification de la route GET /api/locataires/me sur le routeur Locataires', () => {
    test('UT-LOC-RO-01 : La route /me est enregistrée sur le routeur Locataires avant /:id', () => {
      const routeLayers = locataireRoutes.stack.filter(layer => layer.route);
      const paths = routeLayers.map(l => l.route.path);

      expect(paths).toContain('/me');
      const indexMe = paths.indexOf('/me');
      const indexId = paths.indexOf('/:id');
      expect(indexMe).toBeLessThan(indexId);
    });
  });
});
