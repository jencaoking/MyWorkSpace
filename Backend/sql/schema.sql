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
