CREATE TABLE sb_t_categoria (
    id_categoria     NUMBER GENERATED AS IDENTITY,
    nome             VARCHAR2(60)  NOT NULL,
    descricao        VARCHAR2(255),
    prazo_max_horas  NUMBER(3)     NOT NULL,
    CONSTRAINT sb_pk_categoria       PRIMARY KEY (id_categoria),
    CONSTRAINT sb_uk_categoria_nome  UNIQUE (nome),
    CONSTRAINT sb_ck_categoria_prazo CHECK (prazo_max_horas BETWEEN 1 AND 48)
);

CREATE TABLE sb_t_usuario (
    id_usuario   NUMBER GENERATED AS IDENTITY,
    nome         VARCHAR2(120) NOT NULL,
    email        VARCHAR2(150) NOT NULL,
    senha_hash   VARCHAR2(100) NOT NULL,
    telefone     VARCHAR2(20),
    papel        VARCHAR2(10)  NOT NULL,
    endereco     VARCHAR2(200),
    cidade       VARCHAR2(80),
    uf           CHAR(2),
    ativo        CHAR(1)       DEFAULT 'S',
    criado_em    TIMESTAMP     DEFAULT SYSTIMESTAMP,
    CONSTRAINT sb_pk_usuario        PRIMARY KEY (id_usuario),
    CONSTRAINT sb_uk_usuario_email  UNIQUE (email),
    CONSTRAINT sb_ck_usuario_papel  CHECK (papel IN ('DOADOR', 'RECEPTOR', 'ADMIN')),
    CONSTRAINT sb_ck_usuario_ativo  CHECK (ativo IN ('S', 'N'))
);

CREATE TABLE sb_t_doacao (
    id_doacao                NUMBER GENERATED AS IDENTITY,
    id_doador                NUMBER         NOT NULL,
    id_categoria             NUMBER         NOT NULL,
    titulo                   VARCHAR2(100)  NOT NULL,
    descricao                VARCHAR2(500),
    quantidade_total         NUMBER(6)      NOT NULL,
    quantidade_disponivel    NUMBER(6)      NOT NULL,
    unidade                  VARCHAR2(10)   NOT NULL,
    prazo_retirada           TIMESTAMP      NOT NULL,
    status                   VARCHAR2(12)   NOT NULL,
    orientacoes_conservacao  VARCHAR2(1000),
    gerada_por_ia            CHAR(1)        DEFAULT 'N',
    criado_em                TIMESTAMP      DEFAULT SYSTIMESTAMP,
    CONSTRAINT sb_pk_doacao           PRIMARY KEY (id_doacao),
    CONSTRAINT sb_fk_doacao_doador    FOREIGN KEY (id_doador)    REFERENCES sb_t_usuario (id_usuario),
    CONSTRAINT sb_fk_doacao_categoria FOREIGN KEY (id_categoria) REFERENCES sb_t_categoria (id_categoria),
    CONSTRAINT sb_ck_doacao_total     CHECK (quantidade_total >= 1),
    CONSTRAINT sb_ck_doacao_disp      CHECK (quantidade_disponivel BETWEEN 0 AND quantidade_total),
    CONSTRAINT sb_ck_doacao_unidade   CHECK (unidade IN ('UNIDADE', 'KG', 'PACOTE')),
    CONSTRAINT sb_ck_doacao_status    CHECK (status IN ('ATIVA', 'ESGOTADA', 'EXPIRADA', 'CANCELADA')),
    CONSTRAINT sb_ck_doacao_ia        CHECK (gerada_por_ia IN ('S', 'N'))
);

CREATE TABLE sb_t_reserva (
    id_reserva       NUMBER GENERATED AS IDENTITY,
    id_doacao        NUMBER       NOT NULL,
    id_receptor      NUMBER       NOT NULL,
    quantidade       NUMBER(6)    NOT NULL,
    status           VARCHAR2(10) NOT NULL,
    codigo_retirada  VARCHAR2(6)  NOT NULL,
    criado_em        TIMESTAMP    DEFAULT SYSTIMESTAMP,
    retirado_em      TIMESTAMP,
    CONSTRAINT sb_pk_reserva          PRIMARY KEY (id_reserva),
    CONSTRAINT sb_fk_reserva_doacao   FOREIGN KEY (id_doacao)   REFERENCES sb_t_doacao (id_doacao),
    CONSTRAINT sb_fk_reserva_receptor FOREIGN KEY (id_receptor) REFERENCES sb_t_usuario (id_usuario),
    CONSTRAINT sb_ck_reserva_qtd      CHECK (quantidade >= 1),
    CONSTRAINT sb_ck_reserva_status   CHECK (status IN ('ATIVA', 'RETIRADA', 'CANCELADA', 'EXPIRADA'))
);