package com.gestionlocative.quittances.mapper;

import com.gestionlocative.quittances.dto.QuittanceConsultationResponse;
import com.gestionlocative.quittances.entity.Quittance;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class QuittanceConsultationMapper {

    public QuittanceConsultationResponse convertir(Quittance quittance) {
        Map<String, String> donnees = quittance.getDonneesFigees();

        return new QuittanceConsultationResponse(
                quittance.getId(),
                quittance.getNumero(),
                quittance.getBailId(),
                quittance.getLocataireId(),
                quittance.getBienId(),
                donnees.get("bailleurNom"),
                donnees.get("locataireNom"),
                donnees.get("locatairePrenom"),
                donnees.get("bienAdresse"),
                quittance.getPeriodeDebut(),
                quittance.getPeriodeFin(),
                quittance.getLoyerDu(),
                quittance.getChargesDues(),
                quittance.getLoyerPaye(),
                quittance.getChargesPayees(),
                quittance.getTotalPaye(),
                quittance.getDevise(),
                quittance.getTypeDocument().name(),
                quittance.getStatut().name(),
                quittance.getStatutSignature().name(),
                quittance.getCreeLe(),
                quittance.getEmisLe(), quittance.getDocumentPrecedentId(), quittance.getMotifRemplacement()
        );
    }
}