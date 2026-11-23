import React, { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import {
  ArrowLeft,
  Users,
  ShieldCheck,
  AlertTriangle,
  FileCheck2,
  Save,
  CheckCircle2,
  XCircle
} from 'lucide-react';
import { locatairesApi } from '../../api/locatairesApi';
import { useToast } from '../../context/ToastContext';
import { Button } from '../../components/ui/Button';
import { Card } from '../../components/ui/Card';
import styles from './LocataireFormPage.module.css';

const DEFAULT_DOC_OPTIONS = [
  "Carte Nationale d'Identité (CIN recto-verso)",
  "Contrat de travail (CDI/CDD)",
  "Trois derniers bulletins de paie",
  "Attestation de domiciliation bancaire"
];

export const LocataireFormPage = () => {
  const { id } = useParams();
  const navigate = useNavigate();
  const { addToast } = useToast();

  const isEditMode = Boolean(id);

  const [formData, setFormData] = useState({
    nom: '',
    prenom: '',
    cin: '',
    email: '',
    telephone: '',
    piecesJustificatives: ["Carte Nationale d'Identité (CIN recto-verso)"],
  });

  const [loading, setLoading] = useState(isEditMode);
  const [submitting, setSubmitting] = useState(false);
  const [cinError, setCinError] = useState(null);
  const [errors, setErrors] = useState({});

  useEffect(() => {
    if (isEditMode) {
      loadLocataire();
    }
  }, [id]);

  const loadLocataire = async () => {
    setLoading(true);
    try {
      const data = await locatairesApi.getById(id);
      setFormData({
        nom: data.nom || '',
        prenom: data.prenom || '',
        cin: data.cin || '',
        email: data.email || '',
        telephone: data.telephone || '',
        piecesJustificatives: Array.isArray(data.piecesJustificatives)
          ? data.piecesJustificatives
          : [],
      });
    } catch (err) {
      addToast('Impossible de charger les données du locataire.', 'error');
      navigate('/locataires');
    } finally {
      setLoading(false);
    }
  };

  const handleInputChange = (e) => {
    const { name, value } = e.target;
    setFormData((prev) => ({
      ...prev,
      [name]: name === 'cin' ? value.toUpperCase().trim() : value,
    }));

    // Réinitialisation de l'erreur CIN dès que l'utilisateur modifie la saisie
    if (name === 'cin') {
      setCinError(null);
    }

    if (errors[name]) {
      setErrors((prev) => ({ ...prev, [name]: null }));
    }
  };

  const handleDocToggle = (docName) => {
    setFormData((prev) => {
      const exists = prev.piecesJustificatives.includes(docName);
      return {
        ...prev,
        piecesJustificatives: exists
          ? prev.piecesJustificatives.filter((d) => d !== docName)
          : [...prev.piecesJustificatives, docName],
      };
    });
  };

  const validate = () => {
    const newErrors = {};
    if (!formData.nom.trim()) newErrors.nom = 'Le nom est obligatoire.';
    if (!formData.prenom.trim()) newErrors.prenom = 'Le prénom est obligatoire.';
    if (!formData.cin.trim()) newErrors.cin = 'Le numéro CIN est obligatoire.';
    if (!formData.email.trim()) {
      newErrors.email = "L'adresse email est obligatoire.";
    } else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(formData.email.trim())) {
      newErrors.email = "Format d'adresse email invalide.";
    }
    if (!formData.telephone.trim()) {
      newErrors.telephone = 'Le numéro de téléphone est obligatoire.';
    }

    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!validate()) return;

    setSubmitting(true);
    setCinError(null);

    try {
      if (isEditMode) {
        await locatairesApi.update(id, formData);
        addToast(`Fiche de ${formData.prenom} ${formData.nom} mise à jour avec succès.`, 'success');
      } else {
        await locatairesApi.create(formData);
        addToast(`Nouveau locataire ${formData.prenom} ${formData.nom} enregistré avec succès.`, 'success');
      }
      navigate('/locataires');
    } catch (err) {
      if (err.response?.status === 409 || err.message?.includes('CIN déjà utilisé')) {
        setCinError(`Fiche refusée : CIN déjà utilisé (${formData.cin}). Règle d'unicité absolue violée.`);
      } else {
        addToast(err.response?.data?.message || err.message || 'Une erreur est survenue.', 'error');
      }
    } finally {
      setSubmitting(false);
    }
  };

  if (loading) {
    return (
      <div className={styles.loadingContainer}>
        <div className={styles.spinner} />
        <p>Chargement du formulaire...</p>
      </div>
    );
  }

  return (
    <div className={styles.container}>
      {/* Bouton retour */}
      <button
        type="button"
        onClick={() => navigate('/locataires')}
        className={styles.backButton}
      >
        <ArrowLeft size={18} />
        <span>Retour au répertoire</span>
      </button>

      {/* En-tête formulaire */}
      <div className={styles.header}>
        <div className={styles.headerBadge}>
          <Users size={14} />
          <span>MODULE 02 · GESTION DES LOCATAIRES</span>
        </div>
        <h1 className={styles.title}>
          {isEditMode ? 'Modifier la fiche locataire' : 'Créer une nouvelle fiche locataire'}
        </h1>
        <p className={styles.subtitle}>
          {isEditMode
            ? `Mise à jour des coordonnées et pièces justificatives du locataire (ID : ${id}).`
            : "Enregistrement d'un nouveau locataire avec contrôle strict d'intégrité et unicité du CIN."}
        </p>
      </div>

      {/* Bannière d'erreur CIN déjà utilisé (HTTP 409) */}
      {cinError && (
        <div className={styles.cinErrorAlert} role="alert">
          <div className={styles.cinErrorIconBox}>
            <AlertTriangle size={24} />
          </div>
          <div className={styles.cinErrorContent}>
            <h4 className={styles.cinErrorTitle}>« Fiche refusée : CIN déjà utilisé »</h4>
            <p className={styles.cinErrorText}>
              Le numéro CIN <b>{formData.cin}</b> est déjà associé à un autre dossier dans la base de données (Erreur HTTP 409 Conflit). Chaque locataire doit posséder un identifiant d'identité nationale unique.
            </p>
          </div>
        </div>
      )}

      {/* Formulaire principal */}
      <form onSubmit={handleSubmit}>
        <Card className={styles.formCard}>
          <h2 className={styles.sectionHeading}>
            <ShieldCheck size={20} />
            <span>Identité &amp; Pièce d'Identité Nationale</span>
          </h2>

          <div className={styles.gridTwoCols}>
            {/* Nom */}
            <div className={styles.formGroup}>
              <label htmlFor="nom" className={styles.label}>
                Nom de famille <span className={styles.required}>*</span>
              </label>
              <input
                id="nom"
                name="nom"
                type="text"
                placeholder="ex: El Amrani"
                value={formData.nom}
                onChange={handleInputChange}
                className={`${styles.input} ${errors.nom ? styles.inputError : ''}`}
              />
              {errors.nom && <span className={styles.errorMsg}>{errors.nom}</span>}
            </div>

            {/* Prénom */}
            <div className={styles.formGroup}>
              <label htmlFor="prenom" className={styles.label}>
                Prénom <span className={styles.required}>*</span>
              </label>
              <input
                id="prenom"
                name="prenom"
                type="text"
                placeholder="ex: Yassine"
                value={formData.prenom}
                onChange={handleInputChange}
                className={`${styles.input} ${errors.prenom ? styles.inputError : ''}`}
              />
              {errors.prenom && <span className={styles.errorMsg}>{errors.prenom}</span>}
            </div>
          </div>

          <div className={styles.gridTwoCols}>
            {/* Numéro CIN */}
            <div className={styles.formGroup}>
              <label htmlFor="cin" className={styles.label}>
                Numéro de CIN marocaine <span className={styles.required}>* (Unique)</span>
              </label>
              <div className={styles.inputWithIcon}>
                <input
                  id="cin"
                  name="cin"
                  type="text"
                  placeholder="ex: AB123456"
                  value={formData.cin}
                  onChange={handleInputChange}
                  disabled={isEditMode}
                  className={`${styles.input} ${cinError || errors.cin ? styles.inputError : ''} ${
                    isEditMode ? styles.inputDisabled : ''
                  }`}
                />
              </div>
              <span className={styles.helperText}>
                {isEditMode
                  ? 'Le CIN ne peut pas être modifié une fois la fiche créée.'
                  : 'Saisie normalisée en majuscules (ex: AB123456). Contrôle automatique d’unicité.'}
              </span>
              {errors.cin && <span className={styles.errorMsg}>{errors.cin}</span>}
            </div>

            {/* Téléphone */}
            <div className={styles.formGroup}>
              <label htmlFor="telephone" className={styles.label}>
                Téléphone mobile <span className={styles.required}>*</span>
              </label>
              <input
                id="telephone"
                name="telephone"
                type="tel"
                placeholder="ex: 0661001122"
                value={formData.telephone}
                onChange={handleInputChange}
                className={`${styles.input} ${errors.telephone ? styles.inputError : ''}`}
              />
              {errors.telephone && <span className={styles.errorMsg}>{errors.telephone}</span>}
            </div>
          </div>

          {/* Email */}
          <div className={styles.formGroup}>
            <label htmlFor="email" className={styles.label}>
              Adresse email professionnelle ou personnelle <span className={styles.required}>*</span>
            </label>
            <input
              id="email"
              name="email"
              type="email"
              placeholder="ex: yassine@mail.ma"
              value={formData.email}
              onChange={handleInputChange}
              className={`${styles.input} ${errors.email ? styles.inputError : ''}`}
            />
            {errors.email && <span className={styles.errorMsg}>{errors.email}</span>}
          </div>

          {/* Section Pièces justificatives */}
          <div className={styles.docsSection}>
            <h3 className={styles.docsTitle}>
              <FileCheck2 size={18} />
              <span>Pièces justificatives requises</span>
            </h3>
            <p className={styles.docsSubtitle}>
              Cochez les pièces justificatives vérifiées pour constituer le dossier locatif :
            </p>

            <div className={styles.docsCheckGrid}>
              {DEFAULT_DOC_OPTIONS.map((doc, idx) => {
                const checked = formData.piecesJustificatives.includes(doc);
                return (
                  <label
                    key={idx}
                    className={`${styles.docCheckLabel} ${checked ? styles.docCheckLabelActive : ''}`}
                  >
                    <input
                      type="checkbox"
                      checked={checked}
                      onChange={() => handleDocToggle(doc)}
                      className={styles.checkbox}
                    />
                    <span>{doc}</span>
                  </label>
                );
              })}
            </div>
          </div>

          {/* Boutons d'action */}
          <div className={styles.formActions}>
            <Button
              variant="secondary"
              type="button"
              onClick={() => navigate('/locataires')}
              disabled={submitting}
            >
              Annuler
            </Button>
            <Button
              variant="primary"
              type="submit"
              icon={Save}
              disabled={submitting}
            >
              {submitting
                ? 'Enregistrement en cours...'
                : isEditMode
                ? 'Mettre à jour la fiche'
                : 'Enregistrer le locataire'}
            </Button>
          </div>
        </Card>
      </form>
    </div>
  );
};