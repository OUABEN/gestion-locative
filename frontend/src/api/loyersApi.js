import { httpClient } from './httpClient';

// Le service Loyers tourne sur 8083. La base peut être redéfinie en déploiement.
const baseURL = import.meta.env.VITE_API_LOYERS_URL || 'http://localhost:8083';
const request = (method, path, config = {}) =>
  httpClient.request({ method, url: `/api/${path}`, baseURL, ...config });

export const loyersApi = {
  getEcheances: async (filters = {}) => {
    const { data } = await request('get', 'echeances', { params: filters });
    return Array.isArray(data) ? data : [];
  },
  getEcheance: async (id) => (await request('get', `echeances/${encodeURIComponent(id)}`)).data,
  getBaux: async (filters = {}) => {
    const { data } = await request('get', 'baux', { params: filters });
    return Array.isArray(data) ? data : [];
  },
  getPaiements: async (filters = {}) => {
    const { data } = await request('get', 'paiements', { params: filters });
    return Array.isArray(data) ? data : [];
  },
  createPaiement: async (payload) => (await request('post', 'paiements', { data: payload })).data,
};
