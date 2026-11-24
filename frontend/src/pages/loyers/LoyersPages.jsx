import React, { useCallback, useEffect, useMemo, useState } from 'react';
import { NavLink } from 'react-router-dom';
import { AlertCircle, CreditCard, RefreshCw } from 'lucide-react';
import { loyersApi } from '../../api/loyersApi';
import { useAuth } from '../../context/AuthContext';
import { useToast } from '../../context/ToastContext';
import { Button } from '../../components/ui/Button';
import { Card } from '../../components/ui/Card';
import { Modal } from '../../components/ui/Modal';
import { formatCurrency, formatDate } from '../../utils/format';
import styles from './LoyersPages.module.css';

const statusLabels = { A_PAYER: 'À payer', EN_RETARD: 'En retard', IMPAYEE: 'Impayée', PAYEE: 'Payée' };
const monthName = (m) => new Intl.DateTimeFormat('fr-FR', { month: 'long' }).format(new Date(2024, Number(m) - 1, 1));
const errorMessage = (err) => err?.response?.data?.message || (err?.response ? 'Une erreur est survenue lors de la communication avec le service Loyers.' : 'Le service Loyers est indisponible. Vérifiez qu’il fonctionne sur le port 8083.');

function Header({ title, subtitle }) {
  return <header className={styles.header}><div><h1 className={styles.title}>{title}</h1><p className={styles.subtitle}>{subtitle}</p></div></header>;
}
function Tabs() {
  return <nav className={styles.tabs} aria-label="Navigation Loyers">
    <NavLink end to="/loyers" className={({ isActive }) => `${styles.tab} ${isActive ? styles.tabActive : ''}`}>Échéances</NavLink>
    <NavLink to="/loyers/historique" className={({ isActive }) => `${styles.tab} ${isActive ? styles.tabActive : ''}`}>Historique des paiements</NavLink>
    <NavLink to="/loyers/retards" className={({ isActive }) => `${styles.tab} ${isActive ? styles.tabActive : ''}`}>Retards</NavLink>
  </nav>;
}
function PageFrame({ title, subtitle, children }) { return <div className={styles.page}><Header title={title} subtitle={subtitle} /><Tabs />{children}</div>; }

export function EcheancesPage() {
  const { canWrite } = useAuth();
  const { addToast } = useToast();
  const [rows, setRows] = useState([]); const [baux, setBaux] = useState([]);
  const [status, setStatus] = useState(''); const [month, setMonth] = useState(''); const [search, setSearch] = useState('');
  const [loading, setLoading] = useState(true); const [error, setError] = useState(''); const [selected, setSelected] = useState(null);
  const load = useCallback(async () => {
    setLoading(true); setError('');
    try { const [e, b] = await Promise.all([loyersApi.getEcheances(status ? { statut: status } : {}), loyersApi.getBaux()]); setRows(e); setBaux(b); }
    catch (err) { setError(errorMessage(err)); } finally { setLoading(false); }
  }, [status]);
  useEffect(() => { load(); }, [load]);
  const bailMap = useMemo(() => new Map(baux.map((b) => [b.id, b])), [baux]);
  const filtered = rows.filter((e) => (!month || Number(e.mois) === Number(month)) && (!search || `${e.bailId} ${bailMap.get(e.bailId)?.locataireId || ''} ${bailMap.get(e.bailId)?.bienId || ''}`.toLowerCase().includes(search.toLowerCase())));
  const pay = async (payload) => { try { await loyersApi.createPaiement(payload); addToast('Paiement enregistré avec succès.'); setSelected(null); await load(); } catch (err) { addToast(errorMessage(err), 'error'); } };
  return <PageFrame title="Échéances de loyer" subtitle="Suivez les loyers dus, les paiements reçus et les soldes de chaque période.">
    <Card><div className={styles.filters}>
      <input className={styles.input} aria-label="Rechercher" placeholder="Rechercher par bail, locataire ou bien…" value={search} onChange={(e) => setSearch(e.target.value)} />
      <select className={styles.select} aria-label="Filtrer par statut" value={status} onChange={(e) => setStatus(e.target.value)}><option value="">Tous les statuts</option>{Object.entries(statusLabels).map(([k,v]) => <option key={k} value={k}>{v}</option>)}</select>
      <select className={styles.select} aria-label="Filtrer par mois" value={month} onChange={(e) => setMonth(e.target.value)}><option value="">Tous les mois</option>{Array.from({length:12},(_,i) => <option key={i+1} value={i+1}>{monthName(i+1)}</option>)}</select>
      <Button variant="secondary" icon={RefreshCw} onClick={load}>Actualiser</Button>
    </div></Card>
    <Card>{loading ? <p className={styles.notice}>Chargement des échéances…</p> : error ? <div className={`${styles.notice} ${styles.error}`}><AlertCircle size={18} /> {error}</div> : filtered.length === 0 ? <p className={styles.notice}>Aucune échéance ne correspond à vos filtres.</p> : <div className={styles.tableWrap}><table className={styles.table}><thead><tr><th>Période</th><th>Bail / locataire</th><th>Montant</th><th>Versé</th><th>Reste à payer</th><th>Statut</th><th>Action</th></tr></thead><tbody>{filtered.map((e) => { const bail = bailMap.get(e.bailId); return <tr key={e.id}><td>{monthName(e.mois)} {e.annee}<div className={styles.muted}>Échéance : {formatDate(e.dateLimite)}</div></td><td><strong>{bail?.locataireId || 'Bail'}</strong><div className={styles.muted}>{bail?.bienId || e.bailId}</div></td><td>{formatCurrency(e.montant)}</td><td>{formatCurrency(e.totalPaye)}</td><td className={styles.money}>{formatCurrency(e.resteAPayer)}</td><td><span className={styles.status} data-status={e.statut}>{statusLabels[e.statut] || e.statut}</span></td><td>{canWrite && Number(e.resteAPayer) > 0 && <Button size="sm" icon={CreditCard} onClick={() => setSelected(e)}>Enregistrer</Button>}</td></tr>; })}</tbody></table></div>}</Card>
    <PaymentModal echeance={selected} onClose={() => setSelected(null)} onSubmit={pay} />
  </PageFrame>;
}

