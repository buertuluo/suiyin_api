-- 碎银记账本：初始化演示用户 + 默认账本 + 默认账户
-- 前置：已执行本目录 schema.sql
-- 用法（在 suiyin-api 项目下，同一会话内可接着灌分类）：
--   SOURCE sql/seed_bootstrap.sql;
--   SOURCE sql/seed_categories.sql;

USE suiyin_ledger;

-- ========== 可按需修改 ==========
SET @demo_openid   = 'demo_openid_suiyin';
SET @demo_nickname = '碎银用户';
SET @ledger_name   = '日常碎银';

-- ---------------------------------------------------------------------------
-- 1. 用户（按 openid 幂等：已存在则复用）
-- ---------------------------------------------------------------------------
INSERT INTO `user` (`openid`, `nickname`, `avatar_url`)
SELECT @demo_openid, @demo_nickname, ''
FROM DUAL
WHERE NOT EXISTS (
  SELECT 1 FROM `user` WHERE `openid` = @demo_openid
);

SET @user_id = (
  SELECT `id` FROM `user` WHERE `openid` = @demo_openid LIMIT 1
);

-- ---------------------------------------------------------------------------
-- 2. 账本（同 owner + 同名且未删除则复用，避免重复初始化）
-- ---------------------------------------------------------------------------
INSERT INTO `ledger` (`name`, `owner_user_id`, `currency`)
SELECT @ledger_name, @user_id, 'CNY'
FROM DUAL
WHERE NOT EXISTS (
  SELECT 1 FROM `ledger`
  WHERE `owner_user_id` = @user_id
    AND `name` = @ledger_name
    AND `deleted_at` IS NULL
);

SET @ledger_id = (
  SELECT `id` FROM `ledger`
  WHERE `owner_user_id` = @user_id
    AND `name` = @ledger_name
    AND `deleted_at` IS NULL
  LIMIT 1
);

-- ---------------------------------------------------------------------------
-- 3. 账本成员（owner）
-- ---------------------------------------------------------------------------
INSERT INTO `ledger_member` (`ledger_id`, `user_id`, `role`)
SELECT @ledger_id, @user_id, 'owner'
FROM DUAL
WHERE NOT EXISTS (
  SELECT 1 FROM `ledger_member`
  WHERE `ledger_id` = @ledger_id AND `user_id` = @user_id
);

-- ---------------------------------------------------------------------------
-- 4. 默认资金账户（仅作初始模板；用户可增删改自定义账户）
-- ---------------------------------------------------------------------------
INSERT INTO `account` (`ledger_id`, `name`, `type`, `icon`, `balance`, `sort_order`, `is_archived`)
SELECT @ledger_id, v.name, v.type, v.icon, 0, v.sort_order, 0
FROM (
  SELECT '现金'   AS name, 'cash'   AS type, 'cash'   AS icon, 10 AS sort_order UNION ALL
  SELECT '微信',          'wechat',          'wechat',          20 UNION ALL
  SELECT '支付宝',        'alipay',          'alipay',          30 UNION ALL
  SELECT '银行卡',        'bank',            'bank',            40
) AS v
WHERE NOT EXISTS (
  SELECT 1 FROM `account` a
  WHERE a.`ledger_id` = @ledger_id
    AND a.`name` = v.name
    AND a.`deleted_at` IS NULL
);

-- 自定义账户示例（默认注释；需要可取消注释）
-- INSERT INTO `account` (`ledger_id`, `name`, `type`, `icon`, `balance`, `sort_order`)
-- VALUES (@ledger_id, '饭卡', 'meal_card', 'card', 0, 50);

-- ---------------------------------------------------------------------------
-- 结果摘要（会话内保留 @user_id / @ledger_id，可供 seed_categories.sql 使用）
-- ---------------------------------------------------------------------------
SELECT
  @user_id   AS user_id,
  @ledger_id AS ledger_id,
  @ledger_name AS ledger_name,
  (SELECT COUNT(*) FROM `account` WHERE `ledger_id` = @ledger_id AND `deleted_at` IS NULL) AS account_count;
