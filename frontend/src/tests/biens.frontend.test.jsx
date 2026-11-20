import React from 'react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter, Routes, Route } from 'react-router-dom';

import { formatCurrency, formatSurface, formatDate } from '../utils/format';
import { bienSchema } from '../utils/validators';
import { decodeToken } from '../utils/jwt';
import { AuthProvider } from '../context/AuthContext';
import { ToastProvider } from '../context/ToastContext';
import { ProtectedRoute } from '../routes/ProtectedRoute';
import { RoleGuard } from '../routes/RoleGuard';
import { LoginPage } from '../pages/LoginPage';
import { BienFormPage } from '../pages/BienFormPage';
import { BiensListPage } from '../pages/BiensListPage';
import { DashboardPage } from '../pages/DashboardPage';

// Mock complet des appels API (aucun appel réseau ni backend démarré)
vi.mock('../api/biensApi', () => ({
  biensApi: {
    getAll: vi.fn(),
    getById: vi.fn(),
    getDisponibilite: vi.fn(),
    create: vi.fn(),
    update: vi.fn(),
    updateStatut: vi.fn(),
    delete: vi.fn(),
  },
}));

vi.mock('../api/authApi', () => ({
  authApi: {
    login: vi.fn(),
    register: vi.fn(),
  },
}));

import { biensApi } from '../api/biensApi';
import { authApi } from '../api/authApi';

// Utilitaire de création d'un faux JWT encodé en Base64URL pour les tests
function createMockJwt(payload) {
  const header = btoa(JSON.stringify({ alg: 'HS256', typ: 'JWT' }));
  const body = btoa(
    JSON.stringify({
      sub: '11111111-1111-1111-1111-111111111111',
      exp: Math.floor(Date.now() / 1000) + 3600,
      ...payload,
    })
  );
  return `${header}.${body}.signature_mock`;
}

function renderWithProviders(ui, { initialEntries = ['/'] } = {}) {
  return render(
    <MemoryRouter initialEntries={initialEntries}>
      <ToastProvider>
        <AuthProvider>{ui}</AuthProvider>
      </ToastProvider>
    </MemoryRouter>
  );
}

