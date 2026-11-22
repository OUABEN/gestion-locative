const jwt = require('jsonwebtoken');
const env = require('../config/env');

/**
 * Middleware d'authentification JWT
 * Valide le jeton transmis dans l'en-tête Authorization: Bearer <token>
 * avec la même clé secrète JWT_SECRET et le même format que backend_gestion_biens.
 */
function authenticate(req, res, next) {
  const authHeader = req.headers.authorization;
  if (!authHeader) {
    return res.status(401).json({
      status: 401,
      error: 'Unauthorized',
      message: "Jeton d'authentification manquant dans l'en-tête Authorization."
    });
  }

  const parts = authHeader.split(' ');
  if (parts.length !== 2 || parts[0] !== 'Bearer') {
    return res.status(401).json({
      status: 401,
      error: 'Unauthorized',
      message: "Format du jeton invalide. Format attendu : Bearer <token>"
    });
  }

  const token = parts[1];
  try {
    const decoded = jwt.verify(token, env.JWT_SECRET);
    if (!decoded || !decoded.sub || !decoded.role) {
      return res.status(401).json({
        status: 401,
        error: 'Unauthorized',
        message: 'Payload du jeton JWT incomplet (sub et role requis).'
      });
    }

    req.user = decoded;
    next();
  } catch (err) {
    return res.status(401).json({
      status: 401,
      error: 'Unauthorized',
      message: "Jeton d'authentification invalide ou expiré."
    });
  }
}

/**
 * Middleware de contrôle d'accès basé sur les rôles (RBAC)
 * Rôles acceptés : 'BAILLEUR', 'GESTIONNAIRE', 'LOCATAIRE'
 */
function requireRole(...allowedRoles) {
  return (req, res, next) => {
    if (!req.user) {
      return res.status(401).json({
        status: 401,
        error: 'Unauthorized',
        message: 'Utilisateur non authentifié.'
      });
    }

    if (!allowedRoles.includes(req.user.role)) {
      return res.status(403).json({
        status: 403,
        error: 'Forbidden',
        message: `Accès refusé : rôle '${req.user.role}' insuffisant. Rôles autorisés : ${allowedRoles.join(', ')}.`
      });
    }

    next();
  };
}

module.exports = {
  authenticate,
  requireRole,
};