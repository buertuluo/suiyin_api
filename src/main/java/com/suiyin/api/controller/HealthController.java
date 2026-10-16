package com.suiyin.api.controller;

import com.suiyin.api.repository.LedgerRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class HealthController {

    private final LedgerRepository repository;

    public HealthController(LedgerRepository repository) {
        this.repository = repository;
    }

    @GetMapping("/health")
    public Map<String, Object> health() {
        return Map.of("ok", true, "service", "suiyin-api", "lang", "java");
    }

    @GetMapping("/health/db")
    public ResponseEntity<Map<String, Object>> dbHealth() {
        try {
            repository.ping();
            return ResponseEntity.ok(Map.of("ok", true, "db", "up"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of(
                    "ok", false,
                    "db", "down",
                    "message", e.getMessage() == null ? "db error" : e.getMessage()
            ));
        }
    }
}
