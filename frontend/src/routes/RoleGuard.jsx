import React from 'react';
import { useAuth } from '../context/AuthContext';
import { ShieldAlert } from 'lucide-react';
import { Link } from 'react-router-dom';

export const RoleGuard = ({ children, allowedRoles = ['BAILLEUR', 'GESTIONNAIRE'] }) => {
  const { user } = useAuth();

  const isAllowed = user && allowedRoles.includes(user.role);

  if (!isAllowed) {
    return (
      <div
        style={{
          padding: '48px 24px',
          maxWidth: '600px',
          margin: '40px auto',
          backgroundColor: 'var(--color-white)',
          borderRadius: 'var(--radius-lg)',
          boxShadow: 'var(--shadow-md)',
          textAlign: 'center',
        }}
      >
        <ShieldAlert size={48} color="var(--color-brown)" style={{ marginBottom: '16px' }} />
        <h2 style={{ marginBottom: '12px', color: 'var(--color-brown)' }}>Accès réservé</h2>
        <p style={{ color: 'var(--color-brown-dark)', marginBottom: '24px' }}>
          Votre rôle actuel (<strong>{user?.role || 'LOCATAIRE'}</strong>) ne vous permet pas d'accéder
          aux fonctionnalités de gestion ou de modification des biens immobiliers.
        </p>
        <Link
          to="/biens"
          style={{
            display: 'inline-block',
            backgroundColor: 'var(--color-green)',
            color: 'var(--color-white)',
            padding: '10px 20px',
            borderRadius: 'var(--radius-md)',
            fontWeight: 600,
          }}
        >
          Retourner au catalogue des biens
        </Link>
      </div>
    );
  }

  return children;
};

