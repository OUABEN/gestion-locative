package com.gestionlocative.quittances.document;

public record PdfStocke(
        String reference,
        String sha256,
        long tailleOctets
) {
}