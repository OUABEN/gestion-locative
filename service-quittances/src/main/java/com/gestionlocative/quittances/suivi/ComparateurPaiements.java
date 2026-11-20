package com.gestionlocative.quittances.suivi;

import com.gestionlocative.quittances.dto.AffectationQuittanceResponse;
import com.gestionlocative.quittances.dto.SituationPeriode.Affectation;
import com.gestionlocative.quittances.dto.SituationPeriode.Nature;
import com.gestionlocative.quittances.dto.SituationPeriode.StatutPaiement;
import com.gestionlocative.quittances.exception.ServiceExterneException;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;

/** Compare les affectations conservées avec la réponse complète du service Paiements. */
public final class ComparateurPaiements {
    private ComparateurPaiements() {}
    public record Ecart(String affectationId, String natureEcart, String detail,
                        AffectationQuittanceResponse avant, AffectationQuittanceResponse maintenant) {}
    public static List<Ecart> comparer(String bailId, LocalDate debut, LocalDate fin,
                                      List<AffectationQuittanceResponse> reference, List<Affectation> actuelles) {
        if (actuelles == null) throw incoherence();
        Map<String, Affectation> sources = new TreeMap<>();
        Map<String, StatutPaiement> statuts = new HashMap<>();
        for (var a : actuelles) {
            verifier(a, bailId, debut, fin);
            var precedent = sources.putIfAbsent(a.affectationId(), a);
            if (precedent != null && !equivalent(convertir(precedent), convertir(a))) throw incoherence();
            var statut = statuts.putIfAbsent(a.paiementId(), a.statut());
            if (statut != null && statut != a.statut()) throw incoherence();
        }
        List<Ecart> ecarts = new ArrayList<>();
        for (var avant : reference) {
            var source = sources.remove(avant.affectationId());
            if (source == null) ecarts.add(new Ecart(avant.affectationId(), "ABSENTE", "Affectation absente de la réponse actuelle.", avant, null));
            else {
                var apres = convertir(source);
                if (!equivalent(avant, apres)) ecarts.add(new Ecart(avant.affectationId(), "MODIFIEE",
                        "Les données ou le statut de cette affectation ont changé.", avant, apres));
            }
        }
        for (var source : sources.values()) {
            if (source.statut() == StatutPaiement.CONFIRME && source.montant().signum() > 0
                    && (source.nature() == Nature.LOYER || source.nature() == Nature.CHARGES)) {
                ecarts.add(new Ecart(source.affectationId(), "AJOUTEE", "Nouvelle affectation confirmée pour la période.", null, convertir(source)));
            }
        }
        return List.copyOf(ecarts);
    }
    private static boolean equivalent(AffectationQuittanceResponse a, AffectationQuittanceResponse b) {
        return Objects.equals(a.affectationId(), b.affectationId()) && Objects.equals(a.paiementId(), b.paiementId())
                && Objects.equals(a.periodeDebut(), b.periodeDebut()) && Objects.equals(a.periodeFin(), b.periodeFin())
                && Objects.equals(a.nature(), b.nature()) && a.montant().compareTo(b.montant()) == 0
                && Objects.equals(a.devise(), b.devise()) && Objects.equals(a.statutSourceALaPreparation(), b.statutSourceALaPreparation());
    }
    private static AffectationQuittanceResponse convertir(Affectation a) {
        return new AffectationQuittanceResponse(a.affectationId(), a.paiementId(), a.periodeDebut(), a.periodeFin(),
                a.nature().name(), a.montant(), a.devise(), a.statut().name());
    }
    private static void verifier(Affectation a, String bailId, LocalDate debut, LocalDate fin) {
        if (a == null || !identifiant(a.affectationId()) || !identifiant(a.paiementId())
                || !bailId.equals(a.bailId()) || !debut.equals(a.periodeDebut()) || !fin.equals(a.periodeFin())
                || a.nature() == null || a.statut() == null || !"MAD".equals(a.devise())
                || a.montant() == null || a.montant().signum() < 0) throw incoherence();
        try { if (a.montant().setScale(2, RoundingMode.UNNECESSARY).precision() > 12) throw incoherence(); }
        catch (ArithmeticException ex) { throw incoherence(); }
    }
    private static boolean identifiant(String valeur) { return valeur != null && !valeur.isBlank() && valeur.length() <= 100; }
    private static ServiceExterneException incoherence() {
        return new ServiceExterneException("PAIEMENTS", "REPONSE_INCOHERENTE",
                "Les données Paiements ne permettent pas une comparaison fiable.", null);
    }
}
