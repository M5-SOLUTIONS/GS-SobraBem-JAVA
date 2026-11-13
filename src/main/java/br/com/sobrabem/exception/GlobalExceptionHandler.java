package br.com.sobrabem.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.data.core.PropertyReferenceException;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import tools.jackson.core.JacksonException;

import java.net.URI;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(NegocioException.class)
    public ResponseEntity<ProblemDetail> negocio(NegocioException ex) {
        return erro(ex.getCodigo(), ex.getMessage(), ex.getErros());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> validacao(MethodArgumentNotValidException ex) {
        List<CampoErro> erros = ex.getBindingResult().getFieldErrors().stream()
                .map(f -> new CampoErro(f.getField(), f.getDefaultMessage()))
                .toList();
        return erro(CodigoErro.VALIDACAO, NegocioException.descreverQuantidade(erros.size()), erros);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ProblemDetail> jsonInvalido(HttpMessageNotReadableException ex) {
        if (ex.getCause() instanceof JacksonException je && !je.getPath().isEmpty()) {
            String campo = je.getPath().getLast().getPropertyName();
            if (campo != null) {
                return erro(CodigoErro.VALIDACAO, "1 campo inválido",
                        List.of(new CampoErro(campo, "valor inválido")));
            }
        }
        return erro(CodigoErro.VALIDACAO, "Corpo da requisição ausente ou com JSON inválido", List.of());
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ProblemDetail> parametroInvalido(MethodArgumentTypeMismatchException ex) {
        return erro(CodigoErro.VALIDACAO, "1 campo inválido",
                List.of(new CampoErro(ex.getName(), "valor inválido")));
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ProblemDetail> naoAutenticado(AuthenticationException ex) {
        return erro(CodigoErro.NAO_AUTENTICADO, "Token ausente, inválido ou expirado", List.of());
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ProblemDetail> acessoNegado(AccessDeniedException ex) {
        return erro(CodigoErro.ACESSO_NEGADO, "Seu papel não permite esta operação", List.of());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ProblemDetail> integridade(DataIntegrityViolationException ex) {
        log.warn("Violação de integridade: {}", ex.getMostSpecificCause().getMessage());
        return erro(CodigoErro.REGISTRO_EM_USO, "A operação viola uma restrição do banco (registro em uso)", List.of());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> inesperado(Exception ex) {
        if (ex instanceof ErrorResponse er && er.getStatusCode().is4xxClientError()) {
            CodigoErro codigo = er.getStatusCode().value() == 404 ? CodigoErro.NAO_ENCONTRADO : CodigoErro.VALIDACAO;
            return erro(codigo, er.getStatusCode(), er.getBody().getDetail(), List.of());
        }
        log.error("Erro inesperado", ex);
        return erro(CodigoErro.ERRO_INTERNO, "Erro inesperado no servidor", List.of());
    }

    /** ?sort=campoQueNaoExiste */
    @ExceptionHandler({PropertyReferenceException.class, InvalidDataAccessApiUsageException.class})
    public ResponseEntity<ProblemDetail> ordenacaoInvalida(RuntimeException ex) {
        log.warn("Parâmetro de consulta inválido: {}", ex.getMessage());
        return erro(CodigoErro.VALIDACAO, "1 campo inválido",
                List.of(new CampoErro("sort", "campo de ordenação inválido")));
    }

    private ResponseEntity<ProblemDetail> erro(CodigoErro codigo, String detalhe, List<CampoErro> erros) {
        return erro(codigo, codigo.getStatus(), detalhe, erros);
    }

    private ResponseEntity<ProblemDetail> erro(CodigoErro codigo, HttpStatusCode status, String detalhe,
                                               List<CampoErro> erros) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(status, detalhe);
        pd.setType(URI.create("https://sobrabem.app/erros/" + codigo.slug()));
        pd.setTitle(codigo.getTitulo());
        pd.setProperty("codigo", codigo.name());
        pd.setProperty("timestamp", Instant.now().truncatedTo(ChronoUnit.SECONDS));
        pd.setProperty("erros", erros);
        return ResponseEntity.status(status).body(pd);
    }
}