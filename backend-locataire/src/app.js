const express = require('express');
const cors = require('cors');
const locataireRoutes = require('./routes/locataire.routes');

const app = express();

// Middlewares globaux
app.use(cors());
app.use(express.json());
app.use(express.urlencoded({ extended: true }));

// Health check
app.get('/health', (req, res) => {
  res.status(200).json({ status: 'UP', service: 'backend-locataire' });
});

// Enregistrement des routes de l'API REST
app.use('/api/v1/locataires', locataireRoutes);

// Gestion 404
app.use((req, res) => {
  res.status(404).json({
    status: 404,
    error: 'Not Found',
    message: `Route ${req.method} ${req.url} introuvable`
  });
});

// Gestionnaire d'erreurs global
app.use((err, req, res, next) => {
  console.error('Erreur non capturée :', err);
  res.status(err.status || 500).json({
    status: err.status || 500,
    error: err.name || 'InternalServerError',
    message: err.message || 'Une erreur interne est survenue'
  });
});

module.exports = app;
