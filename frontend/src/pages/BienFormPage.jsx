import React, { useEffect, useState } from 'react';
import { useParams, useNavigate, Link } from 'react-router-dom';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import {
  ArrowLeft,
  Save,
  Building2,
  MapPin,
  Maximize,
  Coins,
  ImagePlus,
  Upload,
  X,
} from 'lucide-react';
import { biensApi } from '../api/biensApi';
import { bienSchema } from '../utils/validators';
import { getBienImage } from '../utils/bienImage';
import { useToast } from '../context/ToastContext';
import { Card } from '../components/ui/Card';
import { Button } from '../components/ui/Button';
import styles from './BienFormPage.module.css';

export const BienFormPage = () => {
  const { id } = useParams();
  const isEditing = Boolean(id);
  const navigate = useNavigate();
  const { addToast } = useToast();

  const [loadingInitial, setLoadingInitial] = useState(isEditing);
  const [submitting, setSubmitting] = useState(false);
  const [backendErrors, setBackendErrors] = useState([]);

  const {
    register,
    handleSubmit,
    setValue,
    watch,
    formState: { errors },
  } = useForm({
    resolver: zodResolver(bienSchema),
    defaultValues: {
      adresse: '',
      ville: '',
      type: 'Appartement',
      surface: '',
      prixMensuel: '',
      charges: 0,
      statut: 'LIBRE',
      imageUrl: '',
    },
  });

  const watchedType = watch('type');
  const watchedImageUrl = watch('imageUrl');

  useEffect(() => {
    if (isEditing) {
      const fetchBien = async () => {
        try {
          const bien = await biensApi.getById(id);
          setValue('adresse', bien.adresse);
          setValue('ville', bien.ville);
          setValue('type', bien.type);
          setValue('surface', Number(bien.surface));
          setValue('prixMensuel', Number(bien.prixMensuel));
          setValue('charges', Number(bien.charges) || 0);
          setValue('statut', bien.statut || 'LIBRE');
          setValue('imageUrl', bien.imageUrl || '');
        } catch (err) {
          addToast(err.response?.data?.message || 'Erreur lors du chargement du bien.', 'error');
          navigate('/biens');
        } finally {
          setLoadingInitial(false);
        }
      };
      fetchBien();
    }
  }, [id, isEditing, setValue, navigate, addToast]);

  const handleImageFileUpload = (e) => {
    const file = e.target.files?.[0];
    if (!file) return;
    if (!file.type.startsWith('image/')) {
      addToast('Veuillez sélectionner un fichier image valide (JPG, PNG, WEBP).', 'error');
      return;
    }
    const reader = new FileReader();
    reader.onload = () => {
      if (typeof reader.result === 'string') {
        setValue('imageUrl', reader.result, { shouldDirty: true });
        addToast('Image chargée et prête à être enregistrée en base de données.');
      }
    };
    reader.readAsDataURL(file);
  };

  const onSubmit = async (data) => {
    setSubmitting(true);
    setBackendErrors([]);
    try {
      const payload = {
        ...data,
        imageUrl: data.imageUrl && String(data.imageUrl).trim() !== '' ? String(data.imageUrl).trim() : null,
      };
      if (isEditing) {
        await biensApi.update(id, payload);
        addToast('Le bien a été modifié avec succès.');
        navigate(`/biens/${id}`);
      } else {
        const created = await biensApi.create(payload);
        addToast('Le nouveau bien a été créé avec succès.');
        navigate(`/biens/${created.id}`);
      }
    } catch (err) {
      const resp = err.response?.data;
      if (resp?.errors && Array.isArray(resp.errors)) {
        setBackendErrors(resp.errors);
      } else {
        setBackendErrors([resp?.message || "Une erreur est survenue lors de l'enregistrement."]);
      }
    } finally {
      setSubmitting(false);
    }
  };

  if (loadingInitial) {
    return <div className={styles.loading}>Chargement des informations du bien...</div>;
  }

  return (
    <div className={styles.container}>
      <div className={styles.header}>
        <Link to={isEditing ? `/biens/${id}` : '/biens'} className={styles.backLink}>
          <ArrowLeft size={18} />
          <span>{isEditing ? 'Retour au détail' : 'Retour au catalogue'}</span>
        </Link>
        <h1 className={styles.title}>
          {isEditing ? 'Modifier la fiche du bien' : 'Ajouter un nouveau bien'}
        </h1>
        <p className={styles.subtitle}>
          Renseignez les détails techniques, locatifs, géographiques et la photo du logement.
        </p>
      </div>

      <Card className={styles.formCard}>
        {backendErrors.length > 0 && (
          <div className={styles.errorBox} role="alert">
            <strong>Erreur(s) lors de la validation :</strong>
            <ul>
              {backendErrors.map((err, idx) => (
                <li key={idx}>{err}</li>
              ))}
            </ul>
          </div>
        )}

        <form onSubmit={handleSubmit(onSubmit)} className={styles.form}>
          <div className={styles.gridRow}>
            {/* Adresse */}
            <div className={`${styles.formGroup} ${styles.fullWidth}`}>
              <label htmlFor="adresse" className={styles.label}>
                Adresse complète *
              </label>
              <div className={styles.inputWrapper}>
                <MapPin size={18} className={styles.inputIcon} />
                <input
                  id="adresse"
                  type="text"
                  placeholder="Ex : 24 Rue Mohammed V"
                  {...register('adresse')}
                  className={`${styles.input} ${errors.adresse ? styles.inputError : ''}`}
                />
              </div>
              {errors.adresse && <span className={styles.fieldError}>{errors.adresse.message}</span>}
            </div>

            {/* Ville */}
            <div className={styles.formGroup}>
              <label htmlFor="ville" className={styles.label}>
                Ville *
              </label>
              <input
                id="ville"
                type="text"
                placeholder="Ex : Casablanca, Rabat, Marrakech..."
                {...register('ville')}
                className={`${styles.input} ${errors.ville ? styles.inputError : ''}`}
              />
              {errors.ville && <span className={styles.fieldError}>{errors.ville.message}</span>}
            </div>

            {/* Type */}
            <div className={styles.formGroup}>
              <label htmlFor="type" className={styles.label}>
                Typologie du bien *
              </label>
              <div className={styles.inputWrapper}>
                <Building2 size={18} className={styles.inputIcon} />
                <input
                  id="type"
                  type="text"
                  placeholder="Ex : Appartement, Studio, Villa, Bureau..."
                  {...register('type')}
                  className={`${styles.input} ${errors.type ? styles.inputError : ''}`}
                />
              </div>
              {errors.type && <span className={styles.fieldError}>{errors.type.message}</span>}
            </div>

            {/* Surface */}
            <div className={styles.formGroup}>
              <label htmlFor="surface" className={styles.label}>
                Surface habitable (m²) *
              </label>
              <div className={styles.inputWrapper}>
                <Maximize size={18} className={styles.inputIcon} />
                <input
                  id="surface"
                  type="number"
                  step="0.1"
                  placeholder="Ex : 75.5"
                  {...register('surface', { valueAsNumber: true })}
                  className={`${styles.input} ${errors.surface ? styles.inputError : ''}`}
                />
              </div>
              {errors.surface && <span className={styles.fieldError}>{errors.surface.message}</span>}
            </div>

            {/* Prix Mensuel */}
            <div className={styles.formGroup}>
              <label htmlFor="prixMensuel" className={styles.label}>
                Loyer mensuel (DH) *
              </label>
              <div className={styles.inputWrapper}>
                <Coins size={18} className={styles.inputIcon} />
                <input
                  id="prixMensuel"
                  type="number"
                  step="1"
                  placeholder="Ex : 4500"
                  {...register('prixMensuel', { valueAsNumber: true })}
                  className={`${styles.input} ${errors.prixMensuel ? styles.inputError : ''}`}
                />
              </div>
              {errors.prixMensuel && (
                <span className={styles.fieldError}>{errors.prixMensuel.message}</span>
              )}
            </div>

            {/* Charges */}
            <div className={styles.formGroup}>
              <label htmlFor="charges" className={styles.label}>
                Charges mensuelles (DH)
              </label>
              <div className={styles.inputWrapper}>
                <Coins size={18} className={styles.inputIcon} />
                <input
                  id="charges"
                  type="number"
                  step="1"
                  placeholder="Ex : 300 (par défaut 0)"
                  {...register('charges', { valueAsNumber: true })}
                  className={`${styles.input} ${errors.charges ? styles.inputError : ''}`}
                />
              </div>
              {errors.charges && <span className={styles.fieldError}>{errors.charges.message}</span>}
            </div>

            {/* Statut (en mode édition) */}
            {isEditing && (
              <div className={styles.formGroup}>
                <label htmlFor="statut" className={styles.label}>
                  Statut d&apos;occupation
                </label>
                <select id="statut" {...register('statut')} className={styles.input}>
                  <option value="LIBRE">LIBRE (Disponible à la location)</option>
                  <option value="OCCUPE">OCCUPÉ (Bail actif en cours)</option>
                </select>
              </div>
            )}

            {/* Barre d'ajout d'image (Upload fichier ou URL stockée en base PostgreSQL) */}
            <div className={`${styles.formGroup} ${styles.fullWidth}`}>
              <label htmlFor="imageUrl" className={styles.label}>
                Photo du bien (Stockée en base de données)
              </label>
              <div className={styles.imageBarContainer}>
                <div className={styles.imageBarInputs}>
                  <div className={styles.inputWrapper} style={{ flex: 1 }}>
                    <ImagePlus size={18} className={styles.inputIcon} />
                    <input
                      id="imageUrl"
                      type="text"
                      placeholder="Collez un lien d'image (https://...) ou importez un fichier depuis votre PC"
                      {...register('imageUrl')}
                      className={styles.input}
                    />
                  </div>

                  <label className={styles.uploadLabelBtn}>
                    <Upload size={16} />
                    <span>Importer image</span>
                    <input
                      type="file"
                      accept="image/*"
                      onChange={handleImageFileUpload}
                      className={styles.hiddenFileInput}
                    />
                  </label>

                  {watchedImageUrl && (
                    <button
                      type="button"
                      onClick={() => setValue('imageUrl', '')}
                      className={styles.clearImageBtn}
                      title="Réinitialiser l'image"
                    >
                      <X size={16} />
                    </button>
                  )}
                </div>

                <div className={styles.imagePreviewBox}>
                  <img
                    src={getBienImage(watchedType, watchedImageUrl)}
                    alt="Aperçu du bien"
                    className={styles.imagePreviewThumb}
                  />
                  <div className={styles.imagePreviewInfo}>
                    <strong>
                      {watchedImageUrl
                        ? 'Image personnalisée sélectionnée (sera enregistrée en base PostgreSQL)'
                        : `Image par défaut selon le type (${watchedType || 'Appartement'})`}
                    </strong>
                    <span>Formats acceptés : JPG, PNG, WEBP ou lien URL direct.</span>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <div className={styles.formActions}>
            <Link to={isEditing ? `/biens/${id}` : '/biens'}>
              <Button type="button" variant="secondary" disabled={submitting}>
                Annuler
              </Button>
            </Link>
            <Button type="submit" variant="primary" disabled={submitting} icon={Save}>
              {submitting ? 'Enregistrement en cours...' : isEditing ? 'Mettre à jour' : 'Créer le bien'}
            </Button>
          </div>
        </form>
      </Card>
    </div>
  );
};
