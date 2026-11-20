const BailService = require('../services/bail.service');
const env = require('../config/env');

const defaultBailService = new BailService();

class BailController {
  constructor(service = defaultBailService) {
    this.service = service;
  }

  creer = async (req, res) => {
    try {
      const data = req.body;
      const bienId = data.bienId || data.bien_id;

      // 1. Vérification optionnelle de disponibilité auprès du service Biens (port 8081)
      if (bienId && env.BIENS_SERVICE_URL) {
        try {
          const authHeader = req.headers.authorization;
          const url = `${env.BIENS_SERVICE_URL}/api/biens/${bienId}/disponibilite`;
          const resp = await fetch(url, {
            headers: authHeader ? { Authorization: authHeader } : {}
          });
          if (resp.ok) {
            const dispo = await resp.json();
            if (dispo && !dispo.disponible) {
              return res.status(409).json({
                status: 409,
                error: 'Conflict',
                message: `Le bien ${bienId} n'est pas disponible (statut actuel : ${dispo.statut}).`
              });
            }
          }
        } catch (fetchErr) {
          // Si le service Biens n'est pas joignable, on continue (mode résilient / tolérance aux pannes)
          console.warn('[BailController] Impossible de joindre Biens pour disponibilité :', fetchErr.message);
        }
      }

      // 2. Création locale du bail
      const bail = this.service.creerBail(data);

      // 3. Mise à jour du statut du bien en OCCUPE auprès du service Biens
      if (bienId && env.BIENS_SERVICE_URL) {
        try {
          const authHeader = req.headers.authorization;
          const url = `${env.BIENS_SERVICE_URL}/api/biens/${bienId}/statut`;
          await fetch(url, {
            method: 'PATCH',
            headers: {
              'Content-Type': 'application/json',
              ...(authHeader ? { Authorization: authHeader } : {})
            },
            body: JSON.stringify({ statut: 'OCCUPE' })
          });
        } catch (patchErr) {
          console.warn('[BailController] Impossible de notifier Biens du changement de statut :', patchErr.message);
        }
      }

      return res.status(201).json(bail.toJSON());
    } catch (err) {
      const code = err.statusCode || 400;
      return res.status(code).json({
        status: code,
        error: code === 409 ? 'Conflict' : 'Bad Request',
        message: err.message
      });
    }
  };

  lister = (req, res) => {
    const { statut, locataireId, locataire_id, bienId, bien_id } = req.query;

    let targetLocataireId = locataireId || locataire_id;

    // Règle de confidentialité : le LOCATAIRE ne voit que ses propres baux
    if (req.user && req.user.role === 'LOCATAIRE') {
      targetLocataireId = req.user.sub;
    }

    const baux = this.service.lister({
      statut,
      locataireId: targetLocataireId,
      bienId: bienId || bien_id
    });

    return res.status(200).json(baux.map(b => b.toJSON()));
  };

  getById = (req, res) => {
    const { id } = req.params;
    const bail = this.service.getById(id);
    if (!bail) {
      return res.status(404).json({
        status: 404,
        error: 'Not Found',
        message: `Bail introuvable avec l'identifiant : ${id}`
      });
    }

    if (req.user && req.user.role === 'LOCATAIRE' && req.user.sub !== bail.locataireId) {
      return res.status(403).json({
        status: 403,
        error: 'Forbidden',
        message: 'Accès refusé : un locataire ne peut consulter que ses propres baux.'
      });
    }

    return res.status(200).json(bail.toJSON());
  };

  mettreAJourStatut = async (req, res) => {
    const { id } = req.params;
    const { statut } = req.body;

    if (!statut || !['ACTIF', 'TERMINE'].includes(statut)) {
      return res.status(400).json({
        status: 400,
        error: 'Bad Request',
        message: "Statut invalide. Valeurs acceptées : 'ACTIF' ou 'TERMINE'."
      });
    }

    const updated = this.service.mettreAJourStatut(id, statut);
    if (!updated) {
      return res.status(404).json({
        status: 404,
        error: 'Not Found',
        message: `Bail introuvable avec l'identifiant : ${id}`
      });
    }

    // Si le bail se termine, repasser le bien en LIBRE
    if (statut === 'TERMINE' && updated.bienId && env.BIENS_SERVICE_URL) {
      try {
        const authHeader = req.headers.authorization;
        const url = `${env.BIENS_SERVICE_URL}/api/biens/${updated.bienId}/statut`;
        await fetch(url, {
          method: 'PATCH',
          headers: {
            'Content-Type': 'application/json',
            ...(authHeader ? { Authorization: authHeader } : {})
          },
          body: JSON.stringify({ statut: 'LIBRE' })
        });
      } catch (patchErr) {
        console.warn('[BailController] Notification Biens LIBRE échouée :', patchErr.message);
      }
    }

    return res.status(200).json(updated.toJSON());
  };

  supprimer = (req, res) => {
    const { id } = req.params;
    const ok = this.service.supprimer(id);
    if (!ok) {
      return res.status(404).json({
        status: 404,
        error: 'Not Found',
        message: `Bail introuvable avec l'identifiant : ${id}`
      });
    }
    return res.status(200).json({
      status: 200,
      message: `Bail ${id} supprimé avec succès.`
    });
  };
}

module.exports = BailController;