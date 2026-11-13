package br.com.sobrabem.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UsuarioUpdateRequest(
        @NotBlank @Size(min = 3, max = 120) String nome,
        @Pattern(regexp = "^\\d{10,11}$", message = "deve ter só dígitos, 10 ou 11 (com DDD)") String telefone,
        @Size(max = 200) String endereco,
        @NotBlank @Size(max = 80) String cidade,
        @NotNull @Pattern(regexp = "^[A-Z]{2}$", message = "deve ter 2 letras maiúsculas") String uf
) {
}