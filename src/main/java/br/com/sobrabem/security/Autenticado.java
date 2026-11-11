package br.com.sobrabem.security;

import br.com.sobrabem.domain.enums.Papel;
import org.springframework.security.oauth2.jwt.Jwt;

public record Autenticado(Long id, Papel papel) {

    public static Autenticado de(Jwt jwt) {
        return new Autenticado(Long.valueOf(jwt.getSubject()), Papel.valueOf(jwt.getClaimAsString("papel")));
    }

    public boolean isAdmin() {
        return papel == Papel.ADMIN;
    }
}