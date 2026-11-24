import React from 'react';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { AuthProvider } from '../context/AuthContext';
import { quittancesApi } from '../api/quittancesApi';
import { QuittanceCreatePage } from '../pages/quittances/QuittanceCreatePage';
import { QuittanceDetailPage } from '../pages/quittances/QuittanceDetailPage';
import { QuittancesListPage } from '../pages/quittances/QuittancesListPage';

const EMPTY_HISTORY = { contenu: [], page: 0, taille: 10, totalElements: 0, totalPages: 0 };
const EMPTY_PAGE = { contenu: [], page: 0, taille: 20, totalElements: 0, totalPages: 0 };

const createToken = (role = 'BAILLEUR') => {
  const header = btoa(JSON.stringify({ alg: 'HS256', typ: 'JWT' }));
  const payload = btoa(JSON.stringify({
    sub: 'common-user-uuid',
    role,
    exp: Math.floor(Date.now() / 1000) + 3600,
  }));
  return `${header}.${payload}.signature`;
};
const TOKEN = createToken();

const draftDocument = (overrides = {}) => ({
  id: 'quittance-uuid',
  numero: null,
  bailId: 'BAIL-1',
  locataireId: 'LOC-1',
  bienId: 'BIEN-1',
  nomBailleur: 'Bailleur',
  nomLocataire: 'Locataire',
  prenomLocataire: 'Test',
  adresseBien: 'Adresse du bien',
  periodeDebut: '2025-01-01',
  periodeFin: '2025-01-31',
  loyerDu: 1000,
  chargesDues: 100,
  loyerPaye: 1000,
  chargesPayees: 100,
  totalPaye: 1100,
  devise: 'MAD',
  typeDocument: 'QUITTANCE',
  statut: 'BROUILLON',
  statutSignature: 'NON_SIGNE',
  creeLe: '2025-02-01T10:00:00Z',
  emisLe: null,
  documentPrecedentId: null,
  motifRemplacement: null,
  ...overrides,
});

const renderWithSession = (element, initialEntry = '/quittances') => {
  sessionStorage.setItem('token', TOKEN);
  return render(
    <MemoryRouter initialEntries={[initialEntry]}>
      <AuthProvider>{element}</AuthProvider>
    </MemoryRouter>
  );
};

const prepareDetailMocks = (identityRole = 'ROLE_BAILLEUR', doc = draftDocument()) => {
  vi.spyOn(quittancesApi, 'getIdentity').mockResolvedValue({
    sujet: 'common-user-uuid',
    autorites: [identityRole],
  });
  vi.spyOn(quittancesApi, 'getDocument').mockResolvedValue(doc);
  vi.spyOn(quittancesApi, 'getAssignments').mockResolvedValue([]);
  vi.spyOn(quittancesApi, 'getHistory').mockResolvedValue(EMPTY_HISTORY);
  vi.spyOn(quittancesApi, 'getArchives').mockResolvedValue(EMPTY_HISTORY);
  vi.spyOn(quittancesApi, 'getMailpitHistory').mockResolvedValue(EMPTY_HISTORY);
};

