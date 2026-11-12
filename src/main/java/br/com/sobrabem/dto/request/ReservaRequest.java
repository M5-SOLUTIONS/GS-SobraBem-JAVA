package br.com.sobrabem.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record ReservaRequest(
        @NotNull Long doacaoId,
        @NotNull @Min(1) @Max(9999) Integer quantidade
) {
}