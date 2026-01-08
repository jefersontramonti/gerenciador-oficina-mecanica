package com.pitstop.ia.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Service para criptografar/descriptografar API keys.
 * Usa AES-256-GCM para segurança.
 */
@Service
@Slf4j
public class CriptografiaService {

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 128;

    private final SecretKeySpec secretKey;

    public CriptografiaService(
            @Value("${pitstop.ia.encryption-key:}") String encryptionKey
    ) {
        // Valida que a chave foi configurada
        if (encryptionKey == null || encryptionKey.isBlank()) {
            log.warn("Chave de criptografia não configurada. Configure 'pitstop.ia.encryption-key' no application.yml para habilitar IA.");
            // Usa chave temporária para não quebrar inicialização (IA ficará desabilitada sem config válida)
            encryptionKey = "UNCONFIGURED-KEY-IA-DISABLED-000";
        }

        if (encryptionKey.length() < 32) {
            log.warn("Chave de criptografia deve ter pelo menos 32 caracteres para segurança adequada.");
        }

        // Garante que a chave tem 32 bytes (256 bits)
        byte[] keyBytes = new byte[32];
        byte[] providedKey = encryptionKey.getBytes(StandardCharsets.UTF_8);
        System.arraycopy(providedKey, 0, keyBytes, 0, Math.min(providedKey.length, 32));
        this.secretKey = new SecretKeySpec(keyBytes, "AES");
    }

    /**
     * Criptografa um texto.
     *
     * @param plainText texto em claro
     * @return texto criptografado em Base64
     */
    public String encrypt(String plainText) {
        if (plainText == null || plainText.isBlank()) {
            return null;
        }

        try {
            // Gera IV aleatório
            byte[] iv = new byte[GCM_IV_LENGTH];
            new SecureRandom().nextBytes(iv);

            // Configura cipher
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, parameterSpec);

            // Criptografa
            byte[] encryptedBytes = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));

            // Combina IV + ciphertext
            byte[] combined = new byte[iv.length + encryptedBytes.length];
            System.arraycopy(iv, 0, combined, 0, iv.length);
            System.arraycopy(encryptedBytes, 0, combined, iv.length, encryptedBytes.length);

            return Base64.getEncoder().encodeToString(combined);
        } catch (Exception e) {
            log.error("Erro ao criptografar", e);
            throw new RuntimeException("Erro ao criptografar dados", e);
        }
    }

    /**
     * Descriptografa um texto.
     *
     * @param encryptedText texto criptografado em Base64
     * @return texto em claro
     */
    public String decrypt(String encryptedText) {
        if (encryptedText == null || encryptedText.isBlank()) {
            return null;
        }

        try {
            byte[] combined = Base64.getDecoder().decode(encryptedText);

            // Extrai IV
            byte[] iv = new byte[GCM_IV_LENGTH];
            System.arraycopy(combined, 0, iv, 0, iv.length);

            // Extrai ciphertext
            byte[] encryptedBytes = new byte[combined.length - GCM_IV_LENGTH];
            System.arraycopy(combined, GCM_IV_LENGTH, encryptedBytes, 0, encryptedBytes.length);

            // Configura cipher
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, parameterSpec);

            // Descriptografa
            byte[] decryptedBytes = cipher.doFinal(encryptedBytes);

            return new String(decryptedBytes, StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.error("Erro ao descriptografar", e);
            throw new RuntimeException("Erro ao descriptografar dados", e);
        }
    }
}