function PaymentModal({ echeance, onClose, onSubmit }) {
  const [amount, setAmount] = useState(''); const [date, setDate] = useState(new Date().toISOString().slice(0,10)); const [mode, setMode] = useState('Virement'); const [saving, setSaving] = useState(false); const [validation, setValidation] = useState('');
  useEffect(() => { setAmount(echeance?.resteAPayer ? String(echeance.resteAPayer) : ''); setValidation(''); }, [echeance]);
  if (!echeance) return null;
  const submit = async (event) => { event.preventDefault(); const n = Number(amount); if (!Number.isFinite(n) || n < 0.01) return setValidation('Le montant doit être supérieur ou égal à 0,01.'); if (n > Number(echeance.resteAPayer)) return setValidation('Le montant ne peut pas dépasser le reste à payer.'); if (date > new Date().toISOString().slice(0,10)) return setValidation('La date de paiement ne peut pas être dans le futur.'); setSaving(true); await onSubmit({ echeanceId: echeance.id, montant: n, datePaiement: date, modePaiement: mode }); setSaving(false); };
  return <Modal isOpen={Boolean(echeance)} onClose={onClose} title="Enregistrer un paiement"><form className={styles.form} onSubmit={submit}>
    <div className={styles.remainder}>Reste à payer : <strong>{formatCurrency(echeance.resteAPayer)}</strong></div>
    <label>Montant (DH)<input className={styles.input} type="number" min="0.01" max={echeance.resteAPayer} step="0.01" required value={amount} onChange={(e) => setAmount(e.target.value)} /></label>
    <label>Date du paiement<input className={styles.input} type="date" max={new Date().toISOString().slice(0,10)} required value={date} onChange={(e) => setDate(e.target.value)} /></label>
    <label>Mode de paiement<select className={styles.select} value={mode} onChange={(e) => setMode(e.target.value)}><option>Virement</option><option>Espèces</option><option>Chèque</option><option>Carte bancaire</option></select></label>
    {validation && <p className={styles.error} role="alert">{validation}</p>}<div className={styles.actions}><Button variant="secondary" onClick={onClose}>Annuler</Button><Button type="submit" disabled={saving}>{saving ? 'Enregistrement…' : 'Confirmer le paiement'}</Button></div>
  </form></Modal>;
}

