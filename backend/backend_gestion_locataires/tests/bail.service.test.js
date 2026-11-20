const BailService = require('../src/services/bail.service');
const { randomUUID } = require('crypto');

describe('Campagne de Tests Unitaires - Module Baux (Ahmed LAARIF)', () => {
  let bailService;

  beforeEach(() => {
    bailService = new BailService();
  });

  test('UT-BAIL-01 : Création valide d un contrat de bail', () => {
    const bienId = randomUUID();
    const locataireId = randomUUID();

    const bail = bailService.creerBail({
      bienId,
      locataireId,
      dateDebut: '2026-03-01',
      dateFin: '2027-02-28',
      montantLoyer: 6500,
      depotGarantie: 13000,
      statut: 'ACTIF'
    });

    expect(bail).toBeDefined();
    expect(bail.id).toBeDefined();
    expect(bail.bienId).toBe(bienId);
    expect(bail.montantLoyer).toBe(6500);
    expect(bail.statut).toBe('ACTIF');
  });

  test('UT-BAIL-02 : Refus d un second bail ACTIF sur le même bien (conflit 409)', () => {
    const bienId = randomUUID();
    const locataireId1 = randomUUID();
    const locataireId2 = randomUUID();

    bailService.creerBail({
      bienId,
      locataireId: locataireId1,
      dateDebut: '2026-01-01',
      montantLoyer: 5000,
      statut: 'ACTIF'
    });

    expect(() => {
      bailService.creerBail({
        bienId,
        locataireId: locataireId2,
        dateDebut: '2026-02-01',
        montantLoyer: 5500,
        statut: 'ACTIF'
      });
    }).toThrow('Ce bien possède déjà un bail actif.');
  });

  test('UT-BAIL-03 : Recherche de bail par ID pour interopérabilité Loyers/Quittances', () => {
    const defaultBailId = 'b1111111-1111-1111-1111-111111111111';
    const bail = bailService.getById(defaultBailId);

    expect(bail).not.toBeNull();
    expect(bail.id).toBe(defaultBailId);
    expect(bail.montantLoyer).toBe(3400);
  });

  test('UT-BAIL-04 : Filtrage des baux actifs (listerActifs)', () => {
    const actifs = bailService.listerActifs();
    expect(Array.isArray(actifs)).toBe(true);
    expect(actifs.length).toBeGreaterThanOrEqual(1);
    actifs.forEach(b => expect(b.statut).toBe('ACTIF'));
  });

  test('UT-BAIL-05 : Mise à jour du statut du bail en TERMINE', () => {
    const defaultBailId = 'b1111111-1111-1111-1111-111111111111';
    const updated = bailService.mettreAJourStatut(defaultBailId, 'TERMINE');

    expect(updated).not.toBeNull();
    expect(updated.statut).toBe('TERMINE');
  });
});