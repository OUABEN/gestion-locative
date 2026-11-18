package com.gestionlocative.quittances.client;

import java.util.Map;

import com.gestionlocative.quittances.dto.ReferentielsExternes.Locataire;

import org.springframework.web.client.RestClient;

public class HttpLocataireClient implements LocataireClient {

    private final RestClient client;
    private final String chemin;
    private final boolean formatAhmed;

    public HttpLocataireClient(RestClient client, String chemin) {
        this(client, chemin, false);
    }

    public HttpLocataireClient(RestClient client, String chemin, boolean formatAhmed) {
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
}