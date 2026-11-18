ALTER TABLE users
ADD COLUMN token VARCHAR(100),
ADD COLUMN token_expired_at BIGINT,
ADD CONSTRAINT fk_token_unique UNIQUE (token);