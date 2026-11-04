-- Jeu de donnees de reference pour l'etude de cas
-- Benchmark de performances des Web Services REST
-- Cible : MariaDB 10.4 (adapte de PostgreSQL, types realignes)

DROP DATABASE IF EXISTS benchmark_rest;
CREATE DATABASE benchmark_rest CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE benchmark_rest;

CREATE TABLE category (
  id         BIGINT AUTO_INCREMENT PRIMARY KEY,
  code       VARCHAR(32)  NOT NULL UNIQUE,
  name       VARCHAR(128) NOT NULL,
  updated_at TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB;

CREATE TABLE item (
  id          BIGINT AUTO_INCREMENT PRIMARY KEY,
  sku         VARCHAR(64)   NOT NULL UNIQUE,
  name        VARCHAR(128)  NOT NULL,
  price       DECIMAL(10,2) NOT NULL,
  stock       INT           NOT NULL,
  category_id BIGINT        NOT NULL,
  updated_at  TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT fk_item_category FOREIGN KEY (category_id) REFERENCES category(id)
) ENGINE=InnoDB;

CREATE INDEX idx_item_category   ON item(category_id);
CREATE INDEX idx_item_updated_at ON item(updated_at);
