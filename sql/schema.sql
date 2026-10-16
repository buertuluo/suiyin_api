-- 碎银记账本 MySQL Schema (V1)
-- Charset: utf8mb4 | Engine: InnoDB
-- 金额单位：分（BIGINT）

CREATE DATABASE IF NOT EXISTS suiyin_ledger
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_unicode_ci;

USE suiyin_ledger;

SET NAMES utf8mb4;

-- ---------------------------------------------------------------------------
-- 1. user — 用户
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `user` (
  `id`           BIGINT       NOT NULL AUTO_INCREMENT,
  `openid`       VARCHAR(64)  NULL COMMENT '微信 openid，预留登录',
  `nickname`     VARCHAR(64)  NOT NULL DEFAULT '',
  `avatar_url`   VARCHAR(512) NOT NULL DEFAULT '',
  `created_at`   DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_at`   DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_openid` (`openid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户';

-- ---------------------------------------------------------------------------
-- 2. ledger — 账本
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `ledger` (
  `id`             BIGINT       NOT NULL AUTO_INCREMENT,
  `name`           VARCHAR(64)  NOT NULL COMMENT '账本名称，如日常碎银',
  `owner_user_id`  BIGINT       NOT NULL,
  `currency`       CHAR(3)      NOT NULL DEFAULT 'CNY',
  `created_at`     DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_at`     DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  `deleted_at`     DATETIME(3)  NULL,
  PRIMARY KEY (`id`),
  KEY `idx_ledger_owner` (`owner_user_id`),
  CONSTRAINT `fk_ledger_owner`
    FOREIGN KEY (`owner_user_id`) REFERENCES `user` (`id`)
    ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='账本';

-- ---------------------------------------------------------------------------
-- 3. ledger_member — 账本成员（V1 可只插本人，为共享预留）
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `ledger_member` (
  `id`         BIGINT       NOT NULL AUTO_INCREMENT,
  `ledger_id`  BIGINT       NOT NULL,
  `user_id`    BIGINT       NOT NULL,
  `role`       ENUM('owner', 'editor', 'viewer') NOT NULL DEFAULT 'editor',
  `joined_at`  DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_ledger_member` (`ledger_id`, `user_id`),
  KEY `idx_ledger_member_user` (`user_id`),
  CONSTRAINT `fk_ledger_member_ledger`
    FOREIGN KEY (`ledger_id`) REFERENCES `ledger` (`id`)
    ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_ledger_member_user`
    FOREIGN KEY (`user_id`) REFERENCES `user` (`id`)
    ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='账本成员';

-- ---------------------------------------------------------------------------
-- 4. account — 资金账户（用户可自定义名称/类型/图标；seed 仅提供默认项）
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `account` (
  `id`           BIGINT       NOT NULL AUTO_INCREMENT,
  `ledger_id`    BIGINT       NOT NULL,
  `name`         VARCHAR(64)  NOT NULL COMMENT '账户名称，用户自定义，如「招商储蓄」「饭卡」',
  `type`         VARCHAR(32)  NOT NULL DEFAULT 'other' COMMENT '账户类型标识，推荐 cash/wechat/alipay/bank/other，也可自定义如 meal_card',
  `icon`         VARCHAR(64)  NOT NULL DEFAULT '' COMMENT '展示图标，用户可选/自定义',
  `balance`      BIGINT       NOT NULL DEFAULT 0 COMMENT '余额，单位：分',
  `sort_order`   INT          NOT NULL DEFAULT 0,
  `is_archived`  TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '归档后不再出现在记账选项中',
  `created_at`   DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_at`   DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  `deleted_at`   DATETIME(3)  NULL,
  PRIMARY KEY (`id`),
  KEY `idx_account_ledger` (`ledger_id`),
  CONSTRAINT `fk_account_ledger`
    FOREIGN KEY (`ledger_id`) REFERENCES `ledger` (`id`)
    ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='资金账户（可自定义）';

-- ---------------------------------------------------------------------------
-- 5. category — 分类（支持一级父子）
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `category` (
  `id`           BIGINT       NOT NULL AUTO_INCREMENT,
  `ledger_id`    BIGINT       NOT NULL,
  `parent_id`    BIGINT       NULL COMMENT 'NULL 表示一级分类',
  `name`         VARCHAR(64)  NOT NULL,
  `type`         ENUM('expense', 'income') NOT NULL,
  `icon`         VARCHAR(64)  NOT NULL DEFAULT '',
  `sort_order`   INT          NOT NULL DEFAULT 0,
  `is_system`    TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '系统预置分类',
  `created_at`   DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_at`   DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  `deleted_at`   DATETIME(3)  NULL,
  PRIMARY KEY (`id`),
  KEY `idx_category_ledger` (`ledger_id`),
  KEY `idx_category_parent` (`parent_id`),
  CONSTRAINT `fk_category_ledger`
    FOREIGN KEY (`ledger_id`) REFERENCES `ledger` (`id`)
    ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_category_parent`
    FOREIGN KEY (`parent_id`) REFERENCES `category` (`id`)
    ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='收支分类';

-- ---------------------------------------------------------------------------
-- 6. transaction — 流水（核心）
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `transaction` (
  `id`             BIGINT       NOT NULL AUTO_INCREMENT,
  `ledger_id`      BIGINT       NOT NULL,
  `user_id`        BIGINT       NOT NULL COMMENT '记账人',
  `type`           ENUM('expense', 'income', 'transfer') NOT NULL,
  `amount`         BIGINT       NOT NULL COMMENT '金额，单位：分，始终为正',
  `category_id`    BIGINT       NULL COMMENT '转账可为空',
  `account_id`     BIGINT       NOT NULL COMMENT '支出/收入账户；转账为转出账户',
  `to_account_id`  BIGINT       NULL COMMENT '仅转账：转入账户',
  `occurred_at`    DATETIME(3)  NOT NULL COMMENT '业务发生时间',
  `note`           VARCHAR(500) NOT NULL DEFAULT '',
  `created_at`     DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_at`     DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  `deleted_at`     DATETIME(3)  NULL,
  PRIMARY KEY (`id`),
  KEY `idx_txn_ledger_occurred` (`ledger_id`, `occurred_at`),
  KEY `idx_txn_ledger_category` (`ledger_id`, `category_id`),
  KEY `idx_txn_ledger_account` (`ledger_id`, `account_id`),
  KEY `idx_txn_user` (`user_id`),
  KEY `idx_txn_to_account` (`to_account_id`),
  CONSTRAINT `fk_txn_ledger`
    FOREIGN KEY (`ledger_id`) REFERENCES `ledger` (`id`)
    ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_txn_user`
    FOREIGN KEY (`user_id`) REFERENCES `user` (`id`)
    ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_txn_category`
    FOREIGN KEY (`category_id`) REFERENCES `category` (`id`)
    ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_txn_account`
    FOREIGN KEY (`account_id`) REFERENCES `account` (`id`)
    ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_txn_to_account`
    FOREIGN KEY (`to_account_id`) REFERENCES `account` (`id`)
    ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='流水';

-- ---------------------------------------------------------------------------
-- 7. budget — 月预算
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `budget` (
  `id`           BIGINT       NOT NULL AUTO_INCREMENT,
  `ledger_id`    BIGINT       NOT NULL,
  `category_id`  BIGINT       NULL COMMENT 'NULL 表示整账本总预算',
  `year_month`   CHAR(7)      NOT NULL COMMENT '格式 YYYY-MM，如 2026-10',
  `amount`       BIGINT       NOT NULL COMMENT '预算金额，单位：分',
  `created_at`   DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_at`   DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  `deleted_at`   DATETIME(3)  NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_budget_ledger_category_month` (`ledger_id`, `category_id`, `year_month`),
  KEY `idx_budget_category` (`category_id`),
  CONSTRAINT `fk_budget_ledger`
    FOREIGN KEY (`ledger_id`) REFERENCES `ledger` (`id`)
    ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_budget_category`
    FOREIGN KEY (`category_id`) REFERENCES `category` (`id`)
    ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='月预算';

-- ---------------------------------------------------------------------------
-- 8. tag / transaction_tag — 标签（多对多）
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `tag` (
  `id`           BIGINT       NOT NULL AUTO_INCREMENT,
  `ledger_id`    BIGINT       NOT NULL,
  `name`         VARCHAR(32)  NOT NULL,
  `created_at`   DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_at`   DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  `deleted_at`   DATETIME(3)  NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_tag_ledger_name` (`ledger_id`, `name`),
  CONSTRAINT `fk_tag_ledger`
    FOREIGN KEY (`ledger_id`) REFERENCES `ledger` (`id`)
    ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='标签';

CREATE TABLE IF NOT EXISTS `transaction_tag` (
  `transaction_id` BIGINT NOT NULL,
  `tag_id`         BIGINT NOT NULL,
  PRIMARY KEY (`transaction_id`, `tag_id`),
  KEY `idx_txn_tag_tag` (`tag_id`),
  CONSTRAINT `fk_txn_tag_transaction`
    FOREIGN KEY (`transaction_id`) REFERENCES `transaction` (`id`)
    ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_txn_tag_tag`
    FOREIGN KEY (`tag_id`) REFERENCES `tag` (`id`)
    ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='流水-标签关联';
