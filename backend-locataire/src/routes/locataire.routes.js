const express = require('express');
const LocataireController = require('../controllers/locataire.controller');

const router = express.Router();
const controller = new LocataireController();

// Routes CRUD et métier du module Locataires
router.post('/', controller.creer);
router.get('/', controller.lister);
router.get('/cin/:cin', controller.getByCin);
router.get('/:id/quittance-summary', controller.getQuittanceSummary);
router.get('/:id', controller.getById);
router.put('/:id', controller.mettreAJour);
router.delete('/:id', controller.supprimer);

module.exports = router;
