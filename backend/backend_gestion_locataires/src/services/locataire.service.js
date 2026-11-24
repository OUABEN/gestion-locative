const Locataire = require('../models/locataire.model');
const CinDejaUtiliseException = require('../exceptions/cinDejaUtilise.exception');
const { randomUUID } = require('crypto');

class LocataireService {
  constructor(autoSeed = false) {
    this.locataires = new Map();
    if (autoSeed) {
      this.initDefaultLocataires();
    }
  }

  initDefaultLocataires() {
    const loc1 = new Locataire({
      id: 'a1111111-1111-1111-1111-111111111111',
      nom: 'El Amrani',
      prenom: 'Yassine',
      cin: 'AB123456',
      email: 'yassine@mail.ma',
      telephone: '0661001122',
      piecesJustificatives: ['CIN_recto.pdf', 'Contrat_travail.pdf']
    });

    const loc2 = new Locataire({
      id: 'a2222222-2222-2222-2222-222222222222',
      nom: 'Bennani',
      prenom: 'Fatima Zahra',
      cin: 'CD789012',
      email: 'fatima@mail.ma',
      telephone: '0662334455',
      piecesJustificatives: ['CIN_copie.pdf']
    });

    const loc3 = new Locataire({
      id: 'a3333333-3333-3333-3333-333333333333',
      nom: 'Benjelloun',
      prenom: 'Mehdi',
      cin: 'EF345678',
      email: 'mehdi@mail.ma',
      telephone: '0661223344',
      piecesJustificatives: ['Fiche_paie_01.pdf', 'Attestation_salaire.pdf']
    });

    this.locataires.set(loc1.id, loc1);
    this.locataires.set(loc2.id, loc2);
    this.locataires.set(loc3.id, loc3);
  }

  creerLocataire(locataireData) {
    if (!locataireData) {
      throw new Error('Le locataire ne peut pas être null');
    }

    if (!locataireData.cin || (typeof locataireData.cin === 'string' && locataireData.cin.trim() === '')) {
      throw new Error('Le CIN est obligatoire');
    }

    const normalizedCin = locataireData.cin.trim().toUpperCase();

    if (this.existsByCin(normalizedCin)) {
      throw new CinDejaUtiliseException(`Fiche refusée : CIN déjà utilisé (${normalizedCin})`);
    }

    if (locataireData.email) {
      const emailRegex = /^[A-Za-z0-9+_.-]+@(.+)$/;
      if (!emailRegex.test(locataireData.email)) {
        throw new Error("Format d'email invalide");
      }
    }

    const locataire = new Locataire({
      id: locataireData.id || randomUUID(),
      nom: locataireData.nom,
      prenom: locataireData.prenom,
      cin: normalizedCin,
      email: locataireData.email,
      telephone: locataireData.telephone,
      piecesJustificatives: locataireData.piecesJustificatives || []
    });

    this.locataires.set(locataire.id, locataire);
    return locataire;
  }

  getById(id) {
    if (!id) return null;
    return this.locataires.get(id) || null;
  }

  getByCin(cin) {
    if (!cin || typeof cin !== 'string') return null;
    const normalized = cin.trim().toUpperCase();
    for (const loc of this.locataires.values()) {
      if (loc.cin === normalized) {
        return loc;
      }
    }
    return null;
  }

  getByEmail(email) {
    if (!email || typeof email !== 'string') return null;
    const normalized = email.trim().toLowerCase();
    for (const loc of this.locataires.values()) {
      if (loc.email && loc.email.trim().toLowerCase() === normalized) {
        return loc;
      }
    }
    return null;
  }

  existsByCin(cin) {
    if (!cin) return false;
    const normalized = cin.trim().toUpperCase();
    for (const loc of this.locataires.values()) {
      if (loc.cin === normalized) {
        return true;
      }
    }
    return false;
  }

  modifierCoordonnees(id, email, telephone) {
    if (!id) return false;
    const loc = this.getById(id);
    if (!loc) return false;

    if (email !== undefined && email !== null) {
      const emailRegex = /^[A-Za-z0-9+_.-]+@(.+)$/;
      if (!emailRegex.test(email)) {
        throw new Error("Format d'email invalide");
      }
      loc.email = email;
    }

    if (telephone !== undefined && telephone !== null) {
      loc.telephone = telephone;
    }

    loc.updatedAt = new Date();
    this.locataires.set(id, loc);
    return true;
  }

  modifier(id, email, telephone) {
    return this.modifierCoordonnees(id, email, telephone);
  }

  nombreLocataires() {
    return this.locataires.size;
  }

  supprimer(id) {
    if (!id) return false;
    if (!this.locataires.has(id)) return false;
    return this.locataires.delete(id);
  }

  getAll({ search, page = 1, limit = 10 } = {}) {
    let list = Array.from(this.locataires.values());

    if (search && typeof search === 'string') {
      const s = search.toLowerCase();
      list = list.filter(l =>
        (l.nom && l.nom.toLowerCase().includes(s)) ||
        (l.prenom && l.prenom.toLowerCase().includes(s)) ||
        (l.cin && l.cin.toLowerCase().includes(s))
      );
    }

    const total = list.length;
    const pageNum = Math.max(1, parseInt(page, 10) || 1);
    const limitNum = Math.max(1, parseInt(limit, 10) || 10);
    const offset = (pageNum - 1) * limitNum;
    const data = list.slice(offset, offset + limitNum);

    return {
      total,
      page: pageNum,
      limit: limitNum,
      totalPages: Math.ceil(total / limitNum),
      data
    };
  }

  getQuittanceSummary(id) {
    const loc = this.getById(id);
    if (!loc) return null;
    return {
      id: loc.id,
      nom: loc.nom,
      prenom: loc.prenom,
      nomComplet: `${loc.prenom} ${loc.nom}`,
      cin: loc.cin,
      email: loc.email,
      telephone: loc.telephone
    };
  }

  clear() {
    this.locataires.clear();
  }
}

module.exports = LocataireService;