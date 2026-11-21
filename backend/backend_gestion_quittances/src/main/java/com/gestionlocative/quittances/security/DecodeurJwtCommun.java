package com.gestionlocative.quittances.security;

import java.text.ParseException;
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
        try {
            SignedJWT jwt = SignedJWT.parse(token);

            if (!JWSAlgorithm.HS256.equals(jwt.getHeader().getAlgorithm())) {
                throw new BadJwtException(
                        "Le mode JWT COMMUN accepte exclusivement HS256."
                );
            }

            JWTClaimsSet claims = jwt.getJWTClaimsSet();
            String sub = claims.getSubject();
            Object role = claims.getClaim("role");
            Number iat = claims.getIssueTime() == null
                    ? null
                    : claims.getIssueTime().getTime() / 1000;
            Number exp = claims.getExpirationTime() == null
                    ? null
                    : claims.getExpirationTime().getTime() / 1000;

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

            return delegate.decode(token);
        } catch (ParseException exception) {
            throw new BadJwtException("Jeton JWT commun malformé.", exception);
        }
    }
}
