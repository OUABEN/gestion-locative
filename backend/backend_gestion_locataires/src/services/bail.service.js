const Bail = require('../models/bail.model');
const { randomUUID } = require('crypto');

class BailService {
  constructor() {
    this.baux = new Map();
    this.initDefaultBaux();
  }

  initDefaultBaux() {
    // Baux par défaut pour tests et démonstrations inter-services
    const defaultBail1 = new Bail({
      id: 'b1111111-1111-1111-1111-111111111111',
      bienId: 'fe365469-6d12-4311-922a-eb6f3a0820fe', // Studio Agdal (Rabat)
      locataireId: 'a1111111-1111-1111-1111-111111111111', // Yassine El Amrani
      dateDebut: '2026-01-01',
      dateFin: '2026-12-31',
      montantLoyer: 3400,
      depotGarantie: 6800,
      statut: 'ACTIF'
    });

    const defaultBail2 = new Bail({
      id: 'b2222222-2222-2222-2222-222222222222',
      bienId: '42386a85-b7e4-435c-9c3a-1e022dc15133', // Local Commercial (Tanger)
      locataireId: 'a2222222-2222-2222-2222-222222222222', // Fatima Zahra Bennani
      dateDebut: '2025-06-01',
      dateFin: '2026-05-31',
      montantLoyer: 16000,
      depotGarantie: 32000,
      statut: 'ACTIF'
    });

    this.baux.set(defaultBail1.id, defaultBail1);
    this.baux.set(defaultBail2.id, defaultBail2);
  }

  creerBail(data) {
    if (!data) throw new Error('Les données du bail sont requises');

    const bienId = data.bienId || data.bien_id;
    const locataireId = data.locataireId || data.locataire_id;
    const montantLoyer = data.montantLoyer !== undefined ? data.montantLoyer : data.montant_loyer;

    if (!bienId) throw new Error('Identifiant du bien (bienId) obligatoire');
    if (!locataireId) throw new Error('Identifiant du locataire (locataireId) obligatoire');
    if (montantLoyer === undefined || montantLoyer === null || Number(montantLoyer) < 0) {
      throw new Error('Le montant du loyer doit être positif ou nul');
    }

    // Règle SQL 04_baux.sql : uq_baux_bien_actif (un seul bail ACTIF par bien)
    const statut = data.statut || 'ACTIF';
    if (statut === 'ACTIF') {
      const existeBailActif = Array.from(this.baux.values()).some(
        b => b.bienId === bienId && b.statut === 'ACTIF'
      );
      if (existeBailActif) {
        const conflictErr = new Error('Ce bien possède déjà un bail actif.');
        conflictErr.statusCode = 409;
        throw conflictErr;
      }
    }

    const bail = new Bail({
      id: data.id || randomUUID(),
      bienId,
      locataireId,
      dateDebut: data.dateDebut || data.date_debut,
      dateFin: data.dateFin || data.date_fin,
      montantLoyer,
      depotGarantie: data.depotGarantie || data.depot_garantie || 0,
      statut
    });

    this.baux.set(bail.id, bail);
    return bail;
  }

  getById(id) {
    if (!id) return null;
    return this.baux.get(id) || null;
  }

  lister({ statut, locataireId, bienId } = {}) {
    let list = Array.from(this.baux.values());

    if (statut) {
      const s = String(statut).toUpperCase();
      list = list.filter(b => b.statut === s);
    }

    if (locataireId) {
      list = list.filter(b => String(b.locataireId) === String(locataireId));
    }

    if (bienId) {
      list = list.filter(b => String(b.bienId) === String(bienId));
    }

    return list;
  }

  listerActifs() {
    return this.lister({ statut: 'ACTIF' });
  }

  mettreAJourStatut(id, statut) {
    const bail = this.getById(id);
    if (!bail) return null;

    bail.statut = statut;
    bail.updatedAt = new Date();
    this.baux.set(id, bail);
    return bail;
  }

  supprimer(id) {
    if (!this.baux.has(id)) return false;
    return this.baux.delete(id);
  }
}

module.exports = BailService;