package br.com.sobrabem.controller;

import br.com.sobrabem.dto.request.LoginRequest;
import br.com.sobrabem.dto.request.RegistroRequest;
import br.com.sobrabem.dto.response.LoginResponse;
import br.com.sobrabem.dto.response.UsuarioResponse;
import br.com.sobrabem.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Autenticação")
@SecurityRequirements // rotas públicas: sem cadeado no Swagger
public class AuthController {

    private final AuthService service;

    public AuthController(AuthService service) {
        this.service = service;
    }

    @PostMapping("/registro")
    @Operation(summary = "Cadastra doador ou receptor")
    @ApiResponse(responseCode = "201", description = "Usuário criado")
    @ApiResponse(responseCode = "400", description = "VALIDACAO")
    @ApiResponse(responseCode = "409", description = "EMAIL_EM_USO")
    public ResponseEntity<UsuarioResponse> registrar(@RequestBody @Valid RegistroRequest req) {
        UsuarioResponse criado = service.registrar(req);
        return ResponseEntity.created(URI.create("/api/usuarios/" + criado.id())).body(criado);
    }

    @PostMapping("/login")
    @Operation(summary = "Login; devolve o JWT (validade de 2 h)")
    @ApiResponse(responseCode = "200", description = "Token emitido")
    @ApiResponse(responseCode = "400", description = "VALIDACAO")
    @ApiResponse(responseCode = "401", description = "CREDENCIAIS_INVALIDAS")
    public LoginResponse login(@RequestBody @Valid LoginRequest req) {
        return service.login(req);
    }
}