export function HistoriquePaiementsPage() {
  const [rows, setRows] = useState([]); const [baux, setBaux] = useState([]); const [selectedBail, setSelectedBail] = useState(''); const [loading, setLoading] = useState(true); const [error, setError] = useState(''); const [bauxLoaded, setBauxLoaded] = useState(false);
  useEffect(() => { loyersApi.getBaux().then(setBaux).catch((e) => setError(errorMessage(e))).finally(() => setBauxLoaded(true)); }, []);
  useEffect(() => { if (!bauxLoaded) return; let active = true; setLoading(true); setError(''); const load = selectedBail ? loyersApi.getPaiements({ bailId: selectedBail }) : Promise.all(baux.map((b) => loyersApi.getPaiements({ bailId: b.id }))).then((byBail) => byBail.flat()); load.then((data) => { if (active) setRows(data); }).catch((e) => { if (active) setError(errorMessage(e)); }).finally(() => active && setLoading(false)); return () => { active = false; }; }, [selectedBail, baux, bauxLoaded]);
  return <PageFrame title="Historique des paiements" subtitle="Consultez les règlements confirmés par échéance et par bail."><Card><div className={styles.filters}><select className={styles.select} aria-label="Filtrer par bail" value={selectedBail} onChange={(e) => setSelectedBail(e.target.value)}><option value="">Tous les baux accessibles</option>{baux.map((b) => <option key={b.id} value={b.id}>{b.locataireId} · {b.bienId}</option>)}</select></div></Card><Card>{loading ? <p className={styles.notice}>Chargement de l’historique…</p> : error ? <p className={`${styles.notice} ${styles.error}`}>{error}</p> : !rows.length ? <p className={styles.notice}>Aucun paiement enregistré pour cette sélection.</p> : <div className={styles.tableWrap}><table className={styles.table}><thead><tr><th>Date</th><th>Échéance</th><th>Montant</th><th>Mode</th><th>Référence</th></tr></thead><tbody>{rows.map((p) => <tr key={p.id}><td>{formatDate(p.datePaiement)}</td><td>{p.echeanceId}</td><td className={styles.money}>{formatCurrency(p.montant)}</td><td>{p.modePaiement}</td><td className={styles.muted}>{p.id}</td></tr>)}</tbody></table></div>}</Card></PageFrame>;
}

export function RetardsPage() {
  const [rows, setRows] = useState([]); const [baux, setBaux] = useState([]); const [loading, setLoading] = useState(true); const [error, setError] = useState('');
  const load = useCallback(async () => { setLoading(true); setError(''); try { const [late, unpaid, contracts] = await Promise.all([loyersApi.getEcheances({ statut: 'EN_RETARD' }), loyersApi.getEcheances({ statut: 'IMPAYEE' }), loyersApi.getBaux()]); setRows([...late, ...unpaid]); setBaux(contracts); } catch (e) { setError(errorMessage(e)); } finally { setLoading(false); } }, []);
  useEffect(() => { load(); }, [load]); const byBail = new Map(baux.map((b) => [b.id, b]));
  return <PageFrame title="Loyers en retard" subtitle="Échéances dépassant leur date limite ou déclarées impayées."><Card>{loading ? <p className={styles.notice}>Chargement des retards…</p> : error ? <p className={`${styles.notice} ${styles.error}`}>{error}</p> : !rows.length ? <p className={styles.notice}>Aucun retard à signaler.</p> : <div className={styles.tableWrap}><table className={styles.table}><thead><tr><th>Période</th><th>Locataire / bien</th><th>Date limite</th><th>Reste dû</th><th>Statut</th></tr></thead><tbody>{rows.map((e) => <tr key={e.id}><td>{monthName(e.mois)} {e.annee}</td><td>{byBail.get(e.bailId)?.locataireId || '—'}<div className={styles.muted}>{byBail.get(e.bailId)?.bienId || e.bailId}</div></td><td>{formatDate(e.dateLimite)}</td><td className={styles.money}>{formatCurrency(e.resteAPayer)}</td><td><span className={styles.status} data-status={e.statut}>{statusLabels[e.statut] || e.statut}</span></td></tr>)}</tbody></table></div>}</Card></PageFrame>;
}
