import React from 'react';
import { Routes, Route, Navigate } from 'react-router-dom';
import { AppLayout } from './components/layout/AppLayout';
import { ProtectedRoute } from './routes/ProtectedRoute';
import { RoleGuard } from './routes/RoleGuard';

import { LoginPage } from './pages/LoginPage';
import { DashboardPage } from './pages/DashboardPage';
import { BiensListPage } from './pages/BiensListPage';
import { BienDetailPage } from './pages/BienDetailPage';
import { BienFormPage } from './pages/BienFormPage';
import { ModuleHubPage } from './pages/ModuleHubPage';
import { QuittancesListPage } from './pages/quittances/QuittancesListPage';
import { QuittanceCreatePage } from './pages/quittances/QuittanceCreatePage';
import { QuittanceDetailPage } from './pages/quittances/QuittanceDetailPage';
import { NotFoundPage } from './pages/NotFoundPage';

export const App = () => {
  return (
    <Routes>
      {/* Route publique d'authentification */}
      <Route path="/login" element={<LoginPage />} />

      {/* Routes protégées intégrées au Layout principal */}
      <Route
        path="/"
        element={
          <ProtectedRoute>
            <AppLayout />
          </ProtectedRoute>
        }
      >
        <Route index element={<DashboardPage />} />
        <Route path="biens" element={<BiensListPage />} />
        <Route path="biens/:id" element={<BienDetailPage />} />

        {/* Routes d'écriture réservées aux rôles BAILLEUR et GESTIONNAIRE */}
        <Route
          path="biens/nouveau"
          element={
            <RoleGuard>
              <BienFormPage />
            </RoleGuard>
          }
        />
        <Route
          path="biens/:id/modifier"
          element={
            <RoleGuard>
              <BienFormPage />
            </RoleGuard>
          }
        />

        {/* Modules complémentaires de la plateforme Gestion Locative */}
        <Route path="locataires" element={<ModuleHubPage moduleKey="locataires" />} />
        <Route path="loyers" element={<ModuleHubPage moduleKey="loyers" />} />
        <Route path="quittances" element={<QuittancesListPage />} />
        <Route path="quittances/nouveau" element={<QuittanceCreatePage />} />
        <Route path="quittances/:id" element={<QuittanceDetailPage />} />

        {/* 404 */}
        <Route path="*" element={<NotFoundPage />} />
      </Route>

      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
};
