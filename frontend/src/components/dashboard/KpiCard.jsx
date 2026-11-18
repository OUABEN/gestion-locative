import React from 'react';
import { Card } from '../ui/Card';
import { useCountUp } from '../../utils/useCountUp';
import styles from './KpiCard.module.css';

/**
 * Carte KPI animée : compteur progressif, icône en dégradé,
 * barre de progression optionnelle et effet de survol.
 */
export const KpiCard = ({
  title,
  value,
  format = (v) => Math.round(v).toLocaleString('fr-MA'),
  subtitle,
  icon: Icon,
  color = 'green',
  progress,
  delay = 0,
}) => {
  const isNumeric = typeof value === 'number';
  const animated = useCountUp(isNumeric ? value : 0);

  return (
    <Card
      className={`${styles.kpiCard} ${styles[color]} animate-fade-up`}
      style={{ animationDelay: `${delay}ms` }}
    >
      <div className={styles.glow} aria-hidden="true" />
      <div className={styles.header}>
        <span className={styles.title}>{title}</span>
        {Icon && (
          <div className={styles.iconWrapper}>
            <Icon size={20} />
          </div>
        )}
      </div>
      <div className={styles.value}>{isNumeric ? format(animated) : value}</div>
      {typeof progress === 'number' && (
        <div className={styles.progressTrack}>
          <div
            className={styles.progressBar}
            style={{ '--target': `${Math.min(100, Math.max(0, progress))}%`, animationDelay: `${delay + 300}ms` }}
          />
        </div>
      )}
      {subtitle && <div className={styles.subtitle}>{subtitle}</div>}
    </Card>
  );
};
