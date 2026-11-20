package com.gestionlocative.loyers.exception;

/** Violation d'une règle métier (échéance déjà payée, surpaiement, doublon...). */
public class BusinessRuleException extends RuntimeException {
    public BusinessRuleException(String message) {
        super(message);
    }
}
