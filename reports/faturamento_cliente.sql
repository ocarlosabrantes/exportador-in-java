-- title: Faturamento por Cliente
-- description: Pedidos de compra detalhados por item, com produto, fornecedor, valores e data de entrega. Campos em branco = sem limite.
-- param: filial | default=@filial | desc=Filial
-- param: filial_sa1 | default= | type=hidden | desc=Filial da SA1 (vazio se a tabela for compartilhada)
-- param: data_de | default=@inicio_mes | type=date | desc=Emissão de (dd/mm/aaaa)
-- param: data_ate | default=@hoje | type=date | desc=Emissão até (dd/mm/aaaa)
-- param: cliente | default= | desc=Código do cliente (vazio = todos)
SELECT F2.F2_CLIENTE           AS "Cliente",
       F2.F2_LOJA              AS "Loja",
       RTRIM(A1.A1_NOME)       AS "Nome Cliente",
       COUNT(*)                AS "Qtd NFs",
       SUM(F2.F2_VALMERC)      AS "Valor Mercadorias",
       SUM(F2.F2_VALBRUT)      AS "Valor Bruto"
FROM SF2${SUF} F2
LEFT JOIN SA1${SUF} A1
       ON A1.A1_FILIAL = :filial_sa1
      AND A1.A1_COD = F2.F2_CLIENTE
      AND A1.A1_LOJA = F2.F2_LOJA
      AND A1.D_E_L_E_T_ = ' '
WHERE F2.D_E_L_E_T_ = ' '
  AND F2.F2_FILIAL = :filial
  AND F2.F2_TIPO = 'N'
  AND (:data_de = '' OR F2.F2_EMISSAO >= :data_de)
  AND (:data_ate = '' OR F2.F2_EMISSAO <= :data_ate)
  AND (:cliente = '' OR F2.F2_CLIENTE = :cliente)
GROUP BY F2.F2_CLIENTE, F2.F2_LOJA, A1.A1_NOME
ORDER BY SUM(F2.F2_VALBRUT) DESC
