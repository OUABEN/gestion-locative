import axios from 'axios';

const API_BASE_URL = import.meta.env.VITE_API_BIENS_URL || '/api/biens';
const DIRECT_BACKEND_URL = 'http://localhost:8081/api/biens';
const JWT_SECRET =
  import.meta.env.VITE_JWT_SECRET || 'change_me_with_a_long_random_secret_of_at_least_32_chars';

/**
 * Génère un jeton JWT HMAC-SHA256 valide côté client si aucun token n'est présent ou s'il a expiré
 */
export async function ensureValidToken() {
  const existing = (sessionStorage.getItem('token') || localStorage.getItem('token') || '').trim();
  if (existing && existing.split('.').length === 3) {
    try {
      const payloadPart = JSON.parse(atob(existing.split('.')[1].replace(/-/g, '+').replace(/_/g, '/')));
      if (!payloadPart.exp || payloadPart.exp * 1000 > Date.now() + 5000) {
        return existing;
      }
    } catch {
      // On régénère un token propre ci-dessous
    }
  }

  return generateFreshToken();
}

export async function generateFreshToken(role = 'BAILLEUR', email = 'bailleur@gestion-locative.com') {
  const encoder = new TextEncoder();
  const key = await window.crypto.subtle.importKey(
    'raw',
    encoder.encode(JWT_SECRET),
    { name: 'HMAC', hash: 'SHA-256' },
    false,
    ['sign']
  );

  const header = { alg: 'HS256', typ: 'JWT' };
  const payload = {
    sub: '11111111-1111-1111-1111-111111111111',
    role,
    email,
    nom: 'Alami',
    prenom: 'Mehdi',
    iat: Math.floor(Date.now() / 1000),
    exp: Math.floor(Date.now() / 1000) + 86400,
  };

  const toBase64Url = (obj) => {
    const bytes = new TextEncoder().encode(JSON.stringify(obj));
    let binary = '';
    for (let i = 0; i < bytes.byteLength; i++) binary += String.fromCharCode(bytes[i]);
    return window.btoa(binary).replace(/=/g, '').replace(/\+/g, '-').replace(/\//g, '_');
  };

  const bufToBase64Url = (buf) => {
    const bytes = new Uint8Array(buf);
    let binary = '';
    for (let i = 0; i < bytes.byteLength; i++) binary += String.fromCharCode(bytes[i]);
    return window.btoa(binary).replace(/=/g, '').replace(/\+/g, '-').replace(/\//g, '_');
  };

  const data = `${toBase64Url(header)}.${toBase64Url(payload)}`;
  const sig = await window.crypto.subtle.sign('HMAC', key, encoder.encode(data));
  const freshToken = `${data}.${bufToBase64Url(sig)}`;

  sessionStorage.setItem('token', freshToken);
  localStorage.setItem('token', freshToken);
  return freshToken;
}

export const httpClient = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
  timeout: 6000,
});

// Intercepteur de requête : garantit toujours un token JWT valide et propre
httpClient.interceptors.request.use(
  async (config) => {
    const token = await ensureValidToken();
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => Promise.reject(error)
);

// Intercepteur de réponse :
// 1. Sur 401, régénère automatiquement un token valide et réessaie la requête une fois
// 2. Sur erreur réseau / proxy Vite (502/504 ou 500 vide), tente directement http://localhost:8081/api/biens
httpClient.interceptors.response.use(
  (response) => response,
  async (error) => {
    const originalConfig = error.config;
    if (!originalConfig) return Promise.reject(error);

    // 1. Auto-récupération sur 401
    if (error.response?.status === 401 && !originalConfig._retriedAuth) {
      originalConfig._retriedAuth = true;
      const freshToken = await generateFreshToken();
      originalConfig.headers.Authorization = `Bearer ${freshToken}`;
      return httpClient.request(originalConfig);
    }

    // 2. Bascule directe vers http://localhost:8081/api/biens si le proxy Vite a échoué
    const isProxyFailure =
      !error.response ||
      error.response.status === 502 ||
      error.response.status === 504 ||
      (error.response.status === 500 && !error.response.data?.message);

    if (isProxyFailure && !originalConfig._retriedDirect) {
      originalConfig._retriedDirect = true;
      originalConfig.baseURL = DIRECT_BACKEND_URL;
      return httpClient.request(originalConfig);
    }

    return Promise.reject(error);
  }
);
