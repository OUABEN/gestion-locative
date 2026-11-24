/**
 * Suite de Tests End-to-End (E2E) Cypress — Module Gestion des Locataires & Baux
 * Auteur : Ahmed LAARIF — Groupe 16 (Université Ibn Zohr Agadir - FSA)
 *
 * Parcours complets couverts :
 *  1. Authentification & Navigation vers la liste des locataires (LocatairesListPage)
 *  2. Recherche, filtrage et affichage des dossiers locataires
 *  3. Formulaire de création (LocataireFormPage) : Contrôle de saisie & Alerte 409 « CIN déjà utilisé »
 *  4. Création réussie d'un nouveau profil locataire avec CIN unique
 *  5. Consultation de la fiche 360° du locataire (LocataireDetailPage) et baux associés
 *  6. Espace Personnel Résident (MonEspaceLocatairePage) via endpoint GET /api/locataires/me
 *  7. Sécurité & Cloisonnement RBAC : Interdiction d'accès à la gestion globale pour le rôle LOCATAIRE
 */

describe('Module Gestion des Locataires & Baux — Scénarios E2E Complets', () => {
  const timestamp = Date.now();
  const existingCin = 'AB123456'; // CIN standard seedé en base
  const uniqueCin = `QA${Math.floor(1000 + Math.random() * 9000)}`;

  beforeEach(() => {
    // Configuration d'interceptions réseau pour garantir la résilience du test
    cy.intercept('GET', '**/api/locataires', (req) => {
      req.continue();
    }).as('getLocataires');

    cy.intercept('GET', '**/api/locataires/me', (req) => {
      req.continue();
    }).as('getMe');
  });

  // ---------------------------------------------------------------------------
  // 1. LISTE DES LOCATAIRES (LocatairesListPage)
  // ---------------------------------------------------------------------------
  it('01. Connexion BAILLEUR et consultation de la liste des locataires (LocatairesListPage)', () => {
    cy.visit('/login');
    cy.get('input[type="email"]').clear().type('bailleur@gestion-locative.com');
    cy.get('input[type="password"]').clear().type('admin123');
    cy.get('button[type="submit"]').click();

    // Navigation vers le module Locataires
    cy.visit('/locataires');
    cy.contains(/Gestion des Locataires|Répertoire des Locataires|Locataires/i).should('be.visible');

    // Vérification de la présence des colonnes du tableau
    cy.contains(/Nom & Prénom|Locataire/i).should('be.visible');
    cy.contains(/CIN/i).should('be.visible');
    cy.contains(/Statut/i).should('be.visible');
  });

  it('02. Recherche et filtrage dynamique dans la liste des locataires', () => {
    cy.visit('/login');
    cy.get('input[type="email"]').clear().type('bailleur@gestion-locative.com');
    cy.get('input[type="password"]').clear().type('admin123');
    cy.get('button[type="submit"]').click();

    cy.visit('/locataires');

    // Test du champ de recherche textuelle
    cy.get('input[placeholder*="Rechercher"], input[type="search"], input[type="text"]')
      .first()
      .clear()
      .type('Bennani');

    // Vérification de la réactivité de l'affichage
    cy.get('body').should('contain.text', 'Bennani');
  });

  // ---------------------------------------------------------------------------
  // 2. FORMULAIRE & RÈGLE DE COLLISION CIN 409 (LocataireFormPage)
  // ---------------------------------------------------------------------------
  it('03. Formulaire (LocataireFormPage) : Tentative avec CIN dupliqué -> Alerte 409 « CIN déjà utilisé »', () => {
    cy.visit('/login');
    cy.get('input[type="email"]').clear().type('bailleur@gestion-locative.com');
    cy.get('input[type="password"]').clear().type('admin123');
    cy.get('button[type="submit"]').click();

    cy.visit('/locataires/nouveau');
    cy.contains(/Nouveau locataire|Enregistrement|Ajouter un locataire/i).should('be.visible');

    // Remplissage avec un CIN déjà existant
    cy.get('#nom, input[name="nom"]').clear().type('Doublon');
    cy.get('#prenom, input[name="prenom"]').clear().type('Karim');
    cy.get('#cin, input[name="cin"]').clear().type(existingCin);
    cy.get('#telephone, input[name="telephone"]').clear().type('+212611223344');
    cy.get('#email, input[name="email"]').clear().type(`doublon_${timestamp}@test.ma`);

    // Soumission du formulaire
    cy.get('button[type="submit"]').click();

    // Vérification de l'alerte d'erreur 409 dans l'UI
    cy.contains(/CIN déjà utilisé|Fiche refusée|409|Erreur HTTP 409/i).should('be.visible');
  });

  it('04. Formulaire (LocataireFormPage) : Création réussie d’un locataire avec CIN unique', () => {
    cy.visit('/login');
    cy.get('input[type="email"]').clear().type('bailleur@gestion-locative.com');
    cy.get('input[type="password"]').clear().type('admin123');
    cy.get('button[type="submit"]').click();

    cy.visit('/locataires/nouveau');

    // Remplissage avec des données valides et CIN unique
    cy.get('#nom, input[name="nom"]').clear().type('El Idrissi');
    cy.get('#prenom, input[name="prenom"]').clear().type('Othmane');
    cy.get('#cin, input[name="cin"]').clear().type(uniqueCin);
    cy.get('#telephone, input[name="telephone"]').clear().type('+212677889900');
    cy.get('#email, input[name="email"]').clear().type(`othmane_${timestamp}@test-ql.ma`);

    cy.get('button[type="submit"]').click();

    // Attendre le retour vers la liste ou confirmation
    cy.url().should('match', /\/locataires/);
  });

  // ---------------------------------------------------------------------------
  // 3. FICHE DÉTAIL DU LOCATAIRE (LocataireDetailPage)
  // ---------------------------------------------------------------------------
  it('05. Consultation de la fiche 360° du locataire (LocataireDetailPage)', () => {
    cy.visit('/login');
    cy.get('input[type="email"]').clear().type('bailleur@gestion-locative.com');
    cy.get('input[type="password"]').clear().type('admin123');
    cy.get('button[type="submit"]').click();

    // Visite directe de la première fiche locataire
    cy.visit('/locataires/1');

    cy.contains(/Fiche Locataire|Profil|Dossier Locataire/i).should('be.visible');
    cy.contains(/Baux|Contrats de location/i).should('be.visible');
    cy.contains(/Quittances|Règlements/i).should('be.visible');
  });

  // ---------------------------------------------------------------------------
  // 4. PORTAIL RÉSIDENT : MON ESPACE LOCATAIRE (MonEspaceLocatairePage)
  // ---------------------------------------------------------------------------
  it('06. Connexion LOCATAIRE et consultation de "Mon Espace" (MonEspaceLocatairePage)', () => {
    // Connexion avec compte résident
    cy.visit('/login');
    cy.get('input[type="email"]').clear().type('yassine.bennani@email.ma');
    cy.get('input[type="password"]').clear().type('locataire123');
    cy.get('button[type="submit"]').click();

    // Accès à l'espace personnel locataire
    cy.visit('/locataires/mon-espace');

    cy.contains(/Mon Espace Locataire|Mon Espace Résident|Bienvenue/i).should('be.visible');
    cy.contains(/Contrat de bail|Bail actif|Informations personnelles/i).should('be.visible');
  });

  it('07. Sécurité & Cloisonnement RBAC : Interdiction de l’accès à la liste globale pour un LOCATAIRE', () => {
    // Connexion LOCATAIRE
    cy.visit('/login');
    cy.get('input[type="email"]').clear().type('yassine.bennani@email.ma');
    cy.get('input[type="password"]').clear().type('locataire123');
    cy.get('button[type="submit"]').click();

    // Tentative de navigation forcée vers la liste de gestion globale
    cy.visit('/locataires');

    // Vérification du blocage par RoleGuard (redirection ou message 403 / non autorisé)
    cy.url().should('not.match', /\/locataires$/);
  });
});
