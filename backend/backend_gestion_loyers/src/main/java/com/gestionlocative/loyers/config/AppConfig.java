package com.gestionlocative.loyers.config;

import java.time.Clock;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
@EnableConfigurationProperties(LoyersProperties.class)
public class AppConfig {

    /** Horloge injectable : permet de figer la date dans les tests unitaires. */
    @Bean
    Clock clock() {
        return Clock.systemDefaultZone();
    }
}
