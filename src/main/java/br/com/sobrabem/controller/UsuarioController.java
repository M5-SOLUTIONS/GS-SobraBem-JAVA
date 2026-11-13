package br.com.sobrabem.controller;

import br.com.sobrabem.domain.enums.Papel;
import br.com.sobrabem.dto.request.UsuarioUpdateRequest;
import br.com.sobrabem.dto.response.UsuarioResponse;
import br.com.sobrabem.security.Autenticado;
import br.com.sobrabem.service.UsuarioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.PagedModel;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/usuarios")
@Tag(name = "Usuários")
public class UsuarioController {

    private final UsuarioService service;

    public UsuarioController(UsuarioService service) {
        this.service = service;
    }

    @GetMapping("/me")
    @Operation(summary = "Dados do usuário logado")
    @ApiResponse(responseCode = "200", description = "Usuário")
    @ApiResponse(responseCode = "401", description = "NAO_AUTENTICADO")
    public UsuarioResponse me(@AuthenticationPrincipal Jwt jwt) {
        return service.buscar(Autenticado.de(jwt).id());
    }

    @PutMapping("/me")
    @Operation(summary = "Atualiza o próprio perfil (nome, telefone, endereço, cidade, UF)")
    @ApiResponse(responseCode = "200", description = "Usuário atualizado")
    @ApiResponse(responseCode = "400", description = "VALIDACAO")
    public UsuarioResponse atualizarMe(@AuthenticationPrincipal Jwt jwt,
                                       @RequestBody @Valid UsuarioUpdateRequest req) {
        return service.atualizarMe(Autenticado.de(jwt).id(), req);
    }

    @GetMapping
    @Operation(summary = "Lista usuários, com filtro opcional por papel (ADMIN)")
    @ApiResponse(responseCode = "200", description = "Página de usuários")
    @ApiResponse(responseCode = "403", description = "ACESSO_NEGADO")
    public PagedModel<UsuarioResponse> listar(
            @RequestParam(required = false) Papel papel,
            @ParameterObject @PageableDefault(size = 10, sort = "nome") Pageable pageable) {
        return new PagedModel<>(service.listar(papel, pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Detalhe de um usuário (ADMIN)")
    @ApiResponse(responseCode = "200", description = "Usuário")
    @ApiResponse(responseCode = "403", description = "ACESSO_NEGADO")
    @ApiResponse(responseCode = "404", description = "NAO_ENCONTRADO")
    public UsuarioResponse buscar(@PathVariable Long id) {
        return service.buscar(id);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Exclui usuário sem doações nem reservas (ADMIN)")
    @ApiResponse(responseCode = "204", description = "Excluído")
    @ApiResponse(responseCode = "403", description = "ACESSO_NEGADO")
    @ApiResponse(responseCode = "404", description = "NAO_ENCONTRADO")
    @ApiResponse(responseCode = "409", description = "REGISTRO_EM_USO")
    public ResponseEntity<Void> excluir(@PathVariable Long id) {
        service.excluir(id);
        return ResponseEntity.noContent().build();
    }
}