package br.com.sobrabem.dto.request;

import jakarta.validation.constraints.NotBlank;

public record RetiradaRequest(@NotBlank String codigoRetirada) {
}