package com.suiyin.api.controller;

import com.suiyin.api.dto.ApiDtos.CreateAccountRequest;
import com.suiyin.api.dto.ApiDtos.ErrorResponse;
import com.suiyin.api.dto.ApiDtos.TransactionListResponse;
import com.suiyin.api.repository.LedgerRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class AccountController {

    private final LedgerRepository repository;

    public AccountController(LedgerRepository repository) {
        this.repository = repository;
    }

    @GetMapping("/api/accounts/{id}")
    public ResponseEntity<?> detail(@PathVariable long id) {
        return repository.findAccountById(id)
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new ErrorResponse("账户不存在")));
    }

    @GetMapping("/api/accounts/{id}/transactions")
    public ResponseEntity<?> transactions(
            @PathVariable long id,
            @RequestParam(defaultValue = "50") int limit
    ) {
        if (repository.findAccountById(id).isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ErrorResponse("账户不存在"));
        }
        int safeLimit = Math.min(Math.max(limit, 1), 200);
        return ResponseEntity.ok(new TransactionListResponse(
                repository.listTransactionsByAccount(id, safeLimit)
        ));
    }

    @PostMapping("/api/accounts")
    public ResponseEntity<?> create(@RequestBody CreateAccountRequest req) {
        try {
            return ResponseEntity.status(HttpStatus.CREATED).body(repository.createAccount(req));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse(e.getMessage()));
        }
    }
}
