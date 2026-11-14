package com.gestionlocative.loyers.exception;

/** Un microservice dont on dépend (Locataires) ne répond pas correctement. */
public class ServiceIndisponibleException extends RuntimeException {
    public ServiceIndisponibleException(String message, Throwable cause) {
        super(message, cause);
    }
}
