package com.suiyin.api.repository;

import com.suiyin.api.dto.ApiDtos.AccountDto;
import com.suiyin.api.dto.ApiDtos.CategoryDto;
import com.suiyin.api.dto.ApiDtos.CreateAccountRequest;
import com.suiyin.api.dto.ApiDtos.CreateTransactionRequest;
import com.suiyin.api.dto.ApiDtos.LedgerDto;
import com.suiyin.api.dto.ApiDtos.TransactionDto;
import com.suiyin.api.dto.ApiDtos.UserDto;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

@Repository
public class LedgerRepository {

    private final JdbcTemplate jdbc;

    public LedgerRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Optional<UserDto> findUserByOpenid(String openid) {
        String sql = """
                SELECT id, openid, nickname, avatar_url
                FROM `user`
                WHERE openid = ?
                LIMIT 1
                """;
        List<UserDto> list = jdbc.query(sql, (rs, rowNum) -> new UserDto(
                rs.getLong("id"),
                rs.getString("openid"),
                rs.getString("nickname"),
                rs.getString("avatar_url")
        ), openid);
        return list.stream().findFirst();
    }

    public Optional<LedgerDto> findFirstLedgerByOwner(long userId) {
        String sql = """
                SELECT id, name, owner_user_id, currency
                FROM ledger
                WHERE owner_user_id = ? AND deleted_at IS NULL
                ORDER BY id ASC
                LIMIT 1
                """;
        List<LedgerDto> list = jdbc.query(sql, (rs, rowNum) -> new LedgerDto(
                rs.getLong("id"),
                rs.getString("name"),
                rs.getLong("owner_user_id"),
                rs.getString("currency")
        ), userId);
        return list.stream().findFirst();
    }

    public List<AccountDto> listAccounts(long ledgerId) {
        String sql = """
                SELECT id, ledger_id, name, type, icon, balance, sort_order, is_archived
                FROM account
                WHERE ledger_id = ? AND deleted_at IS NULL AND is_archived = 0
                ORDER BY sort_order ASC, id ASC
                """;
        return jdbc.query(sql, (rs, rowNum) -> new AccountDto(
                rs.getLong("id"),
                rs.getLong("ledger_id"),
                rs.getString("name"),
                rs.getString("type"),
                rs.getString("icon"),
                rs.getLong("balance"),
                rs.getInt("sort_order"),
                rs.getInt("is_archived") == 1
        ), ledgerId);
    }

    public List<CategoryDto> listCategories(long ledgerId) {
        String sql = """
                SELECT id, ledger_id, parent_id, name, type, icon, sort_order
                FROM category
                WHERE ledger_id = ? AND deleted_at IS NULL
                ORDER BY type ASC, sort_order ASC, id ASC
                """;
        return jdbc.query(sql, (rs, rowNum) -> {
            long parent = rs.getLong("parent_id");
            boolean parentNull = rs.wasNull();
            return new CategoryDto(
                    rs.getLong("id"),
                    rs.getLong("ledger_id"),
                    parentNull ? null : parent,
                    rs.getString("name"),
                    rs.getString("type"),
                    rs.getString("icon"),
                    rs.getInt("sort_order")
            );
        }, ledgerId);
    }

    /** 若账本下没有分类，自动写入默认一级分类（避免未执行 seed_categories.sql） */
    @Transactional
    public void ensureDefaultCategories(long ledgerId) {
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM category WHERE ledger_id = ? AND deleted_at IS NULL",
                Integer.class,
                ledgerId
        );
        if (count != null && count > 0) {
            return;
        }

        Object[][] expense = {
                {"餐饮", "food", 10},
                {"交通", "transport", 20},
                {"购物", "shopping", 30},
                {"住房", "home", 40},
                {"娱乐", "fun", 50},
                {"医疗", "medical", 60},
                {"教育", "education", 70},
                {"通讯", "phone", 80},
                {"日用", "daily", 90},
                {"人情", "gift", 100},
                {"其他支出", "other", 110},
        };
        Object[][] income = {
                {"工资", "salary", 10},
                {"兼职", "parttime", 20},
                {"红包", "redpacket", 30},
                {"理财", "invest", 40},
                {"报销", "reimburse", 50},
                {"其他收入", "other", 60},
        };

