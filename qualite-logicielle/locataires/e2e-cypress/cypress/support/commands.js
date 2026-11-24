// cypress/support/commands.js

Cypress.Commands.add('loginAsBailleur', () => {
  cy.visit('/login');
  cy.get('input[type="email"]').clear().type('bailleur@gestion-locative.com');
  cy.get('input[type="password"]').clear().type('admin123');
  cy.get('button[type="submit"]').click();
});

Cypress.Commands.add('loginAsLocataire', () => {
  cy.visit('/login');
  cy.get('input[type="email"]').clear().type('yassine.bennani@email.ma');
  cy.get('input[type="password"]').clear().type('locataire123');
  cy.get('button[type="submit"]').click();
});
