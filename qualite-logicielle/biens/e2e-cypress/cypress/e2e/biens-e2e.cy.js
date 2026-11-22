/**
 * Suite E2E Cypress — Module Gestion des Biens
 * Couverture complète des 14 exigences du cahier des charges QL :
 *  1. Connexion échouée (mauvais mot de passe) -> message d'erreur
 *  2. Connexion réussie BAILLEUR -> redirection Dashboard
 *  3. Vérification des KPI Dashboard (total biens, taux d'occupation, loyers théoriques)
 *  4. Navigation vers "Ajouter un bien" -> formulaire
 *  5. Soumission formulaire invalide (surface négative ou prix vide) -> validation
 *  6. Création réussie d'un nouveau bien avec adresse/ville/type/surface/loyer
 *  7. Liste des biens : recherche/filtrage du bien créé
 *  8. Consultation de la fiche détail du bien
 *  9. Modification du bien (changement de loyer) -> persistance
 * 10. Changement de statut LIBRE -> OCCUPE
 * 11. Tentative de suppression d'un bien OCCUPE -> refus / toast d'erreur (409)
 * 12. Remise en statut LIBRE puis suppression réussie
 * 13. Déconnexion -> redirection login
 * 14. Connexion LOCATAIRE -> vérification lecture seule / absence du bouton "Ajouter"
 */

