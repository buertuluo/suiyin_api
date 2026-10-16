-- 碎银记账本：默认收支分类种子数据
-- 用法：
--   1) 推荐：先 SOURCE seed_bootstrap.sql（会设置 @ledger_id），再 SOURCE 本脚本
--   2) 手动：SET @ledger_id = <账本ID>; 再 SOURCE 本脚本
-- 若会话中未设置 @ledger_id，默认使用 1

USE suiyin_ledger;

SET @ledger_id = IFNULL(@ledger_id, 1);

-- 安全校验：账本必须存在
SELECT IF(
  EXISTS (SELECT 1 FROM `ledger` WHERE `id` = @ledger_id AND `deleted_at` IS NULL),
  'OK',
  'ERROR: ledger_id 不存在或已删除，请先创建账本并设置 @ledger_id'
) AS seed_check;

-- ---------------------------------------------------------------------------
-- 支出分类（一级）
-- ---------------------------------------------------------------------------
INSERT INTO `category` (`ledger_id`, `parent_id`, `name`, `type`, `icon`, `sort_order`, `is_system`)
VALUES
  (@ledger_id, NULL, '餐饮',   'expense', 'food',       10, 1),
  (@ledger_id, NULL, '交通',   'expense', 'transport',  20, 1),
  (@ledger_id, NULL, '购物',   'expense', 'shopping',   30, 1),
  (@ledger_id, NULL, '住房',   'expense', 'home',       40, 1),
  (@ledger_id, NULL, '娱乐',   'expense', 'fun',        50, 1),
  (@ledger_id, NULL, '医疗',   'expense', 'medical',    60, 1),
  (@ledger_id, NULL, '教育',   'expense', 'education',  70, 1),
  (@ledger_id, NULL, '通讯',   'expense', 'phone',      80, 1),
  (@ledger_id, NULL, '日用',   'expense', 'daily',      90, 1),
  (@ledger_id, NULL, '人情',   'expense', 'gift',      100, 1),
  (@ledger_id, NULL, '其他支出', 'expense', 'other',    110, 1);

-- 支出二级（挂到对应一级）
SET @cat_food      = (SELECT `id` FROM `category` WHERE `ledger_id` = @ledger_id AND `name` = '餐饮' AND `parent_id` IS NULL AND `type` = 'expense' LIMIT 1);
SET @cat_transport = (SELECT `id` FROM `category` WHERE `ledger_id` = @ledger_id AND `name` = '交通' AND `parent_id` IS NULL AND `type` = 'expense' LIMIT 1);
SET @cat_shopping  = (SELECT `id` FROM `category` WHERE `ledger_id` = @ledger_id AND `name` = '购物' AND `parent_id` IS NULL AND `type` = 'expense' LIMIT 1);
SET @cat_home      = (SELECT `id` FROM `category` WHERE `ledger_id` = @ledger_id AND `name` = '住房' AND `parent_id` IS NULL AND `type` = 'expense' LIMIT 1);

INSERT INTO `category` (`ledger_id`, `parent_id`, `name`, `type`, `icon`, `sort_order`, `is_system`)
VALUES
  (@ledger_id, @cat_food,      '早餐',   'expense', 'food',       11, 1),
  (@ledger_id, @cat_food,      '午餐',   'expense', 'food',       12, 1),
  (@ledger_id, @cat_food,      '晚餐',   'expense', 'food',       13, 1),
  (@ledger_id, @cat_food,      '咖啡饮品', 'expense', 'food',     14, 1),
  (@ledger_id, @cat_transport, '公交地铁', 'expense', 'transport', 21, 1),
  (@ledger_id, @cat_transport, '打车',   'expense', 'transport',  22, 1),
  (@ledger_id, @cat_transport, '加油',   'expense', 'transport',  23, 1),
  (@ledger_id, @cat_shopping,  '服饰',   'expense', 'shopping',   31, 1),
  (@ledger_id, @cat_shopping,  '数码',   'expense', 'shopping',   32, 1),
  (@ledger_id, @cat_home,      '房租',   'expense', 'home',       41, 1),
  (@ledger_id, @cat_home,      '水电煤', 'expense', 'home',       42, 1);

-- ---------------------------------------------------------------------------
-- 收入分类（一级）
-- ---------------------------------------------------------------------------
INSERT INTO `category` (`ledger_id`, `parent_id`, `name`, `type`, `icon`, `sort_order`, `is_system`)
VALUES
  (@ledger_id, NULL, '工资',   'income', 'salary',  10, 1),
  (@ledger_id, NULL, '兼职',   'income', 'parttime', 20, 1),
  (@ledger_id, NULL, '红包',   'income', 'redpacket', 30, 1),
  (@ledger_id, NULL, '理财',   'income', 'invest',  40, 1),
  (@ledger_id, NULL, '报销',   'income', 'reimburse', 50, 1),
  (@ledger_id, NULL, '其他收入', 'income', 'other',  60, 1);
