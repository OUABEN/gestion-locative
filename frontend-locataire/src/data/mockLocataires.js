export const initialLocataires = [
  {
    id: "c1f7b823-3f1d-405a-87f1-79b8c0a969e2",
    nom: "El Amrani",
    prenom: "Yassine",
    cin: "AB123456",
    email: "yassine.elamrani@mail.ma",
    telephone: "+212 6 61 00 11 22",
    hasBailActif: true,
    bailInfo: {
      bienId: "b890-casablanca-gauthier",
      residence: "Résidence Al Mansour",
      appartement: "Apt B-12 (3e étage)",
      adresse: "45, Boulevard d'Anfa, Casablanca",
      loyerMensuel: 4800,
      charges: 300,
      dateDebut: "01/01/2026",
      statut: "ACTIF"
    },
    piecesJustificatives: [
      { id: "p1", nom: "CIN_ElAmrani_RectoVerso.pdf", taille: "1.4 Mo", dateUpload: "15/12/2025" },
      { id: "p2", nom: "Contrat_Travail_CDI.pdf", taille: "2.1 Mo", dateUpload: "15/12/2025" },
      { id: "p3", nom: "Trois_Derniers_Bulletins_Paie.pdf", taille: "3.5 Mo", dateUpload: "15/12/2025" }
    ],
    createdAt: "2025-12-15T10:30:00Z",
    updatedAt: "2026-01-10T14:22:00Z"
  },
  {
    id: "8d9e2b14-6c3f-4e89-a1b2-3c4d5e6f7a8b",
    nom: "Bennani",
    prenom: "Fatima Zahra",
    cin: "CD789012",
    email: "fatima.bennani@mail.ma",
    telephone: "+212 6 62 33 44 55",
    hasBailActif: true,
    bailInfo: {
      bienId: "b102-rabat-agdal",
      residence: "Les Jardins de l'Agdal",
      appartement: "Apt C-04 (1er étage)",
      adresse: "12, Rue Michlifen, Rabat",
      loyerMensuel: 5500,
      charges: 400,
      dateDebut: "15/02/2026",
      statut: "ACTIF"
    },
    piecesJustificatives: [
      { id: "p4", nom: "CIN_Bennani_FZ.pdf", taille: "1.2 Mo", dateUpload: "02/02/2026" },
      { id: "p5", nom: "Attestation_Titularisation.pdf", taille: "850 Ko", dateUpload: "02/02/2026" }
    ],
    createdAt: "2026-02-02T09:15:00Z",
    updatedAt: "2026-02-15T11:00:00Z"
  },
  {
    id: "5a6b7c8d-9e0f-4a1b-8c2d-3e4f5a6b7c8d",
    nom: "Benjelloun",
    prenom: "Mehdi",
    cin: "EF345678",
    email: "mehdi.benjelloun@mail.ma",
    telephone: "+212 6 61 22 33 44",
    hasBailActif: true,
    bailInfo: {
      bienId: "b304-marrakech-gueliz",
      residence: "Le Carré Majorelle",
      appartement: "Apt A-01 (RDC)",
      adresse: "28, Avenue Mohammed V, Marrakech",
      loyerMensuel: 4200,
      charges: 250,
      dateDebut: "01/03/2026",
      statut: "ACTIF"
    },
    piecesJustificatives: [
      { id: "p6", nom: "CIN_Benjelloun_M.pdf", taille: "1.8 Mo", dateUpload: "20/02/2026" },
      { id: "p7", nom: "Registre_Commerce_Extrait.pdf", taille: "1.1 Mo", dateUpload: "20/02/2026" }
    ],
    createdAt: "2026-02-20T16:45:00Z",
    updatedAt: "2026-03-01T08:00:00Z"
  },
  {
    id: "2f3a4b5c-6d7e-4f8a-9b0c-1d2e3f4a5b6c",
    nom: "Tazi",
    prenom: "Kenza",
    cin: "GH901234",
    email: "kenza.tazi@mail.ma",
    telephone: "+212 6 64 55 66 77",
    hasBailActif: false,
    bailInfo: null,
    piecesJustificatives: [
      { id: "p8", nom: "CIN_Tazi_Kenza.pdf", taille: "980 Ko", dateUpload: "01/11/2026" },
      { id: "p9", nom: "Dossier_Candidature_Locative.pdf", taille: "3.2 Mo", dateUpload: "01/11/2026" }
    ],
    createdAt: "2026-11-01T11:20:00Z",
    updatedAt: "2026-11-01T11:20:00Z"
  }
];
