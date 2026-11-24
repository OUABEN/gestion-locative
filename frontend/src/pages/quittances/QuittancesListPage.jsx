import React, { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import {
  ArrowRight,
  ChevronLeft,
  ChevronRight,
  Download,
  FilePlus2,
  FileText,
  RefreshCw,
  Search,
} from 'lucide-react';
import { quittancesApi, saveBlob } from '../../api/quittancesApi';
import { useAuth } from '../../context/AuthContext';
import { Button } from '../../components/ui/Button';
import { Card } from '../../components/ui/Card';
import {
  formatLocalDate,
  formatMad,
  quittanceErrorMessage,
  readQuittancesIdentity,
} from './quittancesUtils';
import styles from './QuittancesPage.module.css';

const PAGE_SIZE = 20;

const emptyFilters = {
  bailId: '',
  numero: '',
  statut: '',
  debut: '',
  fin: '',
  tri: 'creeLe',
  sens: 'desc',
};

const documentStatusLabel = (statut) =>
  String(statut || 'INCONNU').replaceAll('_', ' ').toLowerCase();

export const QuittancesListPage = () => {
  const { token } = useAuth();
  const [identity, setIdentity] = useState(null);
  const [filters, setFilters] = useState(emptyFilters);
  const [appliedFilters, setAppliedFilters] = useState(emptyFilters);
  const [page, setPage] = useState(0);
  const [result, setResult] = useState(null);
  const [dashboard, setDashboard] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [dashboardError, setDashboardError] = useState('');
  const [exporting, setExporting] = useState(false);
  const [exportError, setExportError] = useState('');
  const [documentaryAccessForbidden, setDocumentaryAccessForbidden] = useState(false);
  const [refresh, setRefresh] = useState(0);

  useEffect(() => {
    let cancelled = false;
    const run = async () => {
      setLoading(true);
      setError('');
      setDashboardError('');
      setDocumentaryAccessForbidden(false);
      try {
        const verifiedIdentity = readQuittancesIdentity(
          await quittancesApi.getIdentity(token)
        );
        if (cancelled) return;
        setIdentity(verifiedIdentity);
        const params = { ...appliedFilters, page, taille: PAGE_SIZE };
        const [listResult, dashboardResult] = await Promise.allSettled([
          quittancesApi.list(token, params),
          quittancesApi.getDashboard(token),
        ]);
        if (cancelled) return;
        if (listResult.status === 'fulfilled') {
          setResult(listResult.value);
        } else {
          setResult(null);
          const listError = quittanceErrorMessage(listResult.reason);
          setError(listError);
          if (listResult.reason?.status === 403) {
            setDocumentaryAccessForbidden(true);
          }
        }
        if (dashboardResult.status === 'fulfilled') {
          setDashboard(dashboardResult.value);
        } else {
          setDashboard(null);
          if (
            listResult.status !== 'fulfilled'
            && listResult.reason?.status === 403
            && dashboardResult.reason?.status === 403
          ) {
            setDashboardError('');
          } else {
            setDashboardError(quittanceErrorMessage(dashboardResult.reason));
          }
          if (dashboardResult.reason?.status === 403) {
            setDocumentaryAccessForbidden(true);
          }
        }
      } catch (loadError) {
        if (!cancelled) {
          setIdentity(null);
          setResult(null);
          setDashboard(null);
          setError(quittanceErrorMessage(loadError));
          if (loadError?.status === 403) {
            setDocumentaryAccessForbidden(true);
          }
        }
      } finally {
        if (!cancelled) setLoading(false);
      }
    };
    run();
    return () => {
      cancelled = true;
    };
  }, [token, appliedFilters, page, refresh]);

  const submitFilters = (event) => {
    event.preventDefault();
    setPage(0);
    setAppliedFilters({ ...filters });
  };

  const exportCsv = async () => {
    setExporting(true);
    setExportError('');
    try {
      const blob = await quittancesApi.exportCsv(token, appliedFilters);
      saveBlob(blob, 'quittances.csv');
    } catch (exportFailure) {
      setExportError(quittanceErrorMessage(exportFailure));
    } finally {
      setExporting(false);
    }
  };

  const pageCount = result?.totalPages || 0;

  return (
    <div className={styles.page}>
      <header className={styles.header}>
        <div>
          <p className={styles.eyebrow}>ESPACE DOCUMENTS</p>
          <h1>Quittances</h1>
          <p className={styles.muted}>
            Consultation et suivi des quittances autorisés par le service Quittances.
          </p>
        </div>
        <div className={styles.headerActions}>
          <Button
            variant="secondary"
            icon={RefreshCw}
            onClick={() => setRefresh((value) => value + 1)}
            disabled={loading}
          >
            Actualiser
          </Button>
          {identity?.role === 'BAILLEUR' && !documentaryAccessForbidden && (
            <Link to="/quittances/nouveau">
              <Button icon={FilePlus2}>Préparer une quittance</Button>
            </Link>
          )}
        </div>
      </header>

      {identity && (
        <p className={styles.identityLine}>
          Identité validée par Quittances · rôle {identity.role}
        </p>
      )}

      {dashboard && (
        <section className={styles.dashboardGrid} aria-label="Indicateurs Quittances">
          <Card className={styles.statCard}>
            <span>Total des documents</span>
            <strong>{dashboard.totalDocuments}</strong>
          </Card>
          {(dashboard.groupes || []).map((group) => (
            <Card className={styles.statCard} key={`${group.statut}-${group.typeDocument}`}>
              <span>{documentStatusLabel(group.statut)} · {group.typeDocument}</span>
              <strong>{group.nombre}</strong>
            </Card>
          ))}
        </section>
      )}
      {dashboardError && <p className={styles.inlineError}>{dashboardError}</p>}

      <Card className={styles.filterCard}>
        <form className={styles.filters} onSubmit={submitFilters}>
          <label>
            Identifiant du bail
            <input
              value={filters.bailId}
              onChange={(event) => setFilters({ ...filters, bailId: event.target.value })}
              maxLength={100}
            />
          </label>
          <label>
            Numéro exact
            <input
              value={filters.numero}
              onChange={(event) => setFilters({ ...filters, numero: event.target.value })}
              maxLength={100}
            />
          </label>
          <label>
            Statut
            <select
              value={filters.statut}
              onChange={(event) => setFilters({ ...filters, statut: event.target.value })}
            >
              <option value="">Tous</option>
              {[
                'BROUILLON',
                'EN_ATTENTE_VALIDATION',
                'EN_ATTENTE_SIGNATURE',
                'EMIS',
                'A_REEXAMINER',
                'ANNULE',
                'REMPLACE',
              ].map((status) => <option key={status} value={status}>{status}</option>)}
            </select>
          </label>
          <label>
            Période depuis
            <input
              type="date"
              value={filters.debut}
              onChange={(event) => setFilters({ ...filters, debut: event.target.value })}
            />
          </label>
          <label>
            Jusqu’au
            <input
              type="date"
              value={filters.fin}
              onChange={(event) => setFilters({ ...filters, fin: event.target.value })}
            />
          </label>
          <label>
            Trier par
            <select
              value={filters.tri}
              onChange={(event) => setFilters({ ...filters, tri: event.target.value })}
            >
              <option value="creeLe">Date de création</option>
              <option value="periodeDebut">Début de période</option>
              <option value="numero">Numéro</option>
              <option value="totalPaye">Montant payé</option>
            </select>
          </label>
          <label>
            Ordre
            <select
              value={filters.sens}
              onChange={(event) => setFilters({ ...filters, sens: event.target.value })}
            >
              <option value="desc">Décroissant</option>
              <option value="asc">Croissant</option>
            </select>
          </label>
          <div className={styles.filterActions}>
            <Button type="submit" icon={Search}>Appliquer</Button>
            <Button
              variant="secondary"
              type="button"
              icon={Download}
              onClick={exportCsv}
              disabled={exporting || !identity || documentaryAccessForbidden}
            >
              {exporting ? 'Export…' : 'Exporter CSV'}
            </Button>
          </div>
        </form>
        {exportError && <p className={styles.inlineError} role="alert">{exportError}</p>}
      </Card>

      {error && (
        <Card className={styles.errorCard} role="alert">
          <FileText size={22} />
          <p>{error}</p>
          <Button variant="secondary" onClick={() => setRefresh((value) => value + 1)}>
            Réessayer
          </Button>
        </Card>
      )}
      {loading && <Card className={styles.stateCard} role="status">Chargement des quittances…</Card>}

      {!loading && !error && result && (
        <>
          {result.contenu.length === 0 ? (
            <Card className={styles.stateCard}>Aucune quittance pour ces critères.</Card>
          ) : (
            <Card className={styles.tableCard}>
              <div className={styles.tableScroll}>
                <table className={styles.table}>
                  <thead>
                    <tr>
                      <th>Numéro / document</th>
                      <th>Locataire</th>
                      <th>Bien</th>
                      <th>Période</th>
                      <th>Montant payé</th>
                      <th>État / signature</th>
                      <th><span className={styles.visuallyHidden}>Actions</span></th>
                    </tr>
                  </thead>
                  <tbody>
                    {result.contenu.map((document) => (
                      <tr key={document.id}>
                        <td>
                          <strong>{document.numero || 'Brouillon'}</strong>
                          <small>{document.id}</small>
                        </td>
                        <td>{[document.prenomLocataire, document.nomLocataire].filter(Boolean).join(' ') || '—'}</td>
                        <td>{document.adresseBien || document.bienId}</td>
                        <td>{formatLocalDate(document.periodeDebut)} – {formatLocalDate(document.periodeFin)}</td>
                        <td>{formatMad(document.totalPaye)}</td>
                        <td>
                          <span className={styles.statusPill}>{documentStatusLabel(document.statut)}</span>
                          <small>{document.statutSignature || '—'}</small>
                        </td>
                        <td>
                          <Link className={styles.rowLink} to={`/quittances/${document.id}`}>
                            Détail <ArrowRight size={14} aria-hidden="true" />
                          </Link>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </Card>
          )}
          <nav className={styles.pagination} aria-label="Pagination des quittances">
            <Button
              variant="secondary"
              icon={ChevronLeft}
              disabled={page <= 0 || loading}
              onClick={() => setPage((value) => Math.max(0, value - 1))}
            >
              Précédent
            </Button>
            <span>Page {page + 1} sur {Math.max(pageCount, 1)} · {result.totalElements} document(s)</span>
            <Button
              variant="secondary"
              icon={ChevronRight}
              disabled={pageCount === 0 || page + 1 >= pageCount || loading}
              onClick={() => setPage((value) => value + 1)}
            >
              Suivant
            </Button>
          </nav>
        </>
      )}
    </div>
  );
};
