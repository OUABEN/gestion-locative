const { randomUUID } = require('crypto');

class Locataire {
  constructor({ id = null, nom, prenom, cin, email = null, telephone = null, piecesJustificatives = [] } = {}) {
    this.id = id || randomUUID();
    this.nom = nom;
    this.prenom = prenom;
    this.cin = cin ? cin.trim().toUpperCase() : cin;
    this.email = email;
    this.telephone = telephone;
    this.piecesJustificatives = Array.isArray(piecesJustificatives) ? piecesJustificatives : [];
    this.createdAt = new Date();
    this.updatedAt = new Date();
  }
}

module.exports = Locataire;
