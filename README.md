# Exportador In Java

## Por que esse projeto existe

Na Sunnyvale, quando o ERP ficava instável, os usuários simplesmente não
conseguiam emitir os relatórios de que precisavam, o ERP travava ou caía, e o trabalho do
dia parava. Esse projeto nasceu para resolver essa dor específica: ele lê direto do banco
SQL, sem depender do ERP e exporta os relatórios para Excel e PDF.

Hoje ele já resolve o problema e os relatórios são salvos em `C:\temp`. Inicialmente o projeto é em desktop, 
mas segue em evolução. A ideia é ir ampliando aos poucos (mais relatórios, tela mais amigável e etc.).

## Relatórios disponíveis hoje

| Relatório | O que traz |
|---|---|
| Faturamento por Cliente | Notas fiscais de saída normais, agrupadas por cliente (SF2) |
| Inconsistências de Estoque | Saldo negativo ou produto sem cadastro na SB1 |
| Pedidos de Compra 🔒| Pedidos de compra detalhados por item, com produto, fornecedor, valores e data de entrega. (SC7 + SB1) |
| Pedidos de Venda | Pedidos com valor total, valor faturado e nota fiscal (SC5 + SC6 + SA1) |
| Saldo em Estoque | Saldo por produto e armazém (SB2 + SB1), com valor pelo custo médio |
| Estoque por Lote/Sub-Lote | Posição de estoque por lote e sub-lote, com produto, armazém, saldo, empenho e validade. (SB8 + SB1 + NNR) |

Cada um é um arquivo `.sql` em `reports/`, com os parâmetros declarados no próprio arquivo

## Como rodar (ambiente de desenvolvimento)

1. Copie `config.example.properties` para `config.properties` e preencha `db.url`,
   `db.user` e `db.table_suffix` (empresa + `0`, ex.: empresa 01 → `010`).
2. A senha não fica no arquivo — veja "Senha do banco" abaixo.
3. No IntelliJ: `Edit Configurations`, Main class `br.com.sunnyvale.reports.ui.DesktopApp`,
   Working directory na raiz do projeto.
4. Se faltar o JavaFX (`Error: JavaFX runtime components are missing`), baixe o SDK em
   https://gluonhq.com/products/javafx/ (versão 21) e adicione em VM options:

       --module-path "C:\caminho\para\javafx-sdk-21\lib" --add-modules javafx.controls

## Senha do banco

Não fica em texto no `config.properties`. Defina como variável de ambiente do Windows:

    setx DB_PASSWORD "sua_senha" /M

O `AppConfig` dá prioridade à variável de ambiente sobre o arquivo. Feche e abra de novo
o IntelliJ/PowerShell depois do `setx` para a variável valer.

## Como adicionar um relatório novo

Crie um `.sql` em `reports/`. O nome do arquivo vira o identificador do relatório na tela.

    -- title: Nome do Relatório
    -- description: Texto explicando o que ele traz
    -- param: filial | default=@filial | desc=Filial
    -- param: data_de | default=@inicio_mes | type=date | desc=Emissão de (dd/mm/aaaa)
    SELECT ...
    FROM SB2${SUF} B2
    WHERE B2.D_E_L_E_T_ = ' ' AND B2.B2_FILIAL = :filial

- `${SUF}` vira o sufixo de tabela do `config.properties` (ex.: `SB2010`).
- `:nome` é um parâmetro — a tela gera o campo sozinha (`type=date` vira calendário,
  `type=choice` com `options=A;B` vira lista, `type=hidden` não aparece e usa sempre o
  padrão).
- Não precisa recompilar: o `.sql` é lido do disco a cada execução.
