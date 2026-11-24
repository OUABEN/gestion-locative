const express = require('express');
const BailController = require('../controllers/bail.controller');
const { authenticate } = require('../middlewares/auth.middleware');

const router = express.Router();
const controller = new BailController();

// Toutes les routes Baux sont sécurisées par JWT
router.use(authenticate);

// Lecture seule côté Locataires (la création et clôture sont gérées par le module Loyers)
// Accessible à tous les rôles authentifiés (avec restriction automatique pour LOCATAIRE)
router.get('/', controller.lister);
router.get('/:id', controller.getById);

module.exports = router;