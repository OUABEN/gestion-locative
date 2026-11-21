import React from 'react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import axios, { AxiosError } from 'axios';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { authApi } from '../api/authApi';
import { biensApi } from '../api/biensApi';
import { httpClient } from '../api/httpClient';
import { quittancesApi } from '../api/quittancesApi';
import { AuthProvider, useAuth } from '../context/AuthContext';
import { ToastProvider } from '../context/ToastContext';
import { LoginPage } from '../pages/LoginPage';
import { decodeToken } from '../utils/jwt';

const originalAdapter = httpClient.defaults.adapter;

const encode = (value) =>
  btoa(JSON.stringify(value)).replace(/=/g, '').replace(/\+/g, '-').replace(/\//g, '_');

const createToken = (claims = {}) =>
  `${encode({ alg: 'HS256', typ: 'JWT' })}.${encode({
    sub: 'account-uuid',
    iat: Math.floor(Date.now() / 1000),
    exp: Math.floor(Date.now() / 1000) + 3600,
    ...claims,
  })}.server-signature`;

const renderLogin = () =>
  render(
    <MemoryRouter initialEntries={['/login']}>
      <ToastProvider>
        <AuthProvider>
          <Routes>
            <Route path="/login" element={<LoginPage />} />
            <Route path="/" element={<div>Application connectée</div>} />
          </Routes>
        </AuthProvider>
      </ToastProvider>
    </MemoryRouter>
  );

const SessionProbe = ({ onMutation, onQuittances }) => {
  const { user, sessionNotice, logout } = useAuth();

  return (
    <div>
      <p data-testid="session-role">{user?.role || 'sans session'}</p>
      {sessionNotice && <p role="status">{sessionNotice}</p>}
      <button type="button" onClick={logout}>Déconnexion</button>
      {onMutation && (
        <button type="button" onClick={onMutation}>Envoyer la mutation</button>
      )}
      {onQuittances && (
        <button type="button" onClick={onQuittances}>Vérifier identité Quittances</button>
      )}
    </div>
  );
};

describe('Authentification et session commune', () => {
  beforeEach(() => {
    sessionStorage.clear();
    localStorage.clear();
    httpClient.defaults.adapter = originalAdapter;
  });

  afterEach(() => {
    vi.restoreAllMocks();
    vi.unstubAllGlobals();
    httpClient.defaults.adapter = originalAdapter;
  });

  it('établit la session à partir de la réponse réelle attendue de /api/auth/login', async () => {
    const token = createToken({ role: 'LOCATAIRE', email: 'locataire@example.ma' });
    const response = {
      data: {
        token,
        user: {
          id: 'account-uuid',
          nom: 'Test',
          prenom: 'Locataire',
          email: 'locataire@example.ma',
          role: 'LOCATAIRE',
        },
      },
    };
    vi.spyOn(axios, 'post').mockResolvedValueOnce(response);

    const user = userEvent.setup();
    renderLogin();
    expect(screen.getByLabelText(/Adresse e-mail/i)).toHaveValue('');
    expect(screen.getByLabelText('Mot de passe')).toHaveValue('');
    await user.type(screen.getByLabelText(/Adresse e-mail/i), 'locataire@example.ma');
    await user.type(screen.getByLabelText('Mot de passe'), 'user-password');
    await user.click(screen.getByRole('button', { name: /Se connecter/i }));

    expect(await screen.findByText('Application connectée')).toBeInTheDocument();
    expect(axios.post).toHaveBeenCalledWith('/api/auth/login', {
      email: 'locataire@example.ma',
      password: 'user-password',
    });
    expect(sessionStorage.getItem('token')).toBe(token);
    expect(localStorage.getItem('token')).toBe(token);
  });

  it.each([
    ['401', { response: { status: 401, data: { message: 'Identifiants refusés.' } } }],
    ['500', { response: { status: 500, data: { message: 'Service indisponible.' } } }],
    ['réseau', new Error('Network Error')],
  ])('ne crée pas de session après un échec de connexion (%s)', async (_label, failure) => {
    vi.spyOn(axios, 'post').mockRejectedValueOnce(failure);
    const user = userEvent.setup();
    renderLogin();

    await user.type(screen.getByLabelText(/Adresse e-mail/i), 'user@example.ma');
    await user.type(screen.getByLabelText('Mot de passe'), 'user-password');
    await user.click(screen.getByRole('button', { name: /Se connecter/i }));

    expect(await screen.findByRole('alert')).toBeInTheDocument();
    expect(screen.queryByText('Application connectée')).not.toBeInTheDocument();
    expect(sessionStorage.getItem('token')).toBeNull();
    expect(localStorage.getItem('token')).toBeNull();
  });

  it('n’annonce pas une inscription réussie et ne crée pas de session après une erreur serveur', async () => {
    vi.spyOn(axios, 'post').mockRejectedValueOnce({
      response: { status: 500, data: { message: 'Inscription indisponible.' } },
    });
    const user = userEvent.setup();
    renderLogin();

    await user.click(screen.getByRole('button', { name: /Inscription/i }));
    expect(screen.getByLabelText(/Profil & Permissions/i)).toHaveValue('');
    await user.type(screen.getByLabelText(/Prénom/i), 'Sam');
    await user.type(screen.getByLabelText(/^Nom/i), 'Test');
    await user.type(screen.getByLabelText(/Adresse e-mail/i), 'sam@example.ma');
    await user.type(screen.getByLabelText('Mot de passe *'), 'user-password');
    await user.selectOptions(screen.getByLabelText(/Profil & Permissions/i), 'BAILLEUR');
    await user.click(screen.getByRole('button', { name: /Créer mon compte/i }));

    expect(await screen.findByRole('alert')).toHaveTextContent('Inscription indisponible.');
    expect(screen.queryByText('Application connectée')).not.toBeInTheDocument();
    expect(sessionStorage.getItem('token')).toBeNull();
    expect(localStorage.getItem('token')).toBeNull();
  });

  it('refuse les jetons sans rôle reconnu au lieu de choisir BAILLEUR', () => {
    expect(decodeToken(createToken())).toBeNull();
    expect(decodeToken(createToken({ role: 'ADMIN' }))).toBeNull();
    expect(decodeToken(createToken({ role: 'BAILLEUR', sub: undefined }))).toBeNull();
    expect(decodeToken(createToken({ role: 'BAILLEUR', exp: undefined }))).toBeNull();
    expect(decodeToken(createToken({ role: 'BAILLEUR', iat: undefined }))).toBeNull();
    expect(decodeToken(createToken({ role: 'GESTIONNAIRE' })).role).toBe('GESTIONNAIRE');
  });

  it('efface au démarrage une session expirée et invite à se reconnecter', () => {
    const expiredToken = createToken({
      role: 'BAILLEUR',
      exp: Math.floor(Date.now() / 1000) - 1,
    });
    sessionStorage.setItem('token', expiredToken);
    localStorage.setItem('token', expiredToken);

    render(
      <AuthProvider>
        <SessionProbe />
      </AuthProvider>
    );

    expect(screen.getByTestId('session-role')).toHaveTextContent('sans session');
    expect(screen.getByRole('status')).toHaveTextContent(/a expiré.*reconnecter/i);
    expect(sessionStorage.getItem('token')).toBeNull();
    expect(localStorage.getItem('token')).toBeNull();
  });

  it('transmet le jeton inchangé et ne rejoue pas une mutation après un 401', async () => {
    const token = createToken({ role: 'BAILLEUR', email: 'owner@example.ma' });
    sessionStorage.setItem('token', token);
    localStorage.setItem('token', token);
    const adapter = vi.fn((config) =>
      Promise.reject(
        new AxiosError('Unauthorized', 'ERR_BAD_REQUEST', config, null, {
          data: { message: 'Unauthorized' },
          status: 401,
          statusText: 'Unauthorized',
          headers: {},
          config,
        })
      )
    );
    httpClient.defaults.adapter = adapter;

    const onMutation = async () => {
      try {
        await httpClient.post('/biens', { adresse: 'Adresse de test' });
      } catch {
        // The rejected request is asserted through the session state below.
      }
    };
    const user = userEvent.setup();
    render(
      <AuthProvider>
        <SessionProbe onMutation={onMutation} />
      </AuthProvider>
    );

    expect(screen.getByTestId('session-role')).toHaveTextContent('BAILLEUR');
    await user.click(screen.getByRole('button', { name: /Envoyer la mutation/i }));
    await waitFor(() => {
      expect(screen.getByTestId('session-role')).toHaveTextContent('sans session');
      expect(screen.getByRole('status')).toHaveTextContent(/session a été refusée/i);
    });

    expect(adapter).toHaveBeenCalledTimes(1);
    expect(adapter.mock.calls[0][0].headers.Authorization).toBe(`Bearer ${token}`);
    expect(sessionStorage.getItem('token')).toBeNull();
    expect(localStorage.getItem('token')).toBeNull();
    expect(httpClient.defaults.baseURL).toBe(import.meta.env.VITE_API_BIENS_URL || '/api/biens');
  });

  it('ne masque pas les 401/403 de Biens par les replis locaux', async () => {
    const statuses = [401, 403];
    const adapter = vi.fn((config) => {
      const status = statuses.shift();
      return Promise.reject(
        new AxiosError(`HTTP ${status}`, 'ERR_BAD_REQUEST', config, null, {
          data: { message: `HTTP ${status}` },
          status,
          statusText: `HTTP ${status}`,
          headers: {},
          config,
        })
      );
    });
    httpClient.defaults.adapter = adapter;

    await expect(biensApi.getAll()).rejects.toMatchObject({ response: { status: 401 } });
    expect(localStorage.getItem('gl_biens_store_v2')).toBeNull();
    await expect(
      biensApi.create({
        adresse: 'Adresse de test',
        ville: 'Test',
        type: 'Appartement',
        surface: 50,
        prixMensuel: 1000,
      })
    ).rejects.toMatchObject({ response: { status: 403 } });

    expect(adapter).toHaveBeenCalledTimes(2);
    expect(localStorage.getItem('gl_biens_store_v2')).toBeNull();
  });

  it('ne transforme pas le 401 spécifique de Quittances en expiration de session commune', async () => {
    const token = createToken({ role: 'BAILLEUR', email: 'owner@example.ma' });
    sessionStorage.setItem('token', token);
    localStorage.setItem('token', token);
    vi.stubGlobal(
      'fetch',
      vi.fn().mockResolvedValue({
        ok: false,
        status: 401,
        text: async () => JSON.stringify({ message: 'Session refusée par Quittances.' }),
      })
    );
    const onQuittances = async () => {
      try {
        await quittancesApi.getIdentity(token);
      } catch {
        // Quittances reports its own authentication incompatibility.
      }
    };
    const user = userEvent.setup();

    render(
      <AuthProvider>
        <SessionProbe onQuittances={onQuittances} />
      </AuthProvider>
    );

    await user.click(screen.getByRole('button', { name: /Vérifier identité Quittances/i }));
    await waitFor(() => expect(fetch).toHaveBeenCalledTimes(1));
    expect(screen.getByTestId('session-role')).toHaveTextContent('BAILLEUR');
    expect(screen.queryByRole('status')).not.toBeInTheDocument();
    expect(sessionStorage.getItem('token')).toBe(token);
    expect(localStorage.getItem('token')).toBe(token);
  });

  it('synchronise le nettoyage du stockage lors de la déconnexion', async () => {
    const token = createToken({ role: 'GESTIONNAIRE', email: 'manager@example.ma' });
    sessionStorage.setItem('token', token);
    localStorage.setItem('token', token);
    const user = userEvent.setup();

    render(
      <AuthProvider>
        <SessionProbe />
      </AuthProvider>
    );

    expect(screen.getByTestId('session-role')).toHaveTextContent('GESTIONNAIRE');
    await user.click(screen.getByRole('button', { name: /Déconnexion/i }));
    expect(screen.getByTestId('session-role')).toHaveTextContent('sans session');
    expect(sessionStorage.getItem('token')).toBeNull();
    expect(localStorage.getItem('token')).toBeNull();
  });

  it('authApi retourne le corps serveur sans transformation ni réponse de secours', async () => {
    const serverBody = {
      token: createToken({ role: 'BAILLEUR', email: 'owner@example.ma' }),
      user: {
        id: 'account-uuid',
        nom: 'Test',
        prenom: 'Bailleur',
        email: 'owner@example.ma',
        role: 'BAILLEUR',
      },
    };
    vi.spyOn(axios, 'post').mockResolvedValueOnce({ data: serverBody });

    await expect(authApi.register({
      nom: 'Test',
      prenom: 'Bailleur',
      email: 'owner@example.ma',
      password: 'user-password',
      role: 'BAILLEUR',
    })).resolves.toEqual(serverBody);
    expect(axios.post).toHaveBeenCalledWith('/api/auth/register', {
      nom: 'Test',
      prenom: 'Bailleur',
      email: 'owner@example.ma',
      password: 'user-password',
      role: 'BAILLEUR',
    });
  });
});
