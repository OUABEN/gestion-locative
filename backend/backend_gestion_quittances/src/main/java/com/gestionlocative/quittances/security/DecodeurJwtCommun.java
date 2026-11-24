package com.gestionlocative.quittances.security;

import java.text.ParseException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

public final class DecodeurJwtCommun implements JwtDecoder {

    private static final Set<String> ROLES_RECONNUS = Set.of(
            "BAILLEUR",
            "GESTIONNAIRE",
            "LOCATAIRE"
    );

    private final NimbusJwtDecoder delegate;

    public DecodeurJwtCommun(NimbusJwtDecoder delegate) {
        this.delegate = delegate;
    }

    @Override
    public Jwt decode(String token) throws BadJwtException {
        SignedJWT jwt;
        try {
            jwt = SignedJWT.parse(token);
        } catch (ParseException exception) {
            throw new BadJwtException("Jeton JWT commun malformé.", exception);
        }

        if (!JWSAlgorithm.HS256.equals(jwt.getHeader().getAlgorithm())) {
            throw new BadJwtException(
                    "Le mode JWT COMMUN accepte exclusivement HS256."
            );
        }

        Jwt verifie = delegate.decode(token);
        JWTClaimsSet claimsVerifiees;
        try {
            claimsVerifiees = jwt.getJWTClaimsSet();
        } catch (ParseException exception) {
            throw new BadJwtException(
                    "Les claims du jeton commun sont invalides.",
                    exception
            );
        }

        String sub = claimsVerifiees.getSubject();
        Object role = claimsVerifiees.getClaim("role");
        Number iat = claimsVerifiees.getIssueTime() == null
                ? null
                : claimsVerifiees.getIssueTime().getTime() / 1000;
        Number exp = claimsVerifiees.getExpirationTime() == null
                ? null
                : claimsVerifiees.getExpirationTime().getTime() / 1000;

        if (sub == null
                || sub.isBlank()
                || !(role instanceof String roleTexte)
                || !ROLES_RECONNUS.contains(roleTexte)
                || iat == null
                || exp == null
                || exp.longValue() <= iat.longValue()) {
            throw new BadJwtException(
                    "Le jeton commun ne contient pas les claims obligatoires."
            );
        }

        Map<String, Object> claims = new HashMap<>(verifie.getClaims());

        claims.remove("bailleur_id");
        claims.remove("locataire_id");

        if ("BAILLEUR".equals(role)) {
            claims.put("bailleur_id", verifie.getSubject());
        }
        return new Jwt(
                verifie.getTokenValue(),
                verifie.getIssuedAt(),
                verifie.getExpiresAt(),
                verifie.getHeaders(),
                claims
        );
    }
}
