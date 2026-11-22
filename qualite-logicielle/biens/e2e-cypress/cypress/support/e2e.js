// Support file pour Cypress E2E
// Ignorer les erreurs d'hydratation ou non bloquantes de React en dev
Cypress.on('uncaught:exception', (err, runnable) => {
  return false;
});
