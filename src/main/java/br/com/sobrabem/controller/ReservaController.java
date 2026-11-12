package br.com.sobrabem.controller;

import br.com.sobrabem.domain.enums.StatusReserva;
import br.com.sobrabem.dto.request.ReservaRequest;
import br.com.sobrabem.dto.request.RetiradaRequest;
import br.com.sobrabem.dto.response.ReservaResponse;
import br.com.sobrabem.security.Autenticado;
import br.com.sobrabem.service.ReservaService;
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
@RequestMapping("/api/reservas")
@Tag(name = "Reservas")
public class ReservaController {

    private final ReservaService service;

    public ReservaController(ReservaService service) {
        this.service = service;
    }

    @PostMapping
    @Operation(summary = "Reserva parte de uma doação (RECEPTOR). Aceita e ignora Idempotency-Key.")
    @ApiResponse(responseCode = "201", description = "Reserva criada, com codigoRetirada")
    @ApiResponse(responseCode = "400", description = "VALIDACAO")
    @ApiResponse(responseCode = "403", description = "ACESSO_NEGADO")
    @ApiResponse(responseCode = "404", description = "NAO_ENCONTRADO")
    @ApiResponse(responseCode = "409", description = "SALDO_INSUFICIENTE, DOACAO_INDISPONIVEL ou RESERVA_DUPLICADA")
    public ResponseEntity<ReservaResponse> reservar(@AuthenticationPrincipal Jwt jwt,
                                                    @RequestBody @Valid ReservaRequest req) {
        ReservaResponse criada = service.reservar(Autenticado.de(jwt).id(), req);
        return ResponseEntity.created(URI.create("/api/reservas/" + criada.id())).body(criada);
    }

    @GetMapping("/minhas")
    @Operation(summary = "Reservas do receptor logado (RECEPTOR)")
    @ApiResponse(responseCode = "200", description = "Página de reservas")
    @ApiResponse(responseCode = "403", description = "ACESSO_NEGADO")
    public PagedModel<ReservaResponse> minhas(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) StatusReserva status,
            @ParameterObject @PageableDefault(size = 10, sort = "criadoEm", direction = Sort.Direction.DESC)
            Pageable pageable) {
        return new PagedModel<>(service.minhas(Autenticado.de(jwt).id(), status, pageable));
    }

    @PatchMapping("/{id}/cancelamento")
    @Operation(summary = "Cancela a própria reserva ATIVA antes do prazo (RECEPTOR dono)")
    @ApiResponse(responseCode = "200", description = "Reserva cancelada (repetir devolve o mesmo estado)")
    @ApiResponse(responseCode = "403", description = "ACESSO_NEGADO")
    @ApiResponse(responseCode = "404", description = "NAO_ENCONTRADO")
    @ApiResponse(responseCode = "409", description = "ESTADO_INVALIDO")
    public ReservaResponse cancelar(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        return service.cancelar(Autenticado.de(jwt).id(), id);
    }

    @PatchMapping("/{id}/retirada")
    @Operation(summary = "Confirma a retirada com o código (DOADOR da doação)")
    @ApiResponse(responseCode = "200", description = "Reserva RETIRADA")
    @ApiResponse(responseCode = "400", description = "CODIGO_RETIRADA_INVALIDO ou VALIDACAO")
    @ApiResponse(responseCode = "403", description = "ACESSO_NEGADO")
    @ApiResponse(responseCode = "404", description = "NAO_ENCONTRADO")
    @ApiResponse(responseCode = "409", description = "ESTADO_INVALIDO")
    public ReservaResponse retirada(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id,
                                    @RequestBody @Valid RetiradaRequest req) {
        return service.confirmarRetirada(Autenticado.de(jwt).id(), id, req);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Exclui uma reserva (ADMIN)")
    @ApiResponse(responseCode = "204", description = "Excluída")
    @ApiResponse(responseCode = "403", description = "ACESSO_NEGADO")
    @ApiResponse(responseCode = "404", description = "NAO_ENCONTRADO")
    public ResponseEntity<Void> excluir(@PathVariable Long id) {
        service.excluir(id);
        return ResponseEntity.noContent().build();
    }
}