import React, { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import {
  PlusCircle,
  Search,
  LayoutGrid,
  List,
  Eye,
  Edit3,
  Trash2,
  AlertTriangle,
  RotateCcw,
  Building2,
} from 'lucide-react';
import { biensApi } from '../api/biensApi';
import { useAuth } from '../context/AuthContext';
import { useToast } from '../context/ToastContext';
import { Card } from '../components/ui/Card';
import { Button } from '../components/ui/Button';
import { Badge } from '../components/ui/Badge';
import { Modal } from '../components/ui/Modal';
import { BienCard } from '../components/biens/BienCard';
import { formatCurrency, formatSurface } from '../utils/format';
import { getBienImage } from '../utils/bienImage';
import styles from './BiensListPage.module.css';

export const BiensListPage = () => {
  const { canWrite } = useAuth();
  const { addToast } = useToast();

  const [biens, setBiens] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  // Filtres
  const [searchTerm, setSearchTerm] = useState('');
  const [debouncedSearch, setDebouncedSearch] = useState('');
  const [filterVille, setFilterVille] = useState('');
  const [filterStatut, setFilterStatut] = useState('');
  const [filterType, setFilterType] = useState('');
  const [sortBy, setSortBy] = useState('date-desc');

  // Vue (Grille ou Tableau) & Pagination
  const [viewMode, setViewMode] = useState('grid');
  const [currentPage, setCurrentPage] = useState(1);
  const itemsPerPage = 8;

  // Modale de confirmation de suppression
  const [bienToDelete, setBienToDelete] = useState(null);
  const [deleting, setDeleting] = useState(false);

  // Debounce sur la recherche texte
  useEffect(() => {
    const handler = setTimeout(() => {
      setDebouncedSearch(searchTerm);
    }, 300);
    return () => clearTimeout(handler);
  }, [searchTerm]);

  const loadBiens = async () => {
    setLoading(true);
    setError(null);
    try {
      const data = await biensApi.getAll({
        ville: filterVille || undefined,
        statut: filterStatut || undefined,
        type: filterType || undefined,
      });
      setBiens(data || []);
      setCurrentPage(1);
    } catch (err) {
      setError(err.response?.data?.message || 'Erreur lors du chargement des biens.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadBiens();
  }, [filterVille, filterStatut, filterType]);

  // Filtrage local supplémentaire pour la recherche texte (adresse / ville)
  const filteredBiens = biens.filter((b) => {
    if (!debouncedSearch) return true;
    const term = debouncedSearch.toLowerCase();
    return (
      (b.adresse && b.adresse.toLowerCase().includes(term)) ||
      (b.ville && b.ville.toLowerCase().includes(term)) ||
      (b.type && b.type.toLowerCase().includes(term))
    );
  });

  // Tri côté client
  const sortedBiens = [...filteredBiens].sort((a, b) => {
    if (sortBy === 'prix-asc') return Number(a.prixMensuel) - Number(b.prixMensuel);
    if (sortBy === 'prix-desc') return Number(b.prixMensuel) - Number(a.prixMensuel);
    if (sortBy === 'surface-desc') return Number(b.surface) - Number(a.surface);
    return new Date(b.createdAt) - new Date(a.createdAt);
  });

  // Pagination
  const totalPages = Math.ceil(sortedBiens.length / itemsPerPage) || 1;
  const paginatedBiens = sortedBiens.slice(
    (currentPage - 1) * itemsPerPage,
    currentPage * itemsPerPage
  );

  // Villes et types uniques pour les menus déroulants
  const availableVilles = Array.from(new Set(biens.map((b) => b.ville).filter(Boolean)));
  const availableTypes = Array.from(new Set(biens.map((b) => b.type).filter(Boolean)));

  // Suppression
  const confirmDelete = async () => {
    if (!bienToDelete) return;
    setDeleting(true);
    try {
      await biensApi.delete(bienToDelete.id);
      addToast('Bien supprimé avec succès.');
      setBienToDelete(null);
      loadBiens();
    } catch (err) {
      if (err.response?.status === 409) {
        addToast('Impossible de supprimer un bien occupé.', 'error');
      } else {
        addToast(err.response?.data?.message || 'Erreur lors de la suppression.', 'error');
      }
    } finally {
      setDeleting(false);
    }
  };

  return (
    <div className={styles.container}>
      <div className={styles.headerRow}>
        <div>
          <h1 className={styles.title}>Catalogue des Biens</h1>
          <p className={styles.subtitle}>
            Gérez votre parc, appliquez des filtres et consultez l'état de chaque logement.
          </p>
        </div>
        {canWrite && (
          <Link to="/biens/nouveau">
            <Button variant="primary" icon={PlusCircle}>
              Ajouter un bien
            </Button>
          </Link>
        )}
      </div>

      {/* Barre d'outils et filtres */}
      <Card className={styles.filterCard}>
        <div className={styles.filterGrid}>
          {/* Recherche */}
          <div className={styles.searchWrapper}>
            <Search size={18} className={styles.searchIcon} />
            <input
              type="text"
              placeholder="Rechercher par adresse, ville, type..."
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
              className={styles.searchInput}
            />
          </div>

          {/* Filtre Ville */}
          <select
            value={filterVille}
            onChange={(e) => setFilterVille(e.target.value)}
            className={styles.select}
          >
            <option value="">Toutes les villes</option>
            {availableVilles.map((v) => (
              <option key={v} value={v}>
                {v}
              </option>
            ))}
          </select>

          {/* Filtre Statut */}
          <select
            value={filterStatut}
            onChange={(e) => setFilterStatut(e.target.value)}
            className={styles.select}
          >
            <option value="">Tous les statuts</option>
            <option value="LIBRE">Libre</option>
            <option value="OCCUPE">Occupé</option>
          </select>

          {/* Filtre Type */}
          <select
            value={filterType}
            onChange={(e) => setFilterType(e.target.value)}
            className={styles.select}
          >
            <option value="">Tous les types</option>
            {availableTypes.map((t) => (
              <option key={t} value={t}>
                {t}
              </option>
            ))}
          </select>

          {/* Tri */}
          <select
            value={sortBy}
            onChange={(e) => setSortBy(e.target.value)}
            className={styles.select}
          >
            <option value="date-desc">Plus récents</option>
            <option value="prix-asc">Loyer croissant</option>
            <option value="prix-desc">Loyer décroissant</option>
            <option value="surface-desc">Surface décroissante</option>
          </select>

          {/* Bascule de Vue */}
          <div className={styles.viewToggle}>
            <button
              onClick={() => setViewMode('grid')}
              className={`${styles.toggleBtn} ${viewMode === 'grid' ? styles.activeView : ''}`}
              title="Vue en grille"
              aria-label="Afficher en grille"
            >
              <LayoutGrid size={18} />
            </button>
            <button
              onClick={() => setViewMode('table')}
              className={`${styles.toggleBtn} ${viewMode === 'table' ? styles.activeView : ''}`}
              title="Vue en tableau"
              aria-label="Afficher en tableau"
            >
              <List size={18} />
            </button>
          </div>
        </div>
      </Card>

      {/* Affichage des états : Erreur / Chargement / Vide / Contenu */}
      {loading ? (
        <div className={styles.loadingGrid}>
          {[1, 2, 3, 4, 5, 6].map((i) => (
            <div key={i} className={`skeleton ${styles.skeletonCard}`} />
          ))}
        </div>
      ) : error ? (
        <Card className={styles.centeredCard}>
          <AlertTriangle size={42} color="var(--color-red)" />
          <h2>Erreur de chargement</h2>
          <p>{error}</p>
          <Button onClick={loadBiens} variant="primary" icon={RotateCcw}>
            Réessayer
          </Button>
        </Card>
      ) : sortedBiens.length === 0 ? (
        <Card className={styles.centeredCard}>
          <Building2 size={48} color="var(--color-beige-dark)" />
          <h2>Aucun bien trouvé</h2>
          <p>Aucun logement ne correspond aux critères de recherche sélectionnés.</p>
          {(searchTerm || filterVille || filterStatut || filterType) && (
            <Button
              onClick={() => {
                setSearchTerm('');
                setFilterVille('');
                setFilterStatut('');
                setFilterType('');
              }}
              variant="secondary"
            >
              Réinitialiser les filtres
            </Button>
          )}
        </Card>
      ) : viewMode === 'grid' ? (
        <div className={styles.biensGrid}>
          {paginatedBiens.map((bien, i) => (
            <BienCard key={bien.id} bien={bien} index={i} onDelete={(b) => setBienToDelete(b)} />
          ))}
        </div>
      ) : (
        <Card className={styles.tableCard}>
          <div className={styles.tableResponsive}>
            <table className={styles.table}>
              <thead>
                <tr>
                  <th>Adresse</th>
                  <th>Ville</th>
                  <th>Type</th>
                  <th>Surface</th>
                  <th>Loyer</th>
                  <th>Charges</th>
                  <th>Statut</th>
                  <th style={{ textAlign: 'right' }}>Actions</th>
                </tr>
              </thead>
              <tbody>
                {paginatedBiens.map((bien, i) => (
                  <tr key={bien.id} className="animate-fade-up" style={{ animationDelay: `${i * 40}ms` }}>
                    <td className={styles.boldCell}>
                      <Link to={`/biens/${bien.id}`} className={styles.linkRow}>
                        <img src={getBienImage(bien.type, bien.imageUrl)} alt="" className={styles.rowThumb} loading="lazy" />
                        <span>{bien.adresse}</span>
                      </Link>
                    </td>
                    <td>{bien.ville}</td>
                    <td>{bien.type}</td>
                    <td>{formatSurface(bien.surface)}</td>
                    <td className={styles.priceCell}>{formatCurrency(bien.prixMensuel)}</td>
                    <td>{formatCurrency(bien.charges)}</td>
                    <td>
                      <Badge statut={bien.statut} />
                    </td>
                    <td style={{ textAlign: 'right' }}>
                      <div className={styles.rowActions}>
                        <Link to={`/biens/${bien.id}`} className={styles.iconBtn} title="Voir">
                          <Eye size={16} />
                        </Link>
                        {canWrite && (
                          <>
                            <Link to={`/biens/${bien.id}/modifier`} className={styles.iconBtn} title="Modifier">
                              <Edit3 size={16} />
                            </Link>
                            <button
                              onClick={() => setBienToDelete(bien)}
                              className={`${styles.iconBtn} ${styles.deleteIconBtn}`}
                              title="Supprimer"
                            >
                              <Trash2 size={16} />
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
        </Card>
      )}

      {/* Pagination */}
      {totalPages > 1 && (
        <div className={styles.pagination}>
          <Button
            size="sm"
            variant="secondary"
            disabled={currentPage === 1}
            onClick={() => setCurrentPage((p) => Math.max(1, p - 1))}
          >
            Précédent
          </Button>
          <span className={styles.pageInfo}>
            Page {currentPage} sur {totalPages} ({sortedBiens.length} biens)
          </span>
          <Button
            size="sm"
            variant="secondary"
            disabled={currentPage === totalPages}
            onClick={() => setCurrentPage((p) => Math.min(totalPages, p + 1))}
          >
            Suivant
          </Button>
        </div>
      )}

      {/* Boîte modale de confirmation de suppression */}
      <Modal
        isOpen={Boolean(bienToDelete)}
        onClose={() => setBienToDelete(null)}
        title="Confirmer la suppression"
      >
        <div className={styles.modalContent}>
          <p>
            Êtes-vous sûr de vouloir supprimer définitivement le bien situé au :
            <br />
            <strong>{bienToDelete?.adresse}, {bienToDelete?.ville}</strong> ?
          </p>

          {bienToDelete?.statut === 'OCCUPE' && (
            <div className={styles.warningAlert}>
              <AlertTriangle size={18} />
              <span>Attention : ce bien est actuellement OCCUPÉ. La suppression sera rejetée par le système.</span>
            </div>
          )}

          <div className={styles.modalActions}>
            <Button variant="secondary" onClick={() => setBienToDelete(null)} disabled={deleting}>
              Annuler
            </Button>
            <Button variant="danger" onClick={confirmDelete} disabled={deleting} icon={Trash2}>
              {deleting ? 'Suppression...' : 'Supprimer'}
            </Button>
          </div>
        </div>
      </Modal>
    </div>
  );
};

