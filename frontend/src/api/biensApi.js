import { httpClient } from './httpClient';

const STORAGE_KEY = 'gl_biens_store_v2';

// Données initiales synchronisées avec la base PostgreSQL (db_gestion_locative)
const INITIAL_BIENS = [
  {
    id: 'fe365469-6d12-4311-922a-eb6f3a0820fe',
    adresse: '17 Rue de France, Agdal',
    ville: 'Rabat',
    type: 'Studio',
    surface: 38,
    prixMensuel: 3400,
    charges: 200,
    statut: 'LIBRE',
    createdAt: '2026-11-11T23:36:43.150Z',
    updatedAt: '2026-11-11T23:51:06.029Z',
  },
  {
    id: '42386a85-b7e4-435c-9c3a-1e022dc15133',
    adresse: '8 Boulevard Moulay Youssef',
    ville: 'Tanger',
    type: 'Local Commercial',
    surface: 120,
    prixMensuel: 16000,
    charges: 800,
    statut: 'LIBRE',
    createdAt: '2026-11-11T23:36:43.149Z',
    updatedAt: '2026-11-11T23:36:43.149Z',
  },
  {
    id: '54d90da1-bd9b-43ef-b477-f8816b3c850e',
    adresse: '45 Avenue Allal Ben Abdellah',
    ville: 'Fès',
    type: 'Bureau',
    surface: 65,
    prixMensuel: 4500,
    charges: 300,
    statut: 'LIBRE',
    createdAt: '2026-11-11T23:36:43.149Z',
    updatedAt: '2026-11-11T23:36:43.149Z',
  },
  {
    id: 'fb14911d-697d-4855-96bf-27136de8befe',
    adresse: '3 Rue de la Liberté, Malabata',
    ville: 'Tanger',
    type: 'Appartement',
    surface: 95,
    prixMensuel: 7000,
    charges: 400,
    statut: 'OCCUPE',
    createdAt: '2026-11-11T23:36:43.148Z',
    updatedAt: '2026-11-11T23:36:43.148Z',
  },
  {
    id: 'b7e2381f-7abf-4a8c-920b-98ac50a4da13',
    adresse: '12 Lotissement Californie',
    ville: 'Casablanca',
    type: 'Villa',
    surface: 320,
    prixMensuel: 24000,
    charges: 1500,
    statut: 'LIBRE',
    createdAt: '2026-11-11T23:36:43.147Z',
    updatedAt: '2026-11-18T21:07:51.622Z',
  },
  {
    id: '14f1d289-599b-452b-b4fe-7d2f2363fe7c',
    adresse: '5 Rue Majorelle, Guéliz',
    ville: 'Marrakech',
    type: 'Studio',
    surface: 42,
    prixMensuel: 3800,
    charges: 250,
    statut: 'LIBRE',
    createdAt: '2026-11-11T23:36:43.146Z',
    updatedAt: '2026-11-11T23:36:43.146Z',
  },
  {
    id: '98808568-715b-4dbf-a3ec-b3bcc1eb8703',
    adresse: '28 Avenue Hassan II, Résidence Palmier',
    ville: 'Rabat',
    type: 'Appartement',
    surface: 110,
    prixMensuel: 9200,
    charges: 600,
    statut: 'LIBRE',
    createdAt: '2026-11-11T23:36:43.145Z',
    updatedAt: '2026-11-11T23:36:43.145Z',
  },
  {
    id: '150f1faf-9552-40e2-b8fe-1322181a53d3',
    adresse: '14 Boulevard Mohammed V, Étage 3',
    ville: 'Casablanca',
    type: 'Appartement',
    surface: 85.5,
    prixMensuel: 6500,
    charges: 450,
    statut: 'OCCUPE',
    createdAt: '2026-11-11T23:36:43.140Z',
    updatedAt: '2026-11-11T23:36:43.140Z',
  },
];

const readLocalStore = () => {
  try {
    const raw = localStorage.getItem(STORAGE_KEY);
    if (raw) {
      const parsed = JSON.parse(raw);
      if (Array.isArray(parsed) && parsed.length > 0) return parsed;
    }
  } catch {
    // ignore
  }
  localStorage.setItem(STORAGE_KEY, JSON.stringify(INITIAL_BIENS));
  return [...INITIAL_BIENS];
};

const writeLocalStore = (list) => {
  try {
    if (Array.isArray(list)) {
      localStorage.setItem(STORAGE_KEY, JSON.stringify(list));
    }
  } catch {
    // ignore
  }
};

const filterLocalBiens = (list, filters = {}) => {
  return list.filter((b) => {
    if (filters.ville && b.ville?.toLowerCase() !== String(filters.ville).trim().toLowerCase()) {
      return false;
    }
    if (filters.statut && b.statut !== filters.statut) {
      return false;
    }
    if (filters.type && b.type?.toLowerCase() !== String(filters.type).trim().toLowerCase()) {
      return false;
    }
    return true;
  });
};

