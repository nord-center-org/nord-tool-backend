-- 003_termo_reprova.sql — termos de reprova e fotos (E07). Idempotente.
-- Depende de 002_arquivo_armazenado.sql. Aplicar ANTES do deploy desta etapa.

CREATE TABLE IF NOT EXISTS apartamento_termo_reprova (
  id_termo_reprova        BIGSERIAL PRIMARY KEY,
  id_apartamento_vistoria BIGINT NOT NULL REFERENCES apartamento_vistoria(id_apartamento_vistoria) ON DELETE CASCADE,
  nr_termo                INTEGER NOT NULL,                    -- 1, 2, 3... (max + 1, calculado no service)
  id_arquivo              BIGINT  NOT NULL REFERENCES arquivo_armazenado(id_arquivo),
  nr_paginas              INTEGER NOT NULL CHECK (nr_paginas > 0 AND nr_paginas <= 40),
  nm_situacao             VARCHAR(20) NOT NULL DEFAULT 'PENDENTE',   -- PENDENTE | EM_ANDAMENTO | CONCLUIDO
  tx_observacao           TEXT,
  dh_criacao              TIMESTAMP NOT NULL DEFAULT now(),
  dh_alteracao            TIMESTAMP NOT NULL DEFAULT now(),
  CONSTRAINT uq_termo_apartamento_nr UNIQUE (id_apartamento_vistoria, nr_termo)
);

CREATE TABLE IF NOT EXISTS apartamento_termo_foto (
  id_termo_foto        BIGSERIAL PRIMARY KEY,
  id_termo_reprova     BIGINT  NOT NULL REFERENCES apartamento_termo_reprova(id_termo_reprova) ON DELETE CASCADE,
  nr_pagina            INTEGER NOT NULL CHECK (nr_pagina > 0),
  nr_ordem             INTEGER NOT NULL DEFAULT 0,
  tx_legenda           VARCHAR(240),
  id_arquivo_imagem    BIGINT  NOT NULL REFERENCES arquivo_armazenado(id_arquivo),
  id_arquivo_miniatura BIGINT  NOT NULL REFERENCES arquivo_armazenado(id_arquivo),
  dh_criacao           TIMESTAMP NOT NULL DEFAULT now(),
  dh_alteracao         TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS ix_termo_foto_pagina ON apartamento_termo_foto (id_termo_reprova, nr_pagina, nr_ordem);

-- Atenção: ON DELETE CASCADE apaga as linhas, mas NÃO os registros de arquivo_armazenado.
-- A aplicação apaga os arquivos (termo, imagens e miniaturas) explicitamente.
