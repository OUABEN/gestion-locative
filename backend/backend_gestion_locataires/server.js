const app = require('./src/app');
const env = require('./src/config/env');

const PORT = env.PORT || 8082;

app.listen(PORT, () => {
  console.log(`[backend_gestion_locataires] Serveur démarré sur le port ${PORT}`);
  console.log(`[backend_gestion_locataires] Documentation Swagger : http://localhost:${PORT}/api-docs`);
  console.log(`[backend_gestion_locataires] API Locataires : http://localhost:${PORT}/api/locataires`);
  console.log(`[backend_gestion_locataires] API Baux : http://localhost:${PORT}/api/baux`);
});