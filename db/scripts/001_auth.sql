-- 001_auth.sql — autenticação (E04). Idempotente.
-- Aplicar ANTES do deploy do backend com a etapa E04.

CREATE TABLE IF NOT EXISTS perfil (
  id_perfil   BIGSERIAL PRIMARY KEY,
  cd_perfil   VARCHAR(30) NOT NULL UNIQUE,      -- 'ADMIN'
  nm_perfil   VARCHAR(80) NOT NULL
);

CREATE TABLE IF NOT EXISTS perfil_permissao (    -- ponto de extensão da hierarquia futura
  id_perfil   BIGINT NOT NULL REFERENCES perfil(id_perfil) ON DELETE CASCADE,
  cd_modulo   VARCHAR(40) NOT NULL,              -- '*' = tudo; depois: 'ENTREGA','CASAMENTO',...
  cd_acao     VARCHAR(20) NOT NULL DEFAULT 'ESCRITA',  -- 'LEITURA' | 'ESCRITA'
  PRIMARY KEY (id_perfil, cd_modulo)
);

CREATE TABLE IF NOT EXISTS usuario (
  id_usuario         BIGSERIAL PRIMARY KEY,
  nm_email           VARCHAR(150) NOT NULL UNIQUE,
  nm_nome            VARCHAR(150) NOT NULL,
  nm_senha_hash      VARCHAR(100) NOT NULL,      -- bcrypt
  id_perfil          BIGINT NOT NULL REFERENCES perfil(id_perfil),
  in_ativo           BOOLEAN NOT NULL DEFAULT TRUE,
  nr_falhas_login    INTEGER NOT NULL DEFAULT 0,
  dh_bloqueado_ate   TIMESTAMP,
  dh_ultimo_login    TIMESTAMP,
  dh_criacao         TIMESTAMP NOT NULL DEFAULT now()
);

-- A busca de login ignora caixa; o índice evita dois e-mails que só diferem na caixa.
CREATE UNIQUE INDEX IF NOT EXISTS ux_usuario_email_lower ON usuario (LOWER(nm_email));

INSERT INTO perfil (cd_perfil, nm_perfil) VALUES ('ADMIN','Administrador')
  ON CONFLICT (cd_perfil) DO NOTHING;

INSERT INTO perfil_permissao (id_perfil, cd_modulo, cd_acao)
  SELECT id_perfil, '*', 'ESCRITA' FROM perfil WHERE cd_perfil = 'ADMIN'
  ON CONFLICT DO NOTHING;
