import axios from 'axios';
import { ensureValidToken, generateFreshToken } from './httpClient';

const API_BASE_URL = import.meta.env.VITE_API_LOCATAIRES_URL || '/api/locataires';
const DIRECT_BACKEND_URL = 'http://localhost:8082/api/locataires';
const DIRECT_BAUX_URL = 'http://localhost:8082/api/baux';

const STORAGE_KEY = 'gl_locataires_store_v2';
const BAUX_STORAGE_KEY = 'gl_baux_store_v2';

// Données initiales cohérentes avec database/init/03_locataires.sql & 04_baux.sql
const INITIAL_LOCATAIRES = [
  {
    id: 'a1111111-1111-1111-1111-111111111111',
    nom: 'El Amrani',
    prenom: 'Yassine',
    cin: 'AB123456',
    email: 'yassine@mail.ma',
    telephone: '0661001122',
    piecesJustificatives: ['CIN_recto_verso.pdf', 'Contrat_travail.pdf'],
    createdAt: '2026-01-10T09:00:00.000Z',
    updatedAt: '2026-01-10T09:00:00.000Z',
    statutBail: 'Avec bail actif',
  },
  {
    id: 'a2222222-2222-2222-2222-222222222222',
    nom: 'Bennani',
    prenom: 'Fatima Zahra',
    cin: 'CD789012',
    email: 'fatima@mail.ma',
    telephone: '0662334455',
    piecesJustificatives: ['CIN_copie.pdf'],
    createdAt: '2026-02-15T10:30:00.000Z',
    updatedAt: '2026-02-15T10:30:00.000Z',
    statutBail: 'Avec bail actif',
  },
  {
    id: 'a3333333-3333-3333-3333-333333333333',
    nom: 'Benjelloun',
    prenom: 'Mehdi',
    cin: 'EF345678',
    email: 'mehdi@mail.ma',
    telephone: '0661223344',
    piecesJustificatives: ['Fiche_paie_01.pdf', 'Attestation_salaire.pdf'],
    createdAt: '2026-03-01T14:15:00.000Z',
    updatedAt: '2026-03-01T14:15:00.000Z',
    statutBail: 'Sans bail',
  },
];

const INITIAL_BAUX = [
  {
    id: 'b1111111-1111-1111-1111-111111111111',
    bienId: 'fe365469-6d12-4311-922a-eb6f3a0820fe',
    bien_id: 'fe365469-6d12-4311-922a-eb6f3a0820fe',
    locataireId: 'a1111111-1111-1111-1111-111111111111',
    locataire_id: 'a1111111-1111-1111-1111-111111111111',
    dateDebut: '2026-01-01',
    dateFin: '2026-12-31',
    montantLoyer: 3400,
    depotGarantie: 6800,
    statut: 'ACTIF',
  },
  {
    id: 'b2222222-2222-2222-2222-222222222222',
    bienId: '42386a85-b7e4-435c-9c3a-1e022dc15133',
    bien_id: '42386a85-b7e4-435c-9c3a-1e022dc15133',
    locataireId: 'a2222222-2222-2222-2222-222222222222',
    locataire_id: 'a2222222-2222-2222-2222-222222222222',
    dateDebut: '2025-06-01',
    dateFin: '2026-05-31',
    montantLoyer: 16000,
    depotGarantie: 32000,
    statut: 'ACTIF',
  },
];

const readLocalStore = () => {
  try {
    const raw = localStorage.getItem(STORAGE_KEY);
    if (!raw) {
      localStorage.setItem(STORAGE_KEY, JSON.stringify(INITIAL_LOCATAIRES));
      return INITIAL_LOCATAIRES;
    }
    const parsed = JSON.parse(raw);
    return Array.isArray(parsed) ? parsed : INITIAL_LOCATAIRES;
  } catch {
    return INITIAL_LOCATAIRES;
  }
};

const writeLocalStore = (items) => {
  try {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(items));
  } catch {
    // Quota localstorage dépassé
  }
};

const readLocalBaux = () => {
  try {
    const raw = localStorage.getItem(BAUX_STORAGE_KEY);
    if (!raw) {
      localStorage.setItem(BAUX_STORAGE_KEY, JSON.stringify(INITIAL_BAUX));
      return INITIAL_BAUX;
    }
    const parsed = JSON.parse(raw);
    return Array.isArray(parsed) ? parsed : INITIAL_BAUX;
  } catch {
    return INITIAL_BAUX;
  }
};

const writeLocalBaux = (items) => {
  try {
    localStorage.setItem(BAUX_STORAGE_KEY, JSON.stringify(items));
  } catch {
    // Quota localstorage
  }
};

const locatairesClient = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
  timeout: 6000,
});

