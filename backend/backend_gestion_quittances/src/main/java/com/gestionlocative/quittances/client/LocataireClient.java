package com.gestionlocative.quittances.client;

import com.gestionlocative.quittances.dto.ReferentielsExternes.Locataire;

public interface LocataireClient {

    Locataire consulter(String id);

    default String identifierConnecte() {
        throw new org.springframework.security.access.AccessDeniedException(
                "Resolution du locataire connecte indisponible.");
    }
}