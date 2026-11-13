package br.com.sobrabem.controller;

import br.com.sobrabem.domain.enums.StatusDoacao;
import br.com.sobrabem.domain.enums.StatusReserva;
import br.com.sobrabem.dto.request.DoacaoRequest;
import br.com.sobrabem.dto.response.DoacaoResponse;
import br.com.sobrabem.dto.response.ReservaResponse;
import br.com.sobrabem.security.Autenticado;
import br.com.sobrabem.service.DoacaoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.PagedModel;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
@RequestMapping("/api/doacoes")
@Tag(name = "Doações")
public class DoacaoController {

    private final DoacaoService service;

    public DoacaoController(DoacaoService service) {
        this.service = service;
    }

    @PostMapping
    @Operation(summary = "Publica uma doação (DOADOR)")
    @ApiResponse(responseCode = "201", description = "Doação publicada")
    @ApiResponse(responseCode = "400", description = "VALIDACAO ou PRAZO_INVALIDO")
    @ApiResponse(responseCode = "403", description = "ACESSO_NEGADO")
    public ResponseEntity<DoacaoResponse> publicar(@AuthenticationPrincipal Jwt jwt,
                                                   @RequestBody @Valid DoacaoRequest req) {
        DoacaoResponse criada = service.publicar(Autenticado.de(jwt).id(), req);
        return ResponseEntity.created(URI.create("/api/doacoes/" + criada.id())).body(criada);
    }

    @GetMapping
    @Operation(summary = "Vitrine: doações ATIVA com prazo no futuro")
    @ApiResponse(responseCode = "200", description = "Página de doações")
    @ApiResponse(responseCode = "401", description = "NAO_AUTENTICADO")
    public PagedModel<DoacaoResponse> vitrine(
            @RequestParam(required = false) Long categoriaId,
            @RequestParam(required = false) String cidade,
            @ParameterObject @PageableDefault(size = 10, sort = "prazoRetirada") Pageable pageable) {
        return new PagedModel<>(service.vitrine(categoriaId, cidade, pageable));
    }

    @GetMapping("/{id}/reservas")
    @Operation(summary = "Reservas de uma doação (DOADOR dono)")
    @ApiResponse(responseCode = "200", description = "Página de reservas")
    @ApiResponse(responseCode = "403", description = "ACESSO_NEGADO")
    @ApiResponse(responseCode = "404", description = "NAO_ENCONTRADO")
    public PagedModel<ReservaResponse> reservas(
            @AuthenticationPrincipal Jwt jwt, @PathVariable Long id,
            @RequestParam(required = false) StatusReserva status,
            @ParameterObject @PageableDefault(size = 10, sort = "criadoEm", direction = Sort.Direction.DESC)
            Pageable pageable) {
        return new PagedModel<>(service.reservasDaDoacao(Autenticado.de(jwt).id(), id, status, pageable));
    }

    @GetMapping("/minhas")
    @Operation(summary = "Doações do doador logado (DOADOR)")
    @ApiResponse(responseCode = "200", description = "Página de doações")
    @ApiResponse(responseCode = "403", description = "ACESSO_NEGADO")
    public PagedModel<DoacaoResponse> minhas(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) StatusDoacao status,
            @ParameterObject @PageableDefault(size = 10, sort = "prazoRetirada") Pageable pageable) {
        return new PagedModel<>(service.minhas(Autenticado.de(jwt).id(), status, pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Detalhe da doação")
    @ApiResponse(responseCode = "200", description = "Doação")
    @ApiResponse(responseCode = "404", description = "NAO_ENCONTRADO")
    public DoacaoResponse buscar(@PathVariable Long id) {
        return service.buscar(id);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Edita a própria doação ATIVA ou ESGOTADA (DOADOR dono)")
    @ApiResponse(responseCode = "200", description = "Doação atualizada")
    @ApiResponse(responseCode = "400", description = "VALIDACAO ou PRAZO_INVALIDO")
    @ApiResponse(responseCode = "403", description = "ACESSO_NEGADO")
    @ApiResponse(responseCode = "404", description = "NAO_ENCONTRADO")
    @ApiResponse(responseCode = "409", description = "ESTADO_INVALIDO")
    public DoacaoResponse atualizar(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id,
                                    @RequestBody @Valid DoacaoRequest req) {
        return service.atualizar(Autenticado.de(jwt).id(), id, req);
    }

    @PatchMapping("/{id}/cancelamento")
    @Operation(summary = "Cancela a doação e as reservas ativas dela (DOADOR dono)")
    @ApiResponse(responseCode = "200", description = "Doação cancelada")
    @ApiResponse(responseCode = "403", description = "ACESSO_NEGADO")
    @ApiResponse(responseCode = "404", description = "NAO_ENCONTRADO")
    @ApiResponse(responseCode = "409", description = "ESTADO_INVALIDO")
    public DoacaoResponse cancelar(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        return service.cancelar(Autenticado.de(jwt).id(), id);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Exclui doação sem reservas (DOADOR dono ou ADMIN)")
    @ApiResponse(responseCode = "204", description = "Excluída")
    @ApiResponse(responseCode = "403", description = "ACESSO_NEGADO")
    @ApiResponse(responseCode = "404", description = "NAO_ENCONTRADO")
    @ApiResponse(responseCode = "409", description = "REGISTRO_EM_USO")
    public ResponseEntity<Void> excluir(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        service.excluir(Autenticado.de(jwt), id);
        return ResponseEntity.noContent().build();
    }
}