const express = require('express');
const LocataireController = require('../controllers/locataire.controller');
const BailController = require('../controllers/bail.controller');
const { authenticate, requireRole } = require('../middlewares/auth.middleware');

const router = express.Router();
const locController = new LocataireController();
const bailController = new BailController();

// Toutes les routes Locataires sont sécurisées par JWT
router.use(authenticate);

// Lecture (accessible à tous les rôles authentifiés avec filtrage auto pour LOCATAIRE)
router.get('/', locController.lister);
router.get('/cin/:cin', locController.getByCin);
router.get('/:id/quittance-summary', locController.getQuittanceSummary);
router.get('/:id/baux', (req, res, next) => {
  req.query.locataireId = req.params.id;
  return bailController.lister(req, res, next);
});
router.get('/:id', locController.getById);

// Écriture (réservée à BAILLEUR et GESTIONNAIRE)
router.post('/', requireRole('BAILLEUR', 'GESTIONNAIRE'), locController.creer);
router.put('/:id', requireRole('BAILLEUR', 'GESTIONNAIRE'), locController.mettreAJour);
router.delete('/:id', requireRole('BAILLEUR', 'GESTIONNAIRE'), locController.supprimer);

module.exports = router;