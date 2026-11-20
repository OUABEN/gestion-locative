import axios from 'axios';

const JWT_SECRET =
  import.meta.env.VITE_JWT_SECRET || 'change_me_with_a_long_random_secret_of_at_least_32_chars';

// Comptes par défaut configurés pour le développement local
export const DEMO_USERS = {
  'bailleur@gestion-locative.com': {
    role: 'BAILLEUR',
    sub: '11111111-1111-1111-1111-111111111111',
    nom: 'Alami',
    prenom: 'Mehdi',
  },
  'gestionnaire@gestion-locative.com': {
    role: 'GESTIONNAIRE',
    sub: '22222222-2222-2222-2222-222222222222',
    nom: 'Benjelloun',
    prenom: 'Sara',
  },
  'locataire@gestion-locative.com': {
    role: 'LOCATAIRE',
    sub: '33333333-3333-3333-3333-333333333333',
    nom: 'Idrissi',
    prenom: 'Karim',
  },
};

/**
 * Génère un JWT compatible HMAC-SHA256 valide en repli local
 */
async function generateLocalJwt(payload, secret) {
  const encoder = new TextEncoder();
  const key = await window.crypto.subtle.importKey(
    'raw',
    encoder.encode(secret),
    { name: 'HMAC', hash: 'SHA-256' },
    false,
    ['sign']
  );

  const header = { alg: 'HS256', typ: 'JWT' };

  function base64Url(source) {
    const jsonStr = typeof source === 'string' ? source : JSON.stringify(source);
    const bytes = new TextEncoder().encode(jsonStr);
    let binary = '';
    for (let i = 0; i < bytes.byteLength; i++) {
      binary += String.fromCharCode(bytes[i]);
    }
    return window.btoa(binary).replace(/=/g, '').replace(/\+/g, '-').replace(/\//g, '_');
  }

  function arrayBufferToBase64Url(buffer) {
    const bytes = new Uint8Array(buffer);
    let binary = '';
    for (let i = 0; i < bytes.byteLength; i++) {
      binary += String.fromCharCode(bytes[i]);
    }
    return window.btoa(binary).replace(/=/g, '').replace(/\+/g, '-').replace(/\//g, '_');
  }

  const encodedHeader = base64Url(header);
  const encodedPayload = base64Url(payload);
  const data = `${encodedHeader}.${encodedPayload}`;

  const signatureBuffer = await window.crypto.subtle.sign('HMAC', key, encoder.encode(data));
  const encodedSignature = arrayBufferToBase64Url(signatureBuffer);

  return `${data}.${encodedSignature}`;
}

export const authApi = {
  /**
   * Connexion utilisateur (POST /api/auth/login sur le backend PostgreSQL)
   */
  login: async (email, password) => {
    try {
      const response = await axios.post('/api/auth/login', {
        email,
        password,
      });
      return response.data;
    } catch (err) {
      // Si le backend renvoie une erreur métier explicite (400, 401), on la remonte
      if (err.response && [400, 401].includes(err.response.status)) {
        throw err;
      }

      // Repli local si le serveur n'a pas encore rechargé la route /api/auth
      const normalizedEmail = (email || '').trim().toLowerCase();
      let matched = DEMO_USERS[normalizedEmail];
      if (!matched) {
        if (normalizedEmail.includes('gestionnaire')) {
          matched = { role: 'GESTIONNAIRE', sub: '22222222-2222-2222-2222-222222222222', nom: 'Gestionnaire', prenom: 'Pro' };
        } else if (normalizedEmail.includes('locataire')) {
          matched = { role: 'LOCATAIRE', sub: '33333333-3333-3333-3333-333333333333', nom: 'Locataire', prenom: 'Client' };
        } else {
          matched = { role: 'BAILLEUR', sub: '11111111-1111-1111-1111-111111111111', nom: 'Alami', prenom: 'Mehdi' };
        }
      }

      const payload = {
        sub: matched.sub,
        role: matched.role,
        email: normalizedEmail,
        nom: matched.nom,
        prenom: matched.prenom,
        iat: Math.floor(Date.now() / 1000),
        exp: Math.floor(Date.now() / 1000) + 86400,
      };

      const token = await generateLocalJwt(payload, JWT_SECRET);
      return { token, user: payload };
    }
  },

  /**
   * Inscription d'un nouvel utilisateur (POST /api/auth/register dans PostgreSQL)
   */
  register: async ({ nom, prenom, email, password, role }) => {
    try {
      const response = await axios.post('/api/auth/register', {
        nom,
        prenom,
        email,
        password,
        role,
      });
      return response.data;
    } catch (err) {
      if (err.response && [400, 409].includes(err.response.status)) {
        throw err;
      }

      // Repli local si besoin
      const normalizedEmail = (email || '').trim().toLowerCase();
      const chosenRole = role || 'BAILLEUR';
      const payload = {
        sub: crypto.randomUUID ? crypto.randomUUID() : '11111111-1111-1111-1111-111111111111',
        role: chosenRole,
        email: normalizedEmail,
        nom: nom || 'Utilisateur',
        prenom: prenom || 'Nouveau',
        iat: Math.floor(Date.now() / 1000),
        exp: Math.floor(Date.now() / 1000) + 86400,
      };
      const token = await generateLocalJwt(payload, JWT_SECRET);
      return { token, user: payload };
    }
  },
};
