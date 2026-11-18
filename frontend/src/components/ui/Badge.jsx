import React from 'react';
import styles from './Badge.module.css';

export const Badge = ({ statut }) => {
  const isLibre = statut === 'LIBRE';

  return (
    <span className={`${styles.badge} ${isLibre ? styles.libre : styles.occupe}`}>
      <span className={styles.dot} />
      {isLibre ? 'Libre' : 'Occupé'}
    </span>
  );
};

