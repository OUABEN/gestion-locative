const DEFAULT_BASE_URL = '/api/quittances/api/v1';

export class QuittancesApiError extends Error {
  constructor(message, status, data) {
    super(message);
    this.name = 'QuittancesApiError';
    this.status = status;
    this.data = data;
  }
}

const getBaseUrl = () => {
  const configured = import.meta.env.VITE_QUITTANCES_API_BASE_URL || DEFAULT_BASE_URL;
  return configured.replace(/\/+$/, '');
};

const buildUrl = (path, params) => {
  const url = `${getBaseUrl()}/${path.replace(/^\/+/, '')}`;
  const query = new URLSearchParams();
  Object.entries(params || {}).forEach(([key, value]) => {
    if (value !== undefined && value !== null && value !== '') {
      query.set(key, String(value));
    }
  });
  const suffix = query.toString();
  return suffix ? `${url}?${suffix}` : url;
};

const messageFrom = (data, fallback) => {
  if (typeof data === 'string' && data.trim()) return data;
  if (data && typeof data === 'object') {
    return data.message || data.detail || data.title || data.error || fallback;
  }
  return fallback;
};

const readErrorBody = async (response) => {
  const text = await response.text();
  if (!text) return null;
  try {
    return JSON.parse(text);
  } catch {
    return text;
  }
};

const request = async (token, path, options = {}) => {
  if (typeof token !== 'string' || token.length === 0) {
    throw new Error('Aucun jeton de session disponible pour Quittances.');
  }

  const { method = 'GET', body, params, responseType = 'json' } = options;
  const headers = new Headers({ Authorization: `Bearer ${token}` });
  const init = { method, headers };

  if (body !== undefined) {
    headers.set('Content-Type', 'application/json');
    init.body = JSON.stringify(body);
  }

  const response = await fetch(buildUrl(path, params), init);
  if (!response.ok) {
    const data = await readErrorBody(response);
    throw new QuittancesApiError(
      messageFrom(data, `Erreur Quittances (${response.status}).`),
      response.status,
      data
    );
  }

  if (response.status === 204) return null;
  if (responseType === 'blob') return response.blob();
  if (responseType === 'text') return response.text();
  return response.json();
};

const documentPath = (id) => `quittances/${encodeURIComponent(id)}`;

export const quittancesApi = {
  getIdentity: (token) => request(token, 'identite'),
  list: (token, filters) => request(token, 'quittances', { params: filters }),
  createDraft: (token, body) => request(token, 'quittances', { method: 'POST', body }),
  getDocument: (token, id) => request(token, documentPath(id)),
  getAssignments: (token, id) => request(token, `${documentPath(id)}/affectations`),
  getHistory: (token, id, params) =>
    request(token, `${documentPath(id)}/historique`, { params }),
  previewDraft: (token, id) =>
    request(token, `${documentPath(id)}/preview`, { responseType: 'blob' }),
  validate: (token, id) =>
    request(token, `${documentPath(id)}/valider`, {
      method: 'POST',
      body: { confirmation: true },
    }),
  cancel: (token, id, motif) =>
    request(token, `${documentPath(id)}/annuler`, {
      method: 'POST',
      body: { motif, confirmation: true },
    }),
  replace: (token, id, motif) =>
    request(token, `${documentPath(id)}/remplacer`, {
      method: 'POST',
      body: { motif, confirmation: true },
    }),
  getArchives: (token, id, params) =>
    request(token, `${documentPath(id)}/archives-brouillon`, { params }),
  archiveDraft: (token, id) =>
    request(token, `${documentPath(id)}/archives-brouillon`, { method: 'POST' }),
  downloadArchive: (token, id, archiveId) =>
    request(
      token,
      `${documentPath(id)}/archives-brouillon/${encodeURIComponent(archiveId)}/pdf`,
      { responseType: 'blob' }
    ),
  issueDemo: (token, id) =>
    request(token, `${documentPath(id)}/emission-demo`, {
      method: 'POST',
      body: { confirmation: true },
    }),
  verifyDemo: (token, id) =>
    request(token, `${documentPath(id)}/emission-demo/verification`),
  downloadDemoPdf: (token, id) =>
    request(token, `${documentPath(id)}/emission-demo/pdf`, { responseType: 'blob' }),
  downloadDemoProof: (token, id) =>
    request(token, `${documentPath(id)}/emission-demo/preuve`, { responseType: 'blob' }),
  getMailpitHistory: (token, id, params) =>
    request(token, `${documentPath(id)}/envois-mailpit`, { params }),
  sendMailpit: (token, id, requeteId) =>
    request(token, `${documentPath(id)}/envois-mailpit`, {
      method: 'POST',
      body: { requeteId, confirmation: true },
    }),
  getDashboard: (token) => request(token, 'quittances/tableau-de-bord'),
  exportCsv: (token, filters) =>
    request(token, 'quittances/export.csv', { params: filters, responseType: 'blob' }),
  checkPayments: (token, id) =>
    request(token, `${documentPath(id)}/controle-paiements`),
};

export const saveBlob = (blob, filename) => {
  const objectUrl = URL.createObjectURL(blob);
  const link = document.createElement('a');
  link.href = objectUrl;
  link.download = filename;
  document.body.appendChild(link);
  link.click();
  link.remove();
  window.setTimeout(() => URL.revokeObjectURL(objectUrl), 0);
};
