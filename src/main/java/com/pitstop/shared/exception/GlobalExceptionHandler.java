package com.pitstop.shared.exception;

import com.pitstop.usuario.exception.*;
import com.pitstop.cliente.exception.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import java.net.URI;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Handler global de exceções para toda a aplicação.
 *
 * Utiliza RFC 7807 (Problem Details for HTTP APIs) para padronizar
 * as respostas de erro.
 *
 * Suporta:
 * - Exceções de validação (Bean Validation)
 * - Exceções de negócio customizadas
 * - Exceções de segurança (Spring Security)
 * - Exceções genéricas
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // ========== EXCEÇÕES DO MÓDULO DE USUÁRIO ==========

    /**
     * Trata exceção quando um usuário não é encontrado.
     * HTTP 404 - Not Found
     */
    @ExceptionHandler(UsuarioNotFoundException.class)
    public ProblemDetail handleUsuarioNotFoundException(
            UsuarioNotFoundException ex,
            WebRequest request
    ) {
        log.warn("Usuário não encontrado: {}", ex.getMessage());

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.NOT_FOUND,
                ex.getMessage()
        );
        problemDetail.setTitle("Usuário Não Encontrado");
        problemDetail.setType(URI.create("https://pitstop.com/errors/usuario-not-found"));
        problemDetail.setProperty("timestamp", Instant.now());

        return problemDetail;
    }

    /**
     * Trata exceção quando tenta-se usar um email já existente.
     * HTTP 400 - Bad Request
     */
    @ExceptionHandler(EmailAlreadyExistsException.class)
    public ProblemDetail handleEmailAlreadyExistsException(
            EmailAlreadyExistsException ex,
            WebRequest request
    ) {
        log.warn("Email já existe: {}", ex.getMessage());

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                ex.getMessage()
        );
        problemDetail.setTitle("Email Duplicado");
        problemDetail.setType(URI.create("https://pitstop.com/errors/email-already-exists"));
        problemDetail.setProperty("timestamp", Instant.now());

        return problemDetail;
    }

    /**
     * Trata exceção quando tenta-se desativar o último admin.
     * HTTP 400 - Bad Request
     */
    @ExceptionHandler(CannotDeleteLastAdminException.class)
    public ProblemDetail handleCannotDeleteLastAdminException(
            CannotDeleteLastAdminException ex,
            WebRequest request
    ) {
        log.warn("Tentativa de deletar último admin: {}", ex.getMessage());

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                ex.getMessage()
        );
        problemDetail.setTitle("Operação Não Permitida");
        problemDetail.setType(URI.create("https://pitstop.com/errors/cannot-delete-last-admin"));
        problemDetail.setProperty("timestamp", Instant.now());

        return problemDetail;
    }

    /**
     * Trata exceção quando tenta-se operar com usuário inativo.
     * HTTP 403 - Forbidden
     */
    @ExceptionHandler(UsuarioInativoException.class)
    public ProblemDetail handleUsuarioInativoException(
            UsuarioInativoException ex,
            WebRequest request
    ) {
        log.warn("Usuário inativo: {}", ex.getMessage());

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.FORBIDDEN,
                ex.getMessage()
        );
        problemDetail.setTitle("Usuário Inativo");
        problemDetail.setType(URI.create("https://pitstop.com/errors/usuario-inativo"));
        problemDetail.setProperty("timestamp", Instant.now());

        return problemDetail;
    }

    /**
     * Trata exceção quando credenciais de login são inválidas.
     * HTTP 401 - Unauthorized
     */
    @ExceptionHandler(InvalidCredentialsException.class)
    public ProblemDetail handleInvalidCredentialsException(
            InvalidCredentialsException ex,
            WebRequest request
    ) {
        log.warn("Credenciais inválidas: {}", ex.getMessage());

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.UNAUTHORIZED,
                ex.getMessage()
        );
        problemDetail.setTitle("Credenciais Inválidas");
        problemDetail.setType(URI.create("https://pitstop.com/errors/invalid-credentials"));
        problemDetail.setProperty("timestamp", Instant.now());

        return problemDetail;
    }

    /**
     * Trata exceção genérica de validação de usuário.
     * HTTP 400 - Bad Request
     */
    @ExceptionHandler(UsuarioValidationException.class)
    public ProblemDetail handleUsuarioValidationException(
            UsuarioValidationException ex,
            WebRequest request
    ) {
        log.warn("Erro de validação: {}", ex.getMessage());

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                ex.getMessage()
        );
        problemDetail.setTitle("Erro de Validação");
        problemDetail.setType(URI.create("https://pitstop.com/errors/validation-error"));
        problemDetail.setProperty("timestamp", Instant.now());

        return problemDetail;
    }

    // ========== EXCEÇÕES DO MÓDULO DE CLIENTE ==========

    /**
     * Trata exceção quando um cliente não é encontrado.
     * HTTP 404 - Not Found
     */
    @ExceptionHandler(ClienteNotFoundException.class)
    public ProblemDetail handleClienteNotFoundException(
            ClienteNotFoundException ex,
            WebRequest request
    ) {
        log.warn("Cliente não encontrado: {}", ex.getMessage());

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.NOT_FOUND,
                ex.getMessage()
        );
        problemDetail.setTitle("Cliente Não Encontrado");
        problemDetail.setType(URI.create("https://pitstop.com/errors/cliente-not-found"));
        problemDetail.setProperty("timestamp", Instant.now());

        return problemDetail;
    }

    /**
     * Trata exceção quando tenta-se usar um CPF/CNPJ já existente.
     * HTTP 409 - Conflict
     */
    @ExceptionHandler(CpfCnpjAlreadyExistsException.class)
    public ProblemDetail handleCpfCnpjAlreadyExistsException(
            CpfCnpjAlreadyExistsException ex,
            WebRequest request
    ) {
        log.warn("CPF/CNPJ duplicado: {}", ex.getMessage());

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.CONFLICT,
                ex.getMessage()
        );
        problemDetail.setTitle("CPF/CNPJ Duplicado");
        problemDetail.setType(URI.create("https://pitstop.com/errors/cpf-cnpj-already-exists"));
        problemDetail.setProperty("timestamp", Instant.now());

        return problemDetail;
    }

    /**
     * Trata exceção genérica de validação de cliente.
     * HTTP 400 - Bad Request
     */
    @ExceptionHandler(ClienteValidationException.class)
    public ProblemDetail handleClienteValidationException(
            ClienteValidationException ex,
            WebRequest request
    ) {
        log.warn("Erro de validação de cliente: {}", ex.getMessage());

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                ex.getMessage()
        );
        problemDetail.setTitle("Erro de Validação de Cliente");
        problemDetail.setType(URI.create("https://pitstop.com/errors/cliente-validation-error"));
        problemDetail.setProperty("timestamp", Instant.now());

        return problemDetail;
    }

    // ========== EXCEÇÕES DE VALIDAÇÃO (BEAN VALIDATION) ==========

    /**
     * Trata erros de validação de campos (Bean Validation).
     * HTTP 400 - Bad Request
     *
     * Retorna mapa de erros por campo.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleMethodArgumentNotValidException(
            MethodArgumentNotValidException ex,
            WebRequest request
    ) {
        log.warn("Erro de validação de campos: {}", ex.getMessage());

        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                "Erro de validação nos campos fornecidos"
        );
        problemDetail.setTitle("Erro de Validação");
        problemDetail.setType(URI.create("https://pitstop.com/errors/validation-error"));
        problemDetail.setProperty("timestamp", Instant.now());
        problemDetail.setProperty("errors", errors);

        return problemDetail;
    }

    // ========== EXCEÇÕES DE SEGURANÇA (SPRING SECURITY) ==========

    /**
     * Trata exceções de autenticação (credenciais inválidas, token expirado, etc).
     * HTTP 401 - Unauthorized
     */
    @ExceptionHandler(AuthenticationException.class)
    public ProblemDetail handleAuthenticationException(
            AuthenticationException ex,
            WebRequest request
    ) {
        log.warn("Erro de autenticação: {}", ex.getMessage());

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.UNAUTHORIZED,
                "Falha na autenticação. Verifique suas credenciais."
        );
        problemDetail.setTitle("Erro de Autenticação");
        problemDetail.setType(URI.create("https://pitstop.com/errors/authentication-failed"));
        problemDetail.setProperty("timestamp", Instant.now());

        return problemDetail;
    }

    /**
     * Trata exceções de acesso negado (usuário autenticado mas sem permissão).
     * HTTP 403 - Forbidden
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ProblemDetail handleAccessDeniedException(
            AccessDeniedException ex,
            WebRequest request
    ) {
        log.warn("Acesso negado: {}", ex.getMessage());

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.FORBIDDEN,
                "Você não tem permissão para acessar este recurso"
        );
        problemDetail.setTitle("Acesso Negado");
        problemDetail.setType(URI.create("https://pitstop.com/errors/access-denied"));
        problemDetail.setProperty("timestamp", Instant.now());

        return problemDetail;
    }

    /**
     * Trata exceções de credenciais inválidas no login.
     * HTTP 401 - Unauthorized
     */
    @ExceptionHandler(BadCredentialsException.class)
    public ProblemDetail handleBadCredentialsException(
            BadCredentialsException ex,
            WebRequest request
    ) {
        log.warn("Credenciais inválidas: {}", ex.getMessage());

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.UNAUTHORIZED,
                "Email ou senha inválidos"
        );
        problemDetail.setTitle("Credenciais Inválidas");
        problemDetail.setType(URI.create("https://pitstop.com/errors/bad-credentials"));
        problemDetail.setProperty("timestamp", Instant.now());

        return problemDetail;
    }

    // ========== EXCEÇÕES GENÉRICAS ==========

    /**
     * Trata exceções genéricas não capturadas por outros handlers.
     * HTTP 500 - Internal Server Error
     */
    @ExceptionHandler(Exception.class)
    public ProblemDetail handleGlobalException(
            Exception ex,
            WebRequest request
    ) {
        log.error("Erro interno do servidor", ex);

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Ocorreu um erro interno no servidor. Por favor, tente novamente mais tarde."
        );
        problemDetail.setTitle("Erro Interno do Servidor");
        problemDetail.setType(URI.create("https://pitstop.com/errors/internal-server-error"));
        problemDetail.setProperty("timestamp", Instant.now());

        // Em produção, não expor detalhes do erro
        // Em desenvolvimento, incluir stack trace
        if (log.isDebugEnabled()) {
            problemDetail.setProperty("message", ex.getMessage());
        }

        return problemDetail;
    }

    /**
     * Trata IllegalArgumentException.
     * HTTP 400 - Bad Request
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail handleIllegalArgumentException(
            IllegalArgumentException ex,
            WebRequest request
    ) {
        log.warn("Argumento inválido: {}", ex.getMessage());

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                ex.getMessage()
        );
        problemDetail.setTitle("Argumento Inválido");
        problemDetail.setType(URI.create("https://pitstop.com/errors/illegal-argument"));
        problemDetail.setProperty("timestamp", Instant.now());

        return problemDetail;
    }

    /**
     * Trata IllegalStateException.
     * HTTP 409 - Conflict
     */
    @ExceptionHandler(IllegalStateException.class)
    public ProblemDetail handleIllegalStateException(
            IllegalStateException ex,
            WebRequest request
    ) {
        log.warn("Estado inválido: {}", ex.getMessage());

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.CONFLICT,
                ex.getMessage()
        );
        problemDetail.setTitle("Estado Inválido");
        problemDetail.setType(URI.create("https://pitstop.com/errors/illegal-state"));
        problemDetail.setProperty("timestamp", Instant.now());

        return problemDetail;
    }
}
