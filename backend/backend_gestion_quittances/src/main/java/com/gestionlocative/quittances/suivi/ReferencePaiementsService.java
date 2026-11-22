package com.gestionlocative.quittances.suivi;

import com.gestionlocative.quittances.dto.AffectationQuittanceResponse;
import com.gestionlocative.quittances.dto.QuittanceConsultationResponse;
import com.gestionlocative.quittances.security.LecteurQuittances;
import com.gestionlocative.quittances.service.ConsultationQuittanceService;
import com.gestionlocative.quittances.service.ConsultationAffectationService;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.UUID;

@Service
public class ReferencePaiementsService {
    private final ConsultationQuittanceService documents;
    private final ConsultationAffectationService affectations;
    public ReferencePaiementsService(ConsultationQuittanceService documents, ConsultationAffectationService affectations) {
        this.documents = documents; this.affectations = affectations;
    }
    public record Reference(QuittanceConsultationResponse document, List<AffectationQuittanceResponse> affectations) {}
    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public Reference lire(Authentication auth, UUID id) {
        if (!LecteurQuittances.depuis(auth).bailleur()) throw new AccessDeniedException("Contrôle réservé au bailleur.");
        var document = documents.consulter(auth, id);
        return new Reference(document, List.copyOf(affectations.consulter(auth, id)));
    }
}