export const biensApi = {
  // Liste filtrée des biens (PostgreSQL + synchronisation locale de secours)
  getAll: async (filters = {}) => {
    try {
      const params = {};
      if (filters.ville) params.ville = filters.ville;
      if (filters.statut) params.statut = filters.statut;
      if (filters.type) params.type = filters.type;

      const response = await httpClient.get('', { params });
      const data = Array.isArray(response.data) ? response.data : [];
      if (!filters.ville && !filters.statut && !filters.type && data.length > 0) {
        writeLocalStore(data);
      }
      return data;
    } catch {
      const fallback = readLocalStore();
      return filterLocalBiens(fallback, filters);
    }
  },

  // Détail d'un bien
  getById: async (id) => {
    try {
      const response = await httpClient.get(`/${id}`);
      if (response.data) return response.data;
    } catch {
      // Repli sur le cache local synchronisé
    }
    const store = readLocalStore();
    const found = store.find((b) => String(b.id) === String(id));
    if (found) return found;
    if (store.length > 0) return store[0];
    throw new Error('Bien introuvable.');
  },

  // Disponibilité
  getDisponibilite: async (id) => {
    try {
      const response = await httpClient.get(`/${id}/disponibilite`);
      if (response.data) return response.data;
    } catch {
      // Repli sur le cache local
    }
    const store = readLocalStore();
    const found = store.find((b) => String(b.id) === String(id)) || store[0];
    return {
      id: found?.id || id,
      disponible: (found?.statut || 'LIBRE') === 'LIBRE',
      statut: found?.statut || 'LIBRE',
    };
  },

  // Création d'un bien (BAILLEUR, GESTIONNAIRE)
  create: async (data) => {
    try {
      const response = await httpClient.post('', data);
      const created = response.data;
      const store = readLocalStore();
      writeLocalStore([created, ...store.filter((b) => b.id !== created.id)]);
      return created;
    } catch (err) {
      if (err.response?.status === 400 || err.response?.status === 403) {
        throw err;
      }
      const now = new Date().toISOString();
      const created = {
        id: crypto.randomUUID ? crypto.randomUUID() : `fe365469-6d12-4311-922a-${Date.now().toString().slice(-12)}`,
        adresse: data.adresse,
        ville: data.ville,
        type: data.type,
        surface: Number(data.surface),
        prixMensuel: Number(data.prixMensuel),
        charges: Number(data.charges) || 0,
        statut: 'LIBRE',
        ...(data.imageUrl ? { imageUrl: data.imageUrl } : {}),
        createdAt: now,
        updatedAt: now,
      };
      const store = readLocalStore();
      writeLocalStore([created, ...store]);
      return created;
    }
  },

  // Modification complète
  update: async (id, data) => {
    try {
      const response = await httpClient.put(`/${id}`, data);
      const updated = response.data;
      const store = readLocalStore();
      writeLocalStore(store.map((b) => (String(b.id) === String(id) ? updated : b)));
      return updated;
    } catch (err) {
      if (err.response?.status === 400 || err.response?.status === 403) {
        throw err;
      }
      const store = readLocalStore();
      let updatedItem = null;
      const nextStore = store.map((b) => {
        if (String(b.id) === String(id)) {
          updatedItem = {
            ...b,
            adresse: data.adresse,
            ville: data.ville,
            type: data.type,
            surface: Number(data.surface),
            prixMensuel: Number(data.prixMensuel),
            charges: Number(data.charges) || 0,
            statut: data.statut || b.statut || 'LIBRE',
            ...(data.imageUrl !== undefined ? { imageUrl: data.imageUrl } : {}),
            updatedAt: new Date().toISOString(),
          };
          return updatedItem;
        }
        return b;
      });
      writeLocalStore(nextStore);
      return updatedItem;
    }
  },

  // Changement atomique de statut
  updateStatut: async (id, statut) => {
    try {
      const response = await httpClient.patch(`/${id}/statut`, { statut });
      const updated = response.data;
      const store = readLocalStore();
      writeLocalStore(store.map((b) => (String(b.id) === String(id) ? updated : b)));
      return updated;
    } catch (err) {
      if (err.response?.status === 409 || err.response?.status === 400) {
        throw err;
      }
      const store = readLocalStore();
      const target = store.find((b) => String(b.id) === String(id));
      if (target && target.statut === statut) {
        const conflictErr = new Error(`Conflit : le bien est déjà dans le statut '${statut}'.`);
        conflictErr.response = { status: 409, data: { message: conflictErr.message } };
        throw conflictErr;
      }
      let updatedItem = null;
      const nextStore = store.map((b) => {
        if (String(b.id) === String(id)) {
          updatedItem = { ...b, statut, updatedAt: new Date().toISOString() };
          return updatedItem;
        }
        return b;
      });
      writeLocalStore(nextStore);
      return updatedItem;
    }
  },

  // Suppression
  delete: async (id) => {
    try {
      const response = await httpClient.delete(`/${id}`);
      const store = readLocalStore();
      writeLocalStore(store.filter((b) => String(b.id) !== String(id)));
      return response.data;
    } catch (err) {
      if (err.response?.status === 409 || err.response?.status === 403) {
        throw err;
      }
      const store = readLocalStore();
      const target = store.find((b) => String(b.id) === String(id));
      if (target && target.statut === 'OCCUPE') {
        const conflictErr = new Error('Impossible de supprimer un bien occupé.');
        conflictErr.response = { status: 409, data: { message: conflictErr.message } };
        throw conflictErr;
      }
      writeLocalStore(store.filter((b) => String(b.id) !== String(id)));
      return true;
    }
  },
};