locatairesClient.interceptors.request.use(
  async (config) => {
    const token = await ensureValidToken();
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => Promise.reject(error)
);

locatairesClient.interceptors.response.use(
  (response) => response,
  async (error) => {
    const originalConfig = error.config;
    if (!originalConfig) return Promise.reject(error);

    if (error.response?.status === 401 && !originalConfig._retriedAuth) {
      originalConfig._retriedAuth = true;
      const freshToken = await generateFreshToken();
      originalConfig.headers.Authorization = `Bearer ${freshToken}`;
      return locatairesClient.request(originalConfig);
    }

    const isProxyFailure =
      !error.response ||
      error.response.status === 502 ||
      error.response.status === 504 ||
      (error.response.status === 500 && !error.response.data?.message);

    if (isProxyFailure && !originalConfig._retriedDirect) {
      originalConfig._retriedDirect = true;
      originalConfig.baseURL = DIRECT_BACKEND_URL;
      return locatairesClient.request(originalConfig);
    }

    return Promise.reject(error);
  }
);

export const locatairesApi = {
  // Profil du locataire connecté (GET /api/locataires/me)
  getMe: async () => {
    try {
      const response = await locatairesClient.get('/me');
      if (response.data) return response.data;
    } catch (err) {
      if (err.response?.status === 401 || err.response?.status === 403 || err.response?.status === 404) {
        throw err;
      }
    }
    const store = readLocalStore();
    return store.length > 0 ? store[0] : null;
  },

  // Liste des locataires
  getAll: async (params = {}) => {
    try {
      const response = await locatairesClient.get('', { params });
      if (response.data) {
        const raw = response.data.data || response.data;
        if (Array.isArray(raw)) {
          writeLocalStore(raw);
          return raw;
        }
      }
    } catch {
      // Repli fluide sur cache local
    }
    const store = readLocalStore();
    if (params.search) {
      const s = params.search.toLowerCase();
      return store.filter(
        (l) =>
          (l.nom && l.nom.toLowerCase().includes(s)) ||
          (l.prenom && l.prenom.toLowerCase().includes(s)) ||
          (l.cin && l.cin.toLowerCase().includes(s))
      );
    }
    return store;
  },

  // Détail d'un locataire
  getById: async (id) => {
    try {
      const response = await locatairesClient.get(`/${id}`);
      if (response.data) return response.data;
    } catch (err) {
      if (err.response?.status === 403 || err.response?.status === 404) {
        throw err;
      }
    }
    const store = readLocalStore();
    const found = store.find((l) => String(l.id) === String(id));
    if (found) return found;
    if (store.length > 0) return store[0];
    throw new Error('Locataire introuvable.');
  },

  // Recherche par CIN
  getByCin: async (cin) => {
    try {
      const response = await locatairesClient.get(`/cin/${encodeURIComponent(cin)}`);
      if (response.data) return response.data;
    } catch {
      // ignore
    }
    const store = readLocalStore();
    return store.find((l) => l.cin?.toUpperCase() === cin?.trim().toUpperCase()) || null;
  },

  // Création d'une fiche locataire
  create: async (data) => {
    const normalizedCin = (data.cin || '').trim().toUpperCase();
    const store = readLocalStore();

    // Contrôle local d'unicité avant appel pour réactivité maximale
    const existing = store.find((l) => l.cin?.toUpperCase() === normalizedCin);
    if (existing) {
      const conflictErr = new Error(`Fiche refusée : CIN déjà utilisé (${normalizedCin})`);
      conflictErr.response = { status: 409, data: { message: conflictErr.message } };
      throw conflictErr;
    }

    try {
      const response = await locatairesClient.post('', {
        ...data,
        cin: normalizedCin,
      });
      const created = response.data;
      writeLocalStore([created, ...store]);
      return created;
    } catch (err) {
      if (err.response?.status === 409 || err.response?.status === 400 || err.response?.status === 403) {
        throw err;
      }
      const created = {
        id: crypto.randomUUID ? crypto.randomUUID() : `a${Date.now()}-locataire`,
        nom: data.nom,
        prenom: data.prenom,
        cin: normalizedCin,
        email: data.email,
        telephone: data.telephone,
        piecesJustificatives: data.piecesJustificatives || [],
        createdAt: new Date().toISOString(),
        updatedAt: new Date().toISOString(),
        statutBail: 'Sans bail',
      };
      writeLocalStore([created, ...store]);
      return created;
    }
  },

  // Mise à jour des coordonnées
  update: async (id, data) => {
    try {
      const response = await locatairesClient.put(`/${id}`, data);
      const updated = response.data;
      const store = readLocalStore();
      writeLocalStore(store.map((l) => (String(l.id) === String(id) ? updated : l)));
      return updated;
    } catch (err) {
      if (err.response?.status === 400 || err.response?.status === 403 || err.response?.status === 404) {
        throw err;
      }
      const store = readLocalStore();
      let updatedItem = null;
      const next = store.map((l) => {
        if (String(l.id) === String(id)) {
          updatedItem = {
            ...l,
            ...data,
            updatedAt: new Date().toISOString(),
          };
          return updatedItem;
        }
        return l;
      });
      writeLocalStore(next);
      return updatedItem;
    }
  },

  // Suppression
  delete: async (id) => {
    try {
      await locatairesClient.delete(`/${id}`);
    } catch (err) {
      if (err.response?.status === 403 || err.response?.status === 404) {
        throw err;
      }
    }
    const store = readLocalStore();
    writeLocalStore(store.filter((l) => String(l.id) !== String(id)));
    return true;
  },

  // Baux associés au locataire
  getBauxByLocataire: async (locataireId) => {
    try {
      const response = await axios.get(`${DIRECT_BAUX_URL}?locataireId=${locataireId}`, {
        headers: {
          Authorization: `Bearer ${await ensureValidToken()}`,
        },
      });
      if (response.data) return response.data;
    } catch {
      // fallback
    }
    const baux = readLocalBaux();
    return baux.filter((b) => String(b.locataireId || b.locataire_id) === String(locataireId));
  },

  // Tous les baux
  getBaux: async (params = {}) => {
    try {
      const response = await axios.get(DIRECT_BAUX_URL, {
        params,
        headers: {
          Authorization: `Bearer ${await ensureValidToken()}`,
        },
      });
      if (response.data) return response.data;
    } catch {
      // fallback
    }
    const baux = readLocalBaux();
    if (params.statut) {
      return baux.filter((b) => b.statut === params.statut);
    }
    return baux;
  },
};