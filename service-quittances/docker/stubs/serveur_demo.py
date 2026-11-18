import json
import os
from copy import deepcopy
from uuid import UUID
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from urllib.parse import parse_qs, urlsplit


HOTE = "127.0.0.1"
PORT = 8091

DEBUT_PERIODE = "2026-10-01"
FIN_PERIODE = "2026-10-31"

LOCATAIRES = {
    "LOC-DEMO-001": {
        "versionContrat": "1",
        "id": "LOC-DEMO-001",
        "nom": "Demonstration",
        "prenom": "Locataire Un",
        "email": "locataire.un@example.test",
        "emailVerifie": False,
    },
    "LOC-DEMO-002": {
        "versionContrat": "1",
        "id": "LOC-DEMO-002",
        "nom": "Demonstration",
        "prenom": "Locataire Deux",
        "email": "locataire.deux@example.test",
        "emailVerifie": False,
    },
}

BIENS = {
    "BIEN-DEMO-001": {
        "versionContrat": "1",
        "id": "BIEN-DEMO-001",
        "adresse": "Adresse fictive de demonstration 1, Agadir",
        "bailleurId": "BAILLEUR-TEST",
    },
    "BIEN-DEMO-002": {
        "versionContrat": "1",
        "id": "BIEN-DEMO-002",
        "adresse": "Adresse fictive de demonstration 2, Agadir",
        "bailleurId": "BAILLEUR-TEST",
    },
}

BAUX = {
    "BAIL-DEMO-001": {
        "versionContrat": "1",
        "bailId": "BAIL-DEMO-001",
        "locataireId": "LOC-DEMO-001",
        "bienId": "BIEN-DEMO-001",
        "bailleurId": "BAILLEUR-TEST",
        "nomBailleur": "Bailleur fictif de demonstration",
        "debutBail": "2026-01-01",
        "finBail": "2026-12-31",
        "periodeDebut": DEBUT_PERIODE,
        "periodeFin": FIN_PERIODE,
        "loyerDu": "3000.00",
        "chargesDues": "200.00",
        "devise": "MAD",
    },
    "BAIL-DEMO-002": {
        "versionContrat": "1",
        "bailId": "BAIL-DEMO-002",
        "locataireId": "LOC-DEMO-002",
        "bienId": "BIEN-DEMO-002",
        "bailleurId": "BAILLEUR-TEST",
        "nomBailleur": "Bailleur fictif de demonstration",
        "debutBail": "2026-01-01",
        "finBail": "2026-12-31",
        "periodeDebut": DEBUT_PERIODE,
        "periodeFin": FIN_PERIODE,
        "loyerDu": "3000.00",
        "chargesDues": "200.00",
        "devise": "MAD",
    },
}


def affectation(identifiant, paiement_id, bail_id, nature, montant):
    return {
        "affectationId": identifiant,
        "paiementId": paiement_id,
        "bailId": bail_id,
        "periodeDebut": DEBUT_PERIODE,
        "periodeFin": FIN_PERIODE,
        "nature": nature,
        "montant": montant,
        "devise": "MAD",
        "statut": "CONFIRME",
    }


AFFECTATIONS = {
    "BAIL-DEMO-001": [
        affectation(
            "AFF-DEMO-001",
            "PAY-DEMO-001",
            "BAIL-DEMO-001",
            "LOYER",
            "1500.00",
        ),
        affectation(
            "AFF-DEMO-002",
            "PAY-DEMO-002",
            "BAIL-DEMO-001",
            "LOYER",
            "1500.00",
        ),
        affectation(
            "AFF-DEMO-003",
            "PAY-DEMO-002",
            "BAIL-DEMO-001",
            "CHARGES",
            "200.00",
        ),
    ],
    "BAIL-DEMO-002": [
        affectation(
            "AFF-DEMO-004",
            "PAY-DEMO-003",
            "BAIL-DEMO-002",
            "LOYER",
            "1500.00",
        )
    ],
}


# Scénario hybride explicite : seul le locataire vient du service de l'équipe.
locataire_integration = os.environ.get("DARLOC_LOCATAIRE_INTEGRATION_ID", "")
if locataire_integration:
    UUID(locataire_integration)  # Refuser une configuration incorrecte au démarrage.
    bail_integration = "BAIL-INTEGRATION-" + locataire_integration
    BAUX[bail_integration] = dict(BAUX["BAIL-DEMO-002"],
        bailId=bail_integration, locataireId=locataire_integration)
    AFFECTATIONS[bail_integration] = deepcopy(AFFECTATIONS["BAIL-DEMO-002"])
    for ligne in AFFECTATIONS[bail_integration]:
        ligne["bailId"] = bail_integration
        ligne["affectationId"] = "AFF-INT-" + locataire_integration
        ligne["paiementId"] = "PAY-INT-" + locataire_integration