        String insert = """
                INSERT INTO category (ledger_id, parent_id, name, type, icon, sort_order, is_system)
                VALUES (?, NULL, ?, ?, ?, ?, 1)
                """;
        for (Object[] row : expense) {
            jdbc.update(insert, ledgerId, row[0], "expense", row[1], row[2]);
        }
        for (Object[] row : income) {
            jdbc.update(insert, ledgerId, row[0], "income", row[1], row[2]);
        }
    }

    public List<TransactionDto> listTransactions(long ledgerId, String yearMonth) {
        StringBuilder sql = new StringBuilder("""
                SELECT id, ledger_id, user_id, type, amount, category_id, account_id,
                       to_account_id, occurred_at, note
                FROM `transaction`
                WHERE ledger_id = ? AND deleted_at IS NULL
                """);
        if (yearMonth != null && !yearMonth.isBlank()) {
            sql.append(" AND DATE_FORMAT(occurred_at, '%Y-%m') = ?");
        }
        sql.append(" ORDER BY occurred_at DESC, id DESC LIMIT 200");

        return jdbc.query(sql.toString(), (rs, rowNum) -> mapTransaction(rs),
                yearMonth == null || yearMonth.isBlank()
                        ? new Object[]{ledgerId}
                        : new Object[]{ledgerId, yearMonth});
    }

    public boolean ping() {
        Integer one = jdbc.queryForObject("SELECT 1", Integer.class);
        return one != null && one == 1;
    }

    public Optional<TransactionDto> findTransactionById(long id) {
        String sql = """
                SELECT id, ledger_id, user_id, type, amount, category_id, account_id,
                       to_account_id, occurred_at, note
                FROM `transaction`
                WHERE id = ? AND deleted_at IS NULL
                LIMIT 1
                """;
        List<TransactionDto> list = jdbc.query(sql, (rs, rowNum) -> mapTransaction(rs), id);
        return list.stream().findFirst();
    }

    public Optional<AccountDto> findAccountById(long id) {
        String sql = """
                SELECT id, ledger_id, name, type, icon, balance, sort_order, is_archived
                FROM account
                WHERE id = ? AND deleted_at IS NULL
                LIMIT 1
                """;
        List<AccountDto> list = jdbc.query(sql, (rs, rowNum) -> new AccountDto(
                rs.getLong("id"),
                rs.getLong("ledger_id"),
                rs.getString("name"),
                rs.getString("type"),
                rs.getString("icon"),
                rs.getLong("balance"),
                rs.getInt("sort_order"),
                rs.getInt("is_archived") == 1
        ), id);
        return list.stream().findFirst();
    }

    public List<TransactionDto> listTransactionsByAccount(long accountId, int limit) {
        String sql = """
                SELECT id, ledger_id, user_id, type, amount, category_id, account_id,
                       to_account_id, occurred_at, note
                FROM `transaction`
                WHERE deleted_at IS NULL
                  AND (account_id = ? OR to_account_id = ?)
                ORDER BY occurred_at DESC, id DESC
                LIMIT ?
                """;
        return jdbc.query(sql, (rs, rowNum) -> mapTransaction(rs), accountId, accountId, limit);
    }

    @Transactional
    public TransactionDto createTransaction(CreateTransactionRequest req) {
        validateCreate(req);
        Timestamp occurred = parseOccurredAt(req.occurredAt());

        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(con -> {
            PreparedStatement ps = con.prepareStatement("""
                    INSERT INTO `transaction`
                    (ledger_id, user_id, type, amount, category_id, account_id, to_account_id, occurred_at, note)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """, Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1, req.ledgerId());
            ps.setLong(2, req.userId());
            ps.setString(3, req.type());
            ps.setLong(4, req.amount());
            if (req.categoryId() == null) {
                ps.setObject(5, null);
            } else {
                ps.setLong(5, req.categoryId());
            }
            ps.setLong(6, req.accountId());
            if (req.toAccountId() == null) {
                ps.setObject(7, null);
            } else {
                ps.setLong(7, req.toAccountId());
            }
            ps.setTimestamp(8, occurred);
            ps.setString(9, req.note() == null ? "" : req.note().trim());
            return ps;
        }, keyHolder);

        long id = keyHolder.getKey().longValue();
        applyBalance(req.type(), req.amount(), req.accountId(), req.toAccountId(), 1);
        return findTransactionById(id).orElseThrow();
    }

    @Transactional
    public void deleteTransaction(long id) {
        TransactionDto txn = findTransactionById(id)
                .orElseThrow(() -> new IllegalArgumentException("流水不存在"));
        jdbc.update("UPDATE `transaction` SET deleted_at = CURRENT_TIMESTAMP(3) WHERE id = ?", id);
        applyBalance(txn.type(), txn.amount(), txn.accountId(), txn.toAccountId(), -1);
    }

    @Transactional
    public AccountDto createAccount(CreateAccountRequest req) {
        String name = req.name() == null ? "" : req.name().trim();
        if (name.isEmpty()) {
            throw new IllegalArgumentException("请输入账户名称");
        }
        String type = req.type() == null || req.type().isBlank() ? "other" : req.type().trim();
        String icon = req.icon() == null || req.icon().isBlank() ? type : req.icon().trim();

        Integer maxSort = jdbc.queryForObject(
                "SELECT COALESCE(MAX(sort_order), 0) FROM account WHERE ledger_id = ?",
                Integer.class,
                req.ledgerId()
        );
        int sortOrder = (maxSort == null ? 0 : maxSort) + 10;

        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(con -> {
            PreparedStatement ps = con.prepareStatement("""
                    INSERT INTO account (ledger_id, name, type, icon, balance, sort_order, is_archived)
                    VALUES (?, ?, ?, ?, 0, ?, 0)
                    """, Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1, req.ledgerId());
            ps.setString(2, name);
            ps.setString(3, type);
            ps.setString(4, icon);
            ps.setInt(5, sortOrder);
            return ps;
        }, keyHolder);

        long id = keyHolder.getKey().longValue();
        return findAccountById(id).orElseThrow();
    }

    private void validateCreate(CreateTransactionRequest req) {
        if (req.amount() <= 0) {
            throw new IllegalArgumentException("金额必须大于 0");
        }
        if (!List.of("expense", "income", "transfer").contains(req.type())) {
            throw new IllegalArgumentException("type 无效");
        }
        if (!"transfer".equals(req.type()) && req.categoryId() == null) {
            throw new IllegalArgumentException("请选择分类");
        }
        if ("transfer".equals(req.type())) {
            if (req.toAccountId() == null) {
                throw new IllegalArgumentException("请选择转入账户");
            }
            if (req.toAccountId() == req.accountId()) {
                throw new IllegalArgumentException("转入转出账户不能相同");
            }
        }
    }

    private void applyBalance(String type, long amount, long accountId, Long toAccountId, int direction) {
        if ("expense".equals(type)) {
            jdbc.update("UPDATE account SET balance = balance - ? WHERE id = ?", direction * amount, accountId);
        } else if ("income".equals(type)) {
            jdbc.update("UPDATE account SET balance = balance + ? WHERE id = ?", direction * amount, accountId);
        } else if ("transfer".equals(type) && toAccountId != null) {
            jdbc.update("UPDATE account SET balance = balance - ? WHERE id = ?", direction * amount, accountId);
            jdbc.update("UPDATE account SET balance = balance + ? WHERE id = ?", direction * amount, toAccountId);
        }
    }

    private Timestamp parseOccurredAt(String raw) {
        if (raw == null || raw.isBlank()) {
            return Timestamp.from(Instant.now());
        }
        String normalized = raw.trim().replace(' ', 'T');
        try {
            return Timestamp.from(Instant.parse(normalized.endsWith("Z") || normalized.contains("+")
                    ? normalized
                    : normalized + "Z"));
        } catch (Exception ignored) {
            // fall through
        }
        try {
            LocalDateTime ldt = LocalDateTime.parse(
                    raw.trim().replace('T', ' ').substring(0, Math.min(19, raw.trim().length())),
                    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
            );
            return Timestamp.valueOf(ldt);
        } catch (Exception e) {
            try {
                LocalDateTime ldt = LocalDateTime.parse(
                        raw.trim().replace('T', ' '),
                        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
                );
                return Timestamp.valueOf(ldt);
            } catch (Exception ex) {
                return Timestamp.from(Instant.now());
            }
        }
    }

    private TransactionDto mapTransaction(java.sql.ResultSet rs) throws java.sql.SQLException {
        long categoryId = rs.getLong("category_id");
        boolean categoryNull = rs.wasNull();
        long toAccountId = rs.getLong("to_account_id");
        boolean toNull = rs.wasNull();
        Timestamp occurred = rs.getTimestamp("occurred_at");
        String occurredAt = occurred == null
                ? null
                : occurred.toInstant().atOffset(ZoneOffset.UTC).toString();
        return new TransactionDto(
                rs.getLong("id"),
                rs.getLong("ledger_id"),
                rs.getLong("user_id"),
                rs.getString("type"),
                rs.getLong("amount"),
                categoryNull ? null : categoryId,
                rs.getLong("account_id"),
                toNull ? null : toAccountId,
                occurredAt,
                rs.getString("note")
        );
    }
}
