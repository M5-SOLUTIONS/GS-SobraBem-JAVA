-- Uma reserva ATIVA por receptor por doação, mesmo com requisições simultâneas.
-- Linhas que não estão ATIVA viram (NULL, NULL), e o Oracle não indexa chaves totalmente nulas.
CREATE UNIQUE INDEX sb_ux_reserva_ativa ON sb_t_reserva (
    CASE WHEN status = 'ATIVA' THEN id_doacao END,
    CASE WHEN status = 'ATIVA' THEN id_receptor END
);