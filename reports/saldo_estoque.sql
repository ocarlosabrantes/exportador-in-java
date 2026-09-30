-- title: Saldo em Estoque
-- description: Saldo por produto e armazém, com valor em estoque pelo custo médio. Campos em branco = todos.
-- param: filial | default=@filial | desc=Filial
-- param: filial_sb1 | default= | type=hidden | desc=Filial da SB1 (vazio se a tabela for compartilhada)
-- param: produto_de | default= | desc=Produto de (vazio = todos)
-- param: produto_ate | default= | desc=Produto até (vazio = todos)
-- param: armazem | default= | desc=Armazém (vazio = todos)
-- param: so_com_saldo | default=S | type=choice | options=S;N | desc=Somente saldo diferente de zero
SELECT RTRIM(B2.B2_COD)                    AS "Produto",
       RTRIM(B1.B1_DESC)                   AS "Descrição",
       B1.B1_UM                            AS "UM",
       B2.B2_LOCAL                         AS "Armazém",
       B2.B2_QATU                          AS "Saldo Atual",
       B2.B2_RESERVA                       AS "Reservado",
       B2.B2_QATU - B2.B2_RESERVA          AS "Disponível",
       B2.B2_CM1                           AS "Custo Médio",
       B2.B2_QATU * B2.B2_CM1              AS "Valor em Estoque"
FROM SB2${SUF} B2
INNER JOIN SB1${SUF} B1
        ON B1.B1_FILIAL = :filial_sb1
       AND B1.B1_COD = B2.B2_COD
       AND B1.D_E_L_E_T_ = ' '
WHERE B2.D_E_L_E_T_ = ' '
  AND B2.B2_FILIAL = :filial
  AND (:produto_de = '' OR B2.B2_COD >= :produto_de)
  AND (:produto_ate = '' OR B2.B2_COD <= :produto_ate)
  AND (:armazem = '' OR B2.B2_LOCAL = :armazem)
  AND (:so_com_saldo <> 'S' OR B2.B2_QATU <> 0)
ORDER BY B2.B2_COD, B2.B2_LOCAL
