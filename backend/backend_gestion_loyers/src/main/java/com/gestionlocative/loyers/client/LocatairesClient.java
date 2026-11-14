package com.gestionlocative.loyers.client;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Accès aux baux, qui appartiennent au service Locataires (port 8082). */
public interface LocatairesClient {

    Optional<BailDto> trouverBail(UUID bailId);

    List<BailDto> listerBauxActifs();
}
