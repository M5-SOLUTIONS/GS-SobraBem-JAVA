package br.com.sobrabem.dto.response;

import br.com.sobrabem.domain.entity.Doacao;
import br.com.sobrabem.domain.enums.StatusDoacao;
import br.com.sobrabem.domain.enums.Unidade;

import java.time.Instant;

public record DoacaoResponse(
        Long id, String titulo, String descricao, CategoriaResumo categoria, DoadorResumo doador,
        Integer quantidadeTotal, Integer quantidadeDisponivel, Unidade unidade, Instant prazoRetirada,
        StatusDoacao status, String orientacoesConservacao, boolean geradaPorIa, Instant criadoEm) {

    public record CategoriaResumo(Long id, String nome) {
    }

    public static DoacaoResponse de(Doacao d) {
        return new DoacaoResponse(
                d.getId(), d.getTitulo(), d.getDescricao(),
                new CategoriaResumo(d.getCategoria().getId(), d.getCategoria().getNome()),
                DoadorResumo.de(d.getDoador()),
                d.getQuantidadeTotal(), d.getQuantidadeDisponivel(), d.getUnidade(), d.getPrazoRetirada(),
                d.getStatus(), d.getOrientacoesConservacao(), d.isGeradaPorIa(), d.getCriadoEm());
    }
}