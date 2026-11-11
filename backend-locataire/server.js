const app = require('./src/app');

const PORT = process.env.PORT || 8082;

app.listen(PORT, () => {
  console.log(`[backend-locataire] Serveur démarré sur le port ${PORT}`);
  console.log(`[backend-locataire] API disponible sur http://localhost:${PORT}/api/v1/locataires`);
});
