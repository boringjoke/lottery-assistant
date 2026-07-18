CREATE TABLE lottery_users (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
    nickname VARCHAR(64) NULL COMMENT '用户昵称',
    avatar_url VARCHAR(512) NULL COMMENT '用户头像地址',
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE' COMMENT '用户状态：ACTIVE、DISABLED',
    last_login_time DATETIME NULL COMMENT '最近登录时间',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_lottery_users_status (status),
    KEY idx_lottery_users_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='彩票助手用户表';

CREATE TABLE lottery_user_credentials (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
    user_id BIGINT NOT NULL COMMENT '用户 ID，由业务代码保证关联有效',
    credential_type VARCHAR(32) NOT NULL COMMENT '凭证类型：USERNAME、PHONE、EMAIL',
    identifier VARCHAR(128) NOT NULL COMMENT '凭证标识，如用户名、手机号、邮箱',
    password_hash VARCHAR(255) NULL COMMENT '密码哈希，验证码登录时可为空',
    verified TINYINT(1) NOT NULL DEFAULT 0 COMMENT '手机号或邮箱是否已验证',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_lottery_user_credentials_type_identifier (credential_type, identifier),
    KEY idx_lottery_user_credentials_user_id (user_id),
    KEY idx_lottery_user_credentials_identifier (identifier)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='彩票助手用户登录凭证表';

CREATE TABLE lottery_user_oauth_accounts (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
    user_id BIGINT NOT NULL COMMENT '用户 ID，由业务代码保证关联有效',
    provider VARCHAR(32) NOT NULL COMMENT '第三方平台：WECHAT_MP、WECHAT_WEB、WECHAT_MINI_PROGRAM',
    open_id VARCHAR(128) NOT NULL COMMENT '第三方平台用户 OpenID',
    union_id VARCHAR(128) NULL COMMENT '微信开放平台 UnionID',
    nickname VARCHAR(64) NULL COMMENT '第三方平台昵称',
    avatar_url VARCHAR(512) NULL COMMENT '第三方平台头像地址',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_lottery_user_oauth_provider_open_id (provider, open_id),
    KEY idx_lottery_user_oauth_user_id (user_id),
    KEY idx_lottery_user_oauth_union_id (union_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='彩票助手用户第三方账号表';

CREATE TABLE lottery_user_roles (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
    user_id BIGINT NOT NULL COMMENT '用户 ID，由业务代码保证关联有效',
    role_code VARCHAR(32) NOT NULL COMMENT '角色编码：USER、ADMIN',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_lottery_user_roles_user_role (user_id, role_code),
    KEY idx_lottery_user_roles_user_id (user_id),
    KEY idx_lottery_user_roles_role_code (role_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='彩票助手用户角色表';
