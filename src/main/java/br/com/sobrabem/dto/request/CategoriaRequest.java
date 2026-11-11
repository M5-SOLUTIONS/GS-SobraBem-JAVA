package br.com.sobrabem.dto.request;

import jakarta.validation.constraints.*;

public record CategoriaRequest(
        @NotBlank @Size(min = 3, max = 60) String nome,
        @Size(max = 255) String descricao,
        @NotNull @Min(1) @Max(48) Integer prazoMaxHoras
) {
}