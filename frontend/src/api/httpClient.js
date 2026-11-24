import { decodeToken } from '../utils/jwt';
import axios from 'axios';

const API_BASE_URL = import.meta.env.VITE_API_BIENS_URL || '/api/biens';

export const httpClient = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
  timeout: 6000,
});

httpClient.interceptors.request.use(
  (config) => {
    const token = sessionStorage.getItem('token') || localStorage.getItem('token');
    config.headers = config.headers || {};
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    } else {
      delete config.headers.Authorization;
    }
    return config;
  },
  (error) => Promise.reject(error)
);

httpClient.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401 && typeof window !== 'undefined') {
      window.dispatchEvent(new Event('auth:session-rejected'));
    }
    return Promise.reject(error);
  }
);

export const ensureValidToken = async () => {
  const token = sessionStorage.getItem('token') || localStorage.getItem('token');
  const session = token ? decodeToken(token) : null;

  if (!session || session.isExpired) {
    window.dispatchEvent(new Event('auth:session-rejected'));
    throw new Error('Session invalide ou expiree.');
  }
  return token;
};

export const generateFreshToken = async () => {
  window.dispatchEvent(new Event('auth:session-rejected'));
  throw new Error('Session expiree. Veuillez vous reconnecter.');
};