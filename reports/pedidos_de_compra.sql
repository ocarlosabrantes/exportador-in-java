-- title: Pedidos de Compra
-- description: Pedidos de compra detalhados por item, com produto, fornecedor, valores e data de entrega. Campos em branco = sem limite.
-- param: filial | default=@filial | desc=Filial
-- param: filial_sb1 | default= | type=hidden | desc=Filial da SB1 (vazio se a tabela for compartilhada)
-- param: data_de | default=@inicio_mes | type=date | desc=Data de (dd/mm/aaaa)
-- param: data_ate | default=@hoje | type=date | desc=Data até (dd/mm/aaaa)
-- param: produto_de | default= | desc=Produto de (vazio = todos)
-- param: produto_ate | default= | desc=Produto até (vazio = todos)
-- param: fornecedor_de | default= | desc=Fornecedor de (vazio = todos)
-- param: fornecedor_ate | default= | desc=Fornecedor até (vazio = todos)
-- requer_login: true

SELECT SC7.C7_NUM     AS "Pedido",
       SC7.C7_ITEM    AS "Item",
       SC7.C7_EMISSAO AS "Data Emissão",
       SC7.C7_FORNECE AS "Fornecedor",
       SC7.C7_PRODUTO AS "Produto",
       SB1.B1_FABRIC  AS "Part Number",
       SC7.C7_DESCRI  AS "Descrição",
       SC7.C7_UM      AS "UM",
       SC7.C7_QUANT   AS "Quantidade",
       SC7.C7_PRECO   AS "Valor Unitário",
       SC7.C7_IPI     AS "IPI",
       SC7.C7_TOTAL   AS "Valor Total",
       SC7.C7_DATPRF  AS "Data Entrega",
       SC7.C7_CC      AS "Centro Custo",
       SC7.C7_NUMSC   AS "SC",
       SC7.C7_MOEDA   AS "Moeda",
       SC7.C7_TXMOEDA AS "Taxa Moeda",
       CASE
           WHEN SC7.C7_MOEDA = '1' THEN 'REAL'
           WHEN SC7.C7_MOEDA = '2' THEN 'DOLAR'
           WHEN SC7.C7_MOEDA = '3' THEN 'EURO'
           WHEN SC7.C7_MOEDA = '4' THEN 'LIBRA'
           WHEN SC7.C7_MOEDA = '5' THEN 'UFIR'
           ELSE 'OUTRA'
           END        AS "Descrição Moeda"
FROM SC7${SUF} SC7
         LEFT JOIN SB1${SUF} SB1
                   ON SB1.B1_FILIAL = :filial_sb1
                       AND SB1.B1_COD = SC7.C7_PRODUTO
                       AND SB1.D_E_L_E_T_ = ' '
WHERE SC7.D_E_L_E_T_ = ' '
  AND SC7.C7_FILIAL = :filial_sb1
  AND (:data_de = '' OR SC7.C7_EMISSAO >= :data_de)
  AND (:data_ate = '' OR SC7.C7_EMISSAO <= :data_ate)
  AND (:produto_de = '' OR SC7.C7_PRODUTO >= :produto_de)
  AND (:produto_ate = '' OR SC7.C7_PRODUTO <= :produto_ate)
  AND (:fornecedor_de = '' OR SC7.C7_FORNECE >= :fornecedor_de)
  AND (:fornecedor_ate = '' OR SC7.C7_FORNECE <= :fornecedor_ate)
ORDER BY SC7.C7_EMISSAO,
         SC7.C7_NUM