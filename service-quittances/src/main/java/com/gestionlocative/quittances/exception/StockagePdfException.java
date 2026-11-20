package com.gestionlocative.quittances.exception;

public class StockagePdfException extends RuntimeException {

    private final String code;

    public StockagePdfException(String code, String message) {
        super(message);
        this.code = code;
    }

    public StockagePdfException(
            String code,
            String message,
            Throwable cause
    ) {
        super(message, cause);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}