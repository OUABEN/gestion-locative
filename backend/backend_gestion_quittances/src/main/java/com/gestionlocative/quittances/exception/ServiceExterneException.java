package com.gestionlocative.quittances.exception;

public class ServiceExterneException extends RuntimeException {

    private final String service;
    private final String code;
    private final Integer statutHttp;

    public ServiceExterneException(
            String service,
            String code,
            String message,
            Integer statutHttp) {

        super(message);
        this.service = service;
        this.code = code;
        this.statutHttp = statutHttp;
    }

    public String getService() {
        return service;
    }

    public String getCode() {
        return code;
    }

    public Integer getStatutHttp() {
        return statutHttp;
    }
}