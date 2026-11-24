import { jwtDecode } from 'jwt-decode';

const RECOGNIZED_ROLES = new Set(['BAILLEUR', 'GESTIONNAIRE', 'LOCATAIRE']);

export const decodeToken = (token) => {
  try {
    if (typeof token !== 'string' || token.length === 0 || token.trim() !== token) {
      return null;
    }

    const decoded = jwtDecode(token);
    if (
      typeof decoded.sub !== 'string' ||
      !decoded.sub.trim() ||
      !RECOGNIZED_ROLES.has(decoded.role) ||
      !Number.isFinite(decoded.iat) ||
      !Number.isFinite(decoded.exp)
    ) {
      return null;
    }

    return {
      sub: decoded.sub,
      role: decoded.role,
      email: typeof decoded.email === 'string' ? decoded.email : '',
      nom: typeof decoded.nom === 'string' ? decoded.nom : '',
      prenom: typeof decoded.prenom === 'string' ? decoded.prenom : '',
      isExpired: decoded.exp * 1000 <= Date.now(),
      raw: decoded,
    };
  } catch {
    return null;
  }
};
