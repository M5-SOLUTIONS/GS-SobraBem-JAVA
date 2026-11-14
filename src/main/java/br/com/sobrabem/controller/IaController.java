package br.com.sobrabem.controller;

import br.com.sobrabem.dto.request.OrientacaoRequest;
import br.com.sobrabem.dto.response.OrientacaoResponse;
import br.com.sobrabem.service.IaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ia")
@Tag(name = "IA")
public class IaController {

    private final IaService service;

    public IaController(IaService service) {
        this.service = service;
    }

    @PostMapping("/orientacoes")
    @Operation(summary = "Sugere orientação de conservação com base no acervo (DOADOR). Não grava nada.")
    @ApiResponse(responseCode = "200", description = "Orientação gerada, ou aviso de que não há base no acervo")
    @ApiResponse(responseCode = "400", description = "VALIDACAO")
    @ApiResponse(responseCode = "403", description = "ACESSO_NEGADO")
    @ApiResponse(responseCode = "404", description = "NAO_ENCONTRADO (categoria)")
    @ApiResponse(responseCode = "503", description = "IA_INDISPONIVEL (erro, cota ou mais de 20 s)")
    public OrientacaoResponse gerar(@RequestBody @Valid OrientacaoRequest req) {
        return service.gerar(req);
    }
}