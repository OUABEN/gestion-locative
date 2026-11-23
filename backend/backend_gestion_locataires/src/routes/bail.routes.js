const express = require('express');
const BailController = require('../controllers/bail.controller');
const { authenticate, requireRole } = require('../middlewares/auth.middleware');

const router = express.Router();
const controller = new BailController();

// Toutes les routes Baux sont sécurisées par JWT
router.use(authenticate);

// Lecture : tous les rôles authentifiés (avec restriction automatique pour LOCATAIRE)
router.get('/', controller.lister);
router.get('/:id', controller.getById);

// Écriture : réservée aux rôles BAILLEUR et GESTIONNAIRE
router.post('/', requireRole('BAILLEUR', 'GESTIONNAIRE'), controller.creer);
router.patch('/:id/statut', requireRole('BAILLEUR', 'GESTIONNAIRE'), controller.mettreAJourStatut);
router.delete('/:id', requireRole('BAILLEUR', 'GESTIONNAIRE'), controller.supprimer);

module.exports = router;