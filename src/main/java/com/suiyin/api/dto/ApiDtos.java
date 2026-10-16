package com.suiyin.api.dto;

import java.util.List;

/** 与小程序端字段风格对齐（camelCase，金额单位：分） */
public final class ApiDtos {

    private ApiDtos() {}

    public record UserDto(long id, String openid, String nickname, String avatarUrl) {}

    public record LedgerDto(long id, String name, long ownerUserId, String currency) {}

    public record AccountDto(
            long id,
            long ledgerId,
            String name,
            String type,
            String icon,
            long balance,
            int sortOrder,
            boolean isArchived
    ) {}

    public record CategoryDto(
            long id,
            long ledgerId,
            Long parentId,
            String name,
            String type,
            String icon,
            int sortOrder
    ) {}

    public record BootstrapResponse(
            UserDto user,
            LedgerDto ledger,
            List<AccountDto> accounts,
            List<CategoryDto> categories
    ) {}

    public record TransactionDto(
            long id,
            long ledgerId,
            long userId,
            String type,
            long amount,
            Long categoryId,
            long accountId,
            Long toAccountId,
            String occurredAt,
            String note
    ) {}

    public record TransactionListResponse(List<TransactionDto> list) {}

    public record CreateTransactionRequest(
            long ledgerId,
            long userId,
            String type,
            long amount,
            Long categoryId,
            long accountId,
            Long toAccountId,
            String occurredAt,
            String note
    ) {}

    public record CreateAccountRequest(
            long ledgerId,
            String name,
            String type,
            String icon
    ) {}

    public record ErrorResponse(String message) {}
}
