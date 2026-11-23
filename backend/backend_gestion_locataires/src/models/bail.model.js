const { randomUUID } = require('crypto');

class Bail {
  constructor({
    id = null,
    bienId = null,
    bien_id = null,
    locataireId = null,
    locataire_id = null,
    dateDebut = null,
    date_debut = null,
    dateFin = null,
    date_fin = null,
    montantLoyer = 0,
    montant_loyer = null,
    depotGarantie = 0,
    depot_garantie = null,
    statut = 'ACTIF',
    createdAt = null,
    updatedAt = null
  } = {}) {
    this.id = id || randomUUID();
    this.bienId = bienId || bien_id;
    this.locataireId = locataireId || locataire_id;
    this.dateDebut = dateDebut || date_debut || new Date().toISOString().split('T')[0];
    this.dateFin = dateFin || date_fin || null;
    this.montantLoyer = Number(montant_loyer !== null ? montant_loyer : montantLoyer);
    this.depotGarantie = Number(depot_garantie !== null ? depot_garantie : depotGarantie);
    this.statut = statut || 'ACTIF';
    this.createdAt = createdAt ? new Date(createdAt) : new Date();
    this.updatedAt = updatedAt ? new Date(updatedAt) : new Date();
  }

  toJSON() {
    return {
      id: this.id,
      bienId: this.bienId,
      bien_id: this.bienId,
      locataireId: this.locataireId,
      locataire_id: this.locataireId,
      dateDebut: this.dateDebut,
      date_debut: this.dateDebut,
      dateFin: this.dateFin,
      date_fin: this.dateFin,
      montantLoyer: this.montantLoyer,
      montant_loyer: this.montantLoyer,
      depotGarantie: this.depotGarantie,
      depot_garantie: this.depotGarantie,
      statut: this.statut,
      createdAt: this.createdAt.toISOString(),
      updatedAt: this.updatedAt.toISOString(),
    };
  }
}

module.exports = Bail;