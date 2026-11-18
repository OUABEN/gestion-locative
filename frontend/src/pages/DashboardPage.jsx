import React, { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import {
  Building2,
  CheckCircle,
  Home,
  Coins,
  TrendingUp,
  PlusCircle,
  AlertTriangle,
  RotateCcw,
  ArrowRight,
  MapPin,
  Sparkles,
  LayoutDashboard,
  Search,
  Save,
  Maximize,
  ExternalLink,
} from 'lucide-react';
import { biensApi } from '../api/biensApi';
import { useAuth } from '../context/AuthContext';
import { useToast } from '../context/ToastContext';
import { bienSchema } from '../utils/validators';
import { KpiCard } from '../components/dashboard/KpiCard';
import { DashboardCharts } from '../components/dashboard/DashboardCharts';
import { BienCard } from '../components/biens/BienCard';
import { Card } from '../components/ui/Card';
import { Button } from '../components/ui/Button';
import { Badge } from '../components/ui/Badge';
import { Modal } from '../components/ui/Modal';
import { formatCurrency, formatSurface, formatDate } from '../utils/format';
import { getBienImage, getHeroImage } from '../utils/bienImage';
import styles from './DashboardPage.module.css';

const greeting = () => {
  const h = new Date().getHours();
  if (h < 12) return 'Bonjour';
  if (h < 18) return 'Bon après-midi';
  return 'Bonsoir';
};

const ROLE_LABEL = {
  BAILLEUR: 'Bailleur',
  GESTIONNAIRE: 'Gestionnaire',
  LOCATAIRE: 'Locataire',
};

const QUICK_TYPES = ['Appartement', 'Villa', 'Studio', 'Bureau', 'Local Commercial'];

export const DashboardPage = () => {
  const { canWrite, user } = useAuth();
  const { addToast } = useToast();

  const [biens, setBiens] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  // Choix actif sur le hub : 'dashboard' | 'biens' | 'ajout'
  const [activeTab, setActiveTab] = useState('dashboard');

  // Filtres rapides pour l'onglet "Afficher les biens"
  const [quickSearch, setQuickSearch] = useState('');
  const [quickStatut, setQuickStatut] = useState('ALL');
  const [bienToDelete, setBienToDelete] = useState(null);
  const [deleting, setDeleting] = useState(false);

  // Formulaire pour l'onglet "Ajouter un bien"
  const [submitting, setSubmitting] = useState(false);
  const {
    register,
    handleSubmit,
    watch,
    setValue,
    reset,
    formState: { errors },
  } = useForm({
    resolver: zodResolver(bienSchema),
    defaultValues: {
      adresse: '',
      ville: 'Casablanca',
      type: 'Appartement',
      surface: 85,
      prixMensuel: 6500,
      charges: 400,
    },
  });

  const watchedType = watch('type');
  const watchedVille = watch('ville');
  const watchedPrix = watch('prixMensuel');
  const watchedSurface = watch('surface');

  const fetchBiens = async () => {
    setLoading(true);
    setError(null);
    try {
      const data = await biensApi.getAll();
      setBiens(data || []);
    } catch (err) {
      setError(err.response?.data?.message || 'Erreur lors de la récupération des biens.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchBiens();
  }, []);

  const onCreateBien = async (formData) => {
    setSubmitting(true);
    try {
      const created = await biensApi.create(formData);
      addToast(`Bien "${created.adresse}" ajouté avec succès !`);
      reset();
      await fetchBiens();
      setActiveTab('biens');
    } catch (err) {
      addToast(err.response?.data?.message || "Erreur lors de l'ajout du bien.", 'error');
    } finally {
      setSubmitting(false);
    }
  };

  const confirmDelete = async () => {
    if (!bienToDelete) return;
    setDeleting(true);
    try {
      await biensApi.delete(bienToDelete.id);
      addToast('Bien supprimé avec succès.');
      setBienToDelete(null);
      await fetchBiens();
    } catch (err) {
      if (err.response?.status === 409) {
        addToast('Impossible de supprimer un bien occupé (409).', 'error');
      } else {
        addToast(err.response?.data?.message || 'Erreur lors de la suppression.', 'error');
      }
      setBienToDelete(null);
    } finally {
      setDeleting(false);
    }
  };

  // Métriques KPI
  const totalBiens = biens.length;
  const biensLibres = biens.filter((b) => b.statut === 'LIBRE').length;
  const biensOccupes = biens.filter((b) => b.statut === 'OCCUPE').length;
  const tauxOccupation = totalBiens > 0 ? Math.round((biensOccupes / totalBiens) * 100) : 0;
  const revenuPotentiel = biens.reduce((sum, b) => sum + (Number(b.prixMensuel) || 0), 0);
  const revenuOccupes = biens
    .filter((b) => b.statut === 'OCCUPE')
    .reduce((sum, b) => sum + (Number(b.prixMensuel) || 0), 0);
  const totalCharges = biens.reduce((sum, b) => sum + (Number(b.charges) || 0), 0);
  const tauxRevenu = revenuPotentiel > 0 ? Math.round((revenuOccupes / revenuPotentiel) * 100) : 0;

  const featured = [...biens]
    .sort((a, b) => (Number(b.prixMensuel) || 0) - (Number(a.prixMensuel) || 0))
    .slice(0, 3);

  const recentBiens = [...biens]
    .sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt))
    .slice(0, 5);

  const filteredBiens = biens.filter((b) => {
    const matchStatut = quickStatut === 'ALL' || b.statut === quickStatut;
    const q = quickSearch.trim().toLowerCase();
    const matchSearch =
      !q ||
      b.adresse?.toLowerCase().includes(q) ||
      b.ville?.toLowerCase().includes(q) ||
      b.type?.toLowerCase().includes(q);
    return matchStatut && matchSearch;
  });

  const userName = user?.email ? user.email.split('@')[0] : 'utilisateur';

  if (loading) {
    return (
      <div className={styles.loadingContainer}>
        <div className={`skeleton ${styles.skeletonHero}`} />
        <div className={styles.skeletonKpis}>
          {[1, 2, 3, 4, 5, 6].map((i) => (
            <div key={i} className={`skeleton ${styles.skeletonCard}`} />
          ))}
        </div>
        <div className={`skeleton ${styles.skeletonMain}`} />
      </div>
    );
  }

  if (error) {
    return (
      <Card className={styles.errorCard}>
        <AlertTriangle size={42} color="var(--color-red)" />
        <h2>Impossible de charger le tableau de bord</h2>
        <p>{error}</p>
        <Button onClick={fetchBiens} variant="primary" icon={RotateCcw}>
          Réessayer
        </Button>
      </Card>
    );
  }

  return (
    <div className={styles.container}>
      {/* ---------- Bandeau Héro Midnight Black & Lime Green ---------- */}
      <section className={`${styles.hero} animate-scale-in`}>
        <img src={getHeroImage()} alt="" className={styles.heroImage} aria-hidden="true" />
        <div className={styles.heroOverlay} />
        <div className={styles.heroContent}>
          <span className={styles.heroEyebrow}>
            <Sparkles size={14} /> {ROLE_LABEL[user?.role] || 'Espace'} · Gestion Locative
          </span>
          <h1 className={styles.heroTitle}>
            {greeting()}, <span className={styles.heroName}>{userName}</span>
          </h1>
          <p className={styles.heroText}>
            Votre portefeuille compte <strong>{totalBiens} biens</strong>, dont{' '}
            <strong>{biensLibres} disponibles</strong> immédiatement. Revenu mensuel actif :{' '}
            <strong className={styles.limeHighlight}>{formatCurrency(revenuOccupes)}</strong>.
          </p>

          {/* Barre de choix rapide intégrée au Héro */}
          <div className={styles.choiceBar} role="tablist" aria-label="Choix de vue">
            <button
              type="button"
              role="tab"
              aria-selected={activeTab === 'dashboard'}
              className={`${styles.choiceBtn} ${activeTab === 'dashboard' ? styles.choiceBtnActive : ''}`}
              onClick={() => setActiveTab('dashboard')}
            >
              <LayoutDashboard size={17} />
              <span>Tableau de bord</span>
            </button>

            <button
              type="button"
              role="tab"
              aria-selected={activeTab === 'biens'}
              className={`${styles.choiceBtn} ${activeTab === 'biens' ? styles.choiceBtnActive : ''}`}
              onClick={() => setActiveTab('biens')}
            >
              <Building2 size={17} />
              <span>Afficher les biens ({totalBiens})</span>
            </button>

            {canWrite && (
              <button
                type="button"
                role="tab"
                aria-selected={activeTab === 'ajout'}
                className={`${styles.choiceBtn} ${activeTab === 'ajout' ? styles.choiceBtnActive : ''}`}
                onClick={() => setActiveTab('ajout')}
              >
                <PlusCircle size={17} />
                <span>Ajouter un bien</span>
              </button>
            )}
          </div>
        </div>

        <div className={styles.heroStat}>
          <span className={styles.heroStatLabel}>Taux d&apos;occupation</span>
          <span className={styles.heroStatValue}>{tauxOccupation}%</span>
          <div className={styles.heroStatTrack}>
            <div className={styles.heroStatBar} style={{ '--target': `${tauxOccupation}%` }} />
          </div>
          <span className={styles.heroStatHint}>
            <span className={styles.liveDot} /> Synchronisé avec PostgreSQL
          </span>
        </div>
      </section>

      {/* =====================================================================
          CHOIX 1 : TABLEAU DE BORD ANALYTIQUE
         ===================================================================== */}
      {activeTab === 'dashboard' && (
        <>
          <div className={styles.kpiGrid}>
            <KpiCard title="Total des biens" value={totalBiens} subtitle="Ensemble du parc" icon={Building2} color="brown" delay={40} />
            <KpiCard title="Disponibles" value={biensLibres} subtitle="Prêts à la location" icon={CheckCircle} color="green" delay={100} progress={totalBiens ? (biensLibres / totalBiens) * 100 : 0} />
            <KpiCard title="Biens Loués" value={biensOccupes} subtitle="Baux actifs" icon={Home} color="brown" delay={160} progress={tauxOccupation} />
            <KpiCard title="Revenu réalisé" value={revenuOccupes} format={(v) => formatCurrency(v)} subtitle={`${tauxRevenu}% du potentiel (${formatCurrency(revenuPotentiel)})`} icon={TrendingUp} color="brown" delay={220} progress={tauxRevenu} />
            <KpiCard title="Charges" value={totalCharges} format={(v) => formatCurrency(v)} subtitle="Provisions mensuelles" icon={Coins} color="beige" delay={280} />
          </div>

          {totalBiens > 0 && <DashboardCharts biens={biens} />}

          {featured.length > 0 && (
            <section className={styles.section}>
              <div className={styles.sectionHeader}>
                <div>
                  <h2 className={styles.sectionTitle}>Biens à la une</h2>
                  <p className={styles.sectionSubtitle}>Les biens premium au loyer le plus élevé de votre parc</p>
                </div>
                <button
                  type="button"
                  onClick={() => setActiveTab('biens')}
                  className={styles.seeAllLink}
                >
                  Afficher tous les biens <ArrowRight size={16} />
                </button>
              </div>
              <div className={styles.featuredGrid}>
                {featured.map((b, i) => (
                  <BienCard key={b.id} bien={b} index={i} onDelete={canWrite ? (item) => setBienToDelete(item) : undefined} />
                ))}
              </div>
            </section>
          )}

          <section className={styles.section}>
            <div className={styles.sectionHeader}>
              <div>
                <h2 className={styles.sectionTitle}>Derniers biens enregistrés</h2>
                <p className={styles.sectionSubtitle}>Activité récente du catalogue</p>
              </div>
              <Link to="/biens" className={styles.seeAllLink}>
                Ouvrir la vue pleine page ({totalBiens}) <ExternalLink size={15} />
              </Link>
            </div>

            {recentBiens.length === 0 ? (
              <Card className={styles.emptyCard}>
                <Building2 size={40} color="#8E8E99" />
                <p>Aucun bien enregistré dans le parc pour le moment.</p>
                {canWrite && (
                  <Button variant="primary" size="sm" icon={PlusCircle} onClick={() => setActiveTab('ajout')}>
                    Ajouter votre premier bien
                  </Button>
                )}
              </Card>
            ) : (
              <Card className={styles.activityCard}>
                {recentBiens.map((b, i) => (
                  <Link
                    key={b.id}
                    to={`/biens/${b.id}`}
                    className={`${styles.activityRow} animate-fade-up`}
                    style={{ animationDelay: `${i * 55}ms` }}
                  >
                    <img src={getBienImage(b.type)} alt="" className={styles.activityThumb} loading="lazy" />
                    <div className={styles.activityMain}>
                      <span className={styles.activityTitle}>{b.adresse}</span>
                      <span className={styles.activityMeta}>
                        <MapPin size={13} /> {b.ville} · {b.type} · {formatSurface(b.surface)}
                      </span>
                    </div>
                    <span className={styles.activityPrice}>{formatCurrency(b.prixMensuel)}</span>
                    <Badge statut={b.statut} />
                    <span className={styles.activityDate}>{formatDate(b.createdAt)}</span>
                    <ArrowRight size={16} className={styles.activityArrow} />
                  </Link>
                ))}
              </Card>
            )}
          </section>
        </>
      )}

      {/* =====================================================================
          CHOIX 2 : AFFICHER LES BIENS (CATALOGUE INTERACTIF AVEC IMAGES)
         ===================================================================== */}
      {activeTab === 'biens' && (
        <section className={`${styles.section} animate-fade-up`}>
          <div className={styles.catalogueBar}>
            <div className={styles.searchBox}>
              <Search size={17} className={styles.searchIcon} />
              <input
                type="text"
                value={quickSearch}
                onChange={(e) => setQuickSearch(e.target.value)}
                placeholder="Rechercher par adresse, ville (Casablanca, Rabat...) ou type..."
                className={styles.searchInput}
              />
            </div>

            <div className={styles.pillsGroup}>
              {[
                { key: 'ALL', label: `Tous (${totalBiens})` },
                { key: 'LIBRE', label: `Libres (${biensLibres})` },
                { key: 'OCCUPE', label: `Occupés (${biensOccupes})` },
              ].map((tab) => (
                <button
                  key={tab.key}
                  type="button"
                  onClick={() => setQuickStatut(tab.key)}
                  className={`${styles.filterPill} ${quickStatut === tab.key ? styles.filterPillActive : ''}`}
                >
                  {tab.label}
                </button>
              ))}
            </div>

            {canWrite && (
              <Button variant="primary" icon={PlusCircle} onClick={() => setActiveTab('ajout')}>
                Ajouter un bien
              </Button>
            )}
          </div>

          {filteredBiens.length === 0 ? (
            <Card className={styles.emptyCard}>
              <Building2 size={42} color="#8E8E99" />
              <h3>Aucun bien ne correspond à votre filtre</h3>
              <Button
                variant="secondary"
                size="sm"
                onClick={() => {
                  setQuickSearch('');
                  setQuickStatut('ALL');
                }}
              >
                Réinitialiser
              </Button>
            </Card>
          ) : (
            <div className={styles.featuredGrid}>
              {filteredBiens.map((b, i) => (
                <BienCard
                  key={b.id}
                  bien={b}
                  index={i}
                  onDelete={canWrite ? (item) => setBienToDelete(item) : undefined}
                />
              ))}
            </div>
          )}
        </section>
      )}

      {/* =====================================================================
          CHOIX 3 : AJOUTER UN BIEN (CRÉATION RAPIDE AVEC APERÇU PHOTO EN DIRECT)
         ===================================================================== */}
      {activeTab === 'ajout' && canWrite && (
        <section className={`${styles.quickAddGrid} animate-fade-up`}>
          <Card className={styles.quickAddFormCard}>
            <div className={styles.quickAddHeader}>
              <span className={styles.quickAddBadge}>NOUVEAU LOGEMENT</span>
              <h2 className={styles.sectionTitle}>Ajouter un bien au patrimoine</h2>
              <p className={styles.sectionSubtitle}>
                Le bien sera créé avec le statut <strong>LIBRE</strong> et apparaîtra immédiatement dans le catalogue.
              </p>
            </div>

            <form onSubmit={handleSubmit(onCreateBien)} className={styles.quickForm}>
              <div className={styles.typeSelectorRow}>
                <span className={styles.fieldLabel}>Choisir la typologie :</span>
                <div className={styles.typeChips}>
                  {QUICK_TYPES.map((t) => (
                    <button
                      key={t}
                      type="button"
                      onClick={() => setValue('type', t, { shouldValidate: true })}
                      className={`${styles.typeChip} ${watchedType === t ? styles.typeChipActive : ''}`}
                    >
                      {t}
                    </button>
                  ))}
                </div>
              </div>

              <div className={styles.formFieldsGrid}>
                <div className={`${styles.fieldGroup} ${styles.fullRow}`}>
                  <label className={styles.fieldLabel} htmlFor="q-adresse">Adresse complète *</label>
                  <div className={styles.fieldInputWrap}>
                    <MapPin size={17} className={styles.fieldIcon} />
                    <input
                      id="q-adresse"
                      type="text"
                      placeholder="Ex : 18 Boulevard d'Anfa, Étage 4"
                      {...register('adresse')}
                      className={styles.fieldInput}
                    />
                  </div>
                  {errors.adresse && <span className={styles.fieldErr}>{errors.adresse.message}</span>}
                </div>

                <div className={styles.fieldGroup}>
                  <label className={styles.fieldLabel} htmlFor="q-ville">Ville *</label>
                  <input
                    id="q-ville"
                    type="text"
                    placeholder="Casablanca, Rabat, Marrakech..."
                    {...register('ville')}
                    className={styles.fieldInputPlain}
                  />
                  {errors.ville && <span className={styles.fieldErr}>{errors.ville.message}</span>}
                </div>

                <div className={styles.fieldGroup}>
                  <label className={styles.fieldLabel} htmlFor="q-type">Type personnalisé *</label>
                  <input
                    id="q-type"
                    type="text"
                    {...register('type')}
                    className={styles.fieldInputPlain}
                  />
                  {errors.type && <span className={styles.fieldErr}>{errors.type.message}</span>}
                </div>

                <div className={styles.fieldGroup}>
                  <label className={styles.fieldLabel} htmlFor="q-surface">Surface (m²) *</label>
                  <div className={styles.fieldInputWrap}>
                    <Maximize size={17} className={styles.fieldIcon} />
                    <input
                      id="q-surface"
                      type="number"
                      step="0.5"
                      {...register('surface', { valueAsNumber: true })}
                      className={styles.fieldInput}
                    />
                  </div>
                  {errors.surface && <span className={styles.fieldErr}>{errors.surface.message}</span>}
                </div>

                <div className={styles.fieldGroup}>
                  <label className={styles.fieldLabel} htmlFor="q-prix">Loyer mensuel (DH) *</label>
                  <div className={styles.fieldInputWrap}>
                    <Coins size={17} className={styles.fieldIcon} />
                    <input
                      id="q-prix"
                      type="number"
                      step="100"
                      {...register('prixMensuel', { valueAsNumber: true })}
                      className={styles.fieldInput}
                    />
                  </div>
                  {errors.prixMensuel && <span className={styles.fieldErr}>{errors.prixMensuel.message}</span>}
                </div>

                <div className={styles.fieldGroup}>
                  <label className={styles.fieldLabel} htmlFor="q-charges">Charges mensuelles (DH)</label>
                  <div className={styles.fieldInputWrap}>
                    <Coins size={17} className={styles.fieldIcon} />
                    <input
                      id="q-charges"
                      type="number"
                      step="50"
                      {...register('charges', { valueAsNumber: true })}
                      className={styles.fieldInput}
                    />
                  </div>
                  {errors.charges && <span className={styles.fieldErr}>{errors.charges.message}</span>}
                </div>
              </div>

              <div className={styles.quickFormActions}>
                <Button type="button" variant="ghost" onClick={() => setActiveTab('dashboard')}>
                  Retour au tableau de bord
                </Button>
                <Button type="submit" variant="primary" size="lg" disabled={submitting} icon={Save}>
                  {submitting ? 'Création en cours...' : 'Enregistrer le bien'}
                </Button>
              </div>
            </form>
          </Card>

          {/* Aperçu visuel en temps réel */}
          <div className={styles.previewCol}>
            <div className={styles.previewCard}>
              <div className={styles.previewMedia}>
                <img src={getBienImage(watchedType)} alt="Aperçu du bien" className={styles.previewImg} />
                <div className={styles.previewOverlay} />
                <span className={styles.previewBadge}>{watchedType || 'Appartement'}</span>
                <div className={styles.previewPrice}>
                  {formatCurrency(Number(watchedPrix) || 0)} <span>/ mois</span>
                </div>
              </div>
              <div className={styles.previewBody}>
                <span className={styles.previewEyebrow}>APERÇU CARTE EN DIRECT</span>
                <h4>{watch('adresse') || 'Adresse du nouveau logement'}</h4>
                <p>
                  <MapPin size={14} /> {watchedVille || 'Ville'} · {formatSurface(Number(watchedSurface) || 0)}
                </p>
              </div>
            </div>
          </div>
        </section>
      )}

      {/* Modale confirmation Suppression */}
      <Modal
        isOpen={Boolean(bienToDelete)}
        onClose={() => setBienToDelete(null)}
        title="Confirmer la suppression"
      >
        <p style={{ marginBottom: '16px' }}>
          Voulez-vous supprimer le bien situé au <strong>{bienToDelete?.adresse}</strong> ?
        </p>
        {bienToDelete?.statut === 'OCCUPE' && (
          <div
            style={{
              backgroundColor: 'var(--color-red-light)',
              color: 'var(--color-red)',
              padding: '12px',
              borderRadius: '10px',
              marginBottom: '16px',
              fontSize: '13px',
            }}
          >
            Attention : ce bien est actuellement OCCUPÉ. Le serveur refusera la suppression (erreur 409).
          </div>
        )}
        <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '12px' }}>
          <Button variant="ghost" onClick={() => setBienToDelete(null)} disabled={deleting}>
            Annuler
          </Button>
          <Button variant="danger" onClick={confirmDelete} disabled={deleting}>
            {deleting ? 'Suppression...' : 'Supprimer'}
          </Button>
        </div>
      </Modal>
    </div>
  );
};
