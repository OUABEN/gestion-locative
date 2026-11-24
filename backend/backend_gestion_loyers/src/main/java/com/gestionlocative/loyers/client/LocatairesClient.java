package com.gestionlocative.loyers.client;

import java.util.Optional;
import java.util.UUID;

/** Appels vers le service Locataires (port 8082), avec le JWT de l'utilisateur. */
public interface LocatairesClient {

    /** GET /api/locataires/{id} ; empty si le locataire n'existe pas. */
    Optional<LocataireDto> trouver(UUID locataireId);

    /** GET /api/locataires/me : fiche du locataire connecté (rôle LOCATAIRE) ; empty s'il n'en a pas. */
    Optional<LocataireDto> moi();
}
