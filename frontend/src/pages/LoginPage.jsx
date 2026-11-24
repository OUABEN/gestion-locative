import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { useToast } from '../context/ToastContext';
import { authApi } from '../api/authApi';
import { Card } from '../components/ui/Card';
import { Button } from '../components/ui/Button';
import {
  Mail,
  Lock,
  Eye,
  EyeOff,
  Building2,
  Sparkles,
  UserPlus,
  LogIn,
  User,
  ShieldCheck,
} from 'lucide-react';
import { getHeroImage } from '../utils/bienImage';
import styles from './LoginPage.module.css';

export const LoginPage = () => {
  const navigate = useNavigate();
  const { loginWithToken, sessionNotice } = useAuth();
  const { addToast } = useToast();

  // 'login' | 'register'
  const [mode, setMode] = useState('login');

  // États Connexion
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');

  // États Inscription
  const [nom, setNom] = useState('');
  const [prenom, setPrenom] = useState('');
  const [regEmail, setRegEmail] = useState('');
  const [regPassword, setRegPassword] = useState('');
  const [regRole, setRegRole] = useState('');

  const [showPassword, setShowPassword] = useState(false);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  // Connexion en 1 clic par Email & Mot de passe
  const handleLoginSubmit = async (e) => {
    e.preventDefault();
    setError('');
    if (!email || !password) {
      setError('Veuillez renseigner votre adresse e-mail et votre mot de passe.');
      return;
    }

    setLoading(true);
    try {
      const data = await authApi.login(email, password);
      if (data?.token) {
        const loggedUser = loginWithToken(data.token);
        const displayName = loggedUser.prenom || loggedUser.email?.split('@')[0] || loggedUser.role;
        addToast(`Bienvenue ${displayName} ! Connecté en tant que ${loggedUser.role}.`);
        navigate('/', { replace: true });
      } else {
        throw new Error("Réponse inattendue du serveur d'authentification.");
      }
    } catch (err) {
      const msg = err.response?.data?.message || err.message || 'Identifiants incorrects.';
      setError(msg);
    } finally {
      setLoading(false);
    }
  };

  // Inscription d'un nouvel utilisateur + entrée directe au Dashboard
  const handleRegisterSubmit = async (e) => {
    e.preventDefault();
    setError('');
    if (!nom.trim() || !prenom.trim() || !regEmail.trim() || !regPassword || !regRole) {
      setError('Veuillez remplir tous les champs obligatoires.');
      return;
    }
    if (regPassword.length < 4) {
      setError('Le mot de passe doit contenir au moins 4 caractères.');
      return;
    }

    setLoading(true);
    try {
      const data = await authApi.register({
        nom: nom.trim(),
        prenom: prenom.trim(),
        email: regEmail.trim(),
        password: regPassword,
        role: regRole,
      });
      if (data?.token) {
        const createdUser = loginWithToken(data.token);
        addToast(`Compte créé avec succès ! Bienvenue ${prenom.trim()} (${createdUser.role}).`);
        navigate('/', { replace: true });
      } else {
        throw new Error("Impossible de finaliser l'inscription.");
      }
    } catch (err) {
      const msg =
        err.response?.data?.message ||
        err.message ||
        "Une erreur est survenue lors de la création du compte.";
      setError(msg);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className={styles.container}>
      <div className={styles.leftPane}>
        <img src={getHeroImage()} alt="" className={styles.leftBg} aria-hidden="true" />
        <div className={styles.leftOverlay} />
        <div className={styles.illustrationWrapper}>
          <div className={styles.brandBadge}>
            <Sparkles size={14} /> PLATEFORME GLOBALE GESTION LOCATIVE
          </div>
          <div className={styles.branding}>
            <div className={styles.logoRow}>
              <div className={styles.logoBox}>
                <Building2 size={26} color="#0B0B0C" />
              </div>
              <span>
                LOCATIVE<b className={styles.limeDot}>.</b>
              </span>
            </div>
            <h1 className={styles.brandTitle}>
              Pilotez votre parc immobilier en <span className={styles.limeText}>temps réel</span>.
            </h1>
            <p className={styles.brandTagline}>
              Authentification sécurisée, tableau de bord analytique, catalogue haute définition et gestion complète des biens, locataires, loyers et quittances.
            </p>
          </div>
        </div>
      </div>

      <div className={styles.rightPane}>
        <Card className={styles.loginCard}>
          <div className={styles.cardHeader}>
            <h2 className={styles.cardTitle}>
              {mode === 'register' ? 'Créer un compte' : 'Espace de Connexion'}
            </h2>
            <p className={styles.cardSubtitle}>
              {mode === 'register'
                ? 'Inscrivez-vous pour accéder immédiatement au tableau de bord.'
                : 'Connectez-vous avec votre compte pour accéder à la plateforme.'}
            </p>
          </div>

          {/* Onglets : Connexion | Inscription */}
          <div className={styles.modeTabs} role="tablist">
            <button
              type="button"
              className={`${styles.tabBtn} ${mode === 'login' ? styles.activeTab : ''}`}
              onClick={() => {
                setMode('login');
                setError('');
              }}
            >
              <LogIn size={14} />
              <span>Connexion</span>
            </button>
            <button
              type="button"
              className={`${styles.tabBtn} ${mode === 'register' ? styles.activeTab : ''}`}
              onClick={() => {
                setMode('register');
                setError('');
              }}
            >
              <UserPlus size={14} />
              <span>Inscription</span>
            </button>
          </div>

          {sessionNotice && (
            <div className={styles.errorAlert} role="alert">
              {sessionNotice}
            </div>
          )}
          {error && (
            <div className={styles.errorAlert} role="alert">
              {error}
            </div>
          )}

          {/* ==================== MODE 1 : CONNEXION ==================== */}
          {mode === 'login' && (
            <form onSubmit={handleLoginSubmit} className={styles.form}>
              <div className={styles.formGroup}>
                <label htmlFor="email" className={styles.label}>
                  Adresse e-mail
                </label>
                <div className={styles.inputWrapper}>
                  <Mail size={18} className={styles.inputIcon} />
                  <input
                    type="email"
                    id="email"
                    value={email}
                    onChange={(e) => setEmail(e.target.value)}
                    placeholder="nom@exemple.ma"
                    className={styles.input}
                    required
                  />
                </div>
              </div>

              <div className={styles.formGroup}>
                <label htmlFor="password" className={styles.label}>
                  Mot de passe
                </label>
                <div className={styles.inputWrapper}>
                  <Lock size={18} className={styles.inputIcon} />
                  <input
                    type={showPassword ? 'text' : 'password'}
                    id="password"
                    value={password}
                    onChange={(e) => setPassword(e.target.value)}
                    placeholder="••••••••"
                    className={styles.input}
                    required
                  />
                  <button
                    type="button"
                    onClick={() => setShowPassword(!showPassword)}
                    className={styles.eyeBtn}
                    aria-label={showPassword ? 'Masquer le mot de passe' : 'Afficher le mot de passe'}
                  >
                    {showPassword ? <EyeOff size={18} /> : <Eye size={18} />}
                  </button>
                </div>
              </div>

              <Button
                type="submit"
                variant="primary"
                size="lg"
                disabled={loading}
                className={styles.submitBtn}
              >
                {loading ? 'Ouverture du tableau de bord...' : 'Se connecter'}
              </Button>

            </form>
          )}

          {/* ==================== MODE 2 : INSCRIPTION ==================== */}
          {mode === 'register' && (
            <form onSubmit={handleRegisterSubmit} className={styles.form}>
              <div className={styles.rowTwo}>
                <div className={styles.formGroup}>
                  <label htmlFor="reg-prenom" className={styles.label}>
                    Prénom *
                  </label>
                  <div className={styles.inputWrapper}>
                    <User size={17} className={styles.inputIcon} />
                    <input
                      type="text"
                      id="reg-prenom"
                      value={prenom}
                      onChange={(e) => setPrenom(e.target.value)}
                      placeholder="Ex : Youssef"
                      className={styles.input}
                      required
                    />
                  </div>
                </div>

                <div className={styles.formGroup}>
                  <label htmlFor="reg-nom" className={styles.label}>
                    Nom *
                  </label>
                  <div className={styles.inputWrapper}>
                    <User size={17} className={styles.inputIcon} />
                    <input
                      type="text"
                      id="reg-nom"
                      value={nom}
                      onChange={(e) => setNom(e.target.value)}
                      placeholder="Ex : Ouali"
                      className={styles.input}
                      required
                    />
                  </div>
                </div>
              </div>

              <div className={styles.formGroup}>
                <label htmlFor="reg-email" className={styles.label}>
                  Adresse e-mail *
                </label>
                <div className={styles.inputWrapper}>
                  <Mail size={18} className={styles.inputIcon} />
                  <input
                    type="email"
                    id="reg-email"
                    value={regEmail}
                    onChange={(e) => setRegEmail(e.target.value)}
                    placeholder="votre.email@exemple.ma"
                    className={styles.input}
                    required
                  />
                </div>
              </div>

              <div className={styles.formGroup}>
                <label htmlFor="reg-password" className={styles.label}>
                  Mot de passe *
                </label>
                <div className={styles.inputWrapper}>
                  <Lock size={18} className={styles.inputIcon} />
                  <input
                    type={showPassword ? 'text' : 'password'}
                    id="reg-password"
                    value={regPassword}
                    onChange={(e) => setRegPassword(e.target.value)}
                    placeholder="Minimum 4 caractères"
                    className={styles.input}
                    required
                  />
                  <button
                    type="button"
                    onClick={() => setShowPassword(!showPassword)}
                    className={styles.eyeBtn}
                    aria-label="Afficher le mot de passe"
                  >
                    {showPassword ? <EyeOff size={18} /> : <Eye size={18} />}
                  </button>
                </div>
              </div>

              <div className={styles.formGroup}>
                <label htmlFor="reg-role" className={styles.label}>
                  Profil &amp; Permissions *
                </label>
                <div className={styles.inputWrapper}>
                  <ShieldCheck size={18} className={styles.inputIcon} />
                  <select
                    id="reg-role"
                    value={regRole}
                    onChange={(e) => setRegRole(e.target.value)}
                    className={styles.input}
                    required
                  >
                    <option value="" disabled>Choisissez un profil</option>
                    <option value="BAILLEUR">BAILLEUR — Propriétaire (Ajout, Modif, Suppression)</option>
                    <option value="GESTIONNAIRE">GESTIONNAIRE — Agence (Ajout, Modif, Suppression)</option>
                    <option value="LOCATAIRE">LOCATAIRE — Consultation en lecture seule</option>
                  </select>
                </div>
              </div>

              <Button
                type="submit"
                variant="primary"
                size="lg"
                disabled={loading}
                className={styles.submitBtn}
              >
                {loading ? 'Création du compte...' : 'Créer mon compte et accéder au Dashboard'}
              </Button>
            </form>
          )}
        </Card>
      </div>
    </div>
  );
};
