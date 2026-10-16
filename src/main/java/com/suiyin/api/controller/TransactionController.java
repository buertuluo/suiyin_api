package com.suiyin.api.controller;

import com.suiyin.api.dto.ApiDtos.CreateTransactionRequest;
import com.suiyin.api.dto.ApiDtos.ErrorResponse;
import com.suiyin.api.dto.ApiDtos.TransactionDto;
import com.suiyin.api.dto.ApiDtos.TransactionListResponse;
import com.suiyin.api.repository.LedgerRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TransactionController {

    private final LedgerRepository repository;

    public TransactionController(LedgerRepository repository) {
        this.repository = repository;
    }

    @GetMapping("/api/transactions")
    public ResponseEntity<?> list(
            @RequestParam long ledgerId,
            @RequestParam(required = false) String yearMonth
    ) {
        if (ledgerId <= 0) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse("ledgerId 无效"));
        }
        if (yearMonth != null && !yearMonth.isBlank() && !yearMonth.matches("\\d{4}-\\d{2}")) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse("yearMonth 格式应为 YYYY-MM"));
        }

        return ResponseEntity.ok(new TransactionListResponse(
                repository.listTransactions(ledgerId, yearMonth)
        ));
    }

    @GetMapping("/api/transactions/{id}")
    public ResponseEntity<?> detail(@PathVariable long id) {
        return repository.findTransactionById(id)
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new ErrorResponse("流水不存在")));
    }

    @PostMapping("/api/transactions")
    public ResponseEntity<?> create(@RequestBody CreateTransactionRequest req) {
        try {
            TransactionDto created = repository.createTransaction(req);
            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse(e.getMessage()));
        }
    }

    @DeleteMapping("/api/transactions/{id}")
    public ResponseEntity<?> delete(@PathVariable long id) {
        try {
            repository.deleteTransaction(id);
            return ResponseEntity.ok(java.util.Map.of("ok", true));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ErrorResponse(e.getMessage()));
        }
    }
}
