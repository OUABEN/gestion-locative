import React, { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import {
  Users,
  Wallet,
  FileCheck2,
  Building2,
  ArrowRight,
  CheckCircle2,
  Sparkles,
  Server,
  ShieldCheck,
  LayoutDashboard,
} from 'lucide-react';
import { biensApi } from '../api/biensApi';
import { Card } from '../components/ui/Card';
import { Button } from '../components/ui/Button';
import { Badge } from '../components/ui/Badge';
import { formatCurrency } from '../utils/format';
import styles from './ModuleHubPage.module.css';

const MODULES_CONFIG = {
  locataires: {
    title: 'Gestion des Locataires & Baux',
    badge: 'MODULE LOCATAIRES · PORT 8082',
    icon: Users,
    endpoint: '/api/locataires & /api/baux',
    description:
      'Gérez les dossiers locataires (CIN, coordonnées), les contrats de bail actifs et leur rattachement aux biens disponibles ou occupés.',
    highlights: [
      'Vérification automatique de disponibilité auprès du service Biens',
      'Unicité garantie : un seul bail ACTIF par bien immobilier',
      'Suivi complet des dépôts de garantie et dates d’entrée/sortie',
    ],
  },
  loyers: {
    title: 'Gestion des Loyers & Paiements',
    badge: 'MODULE LOYERS · PORT 8083',
    icon: Wallet,
    endpoint: '/api/loyers & /api/paiements',
    description:
      'Suivez les échéances mensuelles, les encaissements de loyers en Dirhams (DH), les retards de paiement et le calcul du revenu réalisé.',
    highlights: [
      'Génération mensuelle des échéances (À payer, Payée, En retard)',
      'Enregistrement multi-modes : Virement, Espèces, Chèque',
      'Déclenchement automatique de la quittance dès paiement complet',
    ],
  },
  quittances: {
    title: 'Gestion des Quittances',
    badge: 'MODULE QUITTANCES · PORT 8084',
    icon: FileCheck2,
    endpoint: '/api/quittances',
    description:
      'Émission, numérotation unique et archivage légal des quittances de loyer pour chaque échéance réglée par vos locataires.',
    highlights: [
      'Numérotation séquentielle certifiée par période (Mois / Année)',
      'Historique des émissions et traçabilité des règlements',
      'Export et consultation rapide pour le bailleur et le locataire',
    ],
  },
};

export const ModuleHubPage = ({ moduleKey = 'locataires' }) => {
  const config = MODULES_CONFIG[moduleKey] || MODULES_CONFIG.locataires;
  const Icon = config.icon;

  const [biens, setBiens] = useState([]);

  useEffect(() => {
    biensApi
      .getAll()
      .then((data) => setBiens(data || []))
      .catch(() => setBiens([]));
  }, []);

  const totalBiens = biens.length;
  const occupes = biens.filter((b) => b.statut === 'OCCUPE');
  const libres = biens.filter((b) => b.statut === 'LIBRE');
  const revenuActif = occupes.reduce((sum, b) => sum + (Number(b.prixMensuel) || 0), 0);

  return (
    <div className={styles.container}>
      <section className={`${styles.hero} animate-scale-in`}>
        <div className={styles.heroLeft}>
          <span className={styles.eyebrow}>
            <Sparkles size={14} /> {config.badge}
          </span>
          <div className={styles.titleRow}>
            <div className={styles.iconBox}>
              <Icon size={28} color="#0B0B0C" />
            </div>
            <h1 className={styles.title}>{config.title}</h1>
          </div>
          <p className={styles.description}>{config.description}</p>

          <div className={styles.actions}>
            <Link to="/">
              <Button variant="primary" icon={LayoutDashboard}>
                Retour au Dashboard
              </Button>
            </Link>
            <Link to="/biens" className={styles.secondaryLink}>
              <Building2 size={17} />
              <span>Voir les Biens ({totalBiens})</span>
              <ArrowRight size={15} />
            </Link>
          </div>
        </div>

        <div className={styles.heroStats}>
          <div className={styles.statPill}>
            <span>Biens occupés (baux actifs)</span>
            <strong>{occupes.length} / {totalBiens}</strong>
          </div>
          <div className={styles.statPill}>
            <span>Biens disponibles</span>
            <strong className={styles.limeText}>{libres.length} logements</strong>
          </div>
          <div className={styles.statPill}>
            <span>Flux mensuel lié aux biens</span>
            <strong className={styles.limeText}>{formatCurrency(revenuActif)}</strong>
          </div>
        </div>
      </section>

      <div className={styles.grid}>
        <Card className={`${styles.card} animate-fade-up`} style={{ animationDelay: '80ms' }}>
          <div className={styles.cardHeader}>
            <ShieldCheck size={20} />
            <h3>Règles métier &amp; Intégration Microservice</h3>
          </div>
          <ul className={styles.featureList}>
            {config.highlights.map((item, idx) => (
              <li key={idx}>
                <CheckCircle2 size={17} className={styles.checkIcon} />
                <span>{item}</span>
              </li>
            ))}
          </ul>
          <div className={styles.endpointBox}>
            <Server size={16} />
            <span>Contrat REST :</span>
            <code>{config.endpoint}</code>
          </div>
        </Card>

        <Card className={`${styles.card} animate-fade-up`} style={{ animationDelay: '160ms' }}>
          <div className={styles.cardHeader}>
            <Building2 size={20} />
            <h3>Biens synchronisés avec ce module</h3>
          </div>
          <div className={styles.biensMiniList}>
            {biens.slice(0, 5).map((b) => (
              <Link key={b.id} to={`/biens/${b.id}`} className={styles.bienRow}>
                <div>
                  <strong>{b.adresse}</strong>
                  <span>{b.ville} · {b.type}</span>
                </div>
                <div className={styles.bienRight}>
                  <span className={styles.price}>{formatCurrency(b.prixMensuel)}</span>
                  <Badge statut={b.statut} />
                </div>
              </Link>
            ))}
          </div>
        </Card>
      </div>
    </div>
  );
};

