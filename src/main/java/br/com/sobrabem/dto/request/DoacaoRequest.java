package br.com.sobrabem.dto.request;

import br.com.sobrabem.domain.enums.Unidade;
import jakarta.validation.constraints.*;

import java.time.OffsetDateTime;

public record DoacaoRequest(
        @NotBlank @Size(min = 3, max = 100) String titulo,
        @Size(max = 500) String descricao,
        @NotNull Long categoriaId,
        @NotNull @Min(1) @Max(9999) Integer quantidadeTotal,
        @NotNull Unidade unidade,
        @NotNull OffsetDateTime prazoRetirada,
        @Size(max = 1000) String orientacoesConservacao,
        Boolean geradaPorIa
) {
}