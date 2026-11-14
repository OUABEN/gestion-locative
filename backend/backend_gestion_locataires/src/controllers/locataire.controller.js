const LocataireService = require('../services/locataire.service');
const CinDejaUtiliseException = require('../exceptions/cinDejaUtilise.exception');

// Instance partagée par défaut
const defaultService = new LocataireService();

class LocataireController {
  constructor(service = defaultService) {
    this.service = service;
  }

  creer = (req, res, next) => {
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
      const { email, telephone } = req.body;

      const succes = this.service.modifierCoordonnees(id, email, telephone);
      if (!succes) {
        return res.status(404).json({
          status: 404,
          error: 'Not Found',
          message: `Locataire introuvable avec l'identifiant : ${id}`
        });
      }

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
