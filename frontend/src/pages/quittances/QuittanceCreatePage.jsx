import React, { useEffect, useRef, useState } from 'react';
import { ArrowLeft, FilePlus2 } from 'lucide-react';
import { Link, useNavigate } from 'react-router-dom';
import { quittancesApi } from '../../api/quittancesApi';
import { useAuth } from '../../context/AuthContext';
import { Button } from '../../components/ui/Button';
import { Card } from '../../components/ui/Card';
import {
  quittanceErrorMessage,
  readQuittancesIdentity,
} from './quittancesUtils';
import styles from './QuittancesPage.module.css';

export const QuittanceCreatePage = () => {
  const { token } = useAuth();
  const navigate = useNavigate();
  const [identity, setIdentity] = useState(null);
  const [identityError, setIdentityError] = useState('');
  const [bailId, setBailId] = useState('');
  const [periodeDebut, setPeriodeDebut] = useState('');
  const [periodeFin, setPeriodeFin] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const submissionLock = useRef(false);

  useEffect(() => {
    let active = true;
    quittancesApi.getIdentity(token)
      .then(readQuittancesIdentity)
      .then((verifiedIdentity) => {
        if (active) setIdentity(verifiedIdentity);
      })
      .catch((requestError) => {
        if (active) setIdentityError(quittanceErrorMessage(requestError));
      });
    return () => {
      active = false;
    };
  }, [token]);

  const submit = async (event) => {
    event.preventDefault();
    if (submissionLock.current) return;
    setError('');
    if (!bailId.trim()) {
      setError('L’identifiant du bail est obligatoire.');
      return;
    }
    if (!periodeDebut || !periodeFin) {
      setError('Les deux dates de période sont obligatoires.');
      return;
    }
    if (periodeFin < periodeDebut) {
      setError('La fin de période doit être postérieure ou égale au début.');
      return;
    }

    submissionLock.current = true;
    setLoading(true);
    try {
      const created = await quittancesApi.createDraft(token, {
        bailId: bailId.trim(),
        periodeDebut,
        periodeFin,
      });
      navigate(`/quittances/${created.id}`);
    } catch (requestError) {
      setError(quittanceErrorMessage(requestError));
    } finally {
      setLoading(false);
      submissionLock.current = false;
    }
  };

  return (
    <div className={styles.page}>
      <Link to="/quittances" className={styles.backLink}><ArrowLeft size={17} /> Retour aux quittances</Link>
      <header className={styles.header}>
        <div>
          <p className={styles.eyebrow}>PRÉPARATION</p>
          <h1>Nouvelle quittance</h1>
          <p className={styles.muted}>Les montants et paiements de la période seront calculés par le backend.</p>
        </div>
      </header>
      <Card className={styles.formCard}>
        {identityError && <p className={styles.inlineError} role="alert">{identityError}</p>}
        {!identity && !identityError && (
          <p className={styles.stateCard} role="status">Vérification de l’identité auprès de Quittances…</p>
        )}
        {identity?.role === 'LOCATAIRE' && (
          <p className={styles.inlineError} role="alert">
            La préparation d’une quittance est réservée au rôle BAILLEUR.
          </p>
        )}
        {identity?.role === 'BAILLEUR' && <form className={styles.createForm} onSubmit={submit}>
          <label>
            Identifiant du bail
            <input
              value={bailId}
              onChange={(event) => setBailId(event.target.value)}
              maxLength={100}
              required
              autoComplete="off"
              aria-describedby="bail-help"
            />
            <small id="bail-help">Saisissez l’identifiant du bail existant.</small>
          </label>
          <div className={styles.dateFields}>
            <label>
              Début de période
              <input
                type="date"
                value={periodeDebut}
                onChange={(event) => setPeriodeDebut(event.target.value)}
                required
              />
            </label>
            <label>
              Fin de période
              <input
                type="date"
                value={periodeFin}
                onChange={(event) => setPeriodeFin(event.target.value)}
                required
              />
            </label>
          </div>
          {error && <p className={styles.inlineError} role="alert">{error}</p>}
          <div className={styles.formActions}>
            <Link to="/quittances">
              <Button variant="secondary" type="button">Annuler</Button>
            </Link>
            <Button type="submit" icon={FilePlus2} disabled={loading}>
              {loading ? 'Préparation…' : 'Créer le brouillon'}
            </Button>
          </div>
        </form>}
      </Card>
    </div>
  );
};
