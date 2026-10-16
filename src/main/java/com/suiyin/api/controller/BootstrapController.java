package com.suiyin.api.controller;

import com.suiyin.api.dto.ApiDtos.BootstrapResponse;
import com.suiyin.api.dto.ApiDtos.ErrorResponse;
import com.suiyin.api.dto.ApiDtos.LedgerDto;
import com.suiyin.api.dto.ApiDtos.UserDto;
import com.suiyin.api.repository.LedgerRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class BootstrapController {

    private final LedgerRepository repository;

    public BootstrapController(LedgerRepository repository) {
        this.repository = repository;
    }

    @GetMapping("/api/bootstrap")
    public ResponseEntity<?> bootstrap(
            @RequestParam(defaultValue = "demo_openid_suiyin") String openid
    ) {
        UserDto user = repository.findUserByOpenid(openid).orElse(null);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ErrorResponse("用户不存在，请先执行 suiyin-api/sql/seed_bootstrap.sql"));
        }

        LedgerDto ledger = repository.findFirstLedgerByOwner(user.id()).orElse(null);
        if (ledger == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ErrorResponse("账本不存在，请先执行 suiyin-api/sql/seed_bootstrap.sql"));
        }

        repository.ensureDefaultCategories(ledger.id());

        BootstrapResponse body = new BootstrapResponse(
                user,
                ledger,
                repository.listAccounts(ledger.id()),
                repository.listCategories(ledger.id())
        );
        return ResponseEntity.ok(body);
    }
}