describe('Module Quittances — frontend React', () => {
  beforeEach(() => {
    vi.restoreAllMocks();
    vi.unstubAllGlobals();
    sessionStorage.clear();
    localStorage.clear();
  });

  it('transmet pagination, filtres et tris réellement disponibles', async () => {
    vi.spyOn(quittancesApi, 'getIdentity').mockResolvedValue({
      sujet: 'common-user-uuid',
      autorites: ['ROLE_BAILLEUR'],
    });
    const list = vi.spyOn(quittancesApi, 'list').mockResolvedValue(EMPTY_PAGE);
    vi.spyOn(quittancesApi, 'getDashboard').mockResolvedValue({
      totalDocuments: 0,
      groupes: [],
    });

    const user = userEvent.setup();
    renderWithSession(<QuittancesListPage />);
    await screen.findByText('Aucune quittance pour ces critères.');

    await user.type(screen.getByLabelText('Identifiant du bail'), 'BAIL-42');
    await user.selectOptions(screen.getByLabelText('Statut'), 'EMIS');
    await user.selectOptions(screen.getByLabelText('Trier par'), 'numero');
    await user.selectOptions(screen.getByLabelText('Ordre'), 'asc');
    await user.click(screen.getByRole('button', { name: 'Appliquer' }));

    await waitFor(() => {
      expect(list).toHaveBeenLastCalledWith(TOKEN, expect.objectContaining({
        page: 0,
        taille: 20,
        bailId: 'BAIL-42',
        statut: 'EMIS',
        tri: 'numero',
        sens: 'asc',
      }));
    });
  });

  it('envoie uniquement bailId et les dates pour préparer un brouillon', async () => {
    vi.spyOn(quittancesApi, 'getIdentity').mockResolvedValue({
      sujet: 'common-user-uuid',
      autorites: ['ROLE_BAILLEUR'],
    });
    const createDraft = vi.spyOn(quittancesApi, 'createDraft').mockResolvedValue({
      id: 'new-document-id',
      statut: 'BROUILLON',
      statutSignature: 'NON_SIGNE',
    });
    sessionStorage.setItem('token', TOKEN);
    const user = userEvent.setup();
    render(
      <MemoryRouter initialEntries={['/quittances/nouveau']}>
        <AuthProvider>
          <Routes>
            <Route path="/quittances/nouveau" element={<QuittanceCreatePage />} />
            <Route path="/quittances/:id" element={<div>Brouillon ouvert</div>} />
          </Routes>
        </AuthProvider>
      </MemoryRouter>
    );

    await user.type(await screen.findByLabelText(/Identifiant du bail/), 'BAIL-42');
    fireEvent.change(screen.getByLabelText('Début de période'), { target: { value: '2025-03-01' } });
    fireEvent.change(screen.getByLabelText('Fin de période'), { target: { value: '2025-03-31' } });
    await user.click(screen.getByRole('button', { name: 'Créer le brouillon' }));

    await screen.findByText('Brouillon ouvert');
    expect(createDraft).toHaveBeenCalledWith(TOKEN, {
      bailId: 'BAIL-42',
      periodeDebut: '2025-03-01',
      periodeFin: '2025-03-31',
    });
  });

  it('n’affiche aucune action d’écriture pour une identité LOCATAIRE', async () => {
    prepareDetailMocks('ROLE_LOCATAIRE');
    renderWithSession(
      <Routes>
        <Route path="/quittances/:id" element={<QuittanceDetailPage />} />
      </Routes>,
      '/quittances/quittance-uuid'
    );

    await screen.findByRole('heading', { name: 'Actions disponibles' });
    expect(screen.queryByRole('button', { name: 'Valider' })).not.toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Annuler' })).not.toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Remplacer' })).not.toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Archiver le brouillon' })).not.toBeInTheDocument();
    expect(screen.queryByRole('button', { name: /Mailpit/i })).not.toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Contrôler les paiements' })).not.toBeInTheDocument();
    expect(quittancesApi.getArchives).not.toHaveBeenCalled();
    expect(quittancesApi.getMailpitHistory).not.toHaveBeenCalled();
  });

  it('refuse explicitement un rôle que le backend Quittances ne prend pas en charge', async () => {
    vi.spyOn(quittancesApi, 'getIdentity').mockResolvedValue({
      sujet: 'common-user-uuid',
      autorites: ['ROLE_GESTIONNAIRE'],
    });
    const list = vi.spyOn(quittancesApi, 'list');
    renderWithSession(<QuittancesListPage />);

    expect(await screen.findByRole('alert')).toHaveTextContent(/Identité Quittances non reconnue/i);
    expect(list).not.toHaveBeenCalled();
    expect(screen.queryByText('Aucune quittance pour ces critères.')).not.toBeInTheDocument();
  });

  it('exige une confirmation explicite avant d’appeler la validation', async () => {
    prepareDetailMocks('ROLE_BAILLEUR', draftDocument({
      statut: 'EN_ATTENTE_VALIDATION',
    }));
    const validate = vi.spyOn(quittancesApi, 'validate').mockResolvedValue({
      id: 'quittance-uuid',
      statut: 'EN_ATTENTE_SIGNATURE',
      statutSignature: 'EN_ATTENTE',
    });
    const user = userEvent.setup();
    renderWithSession(
      <Routes>
        <Route path="/quittances/:id" element={<QuittanceDetailPage />} />
      </Routes>,
      '/quittances/quittance-uuid'
    );

    await user.click(await screen.findByRole('button', { name: 'Valider' }));
    expect(screen.getByRole('dialog')).toHaveTextContent('Le document passera en attente de signature.');
    expect(validate).not.toHaveBeenCalled();

    await user.click(screen.getByRole('button', { name: 'Confirmer' }));
    await waitFor(() => expect(validate).toHaveBeenCalledWith(TOKEN, 'quittance-uuid'));
  });
});

