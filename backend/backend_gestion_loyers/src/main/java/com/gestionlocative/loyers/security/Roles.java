package com.gestionlocative.loyers.security;

/** Expressions @PreAuthorize réutilisables. */
public final class Roles {
    private Roles() {
    }

    /** Écriture et lecture complète : bailleur et gestionnaire. */
    public static final String STAFF = "hasAnyRole('BAILLEUR','GESTIONNAIRE')";

    /** Lecture : le locataire ne voit que ses propres données (contrôlé par AccesService). */
    public static final String TOUS = "hasAnyRole('BAILLEUR','GESTIONNAIRE','LOCATAIRE')";
}
