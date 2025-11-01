package com.pitstop.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * TEMPORARY DEBUG CONTROLLER
 * DELETE IN PRODUCTION!
 */
@RestController
@RequestMapping("/api/debug/password")
public class DebugPasswordController {

    @Autowired
    private PasswordEncoder passwordEncoder;

    @PostMapping("/encode")
    public Map<String, String> encodePassword(@RequestBody Map<String, String> request) {
        String rawPassword = request.get("password");
        String encoded = passwordEncoder.encode(rawPassword);
        return Map.of(
                "raw", rawPassword,
                "encoded", encoded
        );
    }

    @PostMapping("/verify")
    public Map<String, Object> verifyPassword(@RequestBody Map<String, String> request) {
        String rawPassword = request.get("raw");
        String encodedPassword = request.get("encoded");
        boolean matches = passwordEncoder.matches(rawPassword, encodedPassword);
        return Map.of(
                "raw", rawPassword,
                "encoded", encodedPassword,
                "matches", matches
        );
    }
}