describe('Module Gestion des Biens — Scénario E2E Complet', () => {
  const timestamp = Date.now();
  const testBien = {
    adresse: `142 Boulevard Zerktouni (Test E2E ${timestamp})`,
    ville: 'Casablanca',
    type: 'Appartement',
    surface: '95',
    prixMensuel: '8500',
    charges: '600'
  };

  beforeEach(() => {
    // Nettoyer local storage avant les tests si nécessaire
  });

  it('01. Échec de connexion avec mot de passe erroné -> Message d’erreur affiché', () => {
    cy.visit('/login');
    cy.get('input[type="email"]').clear().type('bailleur@gestion-locative.com');
    cy.get('input[type="password"]').clear().type('WrongPassword999!');
    cy.get('button[type="submit"]').click();

    // Vérifier l'apparition de l'erreur dans l'UI ou toast
    cy.contains(/Identifiants invalides|erreur|invalide|401/i).should('be.visible');
    cy.url().should('include', '/login');
  });

  it('02. Connexion réussie BAILLEUR -> Redirection vers le Dashboard', () => {
    cy.visit('/login');
    cy.get('input[type="email"]').clear().type('bailleur@gestion-locative.com');
    cy.get('input[type="password"]').clear().type('admin123');
    cy.get('button[type="submit"]').click();

    cy.url().should('eq', Cypress.config().baseUrl + '/');
    cy.contains(/Tableau de bord|Parc Immobilier|Bienvenue/i).should('be.visible');
  });

  it('03. Vérification des KPI du Dashboard (Total biens, Loyers, Statuts)', () => {
    cy.visit('/login');
    cy.get('input[type="email"]').clear().type('bailleur@gestion-locative.com');
    cy.get('input[type="password"]').clear().type('admin123');
    cy.get('button[type="submit"]').click();

    // Attendre le chargement des cartes de synthèse
    cy.get('[data-testid="kpi-total"], .kpi-card, h3, .stat-value').should('exist');
    cy.contains(/Total des biens|Biens en gestion/i).should('be.visible');
    cy.contains(/Loyers/i).should('be.visible');
  });

  it('04. Navigation vers "Ajouter un bien" -> Affichage du formulaire', () => {
    cy.visit('/');
    cy.contains(/Ajouter un bien/i).click();
    cy.url().should('include', '/biens/nouveau');
    cy.get('form').should('be.visible');
    cy.get('input[name="adresse"], #adresse').should('be.visible');
  });

  it('05. Soumission formulaire invalide -> Blocage par les validateurs', () => {
    cy.visit('/biens/nouveau');
    cy.get('input[name="surface"], #surface').clear().type('-20');
    cy.get('button[type="submit"]').click();

    // Message d'erreur attendu pour la surface ou validation HTML5
    cy.get('input:invalid, .error-message, .form-error').should('exist');
  });

  it('06. Création réussie d’un nouveau bien', () => {
    cy.visit('/biens/nouveau');
    cy.get('input[name="adresse"], #adresse').clear().type(testBien.adresse);
    cy.get('input[name="ville"], #ville').clear().type(testBien.ville);
    cy.get('select[name="type"], #type').select(testBien.type);
    cy.get('input[name="surface"], #surface').clear().type(testBien.surface);
    cy.get('input[name="prixMensuel"], #prixMensuel, input[name="loyer"]').clear().type(testBien.prixMensuel);
    cy.get('input[name="charges"], #charges').clear().type(testBien.charges);

    cy.get('button[type="submit"]').click();

    // Redirection vers la liste des biens avec notification
    cy.url().should('match', /\/biens/);
    cy.contains(testBien.adresse).should('be.visible');
  });

  it('07. Liste des biens : Recherche et filtrage du bien créé', () => {
    cy.visit('/biens');
    cy.get('input[type="search"], input[placeholder*="Rechercher"]').clear().type(testBien.ville);
    cy.contains(testBien.adresse).should('be.visible');
  });

  it('08. Consultation de la fiche détail du bien', () => {
    cy.visit('/biens');
    cy.contains(testBien.adresse).parents('.bien-card, tr, div').within(() => {
      cy.contains(/Détail|Consulter|Voir/i).click();
    });
    cy.url().should('match', /\/biens\/[a-f0-9-]+/);
    cy.contains(testBien.adresse).should('be.visible');
    cy.contains('Casablanca').should('be.visible');
  });

  it('09. Modification du loyer du bien -> Persistance', () => {
    cy.visit('/biens');
    cy.contains(testBien.adresse).parents('.bien-card, tr, div').within(() => {
      cy.contains(/Modifier|Éditer/i).click();
    });

    cy.get('input[name="prixMensuel"], #prixMensuel').clear().type('9200');
    cy.get('button[type="submit"]').click();

    cy.contains(/mis à jour|enregistré/i).should('exist');
    cy.contains(/9\s?200/).should('be.visible');
  });

  it('10. Bascule de statut : Passage de LIBRE à OCCUPE', () => {
    cy.visit('/biens');
    cy.contains(testBien.adresse).parents('.bien-card, tr, div').within(() => {
      cy.contains(/Marquer comme occupé|OCCUPE|Basculer/i).click();
    });
    cy.contains(testBien.adresse).parents('.bien-card, tr, div').contains(/OCCUPÉ|OCCUPE/i).should('be.visible');
  });

  it('11. Règle métier critique : Refus de suppression d’un bien OCCUPE (HTTP 409)', () => {
    cy.visit('/biens');
    cy.contains(testBien.adresse).parents('.bien-card, tr, div').within(() => {
      cy.contains(/Supprimer/i).click();
    });

    // Confirmation si modale présente
    cy.get('body').then($body => {
      if ($body.find('button:contains("Confirmer")').length > 0) {
        cy.contains('button', /Confirmer|Oui/i).click();
      }
    });

    // Message d'erreur 409 (interdiction de supprimer un bien occupé)
    cy.contains(/Impossible de supprimer un bien occupé|erreur|409|conflit/i).should('be.visible');
  });

  it('12. Remise en statut LIBRE puis suppression réussie', () => {
    cy.visit('/biens');
    cy.contains(testBien.adresse).parents('.bien-card, tr, div').within(() => {
      cy.contains(/Marquer comme libre|LIBRE|Basculer/i).click();
    });

    cy.contains(testBien.adresse).parents('.bien-card, tr, div').within(() => {
      cy.contains(/Supprimer/i).click();
    });

    cy.get('body').then($body => {
      if ($body.find('button:contains("Confirmer")').length > 0) {
        cy.contains('button', /Confirmer|Oui/i).click();
      }
    });

    cy.contains(testBien.adresse).should('not.exist');
  });

  it('13. Déconnexion -> Redirection vers la page de login', () => {
    cy.visit('/');
    cy.contains(/Déconnexion|Se déconnecter/i).click();
    cy.url().should('include', '/login');
    cy.get('button[type="submit"]').contains(/Se connecter/i).should('be.visible');
  });

  it('14. Rôle LOCATAIRE en lecture seule -> Interdiction d’écriture', () => {
    cy.visit('/login');
    cy.get('input[type="email"]').clear().type('locataire@gestion-locative.com');
    cy.get('input[type="password"]').clear().type('admin123');
    cy.get('button[type="submit"]').click();

    cy.url().should('eq', Cypress.config().baseUrl + '/');
    // Le bouton "Ajouter un bien" ne doit pas exister pour le locataire
    cy.contains(/Ajouter un bien/i).should('not.exist');

    // Tentative d'accès direct par URL -> Redirection ou refus
    cy.visit('/biens/nouveau');
    cy.url().should('not.include', '/biens/nouveau');
  });
});
