package com.gestionlocative.quittances.client;

import java.util.Map;

import com.gestionlocative.quittances.dto.ReferentielsExternes.Locataire;

import org.springframework.web.client.RestClient;

public class HttpLocataireClient implements LocataireClient {

    private final RestClient client;
    private final String chemin;

    public HttpLocataireClient(RestClient client, String chemin) {
        this.client = client;
        this.chemin = chemin;
    }

    @Override
    public Locataire consulter(String id) {
        LectureApiExterne.verifierIdentifiant(id);

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