-- title: Pedidos de Venda
-- description: Pedidos com valor total e valor já faturado (item com nota preenchida). Campos em branco = sem limite.
-- param: filial | default=@filial | desc=Filial
-- param: filial_sa1 | default= | type=hidden | desc=Filial da SA1 (vazio se a tabela for compartilhada)
-- param: pedido_de | default= | desc=Pedido de (vazio = todos)
-- param: pedido_ate | default= | desc=Pedido até (vazio = todos)
-- param: data_de | default=@inicio_mes | type=date | desc=Emissão de (dd/mm/aaaa)
-- param: data_ate | default=@hoje | type=date | desc=Emissão até (dd/mm/aaaa)
-- param: cliente | default= | desc=Código do cliente (vazio = todos)
SELECT C5.C5_NUM                                AS "Pedido",
       C5.C5_EMISSAO                            AS "Data Emissão",
       C5.C5_CLIENTE                            AS "Cliente",
       C5.C5_LOJACLI                            AS "Loja",
       RTRIM(A1.A1_NOME)                        AS "Nome Cliente",
       COUNT(*)                                 AS "Itens",
       SUM(C6.C6_QTDVEN)                        AS "Qtd Vendida",
       SUM(C6.C6_VALOR)                         AS "Valor Total",
       SUM(CASE WHEN RTRIM(C6.C6_NOTA) <> '' THEN C6.C6_VALOR ELSE 0 END) AS "Valor Faturado"
FROM SC5${SUF} C5
INNER JOIN SC6${SUF} C6
        ON C6.C6_FILIAL = C5.C5_FILIAL
       AND C6.C6_NUM = C5.C5_NUM
       AND C6.D_E_L_E_T_ = ' '
LEFT JOIN SA1${SUF} A1
        ON A1.A1_FILIAL = :filial_sa1
       AND A1.A1_COD = C5.C5_CLIENTE
       AND A1.A1_LOJA = C5.C5_LOJACLI
       AND A1.D_E_L_E_T_ = ' '
WHERE C5.D_E_L_E_T_ = ' '
  AND C5.C5_FILIAL = :filial
  AND (:pedido_de = '' OR C5.C5_NUM >= :pedido_de)
  AND (:pedido_ate = '' OR C5.C5_NUM <= :pedido_ate)
  AND (:data_de = '' OR C5.C5_EMISSAO >= :data_de)
  AND (:data_ate = '' OR C5.C5_EMISSAO <= :data_ate)
  AND (:cliente = '' OR C5.C5_CLIENTE = :cliente)
GROUP BY C5.C5_NUM, C5.C5_EMISSAO, C5.C5_CLIENTE, C5.C5_LOJACLI, A1.A1_NOME
ORDER BY C5.C5_EMISSAO, C5.C5_NUM
