package br.com.sobrabem.dto.response;

import br.com.sobrabem.domain.entity.Usuario;
import br.com.sobrabem.domain.enums.Papel;

import java.time.Instant;

/** Nunca leva a senha nem o hash. */
public record UsuarioResponse(
        Long id, String nome, String email, String telefone, Papel papel,
        String endereco, String cidade, String uf, boolean ativo, Instant criadoEm) {

    public static UsuarioResponse de(Usuario u) {
        return new UsuarioResponse(u.getId(), u.getNome(), u.getEmail(), u.getTelefone(), u.getPapel(),
                u.getEndereco(), u.getCidade(), u.getUf(), u.isAtivo(), u.getCriadoEm());
    }
}