package br.com.sobrabem.dto.response;

import br.com.sobrabem.domain.entity.Usuario;
import br.com.sobrabem.domain.enums.Papel;
import br.com.sobrabem.domain.enums.TipoPerfil;

import java.time.Instant;

public record UsuarioResponse(
        Long id, String nome, String email, String telefone, Papel papel, TipoPerfil tipoPerfil,
        String endereco, String cidade, String uf, boolean ativo, Instant criadoEm) {

    public static UsuarioResponse de(Usuario u) {
        return new UsuarioResponse(u.getId(), u.getNome(), u.getEmail(), u.getTelefone(), u.getPapel(),
                u.getTipoPerfil(), u.getEndereco(), u.getCidade(), u.getUf(), u.isAtivo(), u.getCriadoEm());
    }
}