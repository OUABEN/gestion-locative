package com.gestionlocative.quittances.client;

import com.gestionlocative.quittances.dto.ReferentielsExternes.Locataire;

public interface LocataireClient {

    Locataire consulter(String id);
}