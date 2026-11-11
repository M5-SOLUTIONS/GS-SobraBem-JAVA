package br.com.sobrabem.controller;

import br.com.sobrabem.dto.request.CategoriaRequest;
import br.com.sobrabem.dto.response.CategoriaResponse;
import br.com.sobrabem.service.CategoriaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/categorias")
@Tag(name = "Categorias")
public class CategoriaController {

    private final CategoriaService service;

    public CategoriaController(CategoriaService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "Lista as categorias (sem paginação, por nome)")
    @ApiResponse(responseCode = "200", description = "Lista de categorias")
    @ApiResponse(responseCode = "401", description = "NAO_AUTENTICADO")
    public List<CategoriaResponse> listar() {
        return service.listar();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Detalhe da categoria")
    @ApiResponse(responseCode = "200", description = "Categoria")
    @ApiResponse(responseCode = "404", description = "NAO_ENCONTRADO")
    public CategoriaResponse buscar(@PathVariable Long id) {
        return service.buscar(id);
    }

    @PostMapping
    @Operation(summary = "Cria categoria (ADMIN)")
    @ApiResponse(responseCode = "201", description = "Categoria criada")
    @ApiResponse(responseCode = "400", description = "VALIDACAO")
    @ApiResponse(responseCode = "403", description = "ACESSO_NEGADO")
    @ApiResponse(responseCode = "409", description = "CATEGORIA_DUPLICADA")
    public ResponseEntity<CategoriaResponse> criar(@RequestBody @Valid CategoriaRequest req) {
        CategoriaResponse criada = service.criar(req);
        return ResponseEntity.created(URI.create("/api/categorias/" + criada.id())).body(criada);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Edita categoria (ADMIN)")
    @ApiResponse(responseCode = "200", description = "Categoria atualizada")
    @ApiResponse(responseCode = "400", description = "VALIDACAO")
    @ApiResponse(responseCode = "403", description = "ACESSO_NEGADO")
    @ApiResponse(responseCode = "404", description = "NAO_ENCONTRADO")
    @ApiResponse(responseCode = "409", description = "CATEGORIA_DUPLICADA")
    public CategoriaResponse atualizar(@PathVariable Long id, @RequestBody @Valid CategoriaRequest req) {
        return service.atualizar(id, req);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Exclui categoria sem doações (ADMIN)")
    @ApiResponse(responseCode = "204", description = "Excluída")
    @ApiResponse(responseCode = "403", description = "ACESSO_NEGADO")
    @ApiResponse(responseCode = "404", description = "NAO_ENCONTRADO")
    @ApiResponse(responseCode = "409", description = "REGISTRO_EM_USO")
    public ResponseEntity<Void> excluir(@PathVariable Long id) {
        service.excluir(id);
        return ResponseEntity.noContent().build();
    }
}