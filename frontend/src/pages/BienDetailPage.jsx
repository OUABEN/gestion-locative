import React, { useState, useEffect } from 'react';
import { useParams, Link, useNavigate } from 'react-router-dom';
import {
  ArrowLeft,
  MapPin,
  Calendar,
  Clock,
  Edit3,
  Trash2,
  CheckCircle2,
  XCircle,
  AlertTriangle,
} from 'lucide-react';
import { biensApi } from '../api/biensApi';
import { useAuth } from '../context/AuthContext';
import { useToast } from '../context/ToastContext';
import { Card } from '../components/ui/Card';
import { Button } from '../components/ui/Button';
import { Badge } from '../components/ui/Badge';
import { Modal } from '../components/ui/Modal';
import { formatCurrency, formatSurface, formatDate } from '../utils/format';
import { getBienImage } from '../utils/bienImage';
import styles from './BienDetailPage.module.css';

export const BienDetailPage = () => {
  const { id } = useParams();
  const navigate = useNavigate();
  const { canWrite } = useAuth();
  const { addToast } = useToast();

  const [bien, setBien] = useState(null);
  const [dispo, setDispo] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  // Changement de statut
  const [updatingStatut, setUpdatingStatut] = useState(false);
  const [statutModalOpen, setStatutModalOpen] = useState(false);
  const [targetStatut, setTargetStatut] = useState('OCCUPE');

  // Suppression
  const [deleteModalOpen, setDeleteModalOpen] = useState(false);
  const [deleting, setDeleting] = useState(false);

  const loadData = async () => {
    setLoading(true);
    setError(null);
    try {
      const [bienData, dispoData] = await Promise.all([
        biensApi.getById(id),
        biensApi.getDisponibilite(id),
      ]);
      setBien(bienData);
      setDispo(dispoData);
    } catch (err) {
      setError(err.response?.data?.message || 'Impossible de charger le bien.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadData();
  }, [id]);

  // Exécution du PATCH /api/biens/:id/statut
  const handlePatchStatut = async () => {
    setUpdatingStatut(true);
    try {
      const updated = await biensApi.updateStatut(id, targetStatut);
      setBien(updated);
      setDispo({
        id: updated.id,
        disponible: updated.statut === 'LIBRE',
        statut: updated.statut,
      });
      addToast(`Le statut du bien a été passé à ${targetStatut}.`);
      setStatutModalOpen(false);
    } catch (err) {
      if (err.response?.status === 409) {
        addToast(`Conflit : le bien est déjà dans le statut '${targetStatut}'.`, 'error');
      } else {
        addToast(err.response?.data?.message || 'Erreur lors du changement de statut.', 'error');
      }
    } finally {
      setUpdatingStatut(false);
    }
  };

  // Exécution du DELETE /api/biens/:id
  const handleDelete = async () => {
    setDeleting(true);
    try {
      await biensApi.delete(id);
      addToast('Bien supprimé avec succès.');
      navigate('/biens');
    } catch (err) {
      if (err.response?.status === 409) {
        addToast('Impossible de supprimer un bien occupé.', 'error');
      } else {
        addToast(err.response?.data?.message || 'Erreur lors de la suppression.', 'error');
      }
      setDeleteModalOpen(false);
    } finally {
      setDeleting(false);
    }
  };

  if (loading) {
    return <div className={styles.loading}>Chargement des détails du logement...</div>;
  }

  if (error || !bien) {
    return (
      <Card className={styles.errorCard}>
        <AlertTriangle size={40} color="var(--color-red)" />
        <h2>Logement introuvable</h2>
        <p>{error || "Ce bien n'existe pas ou a été supprimé."}</p>
        <Link to="/biens">
          <Button variant="secondary" icon={ArrowLeft}>
            Retour à la liste des biens
          </Button>
        </Link>
      </Card>
    );
  }

  const nextStatut = bien.statut === 'LIBRE' ? 'OCCUPE' : 'LIBRE';

  return (
    <div className={styles.container}>
      <div className={styles.topNav}>
        <Link to="/biens" className={styles.backLink}>
          <ArrowLeft size={18} />
          <span>Retour au catalogue</span>
        </Link>

        {canWrite && (
          <div className={styles.topActions}>
            <Link to={`/biens/${bien.id}/modifier`}>
              <Button variant="secondary" icon={Edit3}>
                Modifier la fiche
              </Button>
            </Link>
            <Button
              variant="danger"
              icon={Trash2}
              onClick={() => setDeleteModalOpen(true)}
            >
              Supprimer
            </Button>
          </div>
        )}
      </div>

      <section className={styles.cover}>
        <img src={getBienImage(bien.type)} alt={`${bien.type} à ${bien.ville}`} className={styles.coverImage} />
        <div className={styles.coverOverlay} />
        <div className={styles.coverContent}>
          <div className={styles.coverBadges}>
            <span className={styles.coverType}>{bien.type}</span>
            <Badge statut={bien.statut} />
          </div>
          <h1 className={styles.coverTitle}>{bien.adresse}</h1>
          <div className={styles.coverVille}>
            <MapPin size={18} />
            <span>{bien.ville}</span>
          </div>
        </div>
        <div className={styles.coverPrice}>
          <span className={styles.coverPriceValue}>{formatCurrency(bien.prixMensuel)}</span>
          <span className={styles.coverPriceLabel}>par mois · hors charges</span>
        </div>
      </section>

      <div className={styles.grid}>
        {/* Colonne Principale */}
        <div className={styles.mainCol}>
          <Card className={`${styles.mainCard} animate-fade-up`} style={{ animationDelay: '120ms' }}>
            <h3 className={styles.metaTitle}>Caractéristiques financières &amp; techniques</h3>

            <div className={styles.detailsGrid}>
              <div className={styles.detailBox}>
                <span className={styles.detailLabel}>Loyer mensuel net</span>
                <span className={styles.detailValueHighlight}>{formatCurrency(bien.prixMensuel)}</span>
              </div>
              <div className={styles.detailBox}>
                <span className={styles.detailLabel}>Charges locatives</span>
                <span className={styles.detailValue}>{formatCurrency(bien.charges)}</span>
              </div>
              <div className={styles.detailBox}>
                <span className={styles.detailLabel}>Surface habitable</span>
                <span className={styles.detailValue}>{formatSurface(bien.surface)}</span>
              </div>
              <div className={styles.detailBox}>
                <span className={styles.detailLabel}>Total mensuel</span>
                <span className={styles.detailValueTotal}>
                  {formatCurrency((Number(bien.prixMensuel) || 0) + (Number(bien.charges) || 0))}
                </span>
              </div>
            </div>
          </Card>

          {/* Informations techniques & Traçabilité */}
          <Card className={styles.metaCard}>
            <h3 className={styles.metaTitle}>Informations d'enregistrement</h3>
            <div className={styles.metaList}>
              <div className={styles.metaItem}>
                <span className={styles.metaLabel}>Identifiant unique (UUID) :</span>
                <code className={styles.uuidCode}>{bien.id}</code>
              </div>
              <div className={styles.metaItem}>
                <Calendar size={16} />
                <span className={styles.metaLabel}>Date de création :</span>
                <span>{formatDate(bien.createdAt)}</span>
              </div>
              <div className={styles.metaItem}>
                <Clock size={16} />
                <span className={styles.metaLabel}>Dernière mise à jour :</span>
                <span>{formatDate(bien.updatedAt)}</span>
              </div>
            </div>
          </Card>
        </div>

        {/* Colonne Latérale : Disponibilité & Changement de statut */}
        <div className={styles.sideCol}>
          <Card className={styles.dispoCard}>
            <h3 className={styles.sideTitle}>Disponibilité opérationnelle</h3>
            
            <div className={`${styles.dispoBanner} ${dispo?.disponible ? styles.dispoTrue : styles.dispoFalse}`}>
              {dispo?.disponible ? (
                <>
                  <CheckCircle2 size={24} />
                  <div>
                    <strong>Logement disponible</strong>
                    <p>Ce bien peut faire l'objet d'un nouveau bail actif.</p>
                  </div>
                </>
              ) : (
                <>
                  <XCircle size={24} />
                  <div>
                    <strong>Logement occupé</strong>
                    <p>Un bail est actuellement en cours pour ce logement.</p>
                  </div>
                </>
              )}
            </div>

            {canWrite && (
              <div className={styles.statutActionBox}>
                <p className={styles.statutExpl}>
                  Changer l'état du logement :
                </p>
                <Button
                  variant={bien.statut === 'LIBRE' ? 'secondary' : 'primary'}
                  className={styles.fullWidthBtn}
                  onClick={() => {
                    setTargetStatut(nextStatut);
                    setStatutModalOpen(true);
                  }}
                >
                  Passer au statut {nextStatut === 'LIBRE' ? 'LIBRE' : 'OCCUPÉ'}
                </Button>
              </div>
            )}
          </Card>
        </div>
      </div>

      {/* Modale confirmation Changement de statut */}
      <Modal
        isOpen={statutModalOpen}
        onClose={() => setStatutModalOpen(false)}
        title="Confirmation du changement d'état"
      >
        <p style={{ marginBottom: '16px' }}>
          Êtes-vous sûr de vouloir basculer l'état de ce bien vers <strong>{targetStatut}</strong> ?
        </p>
        <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '12px' }}>
          <Button variant="secondary" onClick={() => setStatutModalOpen(false)} disabled={updatingStatut}>
            Annuler
          </Button>
          <Button variant="primary" onClick={handlePatchStatut} disabled={updatingStatut}>
            {updatingStatut ? 'Mise à jour...' : 'Confirmer le changement'}
          </Button>
        </div>
      </Modal>

      {/* Modale confirmation Suppression */}
      <Modal
        isOpen={deleteModalOpen}
        onClose={() => setDeleteModalOpen(false)}
        title="Confirmer la suppression"
      >
        <p style={{ marginBottom: '16px' }}>
          Confirmez-vous la suppression de ce bien ?
          <br />
          <strong>{bien.adresse}, {bien.ville}</strong>
        </p>
        {bien.statut === 'OCCUPE' && (
          <div style={{ backgroundColor: 'var(--color-red-light)', color: 'var(--color-red)', padding: '12px', borderRadius: '8px', marginBottom: '16px', fontSize: '13px' }}>
            Ce bien est actuellement OCCUPÉ. La suppression sera refusée.
          </div>
        )}
        <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '12px' }}>
          <Button variant="secondary" onClick={() => setDeleteModalOpen(false)} disabled={deleting}>
            Annuler
          </Button>
          <Button variant="danger" onClick={handleDelete} disabled={deleting} icon={Trash2}>
            {deleting ? 'Suppression...' : 'Supprimer'}
          </Button>
        </div>
      </Modal>
    </div>
  );
};

