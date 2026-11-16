package br.com.sobrabem.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record OrientacaoRequest(
        @NotBlank @Size(min = 3, max = 100) String titulo,
        @Size(max = 500) String descricao,
        @NotNull Long categoriaId
) {
}