package com.pitstop.shared.exception;

import com.pitstop.usuario.exception.*;
import com.pitstop.cliente.exception.*;
import com.pitstop.ordemservico.exception.*;
import com.pitstop.estoque.exception.*;
import com.pitstop.anexo.exception.*;
import com.pitstop.fornecedor.exception.*;
import com.pitstop.shared.security.feature.FeatureNotEnabledException;
import com.pitstop.shared.security.tenant.TenantNotSetException;
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

    // ========== EXCEÇÕES DO MÓDULO DE ESTOQUE ==========

    /**
     * Trata exceção quando uma peça não é encontrada.
     * HTTP 404 - Not Found
     */
    @ExceptionHandler(PecaNotFoundException.class)
    public ProblemDetail handlePecaNotFoundException(
            PecaNotFoundException ex,
            WebRequest request
    ) {
        log.warn("Peça não encontrada: {}", ex.getMessage());

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.NOT_FOUND,
                ex.getMessage()
        );
        problemDetail.setTitle("Peça Não Encontrada");
        problemDetail.setType(URI.create("https://pitstop.com/errors/peca-not-found"));
        problemDetail.setProperty("timestamp", Instant.now());

        if (ex.getPecaId() != null) {
            problemDetail.setProperty("pecaId", ex.getPecaId());
        }
        if (ex.getCodigo() != null) {
            problemDetail.setProperty("codigo", ex.getCodigo());
        }

        return problemDetail;
    }

    /**
     * Trata exceção de estoque insuficiente.
     * HTTP 409 - Conflict
     */
    @ExceptionHandler(EstoqueInsuficienteException.class)
    public ProblemDetail handleEstoqueInsuficienteException(
            EstoqueInsuficienteException ex,
            WebRequest request
    ) {
        log.error("Estoque insuficiente: {}", ex.getMessage());

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.CONFLICT,
                ex.getMessage()
        );
        problemDetail.setTitle("Estoque Insuficiente");
        problemDetail.setType(URI.create("https://pitstop.com/errors/estoque-insuficiente"));
        problemDetail.setProperty("timestamp", Instant.now());
        problemDetail.setProperty("pecaId", ex.getPecaId());
        problemDetail.setProperty("quantidadeRequerida", ex.getQuantidadeRequerida());
        problemDetail.setProperty("quantidadeDisponivel", ex.getQuantidadeDisponivel());
        problemDetail.setProperty("deficit", ex.getDeficit());

        if (ex.getCodigoPeca() != null) {
            problemDetail.setProperty("codigoPeca", ex.getCodigoPeca());
        }
        if (ex.getDescricaoPeca() != null) {
            problemDetail.setProperty("descricaoPeca", ex.getDescricaoPeca());
        }

        return problemDetail;
    }

    /**
     * Trata exceção de código de peça duplicado.
     * HTTP 409 - Conflict
     */
    @ExceptionHandler(CodigoPecaDuplicadoException.class)
    public ProblemDetail handleCodigoPecaDuplicadoException(
            CodigoPecaDuplicadoException ex,
            WebRequest request
    ) {
        log.warn("Código de peça duplicado: {}", ex.getMessage());

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.CONFLICT,
                ex.getMessage()
        );
        problemDetail.setTitle("Código de Peça Duplicado");
        problemDetail.setType(URI.create("https://pitstop.com/errors/codigo-peca-duplicado"));
        problemDetail.setProperty("timestamp", Instant.now());
        problemDetail.setProperty("codigo", ex.getCodigo());

        return problemDetail;
    }

    /**
     * Trata exceção de movimentação inválida.
     * HTTP 400 - Bad Request
     */
    @ExceptionHandler(MovimentacaoInvalidaException.class)
    public ProblemDetail handleMovimentacaoInvalidaException(
            MovimentacaoInvalidaException ex,
            WebRequest request
    ) {
        log.warn("Movimentação inválida: {}", ex.getMessage());

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                ex.getMessage()
        );
        problemDetail.setTitle("Movimentação Inválida");
        problemDetail.setType(URI.create("https://pitstop.com/errors/movimentacao-invalida"));
        problemDetail.setProperty("timestamp", Instant.now());

        return problemDetail;
    }

    /**
     * Trata exceção de ciclo hierárquico em locais de armazenamento.
     * HTTP 400 - Bad Request
     */
    @ExceptionHandler(CicloHierarquicoException.class)
    public ProblemDetail handleCicloHierarquicoException(
            CicloHierarquicoException ex,
            WebRequest request
    ) {
        log.warn("Ciclo hierárquico detectado: {}", ex.getMessage());

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                ex.getMessage()
        );
        problemDetail.setTitle("Ciclo Hierárquico");
        problemDetail.setType(URI.create("https://pitstop.com/errors/ciclo-hierarquico"));
        problemDetail.setProperty("timestamp", Instant.now());

        return problemDetail;
    }

    /**
     * Trata exceção quando tenta-se excluir local com peças vinculadas.
     * HTTP 409 - Conflict
     */
    @ExceptionHandler(LocalComPecasVinculadasException.class)
    public ProblemDetail handleLocalComPecasVinculadasException(
            LocalComPecasVinculadasException ex,
            WebRequest request
    ) {
        log.warn("Tentativa de excluir local com peças: {}", ex.getMessage());

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.CONFLICT,
                ex.getMessage()
        );
        problemDetail.setTitle("Local Com Peças Vinculadas");
        problemDetail.setType(URI.create("https://pitstop.com/errors/local-com-pecas-vinculadas"));
        problemDetail.setProperty("timestamp", Instant.now());
        problemDetail.setProperty("quantidadePecas", ex.getQuantidadePecas());

        return problemDetail;
    }

    // ========== EXCEÇÕES DO MÓDULO DE FORNECEDOR ==========

    @ExceptionHandler(FornecedorNotFoundException.class)
    public ProblemDetail handleFornecedorNotFoundException(
            FornecedorNotFoundException ex,
            WebRequest request
    ) {
        log.warn("Fornecedor não encontrado: {}", ex.getMessage());

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.NOT_FOUND,
                ex.getMessage()
        );
        problemDetail.setTitle("Fornecedor Não Encontrado");
        problemDetail.setType(URI.create("https://pitstop.com/errors/fornecedor-not-found"));
        problemDetail.setProperty("timestamp", Instant.now());

        return problemDetail;
    }

    @ExceptionHandler(CpfCnpjFornecedorDuplicadoException.class)
    public ProblemDetail handleCpfCnpjFornecedorDuplicadoException(
            CpfCnpjFornecedorDuplicadoException ex,
            WebRequest request
    ) {
        log.warn("CPF/CNPJ de fornecedor duplicado: {}", ex.getMessage());

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.CONFLICT,
                ex.getMessage()
        );
        problemDetail.setTitle("CPF/CNPJ de Fornecedor Duplicado");
        problemDetail.setType(URI.create("https://pitstop.com/errors/cpf-cnpj-fornecedor-duplicado"));
        problemDetail.setProperty("timestamp", Instant.now());

        return problemDetail;
    }

    @ExceptionHandler(FornecedorValidationException.class)
    public ProblemDetail handleFornecedorValidationException(
            FornecedorValidationException ex,
            WebRequest request
    ) {
        log.warn("Erro de validação de fornecedor: {}", ex.getMessage());

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                ex.getMessage()
        );
        problemDetail.setTitle("Erro de Validação de Fornecedor");
        problemDetail.setType(URI.create("https://pitstop.com/errors/fornecedor-validation-error"));
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

    // ========== EXCEÇÕES DE MULTI-TENANCY ==========

    /**
     * Trata exceção quando o contexto de tenant não está definido.
     * HTTP 403 - Forbidden
     *
     * <p>Isso acontece quando:</p>
     * <ul>
     *   <li>SUPER_ADMIN tenta acessar endpoints que requerem tenant</li>
     *   <li>Erro de configuração no TenantFilter</li>
     *   <li>Endpoints públicos tentando acessar dados de tenant</li>
     * </ul>
     *
     * <p>Retorna 403 ao invés de 500 para indicar que é um problema de autorização,
     * não um erro interno do servidor.</p>
     */
    @ExceptionHandler(TenantNotSetException.class)
    public ProblemDetail handleTenantNotSetException(
            TenantNotSetException ex,
            WebRequest request
    ) {
        log.error("Contexto de tenant não definido: {}", ex.getMessage());

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.FORBIDDEN,
                "Contexto de oficina não definido. " +
                "Se você é SUPER_ADMIN, use os endpoints /api/saas/* para gerenciar oficinas."
        );
        problemDetail.setTitle("Contexto de Oficina Não Definido");
        problemDetail.setType(URI.create("https://pitstop.com/errors/tenant-not-set"));
        problemDetail.setProperty("timestamp", Instant.now());
        problemDetail.setProperty("hint", "SUPER_ADMIN deve usar /api/saas/* para operações cross-tenant");

        return problemDetail;
    }

    // ========== EXCEÇÕES DE FEATURE FLAGS ==========

    /**
     * Trata exceção quando uma feature flag não está habilitada para a oficina.
     * HTTP 403 - Forbidden
     *
     * <p>Esta exceção é lançada pelo {@link com.pitstop.shared.security.feature.FeatureGateAspect}
     * quando um endpoint/método anotado com {@link com.pitstop.shared.security.feature.RequiresFeature}
     * é acessado por uma oficina que não tem a feature habilitada.</p>
     *
     * <p>A resposta inclui informações úteis para o frontend exibir mensagem de upgrade.</p>
     */
    @ExceptionHandler(FeatureNotEnabledException.class)
    public ProblemDetail handleFeatureNotEnabledException(
            FeatureNotEnabledException ex,
            WebRequest request
    ) {
        log.warn("Feature não habilitada: {} - {}", ex.getFeatureCode(), ex.getMessage());

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.FORBIDDEN,
                ex.getMessage()
        );
        problemDetail.setTitle("Funcionalidade Não Disponível");
        problemDetail.setType(URI.create("https://pitstop.com/errors/feature-not-enabled"));
        problemDetail.setProperty("timestamp", Instant.now());
        problemDetail.setProperty("featureCode", ex.getFeatureCode());

        if (ex.getFeatureName() != null) {
            problemDetail.setProperty("featureName", ex.getFeatureName());
        }
        if (ex.getRequiredPlan() != null) {
            problemDetail.setProperty("requiredPlan", ex.getRequiredPlan());
            problemDetail.setProperty("upgradeRequired", true);
        }

        return problemDetail;
    }

    // ========== EXCEÇÕES GENÉRICAS ==========

    /**
     * Trata exceção de regra de negócio violada.
     * HTTP 400 - Bad Request
     */
    @ExceptionHandler(BusinessException.class)
    public ProblemDetail handleBusinessException(
            BusinessException ex,
            WebRequest request
    ) {
        log.warn("Erro de negócio: {}", ex.getMessage());

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                ex.getMessage()
        );
        problemDetail.setTitle("Erro de Negócio");
        problemDetail.setType(URI.create("https://pitstop.com/errors/business-error"));
        problemDetail.setProperty("timestamp", Instant.now());

        return problemDetail;
    }

    /**
     * Trata exceção de acesso proibido por regras de negócio (plano, feature flags, etc).
     * HTTP 403 - Forbidden
     *
     * <p>Diferente de AccessDeniedException (Spring Security), esta exceção é
     * lançada explicitamente pelo código de negócio quando uma funcionalidade
     * não está disponível para o usuário/oficina.</p>
     */
    @ExceptionHandler(ForbiddenException.class)
    public ProblemDetail handleForbiddenException(
            ForbiddenException ex,
            WebRequest request
    ) {
        log.warn("Acesso proibido: {}", ex.getMessage());

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.FORBIDDEN,
                ex.getMessage()
        );
        problemDetail.setTitle("Acesso Negado");
        problemDetail.setType(URI.create("https://pitstop.com/errors/forbidden"));
        problemDetail.setProperty("timestamp", Instant.now());

        return problemDetail;
    }

    /**
     * Trata exceção genérica de recurso não encontrado.
     * HTTP 404 - Not Found
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ProblemDetail handleResourceNotFoundException(
            ResourceNotFoundException ex,
            WebRequest request
    ) {
        log.warn("Recurso não encontrado: {}", ex.getMessage());

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.NOT_FOUND,
                ex.getMessage()
        );
        problemDetail.setTitle("Recurso Não Encontrado");
        problemDetail.setType(URI.create("https://pitstop.com/errors/resource-not-found"));
        problemDetail.setProperty("timestamp", Instant.now());

        return problemDetail;
    }

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

    // ========== EXCEÇÕES DO MÓDULO DE ORDEM DE SERVIÇO ==========

    /**
     * Trata exceção quando uma OS não é encontrada.
     * HTTP 404 - Not Found
     */
    @ExceptionHandler(OrdemServicoNotFoundException.class)
    public ProblemDetail handleOrdemServicoNotFoundException(
            OrdemServicoNotFoundException ex,
            WebRequest request
    ) {
        log.warn("Ordem de Serviço não encontrada: {}", ex.getMessage());

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.NOT_FOUND,
                ex.getMessage()
        );
        problemDetail.setTitle("Ordem de Serviço Não Encontrada");
        problemDetail.setType(URI.create("https://pitstop.com/errors/ordem-servico-not-found"));
        problemDetail.setProperty("timestamp", Instant.now());

        return problemDetail;
    }

    /**
     * Trata exceção quando uma transição de status é inválida.
     * HTTP 400 - Bad Request
     */
    @ExceptionHandler(TransicaoStatusInvalidaException.class)
    public ProblemDetail handleTransicaoStatusInvalidaException(
            TransicaoStatusInvalidaException ex,
            WebRequest request
    ) {
        log.warn("Transição de status inválida: {}", ex.getMessage());

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                ex.getMessage()
        );
        problemDetail.setTitle("Transição de Status Inválida");
        problemDetail.setType(URI.create("https://pitstop.com/errors/transicao-status-invalida"));
        problemDetail.setProperty("timestamp", Instant.now());

        return problemDetail;
    }

    /**
     * Trata exceção quando tenta-se editar uma OS não editável.
     * HTTP 400 - Bad Request
     */
    @ExceptionHandler(OrdemServicoNaoEditavelException.class)
    public ProblemDetail handleOrdemServicoNaoEditavelException(
            OrdemServicoNaoEditavelException ex,
            WebRequest request
    ) {
        log.warn("OS não editável: {}", ex.getMessage());

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                ex.getMessage()
        );
        problemDetail.setTitle("Ordem de Serviço Não Editável");
        problemDetail.setType(URI.create("https://pitstop.com/errors/ordem-servico-nao-editavel"));
        problemDetail.setProperty("timestamp", Instant.now());

        return problemDetail;
    }

    /**
     * Trata exceção quando desconto excede o limite permitido.
     * HTTP 400 - Bad Request
     */
    @ExceptionHandler(DescontoExcedidoException.class)
    public ProblemDetail handleDescontoExcedidoException(
            DescontoExcedidoException ex,
            WebRequest request
    ) {
        log.warn("Desconto excedido: {}", ex.getMessage());

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                ex.getMessage()
        );
        problemDetail.setTitle("Desconto Excedido");
        problemDetail.setType(URI.create("https://pitstop.com/errors/desconto-excedido"));
        problemDetail.setProperty("timestamp", Instant.now());

        return problemDetail;
    }

    /**
     * Trata exceções de validação de OS.
     * HTTP 400 - Bad Request
     */
    @ExceptionHandler(OrdemServicoValidationException.class)
    public ProblemDetail handleOrdemServicoValidationException(
            OrdemServicoValidationException ex,
            WebRequest request
    ) {
        log.warn("Validação de OS falhou: {}", ex.getMessage());

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                ex.getMessage()
        );
        problemDetail.setTitle("Erro de Validação");
        problemDetail.setType(URI.create("https://pitstop.com/errors/ordem-servico-validation"));
        problemDetail.setProperty("timestamp", Instant.now());

        return problemDetail;
    }

    /**
     * Trata exceção quando tenta-se entregar uma OS não paga.
     * HTTP 402 - Payment Required
     */
    @ExceptionHandler(OrdemServicoNaoPagaException.class)
    public ProblemDetail handleOrdemServicoNaoPagaException(
            OrdemServicoNaoPagaException ex,
            WebRequest request
    ) {
        log.warn("OS não paga: {}", ex.getMessage());

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.PAYMENT_REQUIRED,
                ex.getMessage()
        );
        problemDetail.setTitle("Pagamento Pendente");
        problemDetail.setType(URI.create("https://pitstop.com/errors/ordem-servico-nao-paga"));
        problemDetail.setProperty("timestamp", Instant.now());

        return problemDetail;
    }

    /**
     * Trata exceção quando horas trabalhadas excedem o limite aprovado pelo cliente.
     * HTTP 400 - Bad Request
     */
    @ExceptionHandler(LimiteHorasExcedidoException.class)
    public ProblemDetail handleLimiteHorasExcedidoException(
            LimiteHorasExcedidoException ex,
            WebRequest request
    ) {
        log.warn("Limite de horas excedido: {}", ex.getMessage());

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                ex.getMessage()
        );
        problemDetail.setTitle("Limite de Horas Excedido");
        problemDetail.setType(URI.create("https://pitstop.com/errors/limite-horas-excedido"));
        problemDetail.setProperty("timestamp", Instant.now());

        if (ex.getHorasTrabalhadas() != null) {
            problemDetail.setProperty("horasTrabalhadas", ex.getHorasTrabalhadas());
        }
        if (ex.getLimiteAprovado() != null) {
            problemDetail.setProperty("limiteAprovado", ex.getLimiteAprovado());
        }

        return problemDetail;
    }

    // ========== EXCEÇÕES GENÉRICAS ==========

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

    // ========== EXCEÇÕES DO MÓDULO DE ANEXOS ==========

    /**
     * Trata exceção quando um anexo não é encontrado.
     * HTTP 404 - Not Found
     */
    @ExceptionHandler(AnexoNotFoundException.class)
    public ProblemDetail handleAnexoNotFoundException(
            AnexoNotFoundException ex,
            WebRequest request
    ) {
        log.warn("Anexo não encontrado: {}", ex.getMessage());

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.NOT_FOUND,
                ex.getMessage()
        );
        problemDetail.setTitle("Anexo Não Encontrado");
        problemDetail.setType(URI.create("https://pitstop.com/errors/anexo-not-found"));
        problemDetail.setProperty("timestamp", Instant.now());

        return problemDetail;
    }

    /**
     * Trata exceção de quota de storage excedida.
     * HTTP 413 - Payload Too Large
     */
    @ExceptionHandler(QuotaExceededException.class)
    public ProblemDetail handleQuotaExceededException(
            QuotaExceededException ex,
            WebRequest request
    ) {
        log.warn("Quota de storage excedida: {}", ex.getMessage());

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.PAYLOAD_TOO_LARGE,
                ex.getMessage()
        );
        problemDetail.setTitle("Quota de Storage Excedida");
        problemDetail.setType(URI.create("https://pitstop.com/errors/quota-exceeded"));
        problemDetail.setProperty("timestamp", Instant.now());
        problemDetail.setProperty("usadoBytes", ex.getUsadoBytes());
        problemDetail.setProperty("limiteBytes", ex.getLimiteBytes());
        problemDetail.setProperty("arquivoBytes", ex.getArquivoBytes());

        return problemDetail;
    }

    /**
     * Trata exceção de tipo de arquivo inválido.
     * HTTP 415 - Unsupported Media Type
     */
    @ExceptionHandler(InvalidFileTypeException.class)
    public ProblemDetail handleInvalidFileTypeException(
            InvalidFileTypeException ex,
            WebRequest request
    ) {
        log.warn("Tipo de arquivo inválido: {}", ex.getMessage());

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                ex.getMessage()
        );
        problemDetail.setTitle("Tipo de Arquivo Não Suportado");
        problemDetail.setType(URI.create("https://pitstop.com/errors/invalid-file-type"));
        problemDetail.setProperty("timestamp", Instant.now());
        problemDetail.setProperty("mimeType", ex.getMimeType());

        return problemDetail;
    }

    /**
     * Trata exceções genéricas de anexo (storage, permissões, etc).
     * HTTP 500 - Internal Server Error
     */
    @ExceptionHandler(AnexoException.class)
    public ProblemDetail handleAnexoException(
            AnexoException ex,
            WebRequest request
    ) {
        log.error("Erro no módulo de anexos: {}", ex.getMessage(), ex);

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Erro ao processar anexo. Por favor, contate o suporte."
        );
        problemDetail.setTitle("Erro de Anexo");
        problemDetail.setType(URI.create("https://pitstop.com/errors/anexo-error"));
        problemDetail.setProperty("timestamp", Instant.now());

        return problemDetail;
    }
}
