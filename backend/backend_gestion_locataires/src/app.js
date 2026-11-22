const express = require('express');
const cors = require('cors');
const swaggerUi = require('swagger-ui-express');
const swaggerSpec = require('./docs/swagger');
const locataireRoutes = require('./routes/locataire.routes');
const bailRoutes = require('./routes/bail.routes');

const app = express();

// Middlewares globaux
app.use(cors());
app.use(express.json());
app.use(express.urlencoded({ extended: true }));

// Documentation Swagger / OpenAPI
app.use('/api-docs', swaggerUi.serve, swaggerUi.setup(swaggerSpec));

// Health check
app.get('/health', (req, res) => {
  res.status(200).json({
    status: 'UP',
    service: 'backend_gestion_locataires',
    timestamp: new Date().toISOString()
  });
});

// Enregistrement des routes de l'API REST
// Support multi-préfixes pour compatibilité gateway Nginx et appels directs inter-services
app.use('/api/locataires', locataireRoutes);
app.use('/api/v1/locataires', locataireRoutes);

app.use('/api/baux', bailRoutes);
app.use('/api/v1/baux', bailRoutes);

// Gestion 404
app.use((req, res) => {
  res.status(404).json({
    status: 404,
    error: 'Not Found',
    message: `Route ${req.method} ${req.url} introuvable sur le service Locataires`
  });
});

// Gestionnaire d'erreurs global
app.use((err, req, res, next) => {
  console.error('[backend_gestion_locataires] Erreur serveur :', err);
  const status = err.status || err.statusCode || 500;
  res.status(status).json({
    status,
    error: err.name || 'InternalServerError',
    message: err.message || 'Une erreur interne est survenue'
  });
});

module.exports = app;