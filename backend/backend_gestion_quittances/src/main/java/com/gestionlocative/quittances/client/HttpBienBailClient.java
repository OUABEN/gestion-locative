package com.gestionlocative.quittances.client;

import java.time.LocalDate;
import java.util.Map;

import com.gestionlocative.quittances.dto.ReferentielsExternes.BailPeriode;
import com.gestionlocative.quittances.dto.ReferentielsExternes.Bien;
import com.gestionlocative.quittances.exception.RegleMetierException;

import org.springframework.web.client.RestClient;

public class HttpBienBailClient implements BienBailClient {

    private final RestClient biensClient;
    private final String biensChemin;
    private final RestClient bauxClient;
    private final String bauxChemin;

    public HttpBienBailClient(
            RestClient biensClient,
            String biensChemin,
            RestClient bauxClient,
            String bauxChemin) {

        this.biensClient = biensClient;
        this.biensChemin = biensChemin;
        this.bauxClient = bauxClient;
        this.bauxChemin = bauxChemin;
    }

    @Override
    public Bien consulterBien(String id) {
        LectureApiExterne.verifierIdentifiant(id);

        Bien resultat = LectureApiExterne.lire(
                biensClient,
                "BIENS",
                biensChemin,
                Map.of("id", id),
                Bien.class
        );

        if (!"1".equals(resultat.versionContrat())
                || !id.equals(resultat.id())
                || !LectureApiExterne.textePresent(resultat.adresse())
                || !LectureApiExterne.identifiantValide(resultat.bailleurId())) {
            throw LectureApiExterne.incoherence("BIENS");
        }

        return resultat;
    }

    @Override
    public BailPeriode consulterBail(
            String id,
            LocalDate debut,
            LocalDate fin) {

        LectureApiExterne.verifierIdentifiant(id);

        if (debut == null || fin == null || fin.isBefore(debut)) {
            throw new RegleMetierException(
                    "PERIODE_INVALIDE",
                    "La période demandée est invalide."
            );
        }

        BailPeriode resultat = LectureApiExterne.lire(
                bauxClient,
                "BAUX",
                bauxChemin,
                Map.of(
                        "id", id,
                        "debut", debut.toString(),
                        "fin", fin.toString()
                ),
                BailPeriode.class
        );

        if (!"1".equals(resultat.versionContrat())
                || !id.equals(resultat.bailId())
                || !debut.equals(resultat.periodeDebut())
                || !fin.equals(resultat.periodeFin())
                || !LectureApiExterne.identifiantValide(resultat.locataireId())
                || !LectureApiExterne.identifiantValide(resultat.bienId())
                || !LectureApiExterne.identifiantValide(resultat.bailleurId())
                || !LectureApiExterne.textePresent(resultat.nomBailleur())
                || resultat.debutBail() == null
                || resultat.loyerDu() == null
                || resultat.chargesDues() == null
                || !"MAD".equals(resultat.devise())) {
            throw LectureApiExterne.incoherence("BAUX");
        }

        return resultat;
    }
}