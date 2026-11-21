import React from 'react';
import { NavLink } from 'react-router-dom';
import {
  LogOut,
  UserCircle,
  LayoutDashboard,
  Building2,
  PlusCircle,
  PanelLeftClose,
  PanelLeftOpen,
} from 'lucide-react';
import { useAuth } from '../../context/AuthContext';
import styles from './Topbar.module.css';

export const Topbar = ({ isSidebarOpen, onToggleSidebar }) => {
  const { user, canWrite, logout } = useAuth();

  return (
    <header className={styles.topbar}>
      {/* Bouton toujours visible pour Ouvrir ou Fermer le menu latéral */}
      <button
        type="button"
        className={`${styles.sidebarToggleBtn} ${!isSidebarOpen ? styles.sidebarToggleClosed : ''}`}
        onClick={onToggleSidebar}
        aria-label={isSidebarOpen ? 'Fermer le menu latéral' : 'Ouvrir le menu latéral'}
        title={isSidebarOpen ? 'Masquer la navigation' : 'Ouvrir la navigation'}
      >
        {isSidebarOpen ? <PanelLeftClose size={18} /> : <PanelLeftOpen size={18} />}
        <span>{isSidebarOpen ? 'Masquer menu' : 'Ouvrir menu'}</span>
      </button>

      {/* Sélecteur rapide des 3 vues clés */}
      <nav className={styles.quickSwitch} aria-label="Accès rapide">
        <NavLink
          to="/"
          end
          className={({ isActive }) => `${styles.switchItem} ${isActive ? styles.switchActive : ''}`}
        >
          <LayoutDashboard size={15} />
          <span>Tableau de bord</span>
        </NavLink>

        <NavLink
          to="/biens"
          end
          className={({ isActive }) => `${styles.switchItem} ${isActive ? styles.switchActive : ''}`}
        >
          <Building2 size={15} />
          <span>Afficher les biens</span>
        </NavLink>

        {canWrite && (
          <NavLink
            to="/biens/nouveau"
            className={({ isActive }) => `${styles.switchItem} ${isActive ? styles.switchActive : ''}`}
          >
            <PlusCircle size={15} />
            <span>Ajouter un bien</span>
          </NavLink>
        )}
      </nav>

      <div className={styles.spacer} />

      <div className={styles.userSection}>
        <div className={styles.userInfo}>
          <div className={styles.avatarBox}>
            <UserCircle size={22} />
          </div>
          <div className={styles.userDetails}>
            <span className={styles.userRoleBadge}>{user?.role || '-'}</span>
            <span className={styles.userEmail}>{user?.email || '-'}</span>
          </div>
        </div>

        <button onClick={logout} className={styles.logoutBtn} title="Se déconnecter" aria-label="Se déconnecter">
          <LogOut size={16} />
          <span className={styles.logoutText}>Déconnexion</span>
        </button>
      </div>
    </header>
  );
};
