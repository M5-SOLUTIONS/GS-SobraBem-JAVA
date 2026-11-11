package br.com.sobrabem.dto.response;

import br.com.sobrabem.domain.enums.Papel;

import java.time.Instant;

public record LoginResponse(String token, String tipo, Instant expiraEm, UsuarioResumo usuario) {

    public record UsuarioResumo(Long id, String nome, String email, Papel papel) {
    }
}