-- title: Estoque por Lote/Sub-Lote
-- description: Posição de estoque por lote e sub-lote, com produto, armazém, saldo, empenho e validade. Campos em branco = sem limite.
-- param: filial | default=@filial | desc=Filial
-- param: filial_sb1 | default= | type=hidden | desc=Filial da SB1 (vazio se a tabela for compartilhada)
-- param: produto_de | default= | desc=Produto de (vazio = todos)
-- param: produto_ate | default= | desc=Produto até (vazio = todos)
-- param: lote_de | default= | desc=Lote de (vazio = todos)
-- param: lote_ate | default= | desc=Lote até (vazio = todos)
-- param: sublote_de | default= | desc=Sub-lote de (vazio = todos)
-- param: sublote_ate | default= | desc=Sub-lote até (vazio = todos)
-- param: local_de | default= | desc=Armazém de (vazio = todos)
-- param: local_ate | default= | desc=Armazém até (vazio = todos)
-- param: validade_de | default= | type=date | desc=Validade de (vazio = todas)
-- param: validade_ate | default= | type=date | desc=Validade até (vazio = todas)

SELECT SB8.B8_PRODUTO        AS "Produto",
       RTRIM(SB1.B1_DESC)    AS "Descrição",
       SB8.B8_LOTECTL        AS "Lote",
       SB8.B8_NUMLOTE        AS "Sub-Lote",
       SB8.B8_LOCAL          AS "Armazém",
       RTRIM(NNR.NNR_DESCRI) AS "Descrição Armazém",
       SB8.B8_SALDO          AS "Saldo",
       SB8.B8_EMPENHO        AS "Empenho",
       SB8.B8_DATA           AS "Data",
       SB8.B8_DTVALID        AS "Data Validade"
FROM SB8${SUF} SB8
         LEFT JOIN SB1${SUF} SB1
                   ON SB1.B1_FILIAL = :filial_sb1
                       AND SB1.B1_COD = SB8.B8_PRODUTO
                       AND SB1.D_E_L_E_T_ = ' '
         LEFT JOIN NNR${SUF} NNR
                   ON NNR.NNR_FILIAL = SB8.B8_FILIAL
                       AND NNR.NNR_CODIGO = SB8.B8_LOCAL
                       AND NNR.D_E_L_E_T_ = ' '
WHERE SB8.B8_FILIAL = :filial
  AND SB8.D_E_L_E_T_ = ' '
  AND (:produto_de = '' OR SB8.B8_PRODUTO >= :produto_de)
  AND (:produto_ate = '' OR SB8.B8_PRODUTO <= :produto_ate)
  AND (:lote_de = '' OR SB8.B8_LOTECTL >= :lote_de)
  AND (:lote_ate = '' OR SB8.B8_LOTECTL <= :lote_ate)
  AND (:sublote_de = '' OR SB8.B8_NUMLOTE >= :sublote_de)
  AND (:sublote_ate = '' OR SB8.B8_NUMLOTE <= :sublote_ate)
  AND (:local_de = '' OR SB8.B8_LOCAL >= :local_de)
  AND (:local_ate = '' OR SB8.B8_LOCAL <= :local_ate)
  AND (:validade_de = '' OR SB8.B8_DTVALID >= :validade_de)
  AND (:validade_ate = '' OR SB8.B8_DTVALID <= :validade_ate)
ORDER BY SB8.B8_PRODUTO,
         SB8.B8_LOCAL,
         SB8.B8_LOTECTL,
         SB8.B8_NUMLOTE