class RequeteInvalide(Exception):
    pass


def parametre_unique(parametres, nom):
    valeurs = parametres.get(nom)

    if valeurs is None or len(valeurs) != 1 or not valeurs[0]:
        raise RequeteInvalide(
            f"Le parametre {nom} doit etre present une seule fois."
        )

    return valeurs[0]


def verifier_periode(parametres):
    debut = parametre_unique(parametres, "debut")
    fin = parametre_unique(parametres, "fin")

    if debut != DEBUT_PERIODE or fin != FIN_PERIODE:
        raise RequeteInvalide(
            "Cette simulation couvre uniquement octobre 2026."
        )


class Gestionnaire(BaseHTTPRequestHandler):
    def repondre(self, statut, contenu):
        corps = json.dumps(
            contenu,
            ensure_ascii=False,
            allow_nan=False,
        ).encode("utf-8")

        self.send_response(statut)
        self.send_header("Content-Type", "application/json; charset=utf-8")
        self.send_header("Content-Length", str(len(corps)))
        self.send_header("Cache-Control", "no-store")
        self.send_header("X-Donnees-Simulees", "true")
        self.end_headers()
        self.wfile.write(corps)

    def erreur(self, statut, code, message):
        self.repondre(
            statut,
            {
                "simulation": True,
                "code": code,
                "message": message,
            },
        )

    def do_GET(self):
        url = urlsplit(self.path)
        chemin = url.path
        parametres = parse_qs(url.query, keep_blank_values=True)

        try:
            if chemin == "/health":
                self.repondre(
                    200,
                    {
                        "service": "simulation-services-externes",
                        "statut": "OPERATIONNEL",
                        "simulation": True,
                    },
                )
                return

            if chemin.startswith("/api/v1/locataires/"):
                identifiant = chemin.removeprefix("/api/v1/locataires/")
                locataire = LOCATAIRES.get(identifiant)

                if locataire is None:
                    self.erreur(404, "INTROUVABLE", "Locataire fictif inconnu.")
                    return

                self.repondre(200, locataire)
                return

            if chemin.startswith("/api/v1/biens/"):
                identifiant = chemin.removeprefix("/api/v1/biens/")
                bien = BIENS.get(identifiant)

                if bien is None:
                    self.erreur(404, "INTROUVABLE", "Bien fictif inconnu.")
                    return

                self.repondre(200, bien)
                return

            if chemin.startswith("/api/v1/baux/") and chemin.endswith(
                "/situation"
            ):
                identifiant = chemin[
                    len("/api/v1/baux/") : -len("/situation")
                ]
                bail = BAUX.get(identifiant)

                if bail is None:
                    self.erreur(404, "INTROUVABLE", "Bail fictif inconnu.")
                    return

                verifier_periode(parametres)
                self.repondre(200, bail)
                return

            if chemin == "/api/v1/paiements/affectations":
                bail_id = parametre_unique(parametres, "bailId")

                if bail_id not in BAUX:
                    self.erreur(404, "INTROUVABLE", "Bail fictif inconnu.")
                    return

                verifier_periode(parametres)

                self.repondre(
                    200,
                    {
                        "versionContrat": "1",
                        "complet": True,
                        "bailId": bail_id,
                        "periodeDebut": DEBUT_PERIODE,
                        "periodeFin": FIN_PERIODE,
                        "affectations": AFFECTATIONS[bail_id],
                    },
                )
                return

            self.erreur(404, "ROUTE_INCONNUE", "Route de simulation inconnue.")

        except RequeteInvalide as exception:
            self.erreur(400, "REQUETE_INVALIDE", str(exception))

    def log_message(self, format_message, *arguments):
        print(
            "[SIMULATION] "
            + self.address_string()
            + " "
            + (format_message % arguments),
            flush=True,
        )


def main():
    serveur = ThreadingHTTPServer((HOTE, PORT), Gestionnaire)

    print(
        f"Services SIMULES disponibles sur http://{HOTE}:{PORT}",
        flush=True,
    )
    print(
        "Donnees fictives uniquement. Arret avec Ctrl+C.",
        flush=True,
    )

    try:
        serveur.serve_forever()
    except KeyboardInterrupt:
        print("\nArret des services simules.", flush=True)
    finally:
        serveur.server_close()


if __name__ == "__main__":
    main()