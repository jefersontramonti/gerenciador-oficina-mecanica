package com.pitstop.oficina.service;

import com.pitstop.shared.exception.ConflictException;
import com.pitstop.oficina.domain.Oficina;
import com.pitstop.oficina.domain.PlanoAssinatura;
import com.pitstop.oficina.domain.StatusOficina;
import com.pitstop.oficina.dto.RegisterOficinaRequest;
import com.pitstop.oficina.dto.RegisterOficinaResponse;
import com.pitstop.oficina.repository.OficinaRepository;
import com.pitstop.shared.security.JwtService;
import com.pitstop.usuario.domain.PerfilUsuario;
import com.pitstop.usuario.domain.Usuario;
import com.pitstop.usuario.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Service responsible for handling the complete oficina registration flow.
 *
 * This service orchestrates the registration of a new workshop (oficina) in the PitStop SaaS platform,
 * including:
 * 1. Validation of unique constraints (CNPJ/CPF and admin email)
 * 2. Creation of the Oficina entity with a 30-day free trial (ECONOMICO plan)
 * 3. Creation of the first admin user with encrypted password
 * 4. Generation of JWT authentication tokens for immediate login
 *
 * The entire operation is wrapped in a transaction to ensure data consistency.
 * If any step fails, all changes are rolled back.
 *
 * Business Rules:
 * - CNPJ/CPF must be unique across all oficinas
 * - Admin email must be unique across all users
 * - New oficinas start with ECONOMICO plan (30 days free trial)
 * - Monthly value is set to 0.00 during trial period
 * - Status is set to ATIVA by default
 * - First user is automatically assigned ADMIN profile
 * - Password is encrypted using BCrypt (12 rounds via PasswordEncoder)
 * - JWT tokens are generated for immediate authentication (15min access + 7 days refresh)
 *
 * @author PitStop Development Team
 * @version 1.0
 * @since 2025-12-21
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OficinaRegistrationService {

    private static final int TRIAL_DAYS = 30;
    private static final BigDecimal TRIAL_MONTHLY_VALUE = BigDecimal.ZERO;

    private final OficinaRepository oficinaRepository;
    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    /**
     * Registers a new oficina in the platform with its first admin user.
     *
     * This method performs the complete registration flow:
     * 1. Validates that CNPJ/CPF is not already registered
     * 2. Validates that admin email is not already in use
     * 3. Creates the Oficina entity with ECONOMICO plan and 30 days trial
     * 4. Creates the first Usuario with ADMIN profile and encrypted password
     * 5. Generates JWT tokens for immediate authentication
     *
     * The operation is atomic - if any step fails, all changes are rolled back.
     *
     * @param request Registration data including oficina details, address, and admin credentials
     * @return RegisterOficinaResponse containing IDs and JWT tokens for immediate login
     * @throws ConflictException if CNPJ/CPF or admin email already exists
     * @throws IllegalArgumentException if request data is invalid (handled by Bean Validation)
     */
    @Transactional
    public RegisterOficinaResponse register(RegisterOficinaRequest request) {
        log.info("Starting oficina registration for CNPJ/CPF: {}", maskCnpjCpf(request.cnpjCpf()));

        // Step 1: Validate unique constraints
        validateCnpjCpfUniqueness(request.cnpjCpf());
        validateAdminEmailUniqueness(request.adminEmail());

        // Step 2: Create Oficina with trial plan
        Oficina oficina = createOficina(request);
        Oficina savedOficina = oficinaRepository.save(oficina);
        log.info("Oficina created successfully with ID: {}", savedOficina.getId());

        // Step 3: Create first admin user
        Usuario adminUser = createAdminUser(request, savedOficina);
        Usuario savedAdminUser = usuarioRepository.save(adminUser);
        log.info("Admin user created successfully with ID: {}", savedAdminUser.getId());

        // Step 4: Generate JWT tokens
        String accessToken = jwtService.generateAccessToken(savedAdminUser);
        String refreshToken = jwtService.generateRefreshToken(savedAdminUser);

        // Convert Date to LocalDateTime for response
        java.util.Date expirationDate = jwtService.extractExpiration(accessToken);
        LocalDateTime expiresAt = expirationDate.toInstant()
                .atZone(java.time.ZoneId.systemDefault())
                .toLocalDateTime();

        log.info("Oficina registration completed successfully. Oficina ID: {}, Admin ID: {}",
                savedOficina.getId(), savedAdminUser.getId());

        return new RegisterOficinaResponse(
                savedOficina.getId(),
                savedAdminUser.getId(),
                accessToken,
                refreshToken,
                expiresAt
        );
    }

    /**
     * Validates that the CNPJ/CPF is not already registered in the system.
     *
     * @param cnpjCpf CNPJ or CPF to validate
     * @throws ConflictException if CNPJ/CPF already exists
     */
    private void validateCnpjCpfUniqueness(String cnpjCpf) {
        if (oficinaRepository.existsByCnpjCpf(cnpjCpf)) {
            log.warn("Registration attempt with duplicate CNPJ/CPF: {}", maskCnpjCpf(cnpjCpf));
            throw new ConflictException("CNPJ/CPF já cadastrado no sistema");
        }
    }

    /**
     * Validates that the admin email is not already in use by another user.
     *
     * @param email Email to validate
     * @throws ConflictException if email already exists
     */
    private void validateAdminEmailUniqueness(String email) {
        if (usuarioRepository.existsByEmail(email)) {
            log.warn("Registration attempt with duplicate admin email: {}", maskEmail(email));
            throw new ConflictException("Email do administrador já cadastrado no sistema");
        }
    }

    /**
     * Creates the Oficina entity from registration request data.
     * Sets up the oficina with:
     * - ECONOMICO plan (30-day free trial)
     * - ATIVA status
     * - Zero monthly value during trial
     * - Automatic expiration date (today + 30 days)
     *
     * @param request Registration request data
     * @return Oficina entity ready to be persisted
     */
    private Oficina createOficina(RegisterOficinaRequest request) {
        LocalDate today = LocalDate.now();
        LocalDate trialExpiration = today.plusDays(TRIAL_DAYS);

        // Create embedded Contato (contact information)
        com.pitstop.oficina.domain.Contato contato = com.pitstop.oficina.domain.Contato.builder()
                .email(request.contatoEmail())
                .telefoneCelular(request.contatoTelefone())
                .build();

        // Create embedded Endereco (address)
        com.pitstop.cliente.domain.Endereco endereco = com.pitstop.cliente.domain.Endereco.builder()
                .logradouro(request.logradouro())
                .numero(request.numero())
                .complemento(request.complemento())
                .bairro(request.bairro())
                .cidade(request.cidade())
                .estado(request.estado())
                .cep(request.cep())
                .build();

        // Build Oficina entity with trial plan
        return Oficina.builder()
                .cnpjCpf(request.cnpjCpf())
                .tipoPessoa(request.tipoPessoa())
                .razaoSocial(request.razaoSocial())
                .nomeFantasia(request.nomeFantasia())
                .nomeResponsavel(request.contatoNome())
                .inscricaoEstadual(request.inscricaoEstadual())
                .inscricaoMunicipal(request.inscricaoMunicipal())
                .contato(contato)
                .endereco(endereco)
                .plano(PlanoAssinatura.ECONOMICO)
                .status(StatusOficina.ATIVA)
                .valorMensalidade(TRIAL_MONTHLY_VALUE)
                .dataAssinatura(today)
                .dataVencimentoPlano(trialExpiration)
                .ativo(true)
                .build();
    }

    /**
     * Creates the first admin user for the oficina.
     * The user is created with:
     * - ADMIN profile (full system access)
     * - Encrypted password (BCrypt with 12 rounds)
     * - Link to the created oficina
     * - Active status by default
     *
     * @param request Registration request containing admin credentials
     * @param savedOficina Persisted Oficina entity
     * @return Usuario entity ready to be persisted
     */
    private Usuario createAdminUser(RegisterOficinaRequest request, Oficina savedOficina) {
        return Usuario.builder()
                .nome(request.adminNome())
                .email(request.adminEmail())
                .senha(passwordEncoder.encode(request.adminSenha()))
                .perfil(PerfilUsuario.ADMIN)
                .oficina(savedOficina)
                .ativo(true)
                .build();
    }

    /**
     * Masks CNPJ/CPF for logging purposes (LGPD compliance).
     * Shows only first and last 3 digits.
     *
     * Examples:
     * - CPF: 123.456.789-01 -> 123****01
     * - CNPJ: 12.345.678/0001-90 -> 123*******90
     *
     * @param cnpjCpf CNPJ or CPF to mask
     * @return Masked CNPJ/CPF
     */
    private String maskCnpjCpf(String cnpjCpf) {
        if (cnpjCpf == null || cnpjCpf.length() < 6) {
            return "***";
        }
        String digits = cnpjCpf.replaceAll("\\D", "");
        int length = digits.length();
        return digits.substring(0, 3) + "*".repeat(length - 6) + digits.substring(length - 3);
    }

    /**
     * Masks email for logging purposes (LGPD compliance).
     * Shows only first 3 characters and domain.
     *
     * Example: joao.silva@example.com -> joa***@example.com
     *
     * @param email Email to mask
     * @return Masked email
     */
    private String maskEmail(String email) {
        if (email == null || !email.contains("@")) {
            return "***@***";
        }
        String[] parts = email.split("@");
        String localPart = parts[0];
        String domain = parts[1];

        if (localPart.length() <= 3) {
            return "***@" + domain;
        }

        return localPart.substring(0, 3) + "***@" + domain;
    }
}
