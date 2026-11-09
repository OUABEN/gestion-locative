package com.gestionlocative.loyers.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Lie le bloc "loyers" de application.yml. */
@ConfigurationProperties(prefix = "loyers")
public record LoyersProperties(int jourEcheance, int joursAvantImpaye, Services services) {

    public record Services(String locatairesUrl, String biensUrl) {
    }
}
