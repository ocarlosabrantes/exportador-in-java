# Exportador In Java

## Por que esse projeto existe

Na Sunnyvale, quando o ERP ficava instável, os usuários simplesmente não
conseguiam emitir os relatórios de que precisavam — o ERP travava ou caía, e o trabalho do
dia parava. Esse projeto nasceu para resolver essa dor específica: ele lê direto do banco
SQL, sem depender do ERP estar de pé, e exporta os relatórios
para Excel e PDF.

Hoje ele já resolve o problema original. Os relatórios são salvos em `C:\temp` e o projeto
segue em evolução — a ideia é ir ampliando aos poucos (mais relatórios, tela mais amigável,
instalador para os PCs da empresa), não é um software "fechado".

## Status atual

- **Funcional:** roda contra o banco de produção via um usuário somente leitura.
- **Interface:** tela desktop (JavaFX) — lista de relatórios à esquerda, formulário de
  parâmetros no meio (gerado automaticamente a partir de cada `.sql`), exportação para
  Excel e PDF à direita.
- **Saída:** os arquivos gerados vão para `C:\temp` (a pasta de saída ainda não é
  configurável pela tela — ver "Próximos passos").
- **Ainda não distribuído:** roda hoje pelo IntelliJ; o empacotamento como instalador
  (`.exe`, via `jpackage`) está documentado mas ainda não foi gerado para os outros PCs.

## Relatórios disponíveis hoje

| Relatório | O que traz |
|---|---|
| Saldo em Estoque | Saldo por produto e armazém (SB2 + SB1), com valor pelo custo médio |
| Pedidos de Venda | Pedidos com valor total, valor faturado e nota fiscal (SC5 + SC6 + SA1) |
| Faturamento por Cliente | Notas fiscais de saída normais, agrupadas por cliente (SF2) |
| Inconsistências de Estoque | Saldo negativo ou produto sem cadastro na SB1 |

Cada um é um arquivo `.sql` em `reports/`, com os parâmetros declarados no próprio arquivo
(ver "Como adicionar um relatório novo" abaixo) — a tela não precisa de código novo para
ganhar um relatório.

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
