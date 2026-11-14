const LocataireService = require('../src/services/locataire.service');
const Locataire = require('../src/models/locataire.model');
const CinDejaUtiliseException = require('../src/exceptions/cinDejaUtilise.exception');
const { randomUUID } = require('crypto');

describe('Campagne de Tests Unitaires - Module Gestion des Locataires (Ahmed LAARIF)', () => {
  let service;
  let id1;
  let id2;

  beforeEach(() => {
    service = new LocataireService();
    id1 = randomUUID();
    id2 = randomUUID();

    // 2 fiches existantes conformément au dossier de test
    const loc1 = new Locataire({
      id: id1,
      nom: 'El Amrani',
      prenom: 'Yassine',
      cin: 'AB123456',
      email: 'yassine@mail.ma',
      telephone: '0661001122'
    });

    const loc2 = new Locataire({
      id: id2,
      nom: 'Bennani',
      prenom: 'Fatima Zahra',
      cin: 'CD789012',
      email: 'fatima@mail.ma',
      telephone: '0662334455'
    });

    service.creerLocataire(loc1);
    service.creerLocataire(loc2);
  });

  test('UT-LOC-01 : Création valide d\'un locataire complet avec CIN unique', () => {
    const nouveau = {
      nom: 'Benjelloun',
      prenom: 'Mehdi',
      cin: 'EF345678',
      email: 'mehdi@mail.ma',
      telephone: '0661223344'
    };

    const resultat = service.creerLocataire(nouveau);

    expect(resultat).toBeDefined();
    expect(resultat.id).toBeDefined();
    expect(resultat.cin).toBe('EF345678');
    expect(resultat.nom).toBe('Benjelloun');
    expect(service.nombreLocataires()).toBe(3);
  });

  test('UT-LOC-02 : Refus si CIN déjà existant (levée de CinDejaUtiliseException)', () => {
    const doublon = {
      nom: 'Alami',
      prenom: 'Karim',
      cin: 'AB123456',
      email: 'k.alami@mail.ma',
      telephone: '0661998877'
    };

    expect(() => {
      service.creerLocataire(doublon);
    }).toThrow(CinDejaUtiliseException);

    try {
      service.creerLocataire(doublon);
    } catch (err) {
      expect(err.message).toContain('Fiche refusée : CIN déjà utilisé');
    }

    expect(service.nombreLocataires()).toBe(2);
  });

  test('UT-LOC-03 : Refus si argument null (levée d\'exception)', () => {
    expect(() => {
      service.creerLocataire(null);
    }).toThrow('Le locataire ne peut pas être null');

    expect(service.nombreLocataires()).toBe(2);
  });

  test('UT-LOC-04 : Refus si CIN vide ou composé d\'espaces', () => {
    const locataireCinVide = {
      nom: 'Naciri',
      prenom: 'Samir',
      cin: '   ',
      email: 'samir@mail.ma',
      telephone: '0663112233'
    };

    expect(() => {
      service.creerLocataire(locataireCinVide);
    }).toThrow('Le CIN est obligatoire');

    expect(service.nombreLocataires()).toBe(2);
  });

  test('UT-LOC-05 : Refus si format d\'email invalide', () => {
    const locataireEmailInvalide = {
      nom: 'Tazi',
      prenom: 'Kenza',
      cin: 'GH901234',
      email: 'email_invalide_sans_arobase',
      telephone: '0664556677'
    };

    expect(() => {
      service.creerLocataire(locataireEmailInvalide);
    }).toThrow("Format d'email invalide");

    expect(service.nombreLocataires()).toBe(2);
  });

  test('UT-LOC-06 : Recherche par UUID existant (locataire conforme retourné)', () => {
    const resultat = service.getById(id1);

    expect(resultat).not.toBeNull();
    expect(resultat.nom).toBe('El Amrani');
    expect(resultat.cin).toBe('AB123456');
  });

  test('UT-LOC-07 : Recherche par UUID inexistant (null retourné)', () => {
    const resultat = service.getById(randomUUID());
    expect(resultat).toBeNull();
  });

  test('UT-LOC-08 : Recherche par CIN existant (fiche correspondante retournée)', () => {
    const resultat = service.getByCin('CD789012');

    expect(resultat).not.toBeNull();
    expect(resultat.prenom).toBe('Fatima Zahra');
  });

  test('UT-LOC-09 : Recherche par CIN inexistant (null retourné)', () => {
    const resultat = service.getByCin('ZZ999999');
    expect(resultat).toBeNull();
  });

  test('UT-LOC-10 : Modification réussie de l\'email et du téléphone', () => {
    const succes = service.modifierCoordonnees(id1, 'yassine.pro@nouvel-email.ma', '0661001122');

    expect(succes).toBe(true);
    const maj = service.getById(id1);
    expect(maj.email).toBe('yassine.pro@nouvel-email.ma');
    expect(maj.telephone).toBe('0661001122');
  });

  test('UT-LOC-11 : Tentative de modification sur UUID inexistant (retourne false)', () => {
    const resultat = service.modifierCoordonnees(randomUUID(), 'inconnu@mail.ma', '0600000000');
    expect(resultat).toBe(false);
  });

  test('UT-LOC-12 : Comptage exact du nombre total de locataires enregistrés', () => {
    expect(service.nombreLocataires()).toBe(2);
  });

  test('UT-LOC-13 : Suppression d\'un locataire existant (retourne true)', () => {
    const suppressionReussie = service.supprimer(id1);

    expect(suppressionReussie).toBe(true);
    expect(service.getById(id1)).toBeNull();
    expect(service.nombreLocataires()).toBe(1);
  });

  test('UT-LOC-14 : Tentative de suppression sur UUID inexistant (retourne false)', () => {
    const suppressionEchouee = service.supprimer(randomUUID());

    expect(suppressionEchouee).toBe(false);
    expect(service.nombreLocataires()).toBe(2);
  });
});
