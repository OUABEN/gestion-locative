import React from 'react';
import { Link } from 'react-router-dom';
import { MapPin, Maximize, Coins, ArrowUpRight, Edit3, Trash2 } from 'lucide-react';
import { Badge } from '../ui/Badge';
import { formatCurrency, formatSurface } from '../../utils/format';
import { getBienImage } from '../../utils/bienImage';
import { useAuth } from '../../context/AuthContext';
import styles from './BienCard.module.css';

export const BienCard = ({ bien, onDelete, index = 0 }) => {
  const { canWrite } = useAuth();

  return (
    <article
      className={`${styles.card} animate-fade-up`}
      style={{ animationDelay: `${Math.min(index, 8) * 70}ms` }}
    >
      <Link to={`/biens/${bien.id}`} className={styles.media} aria-label={`Voir ${bien.adresse}`}>
        <img src={getBienImage(bien.type, bien.imageUrl)} alt={`${bien.type} à ${bien.ville}`} loading="lazy" className={styles.image} />
        <div className={styles.overlay} />
        <div className={styles.topRow}>
          <span className={styles.typeTag}>{bien.type}</span>
          <Badge statut={bien.statut} />
        </div>
        <div className={styles.priceTag}>
          <span className={styles.price}>{formatCurrency(bien.prixMensuel)}</span>
          <span className={styles.period}>/ mois</span>
        </div>
        <span className={styles.viewHint}>
          <ArrowUpRight size={18} />
        </span>
      </Link>

      <div className={styles.content}>
        <h3 className={styles.adresse} title={bien.adresse}>{bien.adresse}</h3>
        <div className={styles.ville}>
          <MapPin size={15} />
          <span>{bien.ville}</span>
        </div>

        <div className={styles.features}>
          <div className={styles.feature}>
            <Maximize size={15} />
            <span>{formatSurface(bien.surface)}</span>
          </div>
          <div className={styles.feature}>
            <Coins size={15} />
            <span>{formatCurrency(bien.charges)} charges</span>
          </div>
        </div>
      </div>

      <div className={styles.actions}>
        <Link to={`/biens/${bien.id}`} className={styles.primaryAction}>
          Consulter
          <ArrowUpRight size={16} />
        </Link>
        {canWrite && (
          <div className={styles.iconActions}>
            <Link to={`/biens/${bien.id}/modifier`} className={styles.iconBtn} title="Modifier" aria-label="Modifier le bien">
              <Edit3 size={16} />
            </Link>
            {onDelete && (
              <button
                onClick={() => onDelete(bien)}
                className={`${styles.iconBtn} ${styles.deleteBtn}`}
                title="Supprimer"
                aria-label="Supprimer le bien"
              >
                <Trash2 size={16} />
              </button>
            )}
          </div>
        )}
      </div>
    </article>
  );
};
