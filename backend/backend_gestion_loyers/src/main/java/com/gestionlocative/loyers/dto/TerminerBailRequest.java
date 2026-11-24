package com.gestionlocative.loyers.dto;

import java.time.LocalDate;

/** Date de fin du bail ; si absente, la date du jour est utilisée. */
public record TerminerBailRequest(LocalDate dateFin) {
}
