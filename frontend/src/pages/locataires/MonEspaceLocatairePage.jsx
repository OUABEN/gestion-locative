import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  UserCheck,
  Building2,
  Receipt,
  Phone,
  Mail,
  ShieldCheck,
  Calendar,
  FileText,
  Wallet,
  ArrowRight,
  Sparkles
} from 'lucide-react';
import { locatairesApi } from '../../api/locatairesApi';
import { useAuth } from '../../context/AuthContext';
import { useToast } from '../../context/ToastContext';
import { Button } from '../../components/ui/Button';
import { Card } from '../../components/ui/Card';
import styles from './MonEspaceLocatairePage.module.css';

export const MonEspaceLocatairePage = () => {
  const navigate = useNavigate();
  const { user } = useAuth();
  const { addToast } = useToast();

  const [locataire, setLocataire] = useState(null);
  const [baux, setBaux] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    loadMyData();
  }, [user]);

  const loadMyData = async () => {
    setLoading(true);
    try {
      let myProfile = null;
      try {
        myProfile = await locatairesApi.getMe();
      } catch {
        const locs = await locatairesApi.getAll();
        myProfile = Array.isArray(locs) && locs.length > 0 ? locs[0] : null;
      }
      setLocataire(myProfile);

      if (myProfile) {
        const myBaux = await locatairesApi.getBauxByLocataire(myProfile.id);
        setBaux(Array.isArray(myBaux) ? myBaux : []);
      }
    } catch (err) {
      addToast('Impossible de charger les données de votre espace.', 'error');
    } finally {
      setLoading(false);
    }
  };

  if (loading) {
    return (
      <div className={styles.loadingContainer}>
        <div className={styles.spinner} />
        <p>Chargement de votre espace locataire sécurisé...</p>
      </div>
    );
  }

  const activeBail = baux.find((b) => b.statut === 'ACTIF') || baux[0];

  return (
    <div className={styles.container}>
      {/* Bannière de bienvenue */}
      <div className={styles.heroBanner}>
        <div className={styles.heroContent}>
          <div className={styles.heroTag}>
            <Sparkles size={14} />
            <span>ESPACE LOCATAIRE SÉCURISÉ</span>
          </div>
          <h1 className={styles.heroTitle}>
            Bienvenue, {locataire?.prenom || user?.prenom || 'Locataire'} {locataire?.nom || user?.nom || ''}
          </h1>
          <p className={styles.heroSubtitle}>
            Consultez votre contrat de location en cours, vos coordonnées vérifiées et accédez directement à vos quittances de loyer.
          </p>
        </div>
      </div>

      <div className={styles.gridContainer}>
        {/* Colonne gauche : Profil & Identité vérifiée */}
        <div className={styles.colLeft}>
          <Card className={styles.profileCard}>
            <div className={styles.profileHeader}>
              <div className={styles.avatar}>
                {locataire?.prenom?.[0] || 'L'}
                {locataire?.nom?.[0] || 'O'}
              </div>
              <div>
                <h3 className={styles.profileName}>
                  {locataire?.prenom} {locataire?.nom}
                </h3>
                <div className={styles.cinBadge}>
                  <ShieldCheck size={14} />
                  <span>CIN : {locataire?.cin || 'AB123456'}</span>
                </div>
              </div>
            </div>

            <div className={styles.profileDetails}>
              <div className={styles.profileRow}>
                <span className={styles.profileLabel}>Email</span>
                <span className={styles.profileValue}>
                  <Mail size={14} />
                  {locataire?.email || user?.email || 'N/A'}
                </span>
              </div>
              <div className={styles.profileRow}>
                <span className={styles.profileLabel}>Téléphone</span>
                <span className={styles.profileValue}>
                  <Phone size={14} />
                  {locataire?.telephone || '0661001122'}
                </span>
              </div>
              <div className={styles.profileRow}>
                <span className={styles.profileLabel}>Identifiant Locataire</span>
                <span className={styles.uuidText}>{locataire?.id || user?.sub}</span>
              </div>
            </div>
          </Card>

          {/* Raccourci vers le module Quittances */}
          <Card className={styles.quittanceShortcutCard}>
            <div className={styles.quittanceShortcutHeader}>
              <div className={styles.quittanceIconBox}>
                <Receipt size={22} />
              </div>
              <div>
                <h4 className={styles.quittanceShortcutTitle}>Mes Quittances de Loyer</h4>
                <p className={styles.quittanceShortcutText}>
                  Téléchargez vos justificatifs officiels de paiement émis par le module Quittances.
                </p>
              </div>
            </div>

            <Button
              variant="primary"
              icon={ArrowRight}
              onClick={() => navigate('/quittances')}
              className={styles.quittanceBtn}
            >
              Consulter mes quittances
            </Button>
          </Card>
        </div>

        {/* Colonne droite : Contrat de bail actif & détails financiers */}
        <div className={styles.colRight}>
          <Card className={styles.leaseCard}>
            <div className={styles.leaseHeader}>
              <div className={styles.leaseTitleBox}>
                <Building2 size={22} />
                <div>
                  <h3 className={styles.leaseTitle}>Mon Contrat de Bail Actif</h3>
                  <p className={styles.leaseSubtitle}>Contrat de location enregistré sur la plateforme</p>
                </div>
              </div>

              <span className={styles.badgeActif}>
                <span className={styles.dotLime} />
                Bail Actif
              </span>
            </div>

            {activeBail ? (
              <div className={styles.leaseBody}>
                <div className={styles.kpiRow}>
                  <div className={styles.kpiBox}>
                    <span className={styles.kpiLabel}>Loyer mensuel dû</span>
                    <span className={styles.kpiValueLime}>
                      {Number(activeBail.montantLoyer).toLocaleString('fr-FR')} DH
                    </span>
                  </div>

                  <div className={styles.kpiBox}>
                    <span className={styles.kpiLabel}>Dépôt de garantie versé</span>
                    <span className={styles.kpiValue}>
                      {Number(activeBail.depotGarantie || 0).toLocaleString('fr-FR')} DH
                    </span>
                  </div>
                </div>

                <div className={styles.leaseInfoList}>
                  <div className={styles.leaseInfoItem}>
                    <Calendar size={16} />
                    <span>Période de location : <b>Du {activeBail.dateDebut || '2026-01-01'} au {activeBail.dateFin || '2026-12-31'}</b></span>
                  </div>
                  <div className={styles.leaseInfoItem}>
                    <Building2 size={16} />
                    <span>Identifiant du bien loué : <code className={styles.codeText}>{activeBail.bienId || activeBail.bien_id}</code></span>
                  </div>
                  <div className={styles.leaseInfoItem}>
                    <Wallet size={16} />
                    <span>Mode de règlement : <b>Virement bancaire / Carte bancaire</b></span>
                  </div>
                </div>
              </div>
            ) : (
              <p className={styles.noLeaseText}>
                Aucun bail actif n'est rattaché à votre compte pour le moment.
              </p>
            )}
          </Card>
        </div>
      </div>
    </div>
  );
};