package br.com.sobrabem.dto.response;

import br.com.sobrabem.domain.entity.Usuario;

public record DoadorResumo(Long id, String nome, String telefone, String endereco, String cidade, String uf) {

    public static DoadorResumo de(Usuario u) {
        return new DoadorResumo(u.getId(), u.getNome(), u.getTelefone(), u.getEndereco(), u.getCidade(), u.getUf());
    }
}