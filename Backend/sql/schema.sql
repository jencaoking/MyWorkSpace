-- 自律工作台 数据库结构（MySQL 8.0）
-- 与 Android 端 Room 实体字段保持一致；统一四字段见各表注释。

CREATE DATABASE IF NOT EXISTS mywork CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE mywork;

CREATE TABLE IF NOT EXISTS tasks (
    id            CHAR(36)     PRIMARY KEY,
    title         VARCHAR(255) NOT NULL DEFAULT '',
    content       TEXT,
    category_id   CHAR(36)     DEFAULT '',
    status        TINYINT      NOT NULL DEFAULT 0,   -- 0待办 1已完成 2进行中
    priority      TINYINT      NOT NULL DEFAULT 2,   -- 1高 2中 3低
    due_date      BIGINT       DEFAULT NULL,
    reminder_time BIGINT       DEFAULT NULL,
    repeat_rule   VARCHAR(255) DEFAULT NULL,
    task_type     TINYINT      NOT NULL DEFAULT 0,   -- 0一次性 1循环 2长期目标
    repeat_type   TINYINT      NOT NULL DEFAULT 0,   -- 0无 1每日 2每周 3每月
    repeat_days   VARCHAR(32)  DEFAULT NULL,         -- 每周循环时的周几位，如 1,3,5
    parent_goal_id CHAR(36)    DEFAULT '',           -- 长期目标下的子任务
    created_at    BIGINT       NOT NULL,
    updated_at    BIGINT       NOT NULL,
    last_modified BIGINT       NOT NULL,
    is_deleted    TINYINT(1)   NOT NULL DEFAULT 0,
    device_id     VARCHAR(64)  NOT NULL DEFAULT '',
    needs_sync    TINYINT(1)   NOT NULL DEFAULT 1,
    INDEX idx_status (status),
    INDEX idx_due (due_date),
    INDEX idx_task_type (task_type),
    INDEX idx_sync (needs_sync)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS task_checkins (
    id            CHAR(36)    PRIMARY KEY,
    task_id       CHAR(36)    NOT NULL,
    checkin_date  DATE        NOT NULL,
    checkin_time  BIGINT      NOT NULL,
    note          TEXT,
    last_modified BIGINT      NOT NULL,
    is_deleted    TINYINT(1)  NOT NULL DEFAULT 0,
    device_id     VARCHAR(64) NOT NULL DEFAULT '',
    needs_sync    TINYINT(1)  NOT NULL DEFAULT 1,
    INDEX idx_task (task_id),
    INDEX idx_date (checkin_date),
    INDEX idx_sync (needs_sync)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS categories (
    id         CHAR(36)    PRIMARY KEY,
    name       VARCHAR(64) NOT NULL DEFAULT '',
    color      VARCHAR(16) DEFAULT '#2E7D62',
    sort_order INT         NOT NULL DEFAULT 0,
    is_system  TINYINT(1)  NOT NULL DEFAULT 0,
    last_modified BIGINT   NOT NULL,
    is_deleted    TINYINT(1) NOT NULL DEFAULT 0,
    device_id     VARCHAR(64) NOT NULL DEFAULT '',
    needs_sync    TINYINT(1)  NOT NULL DEFAULT 1
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS notes (
    id          CHAR(36)    PRIMARY KEY,
    title       VARCHAR(255) NOT NULL DEFAULT '',
    content     MEDIUMTEXT,
    is_pinned   TINYINT(1)  NOT NULL DEFAULT 0,
    is_favorite TINYINT(1)  NOT NULL DEFAULT 0,
    created_at  BIGINT      NOT NULL,
    updated_at  BIGINT      NOT NULL,
    last_modified BIGINT    NOT NULL,
    is_deleted    TINYINT(1) NOT NULL DEFAULT 0,
    device_id     VARCHAR(64) NOT NULL DEFAULT '',
    needs_sync    TINYINT(1)  NOT NULL DEFAULT 1,
    INDEX idx_pin (is_pinned),
    INDEX idx_sync (needs_sync),
    -- 阶段3：全文索引（ngram 解析器支持中文分词；MySQL 5.7.6+ / 8.0）
    FULLTEXT INDEX ft_title_content (title, content) WITH PARSER ngram
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 已建库升级用（表已存在时执行）：
-- ALTER TABLE notes ADD FULLTEXT INDEX ft_title_content (title, content) WITH PARSER ngram;

CREATE TABLE IF NOT EXISTS sport_records (
    id           CHAR(36) PRIMARY KEY,
    type         VARCHAR(64) DEFAULT '',
    duration_min INT         NOT NULL DEFAULT 0,
    distance_km  FLOAT       DEFAULT NULL,
    calories     INT         DEFAULT NULL,
    record_date  BIGINT      NOT NULL,
    note         TEXT,
    last_modified BIGINT     NOT NULL,
    is_deleted    TINYINT(1) NOT NULL DEFAULT 0,
    device_id     VARCHAR(64) NOT NULL DEFAULT '',
    needs_sync    TINYINT(1)  NOT NULL DEFAULT 1,
    INDEX idx_date (record_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS english_words (
    id           CHAR(36) PRIMARY KEY,
    word         VARCHAR(128) NOT NULL DEFAULT '',
    phonetic     VARCHAR(128) DEFAULT '',
    meaning      TEXT,
    example      TEXT,
    audio_path   TEXT,
    familiarity  TINYINT      NOT NULL DEFAULT 0,
    next_review  BIGINT       NOT NULL DEFAULT 0,
    last_modified BIGINT      NOT NULL,
    is_deleted    TINYINT(1)  NOT NULL DEFAULT 0,
    device_id     VARCHAR(64) NOT NULL DEFAULT '',
    needs_sync    TINYINT(1)  NOT NULL DEFAULT 1,
    INDEX idx_review (next_review)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS movie_books (
    id         CHAR(36) PRIMARY KEY,
    type       VARCHAR(16) NOT NULL DEFAULT 'movie',
    title      VARCHAR(255) NOT NULL DEFAULT '',
    tmdb_id    VARCHAR(64) DEFAULT '',
    status     VARCHAR(16) NOT NULL DEFAULT 'want',
    rating     FLOAT       NOT NULL DEFAULT 0,
    poster_url VARCHAR(512) DEFAULT '',
    note       TEXT,
    last_modified BIGINT   NOT NULL,
    is_deleted    TINYINT(1) NOT NULL DEFAULT 0,
    device_id     VARCHAR(64) NOT NULL DEFAULT '',
    needs_sync    TINYINT(1)  NOT NULL DEFAULT 1,
    INDEX idx_type (type),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS health_records (
    id          CHAR(36) PRIMARY KEY,
    type        VARCHAR(64) NOT NULL DEFAULT '',
    value       FLOAT       NOT NULL DEFAULT 0,
    unit        VARCHAR(16) DEFAULT '',
    record_time BIGINT      NOT NULL,
    note        TEXT,
    last_modified BIGINT    NOT NULL,
    is_deleted    TINYINT(1) NOT NULL DEFAULT 0,
    device_id     VARCHAR(64) NOT NULL DEFAULT '',
    needs_sync    TINYINT(1)  NOT NULL DEFAULT 1,
    INDEX idx_time (record_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS account_records (
    id          CHAR(36) PRIMARY KEY,
    type        VARCHAR(16) NOT NULL DEFAULT 'expense',
    category    VARCHAR(64) DEFAULT '',
    amount      FLOAT       NOT NULL DEFAULT 0,
    currency    VARCHAR(8)  NOT NULL DEFAULT 'CNY',
    record_date BIGINT      NOT NULL,
    note        TEXT,
    last_modified BIGINT    NOT NULL,
    is_deleted    TINYINT(1) NOT NULL DEFAULT 0,
    device_id     VARCHAR(64) NOT NULL DEFAULT '',
    needs_sync    TINYINT(1)  NOT NULL DEFAULT 1,
    INDEX idx_date (record_date),
    INDEX idx_type (type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 每日未完成作业归档（id 为确定性主键 taskId_YYYY-MM-DD，客户端/服务端归档幂等收敛）
CREATE TABLE IF NOT EXISTS daily_pending_log (
    id                VARCHAR(64)  PRIMARY KEY,          -- taskId_YYYY-MM-DD
    task_id           CHAR(36)     NOT NULL,
    task_title        VARCHAR(255) NOT NULL DEFAULT '',
    category_name     VARCHAR(64)  DEFAULT '',
    priority          TINYINT      NOT NULL DEFAULT 2,   -- 1高 2中 3低
    original_due_date BIGINT       NOT NULL,             -- 原截止时间（毫秒）
    log_date          DATE         NOT NULL,             -- 作业产生日（原截止日）
    disposition       VARCHAR(16)  NOT NULL DEFAULT 'pending', -- pending|completed|rescheduled|abandoned
    disposed_at       BIGINT       DEFAULT NULL,         -- 处置时间（毫秒）
    new_due_date      BIGINT       DEFAULT NULL,         -- 改期后的新截止时间（毫秒）
    created_at        BIGINT       NOT NULL,
    last_modified     BIGINT       NOT NULL,
    is_deleted        TINYINT(1)   NOT NULL DEFAULT 0,
    device_id         VARCHAR(64)  NOT NULL DEFAULT '',
    needs_sync        TINYINT(1)   NOT NULL DEFAULT 1,
    UNIQUE KEY uk_task_date (task_id, log_date),
    INDEX idx_log_date (log_date),
    INDEX idx_disposition (disposition),
    INDEX idx_dpl_sync (needs_sync)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 用户设置镜像（云端，对应本地 DataStore 的主题/板块开关）
CREATE TABLE IF NOT EXISTS user_settings (
    id           CHAR(36) PRIMARY KEY,   -- 固定为 'local'
    theme        VARCHAR(16) NOT NULL DEFAULT 'system',
    module_toggles JSON,
    language     VARCHAR(8)  NOT NULL DEFAULT 'zh-CN',
    created_at   BIGINT      NOT NULL,
    updated_at   BIGINT      NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 后台操作审计日志（编辑/删除记录）
CREATE TABLE IF NOT EXISTS admin_audit_log (
    id          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    actor       VARCHAR(64) NOT NULL DEFAULT 'admin',
    action      VARCHAR(16) NOT NULL COMMENT 'update|delete',
    table_name  VARCHAR(64) NOT NULL,
    row_id      CHAR(36)     NOT NULL,
    change_mode VARCHAR(8)   NULL COMMENT 'soft|hard',
    changes     TEXT         NULL COMMENT 'JSON: 实际变更的字段',
    ip          VARCHAR(45)  NULL,
    user_agent  TEXT         NULL,
    created_at  BIGINT       NOT NULL,
    PRIMARY KEY (id),
    KEY idx_table_row (table_name, row_id),
    KEY idx_created (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 通用键值配置（后台管理中填写的第三方 API 密钥等；仅服务端可读，不下发客户端）
CREATE TABLE IF NOT EXISTS app_config (
    cfg_key   VARCHAR(64) NOT NULL,
    cfg_value TEXT,
    PRIMARY KEY (cfg_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ===== 工具箱模块（8 个独立模块，统一同步字段；AI 调用次数走 app_config 的 ai_usage_YYYY-MM-DD） =====

-- 计算器历史
CREATE TABLE IF NOT EXISTS calc_history (
    id           CHAR(36)     NOT NULL,
    expr         VARCHAR(255) NOT NULL,
    result       TEXT         NOT NULL,
    created_at   BIGINT       NOT NULL,
    last_modified BIGINT      NOT NULL,
    is_deleted   TINYINT(1)   NOT NULL DEFAULT 0,
    device_id    VARCHAR(64)  NOT NULL DEFAULT '',
    needs_sync   TINYINT(1)   NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    INDEX idx_calc_device (device_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 扫码历史
CREATE TABLE IF NOT EXISTS qr_scan_history (
    id          CHAR(36)     NOT NULL,
    content     TEXT         NOT NULL,
    format      VARCHAR(32)  NOT NULL DEFAULT 'QR',
    note        VARCHAR(255) DEFAULT '',
    created_at  BIGINT       NOT NULL,
    last_modified BIGINT     NOT NULL,
    is_deleted  TINYINT(1)   NOT NULL DEFAULT 0,
    device_id   VARCHAR(64)  NOT NULL DEFAULT '',
    needs_sync  TINYINT(1)   NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    INDEX idx_qr_device (device_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 倒计时事件
CREATE TABLE IF NOT EXISTS countdown_events (
    id           CHAR(36)    NOT NULL,
    title        VARCHAR(255) NOT NULL,
    target_time  BIGINT      NOT NULL,             -- 目标时间（毫秒）
    remark       VARCHAR(255) DEFAULT '',
    created_at   BIGINT      NOT NULL,
    last_modified BIGINT     NOT NULL,
    is_deleted   TINYINT(1)  NOT NULL DEFAULT 0,
    device_id    VARCHAR(64) NOT NULL DEFAULT '',
    needs_sync   TINYINT(1)  NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    INDEX idx_cd_device (device_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 习惯计划（长期目标）
CREATE TABLE IF NOT EXISTS habit_plans (
    id           CHAR(36)    NOT NULL,
    title        VARCHAR(255) NOT NULL,
    description  TEXT,
    period       TINYINT     NOT NULL DEFAULT 0,   -- 0未设 1周 2月 3季度 4年
    start_date   BIGINT      NOT NULL,
    created_at   BIGINT      NOT NULL,
    last_modified BIGINT     NOT NULL,
    is_deleted   TINYINT(1)  NOT NULL DEFAULT 0,
    device_id    VARCHAR(64) NOT NULL DEFAULT '',
    needs_sync   TINYINT(1)  NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    INDEX idx_hp_device (device_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 习惯项（隶属于计划）
CREATE TABLE IF NOT EXISTS habits (
    id           CHAR(36)    NOT NULL,
    plan_id      CHAR(36)    NOT NULL,
    title        VARCHAR(255) NOT NULL,
    frequency    TINYINT     NOT NULL DEFAULT 1,   -- 1每天 2每周 3每月
    days         VARCHAR(64) DEFAULT '',          -- 周几位图 "1,3,5"
    time_min     INT         NOT NULL DEFAULT 0,  -- 提醒分钟(0-1439)，0 表示不提醒
    created_at   BIGINT      NOT NULL,
    last_modified BIGINT     NOT NULL,
    is_deleted   TINYINT(1)  NOT NULL DEFAULT 0,
    device_id    VARCHAR(64) NOT NULL DEFAULT '',
    needs_sync   TINYINT(1)  NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    INDEX idx_habit_plan (plan_id),
    INDEX idx_habit_device (device_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 习惯打卡
CREATE TABLE IF NOT EXISTS habit_checkins (
    id           CHAR(36)    NOT NULL,
    habit_id     CHAR(36)    NOT NULL,
    date         DATE        NOT NULL,
    created_at   BIGINT      NOT NULL,
    last_modified BIGINT     NOT NULL,
    is_deleted   TINYINT(1)  NOT NULL DEFAULT 0,
    device_id    VARCHAR(64) NOT NULL DEFAULT '',
    needs_sync   TINYINT(1)  NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_habit_date (habit_id, date),
    INDEX idx_hc_device (device_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 闪卡牌组
CREATE TABLE IF NOT EXISTS flashcard_decks (
    id           CHAR(36)    NOT NULL,
    name         VARCHAR(255) NOT NULL,
    description  TEXT,
    created_at   BIGINT      NOT NULL,
    last_modified BIGINT     NOT NULL,
    is_deleted   TINYINT(1)  NOT NULL DEFAULT 0,
    device_id    VARCHAR(64) NOT NULL DEFAULT '',
    needs_sync   TINYINT(1)  NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    INDEX idx_fd_device (device_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 闪卡
CREATE TABLE IF NOT EXISTS flashcards (
    id            CHAR(36)    NOT NULL,
    deck_id       CHAR(36)    NOT NULL,
    front         TEXT        NOT NULL,
    back          TEXT        NOT NULL,
    next_review   BIGINT      NOT NULL DEFAULT 0,   -- 下次复习时间（毫秒）
    interval_days INT         NOT NULL DEFAULT 0,
    ease          FLOAT       NOT NULL DEFAULT 2.5,
    created_at    BIGINT      NOT NULL,
    last_modified BIGINT      NOT NULL,
    is_deleted    TINYINT(1)  NOT NULL DEFAULT 0,
    device_id     VARCHAR(64) NOT NULL DEFAULT '',
    needs_sync    TINYINT(1)  NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    INDEX idx_fc_deck (deck_id),
    INDEX idx_fc_device (device_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 灵感/语录收藏
CREATE TABLE IF NOT EXISTS inspiration_items (
    id           CHAR(36)    NOT NULL,
    content      TEXT        NOT NULL,
    author       VARCHAR(128) DEFAULT '',
    source       VARCHAR(128) DEFAULT '',
    tags         VARCHAR(255) DEFAULT '',
    favorite     TINYINT(1)  NOT NULL DEFAULT 0,
    created_at   BIGINT      NOT NULL,
    last_modified BIGINT     NOT NULL,
    is_deleted   TINYINT(1)  NOT NULL DEFAULT 0,
    device_id    VARCHAR(64) NOT NULL DEFAULT '',
    needs_sync   TINYINT(1)  NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    INDEX idx_insp_device (device_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 快递包裹
CREATE TABLE IF NOT EXISTS express_packages (
    id            CHAR(36)    NOT NULL,
    company       VARCHAR(32) NOT NULL DEFAULT '',  -- 快递公司编码
    company_name  VARCHAR(64) NOT NULL DEFAULT '',
    tracking_no   VARCHAR(64) NOT NULL DEFAULT '',
    goods         VARCHAR(255) DEFAULT '',
    current_status VARCHAR(255) DEFAULT '',
    last_update   BIGINT      DEFAULT 0,
    created_at    BIGINT      NOT NULL,
    last_modified BIGINT      NOT NULL,
    is_deleted    TINYINT(1)  NOT NULL DEFAULT 0,
    device_id     VARCHAR(64) NOT NULL DEFAULT '',
    needs_sync    TINYINT(1)  NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    INDEX idx_express_device (device_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 单位换算缓存（服务端加速，本地计算为主）
CREATE TABLE IF NOT EXISTS unit_conversion_cache (
    id          CHAR(36)    NOT NULL,
    from_unit   VARCHAR(32) NOT NULL,
    to_unit     VARCHAR(32) NOT NULL,
    value       DECIMAL(24,8) NOT NULL,
    result      DECIMAL(24,8) NOT NULL,
    created_at  BIGINT      NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_units (from_unit, to_unit, value)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 设备用户注册表（后台用户管理：以客户端生成的 device_id 作为唯一用户标识，记录封禁状态与备注）
CREATE TABLE IF NOT EXISTS device_users (
    device_id   VARCHAR(64) NOT NULL,
    status      VARCHAR(16) NOT NULL DEFAULT 'active',  -- active | banned
    note        VARCHAR(255) DEFAULT '',
    created_at  BIGINT      NOT NULL,
    updated_at  BIGINT      NOT NULL,
    PRIMARY KEY (device_id),
    KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
