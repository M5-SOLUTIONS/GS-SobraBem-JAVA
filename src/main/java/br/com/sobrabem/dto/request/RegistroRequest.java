package br.com.sobrabem.dto.request;

import br.com.sobrabem.domain.enums.Papel;
import br.com.sobrabem.domain.enums.TipoPerfil;
import jakarta.validation.constraints.*;

public record RegistroRequest(
        @NotBlank @Size(min = 3, max = 120) String nome,
        @NotBlank @Email @Size(max = 150) String email,
        @NotBlank @Size(min = 8, max = 60)
        @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).+$", message = "deve ter ao menos 1 letra e 1 número")
        String senha,
        @Pattern(regexp = "^\\d{10,11}$", message = "deve ter só dígitos, 10 ou 11 (com DDD)") String telefone,
        @NotNull Papel papel,
        @NotNull TipoPerfil tipoPerfil,
        @Size(max = 200) String endereco,
        @NotBlank @Size(max = 80) String cidade,
        @NotNull @Pattern(regexp = "^[A-Z]{2}$", message = "deve ter 2 letras maiúsculas") String uf
) {
}