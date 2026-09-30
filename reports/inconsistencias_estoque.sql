-- title: Inconsistências de Estoque
-- description: Saldos negativos e registros da SB2 sem produto na SB1. Se todos aparecerem sem cadastro, confira filial_sb1.
-- param: filial | default=@filial | desc=Filial da SB2
-- param: filial_sb1 | default= | type=hidden | desc=Filial da SB1 (vazio se a tabela for compartilhada)
SELECT RTRIM(B2.B2_COD)   AS "Produto",
       RTRIM(B1.B1_DESC)  AS "Descrição",
       B2.B2_LOCAL        AS "Armazém",
       B2.B2_QATU         AS "Saldo Atual",
       CASE WHEN B1.B1_COD IS NULL THEN 'Produto sem cadastro (SB1)'
            WHEN B2.B2_QATU < 0    THEN 'Saldo negativo'
            ELSE '' END   AS "Motivo"
FROM SB2${SUF} B2
LEFT JOIN SB1${SUF} B1
       ON B1.B1_FILIAL = :filial_sb1
      AND B1.B1_COD = B2.B2_COD
      AND B1.D_E_L_E_T_ = ' '
WHERE B2.D_E_L_E_T_ = ' '
  AND B2.B2_FILIAL = :filial
  AND (B2.B2_QATU < 0 OR B1.B1_COD IS NULL)
ORDER BY B2.B2_COD, B2.B2_LOCAL
