package br.com.sobrabem.dto.response;

import br.com.sobrabem.domain.entity.Categoria;

public record CategoriaResponse(Long id, String nome, String descricao, Integer prazoMaxHoras) {

    public static CategoriaResponse de(Categoria c) {
        return new CategoriaResponse(c.getId(), c.getNome(), c.getDescricao(), c.getPrazoMaxHoras());
    }
}