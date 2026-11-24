package com.gestionlocative.quittances.client;

import java.util.Map;

import com.gestionlocative.quittances.dto.ReferentielsExternes.Locataire;

import org.springframework.web.client.RestClient;

public class HttpLocataireClient implements LocataireClient {

    private final RestClient client;
    private final String chemin;
    private final boolean formatAhmed;
    private final boolean resolutionUtilisateurAutorisee;

    public HttpLocataireClient(RestClient client, String chemin) {
        this(client, chemin, false);
    }

    public HttpLocataireClient(RestClient client, String chemin, boolean formatAhmed) {
        this(client, chemin, formatAhmed, false);
    }

    public HttpLocataireClient(
            RestClient client,
            String chemin,
            boolean formatAhmed,
            boolean resolutionUtilisateurAutorisee) {
        this.resolutionUtilisateurAutorisee = resolutionUtilisateurAutorisee;
        this.formatAhmed = formatAhmed;
        this.client = client;
        this.chemin = chemin;
    }

    public record SyntheseAhmed(String id, String nom, String prenom, String email,
            String nomComplet, String cin, String telephone) { }

    @Override
    public Locataire consulter(String id) {
        LectureApiExterne.verifierIdentifiant(id);

        if (formatAhmed) {
            SyntheseAhmed reponse = LectureApiExterne.lire(client, "LOCATAIRES", chemin,
                    Map.of("id", id), SyntheseAhmed.class);
            if (!id.equals(reponse.id()) || !LectureApiExterne.textePresent(reponse.nom())
                    || !LectureApiExterne.textePresent(reponse.prenom())) {
                throw LectureApiExterne.incoherence("LOCATAIRES");
            }
            // Version du contrat INTERNE après adaptation, pas une version déclarée par Ahmed.
            // L'API ne prouve pas la vérification de l'adresse : jamais true par défaut.
            return new Locataire("1", reponse.id(), reponse.nom(), reponse.prenom(),
                    reponse.email(), false);
        }

        Locataire resultat = LectureApiExterne.lire(
                client,
                "LOCATAIRES",
                chemin,
                Map.of("id", id),
                Locataire.class
        );

        if (!"1".equals(resultat.versionContrat())
                || !id.equals(resultat.id())
                || !LectureApiExterne.textePresent(resultat.nom())
                || resultat.emailVerifie() == null
                || (Boolean.TRUE.equals(resultat.emailVerifie())
                && !LectureApiExterne.textePresent(resultat.email()))) {
            throw LectureApiExterne.incoherence("LOCATAIRES");
        }

        return resultat;
    }

    public record FicheConnectee(String id) {}

    @Override
    public String identifierConnecte() {
        if (!resolutionUtilisateurAutorisee) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "La resolution du locataire exige le JWT utilisateur.");
        }

        try {
            FicheConnectee fiche = client.get()
                    .uri("/api/locataires/me")
                    .retrieve()
                    .body(FicheConnectee.class);

            if (fiche == null
                    || !LectureApiExterne.identifiantValide(fiche.id())
                    || !fiche.id().equals(fiche.id().trim())) {
                throw LectureApiExterne.incoherence("LOCATAIRES");
            }

            return fiche.id();

        } catch (org.springframework.web.client.RestClientResponseException e) {
            if (e.getStatusCode().value() == 404
                    || e.getStatusCode().value() == 403
                    || e.getStatusCode().value() == 401) {
                throw new org.springframework.security.access.AccessDeniedException(
                        "Aucune fiche locataire accessible pour ce compte.");
            }

            throw new com.gestionlocative.quittances.exception.ServiceExterneException(
                    "LOCATAIRES",
                    "ERREUR_HTTP_EXTERNE",
                    "Impossible de resoudre le locataire connecte.",
                    e.getStatusCode().value());

        } catch (org.springframework.web.client.RestClientException e) {
            throw new com.gestionlocative.quittances.exception.ServiceExterneException(
                    "LOCATAIRES",
                    "RESOLUTION_INDISPONIBLE",
                    "Le service Locataires est indisponible.",
                    null);
        }
    }
}