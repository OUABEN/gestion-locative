import React from 'react';
import { NavLink } from 'react-router-dom';
import {
  LayoutDashboard,
  Building2,
  PlusCircle,
  Users,
  Wallet,
  FileCheck2,
  Sparkles,
  PanelLeftClose,
  PanelLeftOpen,
  ChevronRight,
  UserCheck,
} from 'lucide-react';
import { useAuth } from '../../context/AuthContext';
import styles from './Sidebar.module.css';

export const Sidebar = ({ isOpen, onToggle, onCloseMobile }) => {
  const { user, canWrite } = useAuth();

  return (
    <>
      {/* Overlay mobile */}
      {isOpen && <div className={styles.overlay} onClick={onToggle} aria-hidden="true" />}

      {/* Bouton flottant élégant pour rouvrir la sidebar quand elle est fermée */}
      <button
        type="button"
        onClick={onToggle}
        className={`${styles.floatingOpenBtn} ${!isOpen ? styles.floatingVisible : ''}`}
        aria-label="Ouvrir le panneau de navigation"
        title="Ouvrir la navigation"
      >
        <span className={styles.floatingLogo}>
          <PanelLeftOpen size={18} color="#0B0B0C" />
        </span>
        <span className={styles.floatingText}>
          LOCATIVE<b>.</b>
        </span>
        <ChevronRight size={15} className={styles.floatingArrow} />
      </button>

      <aside
        className={`${styles.sidebar} ${isOpen ? styles.open : styles.closed}`}
        aria-hidden={!isOpen}
      >
        {/* En-tête de la marque + Bouton Fermer */}
        <div className={styles.header}>
          <div className={styles.brand}>
            <div className={styles.logoIcon} aria-hidden="true">
              <Building2 size={22} color="#0B0B0C" strokeWidth={2.4} />
            </div>
            <div className={styles.brandText}>
              <span className={styles.brandTitle}>
                LOCATIVE<span className={styles.brandDot}>.</span>
              </span>
              <span className={styles.brandSubtitle}>Gestion Immobilière</span>
            </div>
          </div>

          <button
            type="button"
            className={styles.toggleSidebarBtn}
            onClick={onToggle}
            aria-label="Fermer le panneau latéral"
            title="Fermer le menu"
          >
            <PanelLeftClose size={18} />
          </button>
        </div>

        {/* Section Navigation Principale */}
        <div className={styles.navSectionLabel}>
          <span>NAVIGATION PRINCIPALE</span>
          <span className={styles.navLine} />
        </div>

        <nav className={styles.nav}>
          <NavLink
            to="/"
            end
            onClick={onCloseMobile}
            className={({ isActive }) => `${styles.navItem} ${isActive ? styles.active : ''}`}
          >
            <span className={styles.navIconBox}>
              <LayoutDashboard size={18} />
            </span>
            <span className={styles.navLabel}>Tableau de bord</span>
            <span className={styles.navPill}>Hub</span>
          </NavLink>

          <NavLink
            to="/biens"
            end
            onClick={onCloseMobile}
            className={({ isActive }) => `${styles.navItem} ${isActive ? styles.active : ''}`}
          >
            <span className={styles.navIconBox}>
              <Building2 size={18} />
            </span>
            <span className={styles.navLabel}>Biens</span>
            <span className={styles.navBadge}>Actif</span>
          </NavLink>

          {canWrite && (
            <NavLink
              to="/biens/nouveau"
              onClick={onCloseMobile}
              className={({ isActive }) => `${styles.subNavItem} ${isActive ? styles.subActive : ''}`}
            >
              <PlusCircle size={15} />
              <span>Ajouter un bien</span>
            </NavLink>
          )}

          <NavLink
            to="/locataires"
            end
            onClick={onCloseMobile}
            className={({ isActive }) => `${styles.navItem} ${isActive ? styles.active : ''}`}
          >
            <span className={styles.navIconBox}>
              <Users size={18} />
            </span>
            <span className={styles.navLabel}>Locataires</span>
          </NavLink>

          {canWrite && (
            <NavLink
              to="/locataires/nouveau"
              onClick={onCloseMobile}
              className={({ isActive }) => `${styles.subNavItem} ${isActive ? styles.subActive : ''}`}
            >
              <PlusCircle size={15} />
              <span>Nouveau locataire</span>
            </NavLink>
          )}

          {user?.role === 'LOCATAIRE' && (
            <NavLink
              to="/locataires/mon-espace"
              onClick={onCloseMobile}
              className={({ isActive }) => `${styles.subNavItem} ${isActive ? styles.subActive : ''}`}
            >
              <UserCheck size={15} />
              <span>Mon espace</span>
            </NavLink>
          )}

          <NavLink
            to="/loyers"
            onClick={onCloseMobile}
            className={({ isActive }) => `${styles.navItem} ${isActive ? styles.active : ''}`}
          >
            <span className={styles.navIconBox}>
              <Wallet size={18} />
            </span>
            <span className={styles.navLabel}>Loyers</span>
          </NavLink>

          <NavLink
            to="/loyers/historique"
            onClick={onCloseMobile}
            className={({ isActive }) => `${styles.subNavItem} ${isActive ? styles.subActive : ''}`}
          >
            <span>Historique des paiements</span>
          </NavLink>
          <NavLink
            to="/loyers/retards"
            onClick={onCloseMobile}
            className={({ isActive }) => `${styles.subNavItem} ${isActive ? styles.subActive : ''}`}
          >
            <span>Loyers en retard</span>
          </NavLink>

          <NavLink
            to="/quittances"
            onClick={onCloseMobile}
            className={({ isActive }) => `${styles.navItem} ${isActive ? styles.active : ''}`}
          >
            <span className={styles.navIconBox}>
              <FileCheck2 size={18} />
            </span>
            <span className={styles.navLabel}>Quittances</span>
          </NavLink>
        </nav>

        {/* Carte Parc Immobilier Pro */}
        <div className={styles.promoBox}>
          <div className={styles.promoTop}>
            <div className={styles.promoIcon}>
              <Sparkles size={15} color="#0B0B0C" />
            </div>
            <span className={styles.promoTag}>PRO</span>
          </div>
          <div className={styles.promoTitle}>Parc Immobilier Pro</div>
          <p className={styles.promoText}>
            Suivi analytique en temps réel de vos loyers et taux d&apos;occupation.
          </p>
        </div>

        {/* Pied de sidebar + bouton rapide de fermeture */}
        <div className={styles.footer}>
          <span className={styles.version}>v2.0 · Midnight &amp; Lime Edition</span>
          <button
            type="button"
            onClick={onToggle}
            className={styles.footerCollapseBtn}
            title="Masquer ce panneau"
          >
            Masquer
          </button>
        </div>
      </aside>
    </>
  );
};
