export const readQuittancesIdentity = (identity) => {
  const subject = typeof identity?.sujet === 'string' ? identity.sujet.trim() : '';
  const authorities = Array.isArray(identity?.autorites) ? identity.autorites : [];
  const roles = authorities.filter((authority) =>
    ['ROLE_BAILLEUR', 'ROLE_LOCATAIRE'].includes(authority)
  );

  if (!subject || roles.length !== 1) {
    throw new Error(
      'Identité Quittances non reconnue : seul un rôle BAILLEUR ou LOCATAIRE unique est pris en charge.'
    );
  }

  return { subject, role: roles[0].slice('ROLE_'.length) };
};

export const quittanceErrorMessage = (error) => {
  if (error?.status === 401) {
    return 'Session refusée par Quittances (401). Le jeton commun n’est pas reconnu par ce service.';
  }
  if (error?.status === 403) {
    const backendCode = error?.data?.code;
    const backendText = [
      error?.message,
      error?.data?.message,
      error?.data?.detail,
      error?.data?.title,
    ].filter(Boolean).join(' ').toLowerCase();
    const rattachementIdentifie =
      backendCode === 'RATTACHEMENT_REQUIS'
      || backendCode === 'RATTACHEMENT_ABSENT'
      || backendText.includes('rattachement');

    if (rattachementIdentifie) {
      return 'Compte connecté, mais l’accès documentaire est refusé (403) : aucun rattachement métier valide n’a été identifié.';
    }
    return 'Compte connecté, mais l’accès documentaire nécessaire est refusé (403). Votre compte ne dispose pas des droits ou du rattachement métier requis.';
  }
  if (error?.status === 409) {
    return `Conflit métier (409) : ${error.message || 'le document a changé.'}`;
  }
  if (error?.status === 503) {
    return `Service indisponible (503) : ${error.message || 'opération non disponible.'}`;
  }
  return error?.message || 'Une erreur inattendue est survenue.';
};

export const formatLocalDate = (value) => {
  if (!value) return '—';
  const dateOnly = String(value).match(/^(\d{4})-(\d{2})-(\d{2})$/);
  if (dateOnly) return `${dateOnly[3]}/${dateOnly[2]}/${dateOnly[1]}`;
  const date = new Date(value);
  return Number.isNaN(date.getTime())
    ? String(value)
    : new Intl.DateTimeFormat('fr-MA', {
        dateStyle: 'short',
        timeStyle: 'short',
      }).format(date);
};

export const formatMad = (value) => {
  const amount = Number(value);
  if (!Number.isFinite(amount)) return '—';
  return new Intl.NumberFormat('fr-MA', {
    style: 'currency',
    currency: 'MAD',
    currencyDisplay: 'code',
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  }).format(amount);
};

export const createRequestId = () => {
  if (!globalThis.crypto?.randomUUID) {
    throw new Error('La génération sécurisée de requeteId n’est pas disponible dans ce navigateur.');
  }
  return globalThis.crypto.randomUUID();
};

export const canPreviewDraft = (document) =>
  !document?.emisLe &&
  ['BROUILLON', 'EN_ATTENTE_VALIDATION', 'EN_ATTENTE_SIGNATURE'].includes(document?.statut);

export const canValidateDraft = (document) =>
  !document?.emisLe &&
  ['BROUILLON', 'EN_ATTENTE_VALIDATION'].includes(document?.statut) &&
  document?.statutSignature === 'NON_SIGNE';

export const canCancelDocument = (document) =>
  !['ANNULE', 'REMPLACE'].includes(document?.statut);

export const canIssueDemo = (document) =>
  !document?.emisLe &&
  document?.statut === 'EN_ATTENTE_SIGNATURE' &&
  document?.statutSignature === 'EN_ATTENTE';

export const isDemoIssued = (document) =>
  document?.statut === 'EMIS' &&
  typeof document?.numero === 'string' &&
  document.numero.startsWith('DEMO-QT-');
