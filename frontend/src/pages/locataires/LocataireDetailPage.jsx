import React, { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import {
  ArrowLeft,
  Users,
  ShieldCheck,
  Phone,
  Mail,
  Calendar,
  FileText,
  Building2,
  FileCheck2,
  Pencil,
  Trash2,
  DollarSign,
  Receipt,
  Download
} from 'lucide-react';
import { locatairesApi } from '../../api/locatairesApi';
import { useAuth } from '../../context/AuthContext';
import { useToast } from '../../context/ToastContext';
import { Button } from '../../components/ui/Button';
import { Card } from '../../components/ui/Card';
import { Modal } from '../../components/ui/Modal';
import styles from './LocataireDetailPage.module.css';

export const LocataireDetailPage = () => {
  const { id } = useParams();
  const navigate = useNavigate();
  const { canWrite } = useAuth();
  const { addToast } = useToast();

  const [locataire, setLocataire] = useState(null);
  const [baux, setBaux] = useState([]);
  const [loading, setLoading] = useState(true);
  const [isDeleting, setIsDeleting] = useState(false);
  const [showDeleteModal, setShowDeleteModal] = useState(false);

  useEffect(() => {
    loadDetails();
  }, [id]);

  const loadDetails = async () => {
    setLoading(true);
    try {
      const [locData, bauxData] = await Promise.all([
        locatairesApi.getById(id),
        locatairesApi.getBauxByLocataire(id).catch(() => [])
      ]);
      setLocataire(locData);
      setBaux(Array.isArray(bauxData) ? bauxData : []);
    } catch (err) {
      addToast('Fiche locataire introuvable ou accès non autorisé.', 'error');
      navigate('/locataires');
    } finally {
      setLoading(false);
    }
  };

  const handleDelete = async () => {
    setIsDeleting(true);
    try {
      await locatairesApi.delete(id);
      addToast(`Le locataire ${locataire.prenom} ${locataire.nom} a été supprimé.`, 'success');
      navigate('/locataires');
    } catch (err) {
      addToast('Impossible de supprimer le locataire.', 'error');
    } finally {
      setIsDeleting(false);
      setShowDeleteModal(false);
    }
  };

  if (loading) {
    return (
      <div className={styles.loadingContainer}>
        <div className={styles.spinner} />
        <p>Chargement de la fiche locataire...</p>
      </div>
    );
  }

  if (!locataire) return null;

  return (
    <div className={styles.container}>
      {/* Barre supérieure de retour et actions */}
      <div className={styles.topBar}>
        <button
          type="button"
          onClick={() => navigate('/locataires')}
          className={styles.backButton}
        >
          <ArrowLeft size={18} />
          <span>Retour au répertoire</span>
        </button>

        {canWrite && (
          <div className={styles.topActions}>
            <Button
              variant="secondary"
              icon={Pencil}
              onClick={() => navigate(`/locataires/${id}/modifier`)}
            >
              Modifier
            </Button>
            <Button
              variant="danger"
              icon={Trash2}
              onClick={() => setShowDeleteModal(true)}
            >
              Supprimer
            </Button>
          </div>
        )}
      </div>

      {/* Carte d'identité principale du locataire */}
      <Card className={styles.identityCard}>
        <div className={styles.identityHeader}>
          <div className={styles.avatar}>
            {locataire.prenom?.[0]}
            {locataire.nom?.[0]}
          </div>

          <div className={styles.headerInfo}>
            <div className={styles.headerTag}>
              <Users size={14} />
              <span>FICHE LOCATAIRE VALIDÉE</span>
            </div>
            <h1 className={styles.tenantName}>
              {locataire.prenom} {locataire.nom}
            </h1>
            <div className={styles.metaRow}>
              <div className={styles.cinBox}>
                <ShieldCheck size={14} />
                <span>CIN : {locataire.cin}</span>
              </div>
              <span className={styles.uuidBadge}>
                UUID : {locataire.id}
              </span>
            </div>
          </div>
        </div>

        <div className={styles.detailsGrid}>
          <div className={styles.detailBox}>
            <span className={styles.detailLabel}>Email professionnel / personnel</span>
            <div className={styles.detailValue}>
              <Mail size={16} />
              <span>{locataire.email || 'Non renseigné'}</span>
            </div>
          </div>

          <div className={styles.detailBox}>
            <span className={styles.detailLabel}>Téléphone mobile</span>
            <div className={styles.detailValue}>
              <Phone size={16} />
              <span>{locataire.telephone || 'Non renseigné'}</span>
            </div>
          </div>

          <div className={styles.detailBox}>
            <span className={styles.detailLabel}>Date de création de la fiche</span>
            <div className={styles.detailValue}>
              <Calendar size={16} />
              <span>
                {locataire.createdAt
                  ? new Date(locataire.createdAt).toLocaleDateString('fr-FR', {
                      day: '2-digit',
                      month: 'long',
                      year: 'numeric'
                    })
                  : 'N/A'}
              </span>
            </div>
          </div>
        </div>

        {/* Pièces justificatives */}
        <div className={styles.docsSection}>
          <h3 className={styles.sectionTitle}>
            <FileText size={18} />
            <span>Pièces justificatives vérifiées</span>
          </h3>

          <div className={styles.docsList}>
            {locataire.piecesJustificatives && locataire.piecesJustificatives.length > 0 ? (
              locataire.piecesJustificatives.map((doc, index) => (
                <div key={index} className={styles.docItem}>
                  <FileCheck2 size={16} className={styles.docIcon} />
                  <span className={styles.docName}>{doc}</span>
                  <span className={styles.docStatus}>Validé</span>
                </div>
              ))
            ) : (
              <p className={styles.emptyDocsText}>
                Aucune pièce complémentaire téléversée pour ce dossier.
              </p>
            )}
          </div>
        </div>
      </Card>

      {/* Section Contrats de bail rattachés */}
      <Card className={styles.bauxCard}>
        <div className={styles.cardHeaderWithAction}>
          <div>
            <h2 className={styles.cardHeading}>Contrats de bail associés</h2>
            <p className={styles.cardSubheading}>
              Interopérabilité avec le module Loyers et le module Biens immobiliers.
            </p>
          </div>
        </div>

        {baux.length === 0 ? (
          <div className={styles.emptyBaux}>
            <Building2 size={36} className={styles.emptyBauxIcon} />
            <p>Ce locataire n'a aucun contrat de bail rattaché actuellement.</p>
          </div>
        ) : (
          <div className={styles.bauxList}>
            {baux.map((b) => (
              <div key={b.id} className={styles.bailCard}>
                <div className={styles.bailHeader}>
                  <div className={styles.bailProperty}>
                    <Building2 size={18} />
                    <span className={styles.bailBienId}>
                      Bien loué ID : {b.bienId ? b.bienId.slice(0, 8) : 'N/A'}...
                    </span>
                  </div>

                  <span
                    className={
                      b.statut === 'ACTIF' ? styles.badgeActif : styles.badgeTermine
                    }
                  >
                    {b.statut === 'ACTIF' ? 'Bail Actif' : 'Bail Terminé'}
                  </span>
                </div>

                <div className={styles.bailDetails}>
                  <div className={styles.bailDetailCol}>
                    <span className={styles.bailLabel}>Loyer mensuel</span>
                    <span className={styles.bailAmount}>
                      {Number(b.montantLoyer).toLocaleString('fr-FR')} DH
                    </span>
                  </div>

                  <div className={styles.bailDetailCol}>
                    <span className={styles.bailLabel}>Dépôt de garantie</span>
                    <span className={styles.bailDeposit}>
                      {Number(b.depotGarantie || 0).toLocaleString('fr-FR')} DH
                    </span>
                  </div>

                  <div className={styles.bailDetailCol}>
                    <span className={styles.bailLabel}>Période contractuelle</span>
                    <span className={styles.bailDates}>
                      Du {b.dateDebut || 'N/A'} au {b.dateFin || 'Indéterminée'}
                    </span>
                  </div>
                </div>
              </div>
            ))}
          </div>
        )}
      </Card>

      {/* Bloc Interopérabilité Quittances (Module Akram) */}
      <Card className={styles.quittanceCard}>
        <div className={styles.quittanceHeader}>
          <div className={styles.quittanceIconBox}>
            <Receipt size={22} />
          </div>
          <div>
            <h3 className={styles.quittanceTitle}>
              Interopérabilité Quittances (Module Quittances · Akram)
            </h3>
            <p className={styles.quittanceSubtitle}>
              Données de référence exposées via REST pour l'impression des quittances officielles de loyer.
            </p>
          </div>
        </div>

        <div className={styles.quittancePayload}>
          <div className={styles.payloadRow}>
            <span className={styles.payloadKey}>Nom complet du titulaire :</span>
            <span className={styles.payloadValue}>
              {locataire.prenom} {locataire.nom}
            </span>
          </div>
          <div className={styles.payloadRow}>
            <span className={styles.payloadKey}>Numéro CIN légal :</span>
            <span className={styles.payloadValue}>{locataire.cin}</span>
          </div>
          <div className={styles.payloadRow}>
            <span className={styles.payloadKey}>Adresse de contact :</span>
            <span className={styles.payloadValue}>{locataire.email || 'N/A'}</span>
          </div>
          <div className={styles.payloadRow}>
            <span className={styles.payloadKey}>Endpoint inter-service Quittances :</span>
            <code className={styles.payloadCode}>
              GET /api/v1/locataires/{locataire.id}/quittance-summary
            </code>
          </div>
        </div>

        <div className={styles.quittanceActions}>
          <Button
            variant="secondary"
            icon={Receipt}
            onClick={() => navigate('/quittances')}
          >
            Consulter le module Quittances
          </Button>
        </div>
      </Card>

      {/* Modal confirmation suppression */}
      <Modal
        isOpen={showDeleteModal}
        onClose={() => setShowDeleteModal(false)}
        title="Supprimer la fiche locataire"
      >
        <p>
          Êtes-vous certain de vouloir supprimer définitivement le locataire{' '}
          <b>{locataire.prenom} {locataire.nom}</b> ?
        </p>
        <div className={styles.modalActions}>
          <Button
            variant="secondary"
            onClick={() => setShowDeleteModal(false)}
            disabled={isDeleting}
          >
            Annuler
          </Button>
          <Button
            variant="danger"
            onClick={handleDelete}
            disabled={isDeleting}
          >
            {isDeleting ? 'Suppression...' : 'Supprimer définitivement'}
          </Button>
        </div>
      </Modal>
    </div>
  );
};