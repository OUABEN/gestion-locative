import React from 'react';
import { describe, it, expect, beforeEach, vi } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { locatairesApi } from '../api/locatairesApi';
import { LocatairesListPage } from '../pages/locataires/LocatairesListPage';
import { LocataireFormPage } from '../pages/locataires/LocataireFormPage';
import { AuthProvider } from '../context/AuthContext';
import { ToastProvider } from '../context/ToastContext';

describe('Frontend Module Locataires — Tests Unitaires & Composants (Vitest + RTL)', () => {
  beforeEach(() => {
    localStorage.clear();
    sessionStorage.clear();
  });

  describe('1. API Locataires (locatairesApi)', () => {
    it('getAll retourne la liste initiale des locataires', async () => {
      const locataires = await locatairesApi.getAll();
      expect(Array.isArray(locataires)).toBe(true);
      expect(locataires.length).toBeGreaterThanOrEqual(2);
      expect(locataires[0]).toHaveProperty('cin');
    });

    it('getById retrouve le locataire par identifiant', async () => {
      const all = await locatairesApi.getAll();
      const first = all[0];
      const found = await locatairesApi.getById(first.id);
      expect(found).toBeDefined();
      expect(found.cin).toBe(first.cin);
    });

    it('create refuse un CIN déjà existant avec une erreur 409', async () => {
      const all = await locatairesApi.getAll();
      const existingCin = all[0].cin;

      await expect(
        locatairesApi.create({
          nom: 'Test',
          prenom: 'Doublon',
          cin: existingCin,
          email: 'test@mail.ma',
          telephone: '0600000000',
        })
      ).rejects.toThrow(/CIN déjà utilisé/);
    });

    it('create ajoute un nouveau locataire avec CIN unique', async () => {
      const nouveau = await locatairesApi.create({
        nom: 'Alami',
        prenom: 'Rachid',
        cin: 'ZZ889900',
        email: 'rachid@mail.ma',
        telephone: '0669887766',
      });

      expect(nouveau).toBeDefined();
      expect(nouveau.cin).toBe('ZZ889900');
      const all = await locatairesApi.getAll();
      expect(all.some((l) => l.cin === 'ZZ889900')).toBe(true);
    });
  });

  describe('2. Pages & Composants Locataires', () => {
    it('LocatairesListPage affiche le titre et la barre de recherche', async () => {
      render(
        <MemoryRouter>
          <AuthProvider>
            <ToastProvider>
              <LocatairesListPage />
            </ToastProvider>
          </AuthProvider>
        </MemoryRouter>
      );

      expect(screen.getByText(/Répertoire des Locataires/i)).toBeInTheDocument();
      expect(
        screen.getByPlaceholderText(/Rechercher par CIN/i)
      ).toBeInTheDocument();
    });

    it('LocataireFormPage affiche les champs obligatoires et le titre', async () => {
      render(
        <MemoryRouter>
          <AuthProvider>
            <ToastProvider>
              <LocataireFormPage />
            </ToastProvider>
          </AuthProvider>
        </MemoryRouter>
      );

      expect(
        screen.getByText(/Créer une nouvelle fiche locataire/i)
      ).toBeInTheDocument();
      expect(screen.getByLabelText(/Nom de famille/i)).toBeInTheDocument();
      expect(screen.getByLabelText(/Prénom/i)).toBeInTheDocument();
      expect(screen.getByLabelText(/Numéro de CIN/i)).toBeInTheDocument();
    });
  });
});