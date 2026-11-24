package com.gestionlocative.quittances.client;

import com.gestionlocative.quittances.dto.SituationPeriode.Affectation;
import com.gestionlocative.quittances.dto.SituationPeriode.Nature;
import com.gestionlocative.quittances.dto.SituationPeriode.StatutPaiement;
import com.gestionlocative.quittances.exception.RegleMetierException;
import com.gestionlocative.quittances.exception.ServiceExterneException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

/**
 * Adaptateur vers /api/echeances et /api/paiements du module Loyers.
 * L'enregistrement d'un paiement dans cette API représente un paiement confirmé.
 * Aucun règlement de charges ne peut être inventé à partir d'un paiement de loyer.
 */
public final class PaiementsCollectifsClient implements PaiementClient {
    private final RestClient client;

    public PaiementsCollectifsClient(RestClient client) {
        this.client = client;
    }

    public record Echeance(UUID id, UUID bailId, Integer mois, Integer annee,
            BigDecimal montant, String statut, BigDecimal totalPaye, BigDecimal resteAPayer) { }
    public record Paiement(UUID id, UUID echeanceId, BigDecimal montant,
            LocalDate datePaiement) { }

    @Override
    public List<Affectation> consulterAffectations(String bailId, LocalDate debut, LocalDate fin) {
        LectureApiExterne.verifierIdentifiant(bailId);
        if (debut == null || fin == null || fin.isBefore(debut)) {
            throw new RegleMetierException("PERIODE_INVALIDE", "Période invalide.");
        }
        YearMonth mois = YearMonth.from(debut);
        if (!debut.equals(mois.atDay(1)) || !fin.equals(mois.atEndOfMonth())) {
            throw new RegleMetierException("PERIODE_NON_MENSUELLE",
                    "Le service Loyers ne sait affecter que des mensualités entières.");
        }
        UUID reference;
        try {
            reference = UUID.fromString(bailId);
        } catch (IllegalArgumentException e) {
            throw new RegleMetierException("BAIL_NON_UUID", "Un identifiant de bail UUID est nécessaire.");
        }

        List<Echeance> echeances;
        List<Paiement> paiements;
        try {
            echeances = client.get().uri("/api/echeances?bailId={id}", Map.of("id", bailId))
                    .retrieve().body(new ParameterizedTypeReference<List<Echeance>>() { });
            paiements = client.get().uri("/api/paiements?bailId={id}", Map.of("id", bailId))
                    .retrieve().body(new ParameterizedTypeReference<List<Paiement>>() { });
        } catch (RestClientResponseException e) {
            throw new ServiceExterneException("PAIEMENTS", "ERREUR_HTTP_EXTERNE",
                    "Loyers a refusé la consultation.", e.getStatusCode().value());
        } catch (ResourceAccessException e) {
            throw new ServiceExterneException("PAIEMENTS", "SERVICE_INJOIGNABLE",
                    "Loyers est inaccessible.", null);
        } catch (RestClientException e) {
            throw new ServiceExterneException("PAIEMENTS", "REPONSE_ILLISIBLE",
                    "Réponse Loyers invalide.", null);
        }
        if (echeances == null || paiements == null) {
            throw LectureApiExterne.incoherence("PAIEMENTS");
        }
        Map<UUID, Echeance> parId = new HashMap<>();
        Set<YearMonth> periodes = new HashSet<>();
        for (Echeance e : echeances) {
            if (e == null || e.id() == null || !reference.equals(e.bailId())
                    || e.mois() == null || e.annee() == null || e.mois() < 1 || e.mois() > 12
                    || e.montant() == null || e.totalPaye() == null || e.resteAPayer() == null
                    || e.montant().signum() < 0 || e.totalPaye().signum() < 0
                    || e.resteAPayer().signum() < 0 || e.totalPaye().compareTo(e.montant()) > 0
                    || e.montant().subtract(e.totalPaye()).compareTo(e.resteAPayer()) != 0
                    || parId.putIfAbsent(e.id(), e) != null
                    || !periodes.add(YearMonth.of(e.annee(), e.mois()))) {
                throw LectureApiExterne.incoherence("PAIEMENTS");
            }
        }
        if (!periodes.contains(mois)) {
            throw LectureApiExterne.incoherence("PAIEMENTS");
        }
        Map<UUID, BigDecimal> sommes = new HashMap<>();
        Set<UUID> ids = new HashSet<>();
        List<Affectation> resultat = new ArrayList<>();
        for (Paiement paiement : paiements) {
            if (paiement == null || paiement.id() == null || paiement.echeanceId() == null
                    || paiement.montant() == null || paiement.montant().signum() <= 0
                    || paiement.datePaiement() == null || paiement.datePaiement().isAfter(LocalDate.now())
                    || !ids.add(paiement.id()) || !parId.containsKey(paiement.echeanceId())) {
                throw LectureApiExterne.incoherence("PAIEMENTS");
            }
            Echeance e = parId.get(paiement.echeanceId());
            sommes.merge(e.id(), paiement.montant(), BigDecimal::add);
            if (e.mois() == mois.getMonthValue() && e.annee() == mois.getYear()) {
                resultat.add(new Affectation("LOYERS-" + paiement.id(), paiement.id().toString(),
                        bailId, debut, fin, Nature.LOYER, paiement.montant(), "MAD",
                        StatutPaiement.CONFIRME));
            }
        }
        for (Echeance e : echeances) {
            if (sommes.getOrDefault(e.id(), BigDecimal.ZERO).compareTo(e.totalPaye()) != 0) {
                throw LectureApiExterne.incoherence("PAIEMENTS");
            }
        }
        return List.copyOf(resultat);
    }
}
