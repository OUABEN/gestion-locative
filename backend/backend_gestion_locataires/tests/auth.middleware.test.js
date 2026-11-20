const jwt = require('jsonwebtoken');
const { authenticate, requireRole } = require('../src/middlewares/auth.middleware');
const env = require('../src/config/env');

describe('Campagne de Tests - Authentification JWT et Rôles (Ahmed LAARIF)', () => {
  let req, res, next;

  beforeEach(() => {
    req = { headers: {} };
    res = {
      status: jest.fn().mockReturnThis(),
      json: jest.fn().mockReturnThis()
    };
    next = jest.fn();
  });

  test('UT-AUTH-01 : Rejet 401 si aucun en-tête Authorization', () => {
    authenticate(req, res, next);
    expect(res.status).toHaveBeenCalledWith(401);
    expect(res.json).toHaveBeenCalledWith(expect.objectContaining({
      status: 401,
      error: 'Unauthorized'
    }));
    expect(next).not.toHaveBeenCalled();
  });

  test('UT-AUTH-02 : Rejet 401 si jeton JWT invalide', () => {
    req.headers.authorization = 'Bearer token_completement_invalide';
    authenticate(req, res, next);
    expect(res.status).toHaveBeenCalledWith(401);
    expect(next).not.toHaveBeenCalled();
  });

  test('UT-AUTH-03 : Acceptation du jeton JWT valide et injection de req.user', () => {
    const payload = {
      sub: 'u1111111-1111-1111-1111-111111111111',
      role: 'BAILLEUR',
      email: 'bailleur@gestion-locative.com'
    };
    const validToken = jwt.sign(payload, env.JWT_SECRET);
    req.headers.authorization = `Bearer ${validToken}`;

    authenticate(req, res, next);
    expect(next).toHaveBeenCalled();
    expect(req.user).toBeDefined();
    expect(req.user.sub).toBe(payload.sub);
    expect(req.user.role).toBe('BAILLEUR');
  });

  test('UT-AUTH-04 : Contrôle de rôle RBAC - Rejet 403 pour rôle insuffisant', () => {
    req.user = { sub: 'u222', role: 'LOCATAIRE' };
    const guard = requireRole('BAILLEUR', 'GESTIONNAIRE');

    guard(req, res, next);
    expect(res.status).toHaveBeenCalledWith(403);
    expect(next).not.toHaveBeenCalled();
  });

  test('UT-AUTH-05 : Contrôle de rôle RBAC - Autorisation pour rôle adéquat', () => {
    req.user = { sub: 'u111', role: 'GESTIONNAIRE' };
    const guard = requireRole('BAILLEUR', 'GESTIONNAIRE');

    guard(req, res, next);
    expect(next).toHaveBeenCalled();
  });
});