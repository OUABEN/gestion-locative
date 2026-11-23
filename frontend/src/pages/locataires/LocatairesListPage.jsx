import React, { useState, useEffect, useMemo } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  Users,
  Search,
  PlusCircle,
  Eye,
  Pencil,
  Trash2,
  Phone,
  Mail,
  ShieldCheck,
  FileCheck2,
  Filter,
  UserCheck
} from 'lucide-react';
import { locatairesApi } from '../../api/locatairesApi';
import { useAuth } from '../../context/AuthContext';
import { useToast } from '../../context/ToastContext';
import { Button } from '../../components/ui/Button';
import { Card } from '../../components/ui/Card';
import { Modal } from '../../components/ui/Modal';
import styles from './LocatairesListPage.module.css';

export const LocatairesListPage = () => {
  const navigate = useNavigate();
  const { user, canWrite } = useAuth();
  const { addToast } = useToast();

  const [locataires, setLocataires] = useState([]);
  const [baux, setBaux] = useState([]);
  const [loading, setLoading] = useState(true);
  const [searchTerm, setSearchTerm] = useState('');
  const [statusFilter, setStatusFilter] = useState('ALL'); // 'ALL' | 'ACTIVE' | 'NONE'

  // Modal de suppression
  const [locataireToDelete, setLocataireToDelete] = useState(null);
  const [isDeleting, setIsDeleting] = useState(false);

  useEffect(() => {
    loadData();
  }, []);

  const loadData = async () => {
    setLoading(true);
    try {
      const [locsData, bauxData] = await Promise.all([
        locatairesApi.getAll(),
        locatairesApi.getBaux().catch(() => [])
      ]);
      setLocataires(Array.isArray(locsData) ? locsData : []);
      setBaux(Array.isArray(bauxData) ? bauxData : []);
    } catch (err) {
      addToast('Erreur lors du chargement des locataires.', 'error');
    } finally {
      setLoading(false);
    }
  };

  // Calcul du statut de bail pour chaque locataire
  const locatairesWithBailStatus = useMemo(() => {
    const activeTenantIds = new Set(
      baux
        .filter((b) => b.statut === 'ACTIF')
        .map((b) => String(b.locataireId || b.locataire_id))
    );

    return locataires.map((loc) => {
      const hasActive = activeTenantIds.has(String(loc.id)) || loc.statutBail === 'Avec bail actif';
      return {
        ...loc,
        hasActiveBail: hasActive,
      };
    });
  }, [locataires, baux]);

  // Filtrage combiné : recherche textuelle + statut de bail
  const filteredLocataires = useMemo(() => {
    return locatairesWithBailStatus.filter((loc) => {
      const matchesSearch =
        searchTerm === '' ||
        (loc.nom && loc.nom.toLowerCase().includes(searchTerm.toLowerCase())) ||
        (loc.prenom && loc.prenom.toLowerCase().includes(searchTerm.toLowerCase())) ||
        (loc.cin && loc.cin.toLowerCase().includes(searchTerm.toLowerCase())) ||
        (loc.email && loc.email.toLowerCase().includes(searchTerm.toLowerCase()));

      const matchesStatus =
        statusFilter === 'ALL' ||
        (statusFilter === 'ACTIVE' && loc.hasActiveBail) ||
        (statusFilter === 'NONE' && !loc.hasActiveBail);

      return matchesSearch && matchesStatus;
    });
  }, [locatairesWithBailStatus, searchTerm, statusFilter]);

  const handleDeleteConfirm = async () => {
    if (!locataireToDelete) return;
    setIsDeleting(true);
    try {
      await locatairesApi.delete(locataireToDelete.id);
      addToast(`Le locataire ${locataireToDelete.prenom} ${locataireToDelete.nom} a été supprimé.`, 'success');
      setLocataires((prev) => prev.filter((l) => l.id !== locataireToDelete.id));
      setLocataireToDelete(null);
    } catch (err) {
      addToast('Impossible de supprimer le locataire.', 'error');
    } finally {
      setIsDeleting(false);
    }
  };

  return (
    <div className={styles.container}>
      {/* En-tête de page */}
      <div className={styles.header}>
        <div>
          <div className={styles.categoryBadge}>
            <Users size={14} />
            <span>MODULE 02 · GESTION DES LOCATAIRES</span>
          </div>
          <h1 className={styles.title}>Répertoire des Locataires</h1>
          <p className={styles.subtitle}>
            Fiches d'identité vérifiées (CIN), gestion des contrats de bail et interopérabilité Quittances.
          </p>
        </div>

        <div className={styles.headerActions}>
          {user?.role === 'LOCATAIRE' && (
            <Button
              variant="secondary"
              icon={UserCheck}
              onClick={() => navigate('/locataires/mon-espace')}
            >
              Mon espace
            </Button>
          )}

          {canWrite && (
            <Button
              variant="primary"
              icon={PlusCircle}
              onClick={() => navigate('/locataires/nouveau')}
            >
              Nouveau locataire
            </Button>
          )}
        </div>
      </div>

      {/* Barre de recherche et filtres */}
      <Card className={styles.filterCard}>
        <div className={styles.filterRow}>
          <div className={styles.searchBox}>
            <Search size={18} className={styles.searchIcon} />
            <input
              type="text"
              placeholder="Rechercher par CIN (ex: AB123456), nom, prénom ou email..."
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
              className={styles.searchInput}
            />
          </div>

          <div className={styles.filterButtonGroup}>
            <span className={styles.filterLabel}>
              <Filter size={15} />
              <span>Statut :</span>
            </span>
            <button
              type="button"
              className={`${styles.filterBtn} ${statusFilter === 'ALL' ? styles.filterBtnActive : ''}`}
              onClick={() => setStatusFilter('ALL')}
            >
              Tous ({locatairesWithBailStatus.length})
            </button>
            <button
              type="button"
              className={`${styles.filterBtn} ${statusFilter === 'ACTIVE' ? styles.filterBtnActive : ''}`}
              onClick={() => setStatusFilter('ACTIVE')}
            >
              Bail actif ({locatairesWithBailStatus.filter((l) => l.hasActiveBail).length})
            </button>
            <button
              type="button"
              className={`${styles.filterBtn} ${statusFilter === 'NONE' ? styles.filterBtnActive : ''}`}
              onClick={() => setStatusFilter('NONE')}
            >
              Sans bail ({locatairesWithBailStatus.filter((l) => !l.hasActiveBail).length})
            </button>
          </div>
        </div>
      </Card>

      {/* Tableau des locataires */}
      <Card className={styles.tableCard}>
        {loading ? (
          <div className={styles.emptyState}>
            <div className={styles.spinner} />
            <p>Chargement des fiches locataires...</p>
          </div>
        ) : filteredLocataires.length === 0 ? (
          <div className={styles.emptyState}>
            <Users size={42} className={styles.emptyIcon} />
            <h3>Aucun locataire trouvé</h3>
            <p>
              {searchTerm || statusFilter !== 'ALL'
                ? 'Aucune fiche ne correspond à vos critères de recherche.'
                : 'Commencez par ajouter votre premier locataire sur la plateforme.'}
            </p>
            {canWrite && !searchTerm && statusFilter === 'ALL' && (
              <Button
                variant="primary"
                icon={PlusCircle}
                onClick={() => navigate('/locataires/nouveau')}
              >
                Créer une fiche locataire
              </Button>
            )}
          </div>
        ) : (
          <div className={styles.tableResponsive}>
            <table className={styles.table}>
              <thead>
                <tr>
                  <th>CIN</th>
                  <th>Nom &amp; Prénom</th>
                  <th>Coordonnées</th>
                  <th>Statut Bail</th>
                  <th>Pièces</th>
                  <th className={styles.thActions}>Actions</th>
                </tr>
              </thead>
              <tbody>
                {filteredLocataires.map((loc) => (
                  <tr key={loc.id} className={styles.tableRow}>
                    <td>
                      <div className={styles.cinBadge}>
                        <ShieldCheck size={14} className={styles.cinIcon} />
                        <span>{loc.cin}</span>
                      </div>
                    </td>
                    <td>
                      <div className={styles.tenantName}>
                        <span className={styles.fullName}>
                          {loc.prenom} {loc.nom}
                        </span>
                        <span className={styles.uuidText}>ID: {loc.id.slice(0, 8)}...</span>
                      </div>
                    </td>
                    <td>
                      <div className={styles.contactList}>
                        {loc.telephone && (
                          <div className={styles.contactItem}>
                            <Phone size={13} />
                            <span>{loc.telephone}</span>
                          </div>
                        )}
                        {loc.email && (
                          <div className={styles.contactItem}>
                            <Mail size={13} />
                            <span>{loc.email}</span>
                          </div>
                        )}
                      </div>
                    </td>
                    <td>
                      {loc.hasActiveBail ? (
                        <span className={styles.badgeActif}>
                          <span className={styles.dotLime} />
                          Bail actif
                        </span>
                      ) : (
                        <span className={styles.badgeSansBail}>
                          <span className={styles.dotGrey} />
                          Sans bail
                        </span>
                      )}
                    </td>
                    <td>
                      <div className={styles.piecesPill}>
                        <FileCheck2 size={13} />
                        <span>
                          {loc.piecesJustificatives && loc.piecesJustificatives.length > 0
                            ? `${loc.piecesJustificatives.length} doc(s)`
                            : '0 doc'}
                        </span>
                      </div>
                    </td>
                    <td>
                      <div className={styles.actionButtons}>
                        <button
                          type="button"
                          className={styles.actionBtn}
                          onClick={() => navigate(`/locataires/${loc.id}`)}
                          title="Consulter la fiche complète"
                        >
                          <Eye size={16} />
                          <span>Voir</span>
                        </button>

                        {canWrite && (
                          <>
                            <button
                              type="button"
                              className={styles.actionBtn}
                              onClick={() => navigate(`/locataires/${loc.id}/modifier`)}
                              title="Modifier les coordonnées"
                            >
                              <Pencil size={15} />
                              <span>Modifier</span>
                            </button>

                            <button
                              type="button"
                              className={`${styles.actionBtn} ${styles.actionBtnDanger}`}
                              onClick={() => setLocataireToDelete(loc)}
                              title="Supprimer la fiche"
                            >
                              <Trash2 size={15} />
                              <span>Supprimer</span>
                            </button>
                          </>
                        )}
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </Card>

      {/* Modal de confirmation de suppression */}
      <Modal
        isOpen={Boolean(locataireToDelete)}
        onClose={() => setLocataireToDelete(null)}
        title="Confirmer la suppression"
      >
        <div className={styles.modalContent}>
          <p>
            Êtes-vous certain de vouloir supprimer définitivement le locataire{' '}
            <b>{locataireToDelete?.prenom} {locataireToDelete?.nom}</b> (CIN : <b>{locataireToDelete?.cin}</b>) ?
          </p>
          <p className={styles.modalWarning}>
            Cette action est irréversible et supprimera également l'accès aux quittances et baux rattachés.
          </p>
          <div className={styles.modalActions}>
            <Button
              variant="secondary"
              onClick={() => setLocataireToDelete(null)}
              disabled={isDeleting}
            >
              Annuler
            </Button>
            <Button
              variant="danger"
              onClick={handleDeleteConfirm}
              disabled={isDeleting}
            >
              {isDeleting ? 'Suppression...' : 'Supprimer définitivement'}
            </Button>
          </div>
        </div>
      </Modal>
    </div>
  );
};