describe('Client API Quittances', () => {
  beforeEach(() => {
    vi.restoreAllMocks();
    vi.unstubAllGlobals();
  });

  it('transmet le jeton tel quel, sans ajouter de jeton à l’URL', async () => {
    const fetchMock = vi.fn().mockResolvedValue({
      ok: true,
      status: 200,
      json: async () => EMPTY_PAGE,
    });
    vi.stubGlobal('fetch', fetchMock);

    await quittancesApi.list(TOKEN, { page: 2, taille: 10, bailId: 'BAIL-1' });

    const [url, init] = fetchMock.mock.calls[0];
    expect(url).toContain('/api/quittances/api/v1/quittances?');
    expect(url).toContain('page=2');
    expect(url).toContain('taille=10');
    expect(url).toContain('bailId=BAIL-1');
    expect(url).not.toContain(TOKEN);
    expect(init.headers.get('Authorization')).toBe(`Bearer ${TOKEN}`);
    expect(fetchMock).toHaveBeenCalledTimes(1);
  });

  it('envoie le corps exact de préparation sans montant calculé côté frontend', async () => {
    const fetchMock = vi.fn().mockResolvedValue({
      ok: true,
      status: 201,
      json: async () => ({ id: 'new-document-id' }),
    });
    vi.stubGlobal('fetch', fetchMock);

    await quittancesApi.createDraft(TOKEN, {
      bailId: 'BAIL-9',
      periodeDebut: '2025-02-01',
      periodeFin: '2025-02-28',
    });

    const [, init] = fetchMock.mock.calls[0];
    expect(init.method).toBe('POST');
    expect(init.headers.get('Authorization')).toBe(`Bearer ${TOKEN}`);
    expect(JSON.parse(init.body)).toEqual({
      bailId: 'BAIL-9',
      periodeDebut: '2025-02-01',
      periodeFin: '2025-02-28',
    });
  });

  it.each([
    [401, 'Session refusée'],
    [403, 'Accès refusé'],
    [409, 'Conflit métier'],
    [503, 'Service indisponible'],
  ])('expose explicitement la réponse HTTP %i', async (status, expectedMessage) => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue({
      ok: false,
      status,
      text: async () => JSON.stringify({ message: 'Réponse métier du serveur.' }),
    }));

    await expect(quittancesApi.validate(TOKEN, 'doc-id')).rejects.toMatchObject({
      status,
      message: 'Réponse métier du serveur.',
    });
    const { quittanceErrorMessage } = await import('../pages/quittances/quittancesUtils');
    await expect(quittancesApi.getDocument(TOKEN, 'doc-id')).rejects.toMatchObject({ status });
    expect(quittanceErrorMessage({ status, message: 'Réponse métier du serveur.' })).toContain(expectedMessage);
  });

  it('ne retente pas automatiquement une mutation après une erreur réseau', async () => {
    const fetchMock = vi.fn().mockRejectedValue(new Error('Réseau indisponible'));
    vi.stubGlobal('fetch', fetchMock);

    await expect(quittancesApi.cancel(TOKEN, 'doc-id', 'motif')).rejects.toThrow('Réseau indisponible');
    expect(fetchMock).toHaveBeenCalledTimes(1);
  });

  it('renvoie le binaire du PDF sans exposer le jeton dans son URL', async () => {
    const pdf = new Blob(['%PDF-1.7'], { type: 'application/pdf' });
    const fetchMock = vi.fn().mockResolvedValue({
      ok: true,
      status: 200,
      blob: async () => pdf,
    });
    vi.stubGlobal('fetch', fetchMock);

    await expect(quittancesApi.previewDraft(TOKEN, 'doc-id')).resolves.toBe(pdf);
    expect(fetchMock.mock.calls[0][0]).toBe('/api/quittances/api/v1/quittances/doc-id/preview');
    expect(fetchMock.mock.calls[0][0]).not.toContain(TOKEN);
    expect(fetchMock.mock.calls[0][1].headers.get('Authorization')).toBe(`Bearer ${TOKEN}`);
  });

  it('décode une erreur JSON renvoyée à la place du binaire', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue({
      ok: false,
      status: 503,
      text: async () => JSON.stringify({ message: 'Signature locale indisponible.' }),
    }));

    await expect(quittancesApi.downloadDemoPdf(TOKEN, 'doc-id')).rejects.toMatchObject({
      status: 503,
      message: 'Signature locale indisponible.',
      data: { message: 'Signature locale indisponible.' },
    });
  });
});