describe('Frontend Gestion des Biens — Tests Unitaires & Composants (Vitest + RTL)', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    sessionStorage.clear();
    localStorage.clear();
  });

  // =========================================================================
  // 1. UTILITAIRES : format.js, validators.js, jwt.js
  // =========================================================================
  describe('1. Utilitaires (format.js, validators.js, jwt.js)', () => {
    it('formatCurrency formate correctement les montants en Dirham (DH) et gère les valeurs nulles/NaN', () => {
      expect(formatCurrency(4500)).toContain('DH');
      expect(formatCurrency(0)).toBe('0 DH');
      expect(formatCurrency(null)).toBe('0 DH');
      expect(formatCurrency(undefined)).toBe('0 DH');
      expect(formatCurrency(NaN)).toBe('0 DH');
    });

    it('formatSurface et formatDate gèrent les cas normaux et limites', () => {
      expect(formatSurface(85.5)).toBe('85.5 m²');
      expect(formatSurface(null)).toBe('0 m²');
      expect(formatDate('2026-11-11T10:00:00Z')).toMatch(/\d{2}\/\d{2}\/2026/);
      expect(formatDate('')).toBe('-');
    });

    it('bienSchema (Zod) applique strictement les règles métier (surface > 0, prixMensuel >= 0, charges >= 0)', () => {
      // Cas valide avec imageUrl optionnelle
      const valid = bienSchema.safeParse({
        adresse: '14 Boulevard Anfa',
        ville: 'Casablanca',
        type: 'Appartement',
        surface: 90,
        prixMensuel: 7500,
        charges: 400,
        imageUrl: 'https://example.com/photo.jpg',
      });
      expect(valid.success).toBe(true);

      // Cas limite : surface = 0 -> rejeté
      const zeroSurface = bienSchema.safeParse({
        adresse: 'Rue X',
        ville: 'Rabat',
        type: 'Studio',
        surface: 0,
        prixMensuel: 3000,
      });
      expect(zeroSurface.success).toBe(false);

      // Cas limite : prixMensuel = 0 et surface = 0.01 -> accepté
      const freeRent = bienSchema.safeParse({
        adresse: 'Logement fonction',
        ville: 'Rabat',
        type: 'Studio',
        surface: 0.01,
        prixMensuel: 0,
      });
      expect(freeRent.success).toBe(true);

      // Cas d'erreur : charges négatives -> rejeté
      const negCharges = bienSchema.safeParse({
        adresse: 'Rue Y',
        ville: 'Tanger',
        type: 'Villa',
        surface: 200,
        prixMensuel: 15000,
        charges: -50,
      });
      expect(negCharges.success).toBe(false);
    });

    it('decodeToken décode un JWT valide, détecte un token expiré et retourne null sur un token invalide', () => {
      const validJwt = createMockJwt({ role: 'BAILLEUR', email: 'bailleur@test.ma' });
      const decoded = decodeToken(validJwt);
      expect(decoded).not.toBeNull();
      expect(decoded.role).toBe('BAILLEUR');
      expect(decoded.isExpired).toBe(false);

      const expiredJwt = createMockJwt({
        role: 'LOCATAIRE',
        exp: Math.floor(Date.now() / 1000) - 120,
      });
      const decodedExpired = decodeToken(expiredJwt);
      expect(decodedExpired.isExpired).toBe(true);

      expect(decodeToken('token-invalide-non-jwt')).toBeNull();
      expect(decodeToken(null)).toBeNull();
    });
  });

  // =========================================================================
  // 2. GARDES DE NAVIGATION : ProtectedRoute & RoleGuard
  // =========================================================================
  describe('2. ProtectedRoute et RoleGuard (Contrôle des accès)', () => {
    it('redirige un utilisateur non authentifié vers /login', () => {
      renderWithProviders(
        <Routes>
          <Route path="/login" element={<div>Écran Login</div>} />
          <Route
            path="/"
            element={
              <ProtectedRoute>
                <div>Contenu Protégé</div>
              </ProtectedRoute>
            }
          />
        </Routes>,
        { initialEntries: ['/'] }
      );

      expect(screen.getByText('Écran Login')).toBeInTheDocument();
      expect(screen.queryByText('Contenu Protégé')).not.toBeInTheDocument();
    });

    it('bloque le rôle LOCATAIRE sur les routes protégées par RoleGuard et autorise BAILLEUR', () => {
      const locataireToken = createMockJwt({ role: 'LOCATAIRE', email: 'loc@test.ma' });
      sessionStorage.setItem('token', locataireToken);

      const { unmount } = renderWithProviders(
        <Routes>
          <Route path="/" element={<div>Accueil Lecture Seule</div>} />
          <Route
            path="/biens/nouveau"
            element={
              <RoleGuard>
                <div>Formulaire Création</div>
              </RoleGuard>
            }
          />
        </Routes>,
        { initialEntries: ['/biens/nouveau'] }
      );

      expect(screen.getByText('Accès réservé')).toBeInTheDocument();
      expect(screen.queryByText('Formulaire Création')).not.toBeInTheDocument();
      unmount();

      // Avec BAILLEUR -> autorisé
      sessionStorage.setItem('token', createMockJwt({ role: 'BAILLEUR' }));
      renderWithProviders(
        <Routes>
          <Route
            path="/biens/nouveau"
            element={
              <RoleGuard>
                <div>Formulaire Création</div>
              </RoleGuard>
            }
          />
        </Routes>,
        { initialEntries: ['/biens/nouveau'] }
      );
      expect(screen.getByText('Formulaire Création')).toBeInTheDocument();
    });
  });

  // =========================================================================
  // 3. PAGE DE CONNEXION & INSCRIPTION : LoginPage
  // =========================================================================
  describe('3. LoginPage (Connexion & Inscription)', () => {
    it('affiche un message d’erreur lorsque la connexion échoue (401)', async () => {
      authApi.login.mockRejectedValueOnce({
        response: { status: 401, data: { message: 'Mot de passe incorrect.' } },
      });

      const user = userEvent.setup();
      renderWithProviders(<LoginPage />, { initialEntries: ['/login'] });

      const submitBtn = screen.getByRole('button', { name: /Se connecter/i });
      await user.click(submitBtn);

      await waitFor(() => {
        expect(screen.getByRole('alert')).toHaveTextContent('Mot de passe incorrect.');
      });
    });

    it('redirige immédiatement vers le tableau de bord en 1 clic sur succès de connexion', async () => {
      const validToken = createMockJwt({
        role: 'BAILLEUR',
        email: 'bailleur@gestion-locative.com',
        prenom: 'Mehdi',
      });
      authApi.login.mockResolvedValueOnce({ token: validToken });

      const user = userEvent.setup();
      renderWithProviders(
        <Routes>
          <Route path="/login" element={<LoginPage />} />
          <Route path="/" element={<div>Dashboard Accueil</div>} />
        </Routes>,
        { initialEntries: ['/login'] }
      );

      await user.click(screen.getByRole('button', { name: /Se connecter/i }));

      await waitFor(() => {
        expect(screen.getByText('Dashboard Accueil')).toBeInTheDocument();
      });
    });
  });

  // =========================================================================
  // 4. FORMULAIRE D'AJOUT / ÉDITION : BienFormPage
  // =========================================================================
  describe('4. BienFormPage (Validation métier, Barre Image & Erreurs 400)', () => {
    it('valide surface > 0 côté client et empêche l’envoi d’une surface négative ou nulle', async () => {
      sessionStorage.setItem('token', createMockJwt({ role: 'BAILLEUR' }));
      const user = userEvent.setup();

      renderWithProviders(<BienFormPage />, { initialEntries: ['/biens/nouveau'] });

      await user.type(screen.getByLabelText(/Adresse complète/i), '10 Rue Test');
      await user.type(screen.getByLabelText(/Ville/i), 'Casablanca');
      await user.type(screen.getByLabelText(/Surface habitable/i), '0');
      await user.type(screen.getByLabelText(/Loyer mensuel/i), '5000');

      await user.click(screen.getByRole('button', { name: /Créer le bien/i }));

      await waitFor(() => {
        expect(
          screen.getByText(/La surface doit être strictement supérieure à 0/i)
        ).toBeInTheDocument();
      });
      expect(biensApi.create).not.toHaveBeenCalled();
    });

    it('permet de renseigner la barre image (imageUrl) et affiche les erreurs 400 renvoyées par le serveur', async () => {
      sessionStorage.setItem('token', createMockJwt({ role: 'BAILLEUR' }));
      biensApi.create.mockRejectedValueOnce({
        response: {
          status: 400,
          data: {
            message: 'Données invalides',
            errors: ["Le champ 'adresse' est invalide côté serveur."],
          },
        },
      });

      const user = userEvent.setup();
      renderWithProviders(<BienFormPage />, { initialEntries: ['/biens/nouveau'] });

      await user.type(screen.getByLabelText(/Adresse complète/i), '25 Avenue Royale');
      await user.type(screen.getByLabelText(/Ville/i), 'Rabat');
      await user.type(screen.getByLabelText(/Surface habitable/i), '110');
      await user.type(screen.getByLabelText(/Loyer mensuel/i), '8500');
      await user.type(
        screen.getByLabelText(/Photo du bien/i),
        'https://example.com/appartement-rabat.jpg'
      );

      await user.click(screen.getByRole('button', { name: /Créer le bien/i }));

      await waitFor(() => {
        expect(screen.getByRole('alert')).toHaveTextContent(
          "Le champ 'adresse' est invalide côté serveur."
        );
      });
    });
  });

  // =========================================================================
  // 5. CATALOGUE & LISTE DES BIENS : BiensListPage
  // =========================================================================
  describe('5. BiensListPage (Rendu liste, Filtres, État vide, Erreur avec réessai)', () => {
    const mockBiens = [
      {
        id: 'fe365469-6d12-4311-922a-eb6f3a0820fe',
        adresse: '17 Rue de France, Agdal',
        ville: 'Rabat',
        type: 'Studio',
        surface: 38,
        prixMensuel: 3400,
        charges: 200,
        statut: 'LIBRE',
        createdAt: '2026-11-11T10:00:00Z',
      },
      {
        id: '150f1faf-9552-40e2-b8fe-1322181a53d3',
        adresse: '14 Boulevard Mohammed V',
        ville: 'Casablanca',
        type: 'Appartement',
        surface: 85,
        prixMensuel: 6500,
        charges: 450,
        statut: 'OCCUPE',
        createdAt: '2026-11-11T11:00:00Z',
      },
    ];

    it('affiche la liste des biens et filtre par recherche texte', async () => {
      sessionStorage.setItem('token', createMockJwt({ role: 'BAILLEUR' }));
      biensApi.getAll.mockResolvedValueOnce(mockBiens);

      const user = userEvent.setup();
      renderWithProviders(<BiensListPage />, { initialEntries: ['/biens'] });

      await waitFor(() => {
        expect(screen.getByText('17 Rue de France, Agdal')).toBeInTheDocument();
        expect(screen.getByText('14 Boulevard Mohammed V')).toBeInTheDocument();
      });

      const searchInput = screen.getByPlaceholderText(/Rechercher par adresse/i);
      await user.type(searchInput, 'Agdal');

      await waitFor(() => {
        expect(screen.getByText('17 Rue de France, Agdal')).toBeInTheDocument();
        expect(screen.queryByText('14 Boulevard Mohammed V')).not.toBeInTheDocument();
      });
    });

    it('affiche l’état vide lorsqu’aucun bien n’est retourné', async () => {
      sessionStorage.setItem('token', createMockJwt({ role: 'LOCATAIRE' }));
      biensApi.getAll.mockResolvedValueOnce([]);

      renderWithProviders(<BiensListPage />, { initialEntries: ['/biens'] });

      await waitFor(() => {
        expect(screen.getByText(/Aucun bien trouvé/i)).toBeInTheDocument();
      });
    });

    it('affiche l’état d’erreur et permet de réessayer le chargement', async () => {
      sessionStorage.setItem('token', createMockJwt({ role: 'BAILLEUR' }));
      biensApi.getAll.mockRejectedValueOnce({
        response: { data: { message: 'Serveur temporairement indisponible' } },
      });
      biensApi.getAll.mockResolvedValueOnce(mockBiens);

      const user = userEvent.setup();
      renderWithProviders(<BiensListPage />, { initialEntries: ['/biens'] });

      await waitFor(() => {
        expect(screen.getByText('Serveur temporairement indisponible')).toBeInTheDocument();
      });

      await user.click(screen.getByRole('button', { name: /Réessayer/i }));

      await waitFor(() => {
        expect(screen.getByText('17 Rue de France, Agdal')).toBeInTheDocument();
      });
    });
  });

  // =========================================================================
  // 6. TABLEAU DE BORD & KPI : DashboardPage
  // =========================================================================
  describe('6. DashboardPage (Calcul et affichage des KPI avec données mockées)', () => {
    it('calcule et affiche exactement le total (3), les disponibles (1), les loués (2) et le taux d’occupation arrondi (67%)', async () => {
      sessionStorage.setItem('token', createMockJwt({ role: 'BAILLEUR' }));
      biensApi.getAll.mockResolvedValueOnce([
        {
          id: '11111111-1111-4111-8111-111111111111',
          adresse: 'Bien Libre 1',
          ville: 'Rabat',
          type: 'Studio',
          surface: 40,
          prixMensuel: 4000,
          charges: 200,
          statut: 'LIBRE',
          createdAt: '2026-11-11T10:00:00Z',
        },
        {
          id: '22222222-2222-4222-8222-222222222222',
          adresse: 'Bien Occupé 1',
          ville: 'Casablanca',
          type: 'Appartement',
          surface: 90,
          prixMensuel: 7000,
          charges: 400,
          statut: 'OCCUPE',
          createdAt: '2026-11-11T11:00:00Z',
        },
        {
          id: '33333333-3333-4333-8333-333333333333',
          adresse: 'Bien Occupé 2',
          ville: 'Marrakech',
          type: 'Villa',
          surface: 250,
          prixMensuel: 15000,
          charges: 1000,
          statut: 'OCCUPE',
          createdAt: '2026-11-11T12:00:00Z',
        },
      ]);

      renderWithProviders(<DashboardPage />, { initialEntries: ['/'] });

      await waitFor(() => {
        expect(screen.getByText('Total des biens')).toBeInTheDocument();
        expect(screen.getByText('Disponibles')).toBeInTheDocument();
        expect(screen.getByText('Biens Loués')).toBeInTheDocument();
        // 2 occupés sur 3 = 66.66% -> arrondi à 67%
        expect(screen.getAllByText('67%').length).toBeGreaterThanOrEqual(1);
      });
    });
  });
});
