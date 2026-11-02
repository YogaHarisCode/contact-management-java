CREATE TABLE users (
                       username         VARCHAR(100) NOT NULL,
                       password         VARCHAR(100) NOT NULL,
                       name             VARCHAR(100) NOT NULL,
                       token            VARCHAR(100),
                       token_expired_at BIGINT,
                       PRIMARY KEY (username),
                       CONSTRAINT unique_token UNIQUE (token)
) ENGINE = InnoDB;

CREATE TABLE contacts (
                          id         VARCHAR(100) NOT NULL,
                          username   VARCHAR(100) NOT NULL,
                          first_name VARCHAR(100) NOT NULL,
                          last_name  VARCHAR(100),
                          email      VARCHAR(200),
                          phone      VARCHAR(20),
                          PRIMARY KEY (id),
                          CONSTRAINT fk_users_contacts FOREIGN KEY (username) REFERENCES users (username)
) ENGINE = InnoDB;