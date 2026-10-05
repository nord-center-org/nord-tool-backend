-- 002_arquivo_armazenado.sql — armazenamento de arquivos (E06). Idempotente.
-- Provedor provisório: POSTGRES (bytes em bin_conteudo). Provedor definitivo ainda
-- não decidido (Nicolas + Nicolei); a tabela já prevê nm_provedor/cd_referencia.

CREATE TABLE IF NOT EXISTS arquivo_armazenado (
  id_arquivo        BIGSERIAL PRIMARY KEY,
  nm_provedor       VARCHAR(20)  NOT NULL DEFAULT 'POSTGRES',  -- POSTGRES | (futuro) DRIVE | S3 | ...
  cd_referencia     VARCHAR(500),                              -- id/chave no provedor externo
  nm_arquivo        VARCHAR(255) NOT NULL,
  nm_content_type   VARCHAR(100) NOT NULL,
  nr_tamanho_bytes  BIGINT       NOT NULL,
  nm_hash_sha256    CHAR(64)     NOT NULL,
  bin_conteudo      BYTEA,                                     -- só no provedor POSTGRES
  dh_criacao        TIMESTAMP    NOT NULL DEFAULT now()
);
