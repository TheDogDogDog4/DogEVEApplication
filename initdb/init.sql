-- 用户表
CREATE TABLE "users" (
                         user_id BIGSERIAL PRIMARY KEY,
                         username VARCHAR(100) NOT NULL,
                         password VARCHAR(255) NOT NULL,
                         email VARCHAR(100),
                         create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                         esi_refresh_token TEXT   -- 加密后的刷新令牌
);

-- EVE 角色表
CREATE TABLE eve_character (
                               character_id BIGSERIAL PRIMARY KEY,
                               user_id BIGINT NOT NULL REFERENCES "users"(user_id) ON DELETE CASCADE,
                               character_name VARCHAR(100) NOT NULL,
                               expires_on TIMESTAMP,
                               scopes TEXT,
                               token_type VARCHAR(50),
                               character_owner_hash VARCHAR(255),
                               intellectual_property TEXT
);

-- 为常用查询字段创建索引
CREATE INDEX idx_eve_character_user_id ON eve_character(user_id);
CREATE INDEX idx_user_username ON "users"(username);