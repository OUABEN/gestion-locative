package com.gestionlocative.loyers.client;

import java.util.Optional;
import java.util.UUID;

/** Appels vers le service Biens (port 8081), avec le JWT de l'utilisateur. */
public interface BiensClient {

    /** Empty si le bien n'existe pas (404). */
    Optional<DisponibiliteBien> disponibilite(UUID bienId);

    /**
     * PATCH /api/biens/{id}/statut avec "OCCUPE" ou "LIBRE".
     * @return false si le bien était déjà dans ce statut (409), true si le statut a changé
     */
    boolean changerStatut(UUID bienId, String statut);
}
