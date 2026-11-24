package com.gestionlocative.loyers.service;

import com.gestionlocative.loyers.client.LocataireDto;
import com.gestionlocative.loyers.client.LocatairesClient;
import com.gestionlocative.loyers.model.Bail;
import com.gestionlocative.loyers.repository.BailRepository;
import com.gestionlocative.loyers.security.AuthenticatedUser;
import com.gestionlocative.loyers.security.CurrentUser;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

/** Règle : BAILLEUR et GESTIONNAIRE voient tout ; LOCATAIRE ne voit que ses propres baux, échéances et paiements. */
@Service
public class AccesService {

    private final BailRepository bailRepository;
    private final LocatairesClient locatairesClient;

    public AccesService(BailRepository bailRepository, LocatairesClient locatairesClient) {
        this.bailRepository = bailRepository;
        this.locatairesClient = locatairesClient;
    }

    public boolean estLocataire() {
        return CurrentUser.get().map(AuthenticatedUser::role).map("LOCATAIRE"::equals).orElse(false);
    }

    /** Identifiants des baux du locataire connecté. */
    public Set<UUID> bauxAccessibles() {
        UUID locataireId = locatairesClient.moi()
                .map(LocataireDto::id)
                .orElseThrow(() -> new AccessDeniedException("Aucune fiche locataire n'est associée à ce compte"));
        return bailRepository.findByLocataireIdOrderByDateDebutDesc(locataireId).stream()
                .map(Bail::getId)
                .collect(Collectors.toSet());
    }

    /** Refuse (403) l'accès d'un locataire à un bail qui n'est pas le sien ; sans effet pour le personnel. */
    public void verifierBail(UUID bailId) {
        if (estLocataire() && !bauxAccessibles().contains(bailId)) {
            throw new AccessDeniedException("Accès refusé");
        }
    }
}
