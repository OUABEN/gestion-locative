import React, { useCallback, useEffect, useRef, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import {
  ArrowLeft,
  Archive,
  Check,
  CircleAlert,
  Download,
  Eye,
  FileCheck2,
  Mail,
  RefreshCw,
  RotateCcw,
  ShieldCheck,
  XCircle,
} from 'lucide-react';
import { quittancesApi, saveBlob } from '../../api/quittancesApi';
import { useAuth } from '../../context/AuthContext';
import { Button } from '../../components/ui/Button';
import { Card } from '../../components/ui/Card';
import { Modal } from '../../components/ui/Modal';
import {
  canCancelDocument,
  canIssueDemo,
  canPreviewDraft,
  canValidateDraft,
  createRequestId,
  formatLocalDate,
  formatMad,
  isDemoIssued,
  quittanceErrorMessage,
  readQuittancesIdentity,
} from './quittancesUtils';
import styles from './QuittancesPage.module.css';

const PAGE_SIZE = 10;

const statusClassName = (status) =>
  `${styles.statusPill} ${status === 'EMIS' ? styles.statusSuccess : ''}`;

const getPageCount = (page) =>
  Math.max(Number(page?.totalPages) || 0, Number(page?.totalElements) > 0 ? 1 : 0);

export const QuittanceDetailPage = () => {
  const { id } = useParams();
  const { token } = useAuth();
  const [identity, setIdentity] = useState(null);
  const [document, setDocument] = useState(null);
  const [assignments, setAssignments] = useState([]);
  const [history, setHistory] = useState(null);
  const [archives, setArchives] = useState(null);
  const [mailpit, setMailpit] = useState(null);
  const [identityError, setIdentityError] = useState('');
  const [error, setError] = useState('');
  const [sectionErrors, setSectionErrors] = useState({});
  const [loading, setLoading] = useState(true);
  const [refresh, setRefresh] = useState(0);
  const [historyPage, setHistoryPage] = useState(0);
  const [archivePage, setArchivePage] = useState(0);
  const [mailpitPage, setMailpitPage] = useState(0);
  const [dialog, setDialog] = useState(null);
  const [actionError, setActionError] = useState('');
  const [actionBusy, setActionBusy] = useState('');
  const [reason, setReason] = useState('');
  const [mailpitRequestId, setMailpitRequestId] = useState('');
  const [mailpitNotice, setMailpitNotice] = useState(null);
  const [previewUrl, setPreviewUrl] = useState('');
  const [verification, setVerification] = useState(null);
  const [paymentReport, setPaymentReport] = useState(null);
  const operationLock = useRef(false);

  const loadData = useCallback(async (isActive = () => true) => {
    setLoading(true);
    setError('');
    setIdentityError('');
    setSectionErrors({});
    try {
      const verifiedIdentity = readQuittancesIdentity(
        await quittancesApi.getIdentity(token)
      );
      if (!isActive()) return;
      setIdentity(verifiedIdentity);

      const [documentResult, assignmentsResult, historyResult] = await Promise.all([
        quittancesApi.getDocument(token, id),
        quittancesApi.getAssignments(token, id),
        quittancesApi.getHistory(token, id, { page: historyPage, taille: PAGE_SIZE }),
      ]);
      if (!isActive()) return;
      setDocument(documentResult);
      setAssignments(assignmentsResult);
      setHistory(historyResult);

      if (verifiedIdentity.role === 'BAILLEUR') {
        const [archivesResult, mailpitResult] = await Promise.allSettled([
          quittancesApi.getArchives(token, id, { page: archivePage, taille: PAGE_SIZE }),
          quittancesApi.getMailpitHistory(token, id, { page: mailpitPage, taille: PAGE_SIZE }),
        ]);
        if (!isActive()) return;
        if (archivesResult.status === 'fulfilled') {
          setArchives(archivesResult.value);
        } else {
          setArchives(null);
          setSectionErrors((current) => ({
            ...current,
            archives: quittanceErrorMessage(archivesResult.reason),
          }));
        }
        if (mailpitResult.status === 'fulfilled') {
          setMailpit(mailpitResult.value);
        } else {
          setMailpit(null);
          setSectionErrors((current) => ({
            ...current,
            mailpit: quittanceErrorMessage(mailpitResult.reason),
          }));
        }
      } else {
        setArchives(null);
        setMailpit(null);
      }
    } catch (loadError) {
      if (isActive()) {
        setDocument(null);
        setAssignments([]);
        setHistory(null);
        setError(quittanceErrorMessage(loadError));
        if (loadError instanceof Error && !loadError.status) {
          setIdentityError(loadError.message);
        }
      }
    } finally {
      if (isActive()) setLoading(false);
    }
  }, [token, id, historyPage, archivePage, mailpitPage]);

  useEffect(() => {
    let active = true;
    loadData(() => active);
    return () => {
      active = false;
    };
  }, [loadData, refresh]);

  useEffect(() => {
    if (!previewUrl) return undefined;
    return () => URL.revokeObjectURL(previewUrl);
  }, [previewUrl]);

  const refreshData = () => setRefresh((value) => value + 1);

  const openAction = (type, title, description, needsReason = false) => {
    setReason('');
    setActionError('');
    setDialog({ type, title, description, needsReason });
  };

  const closeDialog = () => {
    if (!actionBusy) setDialog(null);
  };

  const updateMailpitAfterFailure = async (failure) => {
    const message = failure?.message || '';
    const uncertain = failure?.status === undefined || /incertain|inconnu|interrompu/i.test(message);
    setMailpitNotice({
      kind: uncertain ? 'uncertain' : 'refused',
      text: uncertain
        ? 'Résultat Mailpit incertain. Aucun nouvel envoi automatique n’a été tenté; le même requeteId est conservé pour consulter l’idempotence.'
        : `Mailpit a refusé ou interrompu l’opération : ${message}. Aucun nouvel envoi automatique n’a été tenté.`,
    });
    if (!uncertain) setMailpitRequestId('');
    try {
      const latest = await quittancesApi.getMailpitHistory(token, id, {
        page: mailpitPage,
        taille: PAGE_SIZE,
      });
      setMailpit(latest);
    } catch (refreshError) {
      setSectionErrors((current) => ({
        ...current,
        mailpit: `Historique Mailpit non actualisé : ${quittanceErrorMessage(refreshError)}`,
      }));
    }
  };

  const performConfirmedAction = async (event) => {
    event?.preventDefault();
    if (!dialog || operationLock.current) return;
    if (dialog.needsReason && !reason.trim()) {
      setActionError('Un motif est obligatoire.');
      return;
    }

    operationLock.current = true;
    setActionBusy(dialog.type);
    setActionError('');
    try {
      let successMessage = '';
      switch (dialog.type) {
        case 'validate':
          await quittancesApi.validate(token, id);
          successMessage = 'Validation acceptée; l’état est actualisé depuis le serveur.';
          break;
        case 'cancel':
          await quittancesApi.cancel(token, id, reason.trim());
          successMessage = 'Annulation acceptée; l’état est actualisé depuis le serveur.';
          break;
        case 'replace':
          await quittancesApi.replace(token, id, reason.trim());
          successMessage = 'Remplacement préparé par le serveur.';
          break;
        case 'issue': {
          const result = await quittancesApi.issueDemo(token, id);
          successMessage = `Émission de démonstration acceptée (${result.numero || 'numéro retourné par le serveur'}). Ce n’est pas une signature certifiée.`;
          break;
        }
        case 'mailpit': {
          const requestId = mailpitRequestId || createRequestId();
          setMailpitRequestId(requestId);
          try {
            const result = await quittancesApi.sendMailpit(token, id, requestId);
            if (result.statut === 'ENVOYE') {
              setMailpitNotice({
                kind: 'accepted',
                text: 'Mailpit a accepté le message de démonstration. Cela ne signifie pas qu’un email réel a été reçu par le locataire.',
              });
              setMailpitRequestId('');
            } else if (result.statut === 'EN_ATTENTE_ENVOI') {
              setMailpitNotice({
                kind: 'pending',
                text: 'Envoi de démonstration en attente dans Mailpit; aucun email reçu par le locataire n’est confirmé.',
              });
            } else {
              setMailpitNotice({
                kind: 'refused',
                text: `État Mailpit retourné : ${result.statut}. Aucun réessai automatique n’a été effectué.`,
              });
            }
            successMessage = '';
          } catch (mailError) {
            await updateMailpitAfterFailure(mailError);
            setActionError(quittanceErrorMessage(mailError));
            return;
          }
          break;
        }
        default:
          return;
      }

      if (successMessage) setMailpitNotice(null);
      setDialog(null);
      setReason('');
      setActionError('');
      if (successMessage) setSectionErrors((current) => ({ ...current, operation: successMessage }));
      refreshData();
    } catch (requestError) {
      setActionError(quittanceErrorMessage(requestError));
    } finally {
      setActionBusy('');
      operationLock.current = false;
    }
  };

  const runReadAction = async (key, action) => {
    if (operationLock.current) return;
    operationLock.current = true;
    setActionBusy(key);
    setSectionErrors((current) => ({ ...current, [key]: '' }));
    try {
      await action();
    } catch (readError) {
      setSectionErrors((current) => ({
        ...current,
        [key]: quittanceErrorMessage(readError),
      }));
    } finally {
      setActionBusy('');
      operationLock.current = false;
    }
  };

  const previewDraft = () =>
    runReadAction('preview', async () => {
      const blob = await quittancesApi.previewDraft(token, id);
      const nextUrl = URL.createObjectURL(blob);
      setPreviewUrl(nextUrl);
    });

  const download = (key, fetchBlob, filename) =>
    runReadAction(key, async () => {
      const blob = await fetchBlob();
      saveBlob(blob, filename);
    });

  const createArchive = () => {
    if (operationLock.current) return;
    operationLock.current = true;
    setActionBusy('archive-create');
    setSectionErrors((current) => ({ ...current, archives: '' }));
    quittancesApi.archiveDraft(token, id)
      .then(() => refreshData())
      .catch((archiveError) => setSectionErrors((current) => ({
        ...current,
        archives: quittanceErrorMessage(archiveError),
      })))
      .finally(() => {
        setActionBusy('');
        operationLock.current = false;
      });
  };

  const checkIntegrity = () =>
    runReadAction('integrity', async () => {
      setVerification(await quittancesApi.verifyDemo(token, id));
    });

  const checkPayments = () =>
    runReadAction('payments', async () => {
      setPaymentReport(await quittancesApi.checkPayments(token, id));
    });

  if (loading && !document) {
    return <Card className={styles.stateCard} role="status">Chargement de la quittance…</Card>;
  }

  if (error || !document) {
    return (
      <Card className={styles.errorCard} role="alert">
        <CircleAlert size={24} />
        <h1>Quittance indisponible</h1>
        <p>{identityError || error || 'Le document n’est pas disponible.'}</p>
        <div className={styles.inlineActions}>
          <Link to="/quittances"><Button variant="secondary" icon={ArrowLeft}>Liste des quittances</Button></Link>
          <Button onClick={refreshData} disabled={loading}>Réessayer</Button>
        </div>
      </Card>
    );
  }

  const isLandlord = identity?.role === 'BAILLEUR';
  const emittedDemo = isDemoIssued(document);
  const replaceAvailable = isLandlord && document.statut === 'ANNULE';
  const previewAvailable = isLandlord && canPreviewDraft(document);
  const archiveAvailable = isLandlord && canPreviewDraft(document);
  const mailpitAvailable = isLandlord && document.statut === 'EMIS' && emittedDemo;
  const issueAvailable = isLandlord && canIssueDemo(document);
  const signatureDescription =
    document.statutSignature === 'SIGNE_VERIFIE'
      ? 'Signature locale de démonstration vérifiée; aucune certification d’identité.'
      : `État de signature : ${document.statutSignature || 'inconnu'}.`;

  return (
    <div className={styles.page}>
      <div className={styles.detailNav}>
        <Link to="/quittances" className={styles.backLink}><ArrowLeft size={17} /> Toutes les quittances</Link>
        <Button
          variant="secondary"
          icon={RefreshCw}
          onClick={refreshData}
          disabled={loading || Boolean(actionBusy)}
        >
          Actualiser
        </Button>
      </div>
      {sectionErrors.operation && <p className={styles.inlineSuccess} role="status">{sectionErrors.operation}</p>}

      <header className={styles.header}>
        <div>
          <p className={styles.eyebrow}>DÉTAIL DE LA QUITTANCE</p>
          <h1>{document.numero || 'Brouillon'}</h1>
          <p className={styles.muted}>Document {document.id} · bail {document.bailId}</p>
        </div>
        <div className={styles.statusStack}>
          <span className={statusClassName(document.statut)}>{document.statut}</span>
          <span className={styles.muted}>{document.statutSignature}</span>
        </div>
      </header>

      <div className={styles.detailGrid}>
        <Card className={styles.detailCard}>
          <h2>Parties et bien</h2>
          <dl className={styles.definitionGrid}>
            <div><dt>Bailleur</dt><dd>{document.nomBailleur || '—'}</dd></div>
            <div><dt>Locataire</dt><dd>{[document.prenomLocataire, document.nomLocataire].filter(Boolean).join(' ') || document.locataireId}</dd></div>
            <div><dt>Bien</dt><dd>{document.adresseBien || document.bienId}</dd></div>
            <div><dt>Identifiant du bien</dt><dd>{document.bienId}</dd></div>
          </dl>
        </Card>
        <Card className={styles.detailCard}>
          <h2>Période et montants</h2>
          <dl className={styles.definitionGrid}>
            <div><dt>Période</dt><dd>{formatLocalDate(document.periodeDebut)} – {formatLocalDate(document.periodeFin)}</dd></div>
            <div><dt>Loyer dû</dt><dd>{formatMad(document.loyerDu)}</dd></div>
            <div><dt>Charges dues</dt><dd>{formatMad(document.chargesDues)}</dd></div>
            <div><dt>Loyer payé</dt><dd>{formatMad(document.loyerPaye)}</dd></div>
            <div><dt>Charges payées</dt><dd>{formatMad(document.chargesPayees)}</dd></div>
            <div><dt>Total payé</dt><dd className={styles.amount}>{formatMad(document.totalPaye)}</dd></div>
          </dl>
        </Card>
        <Card className={styles.detailCard}>
          <h2>Document et signature</h2>
          <dl className={styles.definitionGrid}>
            <div><dt>Type</dt><dd>{document.typeDocument || '—'}</dd></div>
            <div><dt>Créé le</dt><dd>{formatLocalDate(document.creeLe)}</dd></div>
            <div><dt>Émis le</dt><dd>{formatLocalDate(document.emisLe)}</dd></div>
            <div><dt>Signature</dt><dd>{signatureDescription}</dd></div>
            {document.documentPrecedentId && <div><dt>Document précédent</dt><dd>{document.documentPrecedentId}</dd></div>}
            {document.motifRemplacement && <div><dt>Motif de remplacement</dt><dd>{document.motifRemplacement}</dd></div>}
          </dl>
        </Card>
      </div>

      <Card className={styles.actionCard}>
        <div className={styles.sectionHeading}>
          <div><h2>Actions disponibles</h2><p className={styles.muted}>Les autorisations sont revérifiées par le backend.</p></div>
        </div>
        <div className={styles.actionGrid}>
          {previewAvailable && (
            <Button variant="secondary" icon={Eye} onClick={previewDraft} disabled={Boolean(actionBusy)}>
              {actionBusy === 'preview' ? 'Prévisualisation…' : 'Prévisualiser le brouillon'}
            </Button>
          )}
          {isLandlord && canValidateDraft(document) && (
            <Button icon={Check} onClick={() => openAction('validate', 'Valider le brouillon', 'Le document passera en attente de signature.', false)}>
              Valider
            </Button>
          )}
          {isLandlord && canCancelDocument(document) && (
            <Button variant="danger" icon={XCircle} onClick={() => openAction('cancel', 'Annuler la quittance', 'Cette action change l’état du document et laisse une trace.', true)}>
              Annuler
            </Button>
          )}
          {replaceAvailable && (
            <Button icon={RotateCcw} onClick={() => openAction('replace', 'Préparer un remplacement', 'Le document annulé sera remplacé par un nouveau brouillon calculé par le backend.', true)}>
              Remplacer
            </Button>
          )}
          {issueAvailable && (
            <Button icon={FileCheck2} onClick={() => openAction('issue', 'Émission de démonstration', 'Cette émission utilise une signature locale de démonstration, non certifiée.', false)}>
              Émettre (démonstration)
            </Button>
          )}
          {archiveAvailable && (
            <Button variant="secondary" icon={Archive} onClick={createArchive} disabled={Boolean(actionBusy)}>
              {actionBusy === 'archive-create' ? 'Archivage…' : 'Archiver le brouillon'}
            </Button>
          )}
          {emittedDemo && (
            <>
              <Button variant="secondary" icon={Download} onClick={() => download('pdf', () => quittancesApi.downloadDemoPdf(token, id), `quittance-demo-${id}.pdf`)} disabled={Boolean(actionBusy)}>
                Télécharger le PDF émis
              </Button>
              <Button variant="secondary" icon={Download} onClick={() => download('proof', () => quittancesApi.downloadDemoProof(token, id), `preuve-demo-${id}.zip`)} disabled={Boolean(actionBusy)}>
                Télécharger la preuve (ZIP)
              </Button>
              <Button variant="secondary" icon={ShieldCheck} onClick={checkIntegrity} disabled={Boolean(actionBusy)}>
                Vérifier l’intégrité
              </Button>
            </>
          )}
          {isLandlord && (
            <Button variant="secondary" icon={RefreshCw} onClick={checkPayments} disabled={Boolean(actionBusy)}>
              Contrôler les paiements
            </Button>
          )}
          {mailpitAvailable && (
            <Button variant="secondary" icon={Mail} onClick={() => openAction('mailpit', 'Envoi local vers Mailpit', 'La demande sera capturée dans Mailpit. Ce n’est pas un email réel au locataire.', false)}>
              Envoyer à Mailpit (démo)
            </Button>
          )}
        </div>
        {isLandlord && !previewAvailable && !canValidateDraft(document) && !canCancelDocument(document) && !replaceAvailable && !issueAvailable && (
          <p className={styles.muted}>Aucune action d’écriture n’est disponible pour cet état.</p>
        )}
        {sectionErrors.preview && <p className={styles.inlineError} role="alert">{sectionErrors.preview}</p>}
        {sectionErrors.pdf && <p className={styles.inlineError} role="alert">{sectionErrors.pdf}</p>}
        {sectionErrors.proof && <p className={styles.inlineError} role="alert">{sectionErrors.proof}</p>}
        {sectionErrors.integrity && <p className={styles.inlineError} role="alert">{sectionErrors.integrity}</p>}
        {sectionErrors.payments && <p className={styles.inlineError} role="alert">{sectionErrors.payments}</p>}
        {previewUrl && (
          <div className={styles.previewPanel}>
            <div className={styles.sectionHeading}>
              <h3>Prévisualisation PDF du brouillon</h3>
              <Button variant="ghost" onClick={() => setPreviewUrl('')}>Fermer</Button>
            </div>
            <iframe title="Prévisualisation du brouillon PDF" src={previewUrl} className={styles.pdfFrame} />
          </div>
        )}
        {verification && (
          <div className={styles.resultPanel} role="status">
            <strong>Intégrité {verification.integriteValide ? 'valide' : 'invalide'} · signature {verification.signatureValide ? 'valide' : 'invalide'}</strong>
            <span>Mode {verification.modeSignature} · SHA-256 {verification.sha256}</span>
            <small>Résultat de démonstration, sans certification d’identité.</small>
          </div>
        )}
        {paymentReport && (
          <div className={styles.resultPanel}>
            <strong>Contrôle des paiements : {paymentReport.resultat}</strong>
            <span>Vérifié le {formatLocalDate(paymentReport.verifieLe)} · {(paymentReport.ecarts || []).length} écart(s)</span>
            {(paymentReport.ecarts || []).map((difference, index) => (
              <p key={`${difference.affectationId || 'ecart'}-${index}`}>
                {difference.natureEcart}: {difference.detail}
              </p>
            ))}
          </div>
        )}
      </Card>

      <Card className={styles.sectionCard}>
        <h2>Affectations de paiements</h2>
        {assignments.length === 0 ? <p className={styles.muted}>Aucune affectation enregistrée.</p> : (
          <div className={styles.tableScroll}>
            <table className={styles.table}>
              <thead><tr><th>Paiement</th><th>Nature</th><th>Période</th><th>Montant</th><th>Devise</th><th>Statut source</th></tr></thead>
              <tbody>{assignments.map((item) => (
                <tr key={item.affectationId}>
                  <td>{item.paiementId}</td><td>{item.nature}</td>
                  <td>{formatLocalDate(item.periodeDebut)} – {formatLocalDate(item.periodeFin)}</td>
                  <td>{formatMad(item.montant)}</td><td>{item.devise}</td><td>{item.statutSourceALaPreparation}</td>
                </tr>
              ))}</tbody>
            </table>
          </div>
        )}
      </Card>

      <Card className={styles.sectionCard}>
        <div className={styles.sectionHeading}>
          <h2>Historique</h2>
          <Pagination page={history} pageIndex={historyPage} setPage={setHistoryPage} label="historique" />
        </div>
        {history?.contenu?.length ? (
          <div className={styles.tableScroll}>
            <table className={styles.table}>
              <thead><tr><th>Date</th><th>Opération</th><th>Résultat</th><th>État</th><th>Motif</th></tr></thead>
              <tbody>{history.contenu.map((event) => (
                <tr key={event.id}>
                  <td>{formatLocalDate(event.horodatage)}</td><td>{event.operation}</td>
                  <td>{event.resultat}</td><td>{event.statutAvant || '—'} → {event.statutApres || '—'}</td>
                  <td>{event.motif || '—'}</td>
                </tr>
              ))}</tbody>
            </table>
          </div>
        ) : <p className={styles.muted}>Aucun événement pour cette page.</p>}
      </Card>

      {isLandlord && (
        <>
          <Card className={styles.sectionCard}>
            <div className={styles.sectionHeading}>
              <h2>Archives de brouillon</h2>
              <Pagination page={archives} pageIndex={archivePage} setPage={setArchivePage} label="archives" />
            </div>
            {sectionErrors.archives && <p className={styles.inlineError} role="alert">{sectionErrors.archives}</p>}
            {archives?.contenu?.length ? (
              <div className={styles.tableScroll}>
                <table className={styles.table}>
                  <thead><tr><th>Version</th><th>Créée le</th><th>SHA-256</th><th>Taille</th><th /></tr></thead>
                  <tbody>{archives.contenu.map((archive) => (
                    <tr key={archive.id}>
                      <td>{archive.versionDocument}</td><td>{formatLocalDate(archive.creeLe)}</td>
                      <td><code>{archive.sha256}</code></td><td>{archive.tailleOctets} octets</td>
                      <td><Button size="sm" variant="secondary" icon={Download} onClick={() => download('archive', () => quittancesApi.downloadArchive(token, id, archive.id), `brouillon-${id}-${archive.id}.pdf`)} disabled={Boolean(actionBusy)}>Télécharger</Button></td>
                    </tr>
                  ))}</tbody>
                </table>
              </div>
            ) : !sectionErrors.archives && <p className={styles.muted}>Aucune archive sur cette page.</p>}
          </Card>

          <Card className={styles.sectionCard}>
            <div className={styles.sectionHeading}>
              <h2>Envois Mailpit · démonstration locale</h2>
              <Pagination page={mailpit} pageIndex={mailpitPage} setPage={setMailpitPage} label="envois Mailpit" />
            </div>
            <p className={styles.muted}>Les messages sont capturés localement. Leur statut ne prouve pas la réception d’un email par le locataire.</p>
            {mailpitNotice && (
              <p className={mailpitNotice.kind === 'accepted' ? styles.inlineSuccess : styles.inlineError} role="status">
                {mailpitNotice.text}
              </p>
            )}
            {sectionErrors.mailpit && <p className={styles.inlineError} role="alert">{sectionErrors.mailpit}</p>}
            {mailpit?.contenu?.length ? (
              <div className={styles.tableScroll}>
                <table className={styles.table}>
                  <thead><tr><th>Destinataire test</th><th>Statut</th><th>Créé le</th><th>Terminé le</th><th>Code</th></tr></thead>
                  <tbody>{mailpit.contenu.map((send) => (
                    <tr key={send.id}>
                      <td>{send.destinataire}</td><td>{send.statut}</td>
                      <td>{formatLocalDate(send.creeLe)}</td><td>{formatLocalDate(send.termineLe)}</td><td>{send.erreurCode || '—'}</td>
                    </tr>
                  ))}</tbody>
                </table>
              </div>
            ) : !sectionErrors.mailpit && <p className={styles.muted}>Aucun envoi enregistré.</p>}
          </Card>
        </>
      )}

      <Modal isOpen={Boolean(dialog)} onClose={closeDialog} title={dialog?.title || ''}>
        {dialog && (
          <form className={styles.confirmForm} onSubmit={performConfirmedAction}>
            <p>{dialog.description}</p>
            {dialog.needsReason && (
              <label>
                Motif (obligatoire)
                <textarea
                  value={reason}
                  onChange={(event) => setReason(event.target.value)}
                  maxLength={2000}
                  rows={4}
                  required
                />
              </label>
            )}
            {dialog.type === 'mailpit' && mailpitRequestId && (
              <p className={styles.muted}>
                Requête déjà créée : {mailpitRequestId}. Une confirmation répétée utilise le même identifiant et ne redéclenche pas de second SMTP.
              </p>
            )}
            {actionError && <p className={styles.inlineError} role="alert">{actionError}</p>}
            <div className={styles.formActions}>
              <Button variant="secondary" type="button" onClick={closeDialog} disabled={Boolean(actionBusy)}>Retour</Button>
              <Button type="submit" variant={dialog.type === 'cancel' ? 'danger' : 'primary'} disabled={Boolean(actionBusy)}>
                {actionBusy ? 'Traitement…' : dialog.type === 'mailpit' && mailpitRequestId ? 'Consulter cette requête' : 'Confirmer'}
              </Button>
            </div>
          </form>
        )}
      </Modal>
    </div>
  );
};

const Pagination = ({ page, pageIndex, setPage, label }) => {
  const pageCount = getPageCount(page);
  if (pageCount <= 1) return null;
  return (
    <div className={styles.paginationCompact} aria-label={`Pagination ${label}`}>
      <Button size="sm" variant="secondary" onClick={() => setPage((value) => Math.max(0, value - 1))} disabled={pageIndex <= 0}>
        Précédent
      </Button>
      <span>{pageIndex + 1} / {pageCount}</span>
      <Button size="sm" variant="secondary" onClick={() => setPage((value) => value + 1)} disabled={pageIndex + 1 >= pageCount}>
        Suivant
      </Button>
    </div>
  );
};
