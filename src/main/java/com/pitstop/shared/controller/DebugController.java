package com.pitstop.shared.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.security.SecureRandom;
import java.util.Base64;

/**
 * TEMPORARY DEBUG CONTROLLER - DELETE IN PRODUCTION!
 * Used only to generate BCrypt hashes for testing.
 */
@RestController
@RequestMapping("/api/debug")
@RequiredArgsConstructor
public class DebugController {

    private final PasswordEncoder passwordEncoder;

    @GetMapping("/hash/{password}")
    public String generateHash(@PathVariable String password) {
        String hash = passwordEncoder.encode(password);
        return "Password: " + password + "\nBCrypt Hash: " + hash;
    }

    @GetMapping("/verify")
    public String verifyPassword(
            @RequestParam String password,
            @RequestParam String hash
    ) {
        boolean matches = passwordEncoder.matches(password, hash);
        return "Password: " + password +
               "\nHash: " + hash +
               "\nMatches: " + matches;
    }

    @GetMapping("/generate-jwt-secret")
    public String generateJwtSecret() {
        SecureRandom random = new SecureRandom();
        byte[] bytes = new byte[64]; // 512 bits
        random.nextBytes(bytes);
        String secret = Base64.getEncoder().encodeToString(bytes);
        return "JWT_SECRET (copy to .env):\n" + secret;
    }
}
