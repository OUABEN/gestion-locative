export const initialLocatairesEtBaux = [
  {
    id: "loc-001-c1f7b823",
    nom: "El Amrani",
    prenom: "Yassine",
    cin: "AB123456",
    telephone: "+212 6 61 00 11 22",
    email: "yassine.elamrani@gmail.com",
    bien: {
      titre: "Appartement B-12 (3e étage)",
      adresse: "45, Boulevard d'Anfa, Quartier Gauthier",
      ville: "Casablanca",
      type: "Appartement F4 High-Standing"
    },
    loyerMensuel: 6500,
    depotGarantie: 13000,
    dateDebut: "01/01/2026",
    dateFin: "31/12/2027",
    statutBail: "ACTIF", // 'ACTIF' | 'TERMINE'
    quittancesEmises: 11,
    piecesJustificatives: [
      { nom: "CIN_ElAmrani_RectoVerso.pdf", taille: "1.4 Mo" },
      { nom: "Contrat_Travail_CDI.pdf", taille: "2.1 Mo" },
      { nom: "Quittance_Caution_Signee.pdf", taille: "850 Ko" }
    ]
  },
  {
    id: "loc-002-8d9e2b14",
    nom: "Bennani",
    prenom: "Fatima Zahra",
    cin: "CD789012",
    telephone: "+212 6 62 33 44 55",
    email: "fatima.bennani@corporate.ma",
    bien: {
      titre: "Penthouse C-04 avec Terrasse",
      adresse: "12, Rue Michlifen, Agdal",
      ville: "Rabat",
      type: "Duplex Meublé"
    },
    loyerMensuel: 9200,
    depotGarantie: 18400,
    dateDebut: "15/02/2026",
    dateFin: "14/02/2028",
    statutBail: "ACTIF",
    quittancesEmises: 9,
    piecesJustificatives: [
      { nom: "CIN_Bennani_FZ.pdf", taille: "1.2 Mo" },
      { nom: "Attestation_Titularisation.pdf", taille: "780 Ko" }
    ]
  },
  {
    id: "loc-003-5a6b7c8d",
    nom: "Benjelloun",
    prenom: "Mehdi",
    cin: "EF345678",
    telephone: "+212 6 61 22 33 44",
    email: "mehdi.benjelloun@techinvest.ma",
    bien: {
      titre: "Villa Moderne Ryad Al Mansour",
      adresse: "28, Avenue Mohammed VI, Hivernage",
      ville: "Marrakech",
      type: "Villa avec Piscine"
    },
    loyerMensuel: 14500,
    depotGarantie: 29000,
    dateDebut: "01/03/2026",
    dateFin: "28/02/2027",
    statutBail: "ACTIF",
    quittancesEmises: 8,
    piecesJustificatives: [
      { nom: "CIN_Benjelloun_M.pdf", taille: "1.8 Mo" },
      { nom: "Extrait_RC_Societe.pdf", taille: "3.4 Mo" }
    ]
  },
  {
    id: "loc-004-2f3a4b5c",
    nom: "Tazi",
    prenom: "Kenza",
    cin: "GH901234",
    telephone: "+212 6 64 55 66 77",
    email: "kenza.tazi@avocat-barreau.ma",
    bien: {
      titre: "Bureau / Plateau Professionnel 105",
      adresse: "102, Angle Bd Zerktouni & Roudani",
      ville: "Casablanca",
      type: "Usage Professionnel / Commercial"
    },
    loyerMensuel: 8000,
    depotGarantie: 16000,
    dateDebut: "01/01/2024",
    dateFin: "31/12/2025",
    statutBail: "TERMINE",
    quittancesEmises: 24,
    piecesJustificatives: [
      { nom: "CIN_Tazi_Kenza.pdf", taille: "980 Ko" },
      { nom: "Pv_Cloture_Bail_2025.pdf", taille: "1.5 Mo" }
    ]
  },
  {
    id: "loc-005-9e4c1b7d",
    nom: "Alaoui",
    prenom: "Hamza",
    cin: "BK456789",
    telephone: "+212 6 63 77 88 99",
    email: "hamza.alaoui@marocfinance.ma",
    bien: {
      titre: "Appartement Marina Bay 402",
      adresse: "Avenue d'Espagne, Port de Tanger",
      ville: "Tanger",
      type: "Appartement Vue Mer"
    },
    loyerMensuel: 7200,
    depotGarantie: 14400,
    dateDebut: "01/04/2026",
    dateFin: "31/03/2028",
    statutBail: "ACTIF",
    quittancesEmises: 7,
    piecesJustificatives: [
      { nom: "CIN_Alaoui_Hamza.pdf", taille: "1.1 Mo" }
    ]
  },
  {
    id: "loc-006-7b2a9e1f",
    nom: "Chraibi",
    prenom: "Salma",
    cin: "EE654321",
    telephone: "+212 6 65 11 22 33",
    email: "salma.chraibi@artdesign.ma",
    bien: {
      titre: "Studio Cosy Palmier",
      adresse: "18, Rue des Palmiers, Bourgogne",
      ville: "Casablanca",
      type: "Studio Meublé"
    },
    loyerMensuel: 4800,
    depotGarantie: 9600,
    dateDebut: "01/09/2024",
    dateFin: "31/08/2025",
    statutBail: "TERMINE",
    quittancesEmises: 12,
    piecesJustificatives: [
      { nom: "CIN_Chraibi_Salma.pdf", taille: "890 Ko" }
    ]
  }
];

export const monthlyRevenueData = [
  { month: "Jan", loyers: 285000, encaisse: 285000 },
  { month: "Fév", loyers: 292000, encaisse: 290000 },
  { month: "Mar", loyers: 298000, encaisse: 298000 },
  { month: "Avr", loyers: 304000, encaisse: 302000 },
  { month: "Mai", loyers: 307000, encaisse: 307000 },
  { month: "Juin", loyers: 312000, encaisse: 312000 },
  { month: "Juil", loyers: 312000, encaisse: 310500 },
  { month: "Août", loyers: 312000, encaisse: 312000 },
  { month: "Sept", loyers: 312000, encaisse: 312000 },
  { month: "Oct", loyers: 312000, encaisse: 312000 },
  { month: "Nov", loyers: 312000, encaisse: 312000 }
];

export const cityDistributionData = [
  { ville: "Casablanca", count: 24, percent: 50, color: "#C6F432" },
  { ville: "Rabat", count: 12, percent: 25, color: "#141417" },
  { ville: "Marrakech", count: 8, percent: 17, color: "#5F92CA" },
  { ville: "Tanger", count: 4, percent: 8, color: "#26262C" }
];
