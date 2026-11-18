import { jwtDecode } from 'jwt-decode';

export const decodeToken = (token) => {
  try {
    if (!token || typeof token !== 'string') return null;
    const decoded = jwtDecode(token.trim());
    const isExpired = decoded.exp ? decoded.exp * 1000 < Date.now() : false;
    return {
      sub: decoded.sub,
      role: decoded.role || 'BAILLEUR',
      email: decoded.email || 'bailleur@gestion-locative.com',
      nom: decoded.nom || '',
      prenom: decoded.prenom || '',
      isExpired,
      raw: decoded,
    };
  } catch (err) {
    console.error('Erreur lors du décodage du token JWT :', err);
    return null;
  }
};
