// cypress/support/e2e.js
import './commands';

Cypress.on('uncaught:exception', (err, runnable) => {
  // Empêcher les erreurs non capturées de faire échouer les tests
  return false;
});
