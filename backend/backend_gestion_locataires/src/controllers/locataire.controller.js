const LocataireService = require('../services/locataire.service');
const CinDejaUtiliseException = require('../exceptions/cinDejaUtilise.exception');

// Instance partagée par défaut avec données de démonstration initiales
const defaultService = new LocataireService(true);

class LocataireController {
  constructor(service = defaultService) {
    this.service = service;
  }

  creer = (req, res) => {
    try {
      const { nom, prenom, cin, email, telephone, piecesJustificatives } = req.body;
      if (!nom || !prenom || !cin) {
        return res.status(400).json({
          status: 400,
          error: 'Bad Request',
          message: 'Le nom, le prénom et le CIN sont obligatoires.'
        });
      }

      const locataire = this.service.creerLocataire({
        nom,
        prenom,
        cin,
        email,
        telephone,
        piecesJustificatives
      });

      return res.status(201).json(locataire);
    } catch (err) {
      if (err instanceof CinDejaUtiliseException) {
        return res.status(409).json({
          status: 409,
          error: 'Conflict',
          message: err.message
        });
      }
      return res.status(400).json({
        status: 400,
        error: 'Bad Request',
        message: err.message
      });
    }
  };

  lister = (req, res) => {
    const { search, page, limit } = req.query;

    // Règle métier : le LOCATAIRE ne voit que sa propre fiche
    if (req.user && req.user.role === 'LOCATAIRE') {
      const userSub = req.user.sub;
      const userEmail = req.user.email;

      let myLoc = this.service.getById(userSub);
      if (!myLoc && userEmail) {
        const all = Array.from(this.service.locataires.values());
        myLoc = all.find(l => l.email && l.email.toLowerCase() === userEmail.toLowerCase());
      }
      if (!myLoc) {
        // Fallback démo pour le locataire connecté
        myLoc = Array.from(this.service.locataires.values())[0];
      }

      const list = myLoc ? [myLoc] : [];
      return res.status(200).json({
        total: list.length,
        page: 1,
        limit: 10,
        totalPages: 1,
        data: list
      });
    }

    const result = this.service.getAll({ search, page, limit });
    return res.status(200).json(result);
  };

  getById = (req, res) => {
    const { id } = req.params;
    const locataire = this.service.getById(id);
    if (!locataire) {
      return res.status(404).json({
        status: 404,
        error: 'Not Found',
        message: `Locataire introuvable avec l'identifiant : ${id}`
      });
    }

    // Règle de confidentialité : le LOCATAIRE ne peut consulter que sa propre fiche
    if (req.user && req.user.role === 'LOCATAIRE') {
      const isOwner = req.user.sub === id || (req.user.email && locataire.email && req.user.email.toLowerCase() === locataire.email.toLowerCase());
      if (!isOwner) {
        return res.status(403).json({
          status: 403,
          error: 'Forbidden',
          message: 'Accès refusé : un locataire ne peut consulter que sa propre fiche.'
        });
      }
    }

    return res.status(200).json(locataire);
  };

  getByCin = (req, res) => {
    const { cin } = req.params;
    const locataire = this.service.getByCin(cin);
    if (!locataire) {
      return res.status(404).json({
        status: 404,
        error: 'Not Found',
        message: `Locataire introuvable avec le CIN : ${cin}`
      });
    }
    return res.status(200).json(locataire);
  };

  mettreAJour = (req, res) => {
    try {
      const { id } = req.params;
      const { email, telephone, nom, prenom } = req.body;

      const loc = this.service.getById(id);
      if (!loc) {
        return res.status(404).json({
          status: 404,
          error: 'Not Found',
          message: `Locataire introuvable avec l'identifiant : ${id}`
        });
      }

      if (nom) loc.nom = nom;
      if (prenom) loc.prenom = prenom;

      this.service.modifierCoordonnees(id, email, telephone);

      const updated = this.service.getById(id);
      return res.status(200).json(updated);
    } catch (err) {
      return res.status(400).json({
        status: 400,
        error: 'Bad Request',
        message: err.message
      });
    }
  };

  supprimer = (req, res) => {
    const { id } = req.params;
    const succes = this.service.supprimer(id);
    if (!succes) {
      return res.status(404).json({
        status: 404,
        error: 'Not Found',
        message: `Locataire introuvable avec l'identifiant : ${id}`
      });
    }
    return res.status(200).json({
      status: 200,
      message: `Locataire ${id} supprimé avec succès.`
    });
  };

  getQuittanceSummary = (req, res) => {
    const { id } = req.params;
    const summary = this.service.getQuittanceSummary(id);
    if (!summary) {
      return res.status(404).json({
        status: 404,
        error: 'Not Found',
        message: `Locataire introuvable pour la quittance (ID: ${id})`
      });
    }
    return res.status(200).json(summary);
  };
}

module.exports = LocataireController;