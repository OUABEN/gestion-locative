// Tests de navigation React en mode simule : aucune requete vers les API reelles.
// Le JWT ci-dessous est SYNTHETIQUE et n'est pas signe : il sert uniquement
// a initialiser l'etat d'interface. Le backend ne le recoit jamais.

const API = '/api/quittances/api/v1';

function testToken(role = 'BAILLEUR') {
  const encode = (data) => btoa(JSON.stringify(data)).replace(/=/g, '');
  return [
    encode({ alg: 'HS256', typ: 'JWT' }),
    encode({ sub: 'e2e-user-synthetique', role, iat: Math.floor(Date.now() / 1000), exp: Math.floor(Date.now() / 1000) + 3600 }),
    'non-signe-test',
  ].join('.');
}

function visitAs(path, role = 'BAILLEUR') {
  const token = testToken(role);
  cy.visit(path, {
    onBeforeLoad(win) {
      win.localStorage.setItem('token', token);
      win.sessionStorage.setItem('token', token);
    },
  });
}

function mockIdentity(role = 'ROLE_BAILLEUR') {
  cy.intercept('GET', `${API}/identite`, {
    statusCode: 200,
    body: { sujet: 'e2e-user-synthetique', autorites: [role] },
  }).as('identity');
}

function mockList() {
  cy.intercept('GET', `${API}/quittances*`, {
    statusCode: 200,
    body: { contenu: [], page: 0, taille: 20, totalElements: 0, totalPages: 0 },
  }).as('list');
  cy.intercept('GET', `${API}/quittances/tableau-de-bord`, {
    statusCode: 200,
    body: { totalDocuments: 0, groupes: [] },
  }).as('dashboard');
}

function fillDraftForm() {
  cy.contains('label', 'Identifiant du bail').find('input').type('BAIL-E2E-42');
  cy.contains('label', 'Début de période').find('input').type('2026-10-01');
  cy.contains('label', 'Fin de période').find('input').type('2026-10-31');
}

describe('Quittances — interface React, fournisseurs API simules', () => {
  beforeEach(() => {
    // Filet de securite : toutes les requetes /api/ non definies sont bloquees.
    // Les interceptions specifiques declarees ensuite prennent priorite.
    cy.intercept('**/api/**', {
      statusCode: 503,
      body: { message: 'Appel API non simule : mode E2E isole.' },
    });
  });

  it('redirige vers la connexion sans session', () => {
    cy.visit('/quittances', {
      onBeforeLoad(win) {
        win.localStorage.clear();
        win.sessionStorage.clear();
      },
    });
    cy.location('pathname').should('eq', '/login');
  });

  it('affiche la liste vide pour un bailleur et ses indicateurs', () => {
    mockIdentity();
    mockList();
    visitAs('/quittances');
    cy.wait(['@identity', '@list', '@dashboard']);
    cy.get('h1').should('contain.text', 'Quittances');
    cy.contains('Aucune quittance pour ces critères.').should('be.visible');
    cy.contains('Préparer une quittance').should('be.visible');
    cy.screenshot('01-liste-vide-synthetique', { capture: 'viewport' });
  });

  it('transmet les filtres sans ecrire de document', () => {
    mockIdentity();
    mockList();
    visitAs('/quittances');
    cy.wait('@list');
    cy.contains('label', 'Identifiant du bail').find('input').type('BAIL-E2E-42');
    cy.contains('label', 'Statut').find('select').select('EMIS');
    cy.contains('button', 'Appliquer').click();
    cy.wait('@list').then(({ request }) => {
      expect(request.url).to.include('bailId=BAIL-E2E-42');
      expect(request.url).to.include('statut=EMIS');
    });
    cy.screenshot('02-filtres-synthetiques', { capture: 'viewport' });
  });

  it('refuse un role GESTIONNAIRE non reconnu par Quittances', () => {
    mockIdentity('ROLE_GESTIONNAIRE');
    visitAs('/quittances', 'GESTIONNAIRE');
    cy.wait('@identity');
    cy.contains('Identité Quittances non reconnue').should('be.visible');
    cy.screenshot('03-refus-role-gestionnaire', { capture: 'viewport' });
  });

  it('n affiche pas le formulaire de creation au LOCATAIRE', () => {
    mockIdentity('ROLE_LOCATAIRE');
    visitAs('/quittances/nouveau', 'LOCATAIRE');
    cy.wait('@identity');
    cy.contains('La préparation d’une quittance est réservée au rôle BAILLEUR.').should('be.visible');
    cy.contains('button', 'Créer le brouillon').should('not.exist');
  });

  it('refuse une periode inversee avant tout POST', () => {
    mockIdentity();
    let mutations = 0;
    cy.intercept('POST', `${API}/quittances`, (req) => {
      mutations += 1;
      req.reply({ statusCode: 500, body: { message: 'POST interdit dans ce test.' } });
    });
    visitAs('/quittances/nouveau');
    cy.wait('@identity');
    cy.contains('label', 'Identifiant du bail').find('input').type('BAIL-E2E-42');
    cy.contains('label', 'Début de période').find('input').type('2026-10-31');
    cy.contains('label', 'Fin de période').find('input').type('2026-10-01');
    cy.contains('button', 'Créer le brouillon').click();
    cy.contains('La fin de période doit être postérieure ou égale au début.').should('be.visible');
    cy.then(() => expect(mutations, 'aucun POST effectue').to.equal(0));
    cy.screenshot('04-periode-invalide', { capture: 'viewport' });
  });

  it('prepare un brouillon via un POST intercepte et navigue vers son detail', () => {
    mockIdentity();
    cy.intercept('POST', `${API}/quittances`, {
      statusCode: 201,
      body: { id: 'document-e2e-synthetique', statut: 'BROUILLON' },
    }).as('create');
    visitAs('/quittances/nouveau');
    cy.wait('@identity');
    fillDraftForm();
    cy.contains('button', 'Créer le brouillon').click();
    cy.wait('@create').then(({ request }) => {
      expect(request.body).to.deep.equal({
        bailId: 'BAIL-E2E-42',
        periodeDebut: '2026-10-01',
        periodeFin: '2026-10-31',
      });
    });
    cy.location('pathname').should('eq', '/quittances/document-e2e-synthetique');
  });
});
