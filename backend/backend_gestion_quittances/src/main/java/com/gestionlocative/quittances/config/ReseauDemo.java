package com.gestionlocative.quittances.config;

import java.net.URI;
import java.util.Set;

/** Mode Docker local explicite : les ports doivent être publiés sur 127.0.0.1. */
public final class ReseauDemo {
    private ReseauDemo() { }
    public static boolean ecouteAutorisee(String adresse, boolean dockerDemo) {
        return "127.0.0.1".equals(adresse)
                || (dockerDemo && "0.0.0.0".equals(adresse));
    }
    public static boolean sourceAutorisee(URI uri, boolean dockerDemo) {
        String hote = uri.getHost();
        if (hote == null) return false;
        if (Set.of("127.0.0.1", "localhost", "::1", "[::1]").contains(hote)) return true;
        return dockerDemo && "services-demo".equals(hote)
                && "http".equals(uri.getScheme()) && uri.getPort() == 8091;
    }
}
