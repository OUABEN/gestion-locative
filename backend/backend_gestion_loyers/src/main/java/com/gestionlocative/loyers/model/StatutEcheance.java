package com.gestionlocative.loyers.model;

/** Doit correspondre à chk_echeances_statut dans 05_loyers_paiements.sql. */
public enum StatutEcheance {
    A_PAYER,
    PAYEE,
    EN_RETARD,
    IMPAYEE
}
