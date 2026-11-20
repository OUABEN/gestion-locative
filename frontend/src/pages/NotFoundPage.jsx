import React from 'react';
import { Link } from 'react-router-dom';
import { Compass, Home } from 'lucide-react';
import { Card } from '../components/ui/Card';
import { Button } from '../components/ui/Button';
import styles from './NotFoundPage.module.css';

export const NotFoundPage = () => {
  return (
    <div className={styles.container}>
      <Card className={styles.card}>
        <div className={styles.iconCircle}>
          <Compass size={48} color="var(--color-brown)" />
        </div>
        <h1 className={styles.code}>404</h1>
        <h2 className={styles.title}>Page introuvable</h2>
        <p className={styles.desc}>
          La page que vous recherchez semble avoir déménagé ou n'a jamais existé dans notre parc locatif.
        </p>
        <Link to="/">
          <Button variant="primary" icon={Home}>
            Retourner au tableau de bord
          </Button>
        </Link>
      </Card>
    </div>
  );
};

