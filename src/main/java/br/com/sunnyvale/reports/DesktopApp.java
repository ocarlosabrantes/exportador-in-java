package br.com.sunnyvale.reports;

import javafx.application.Application;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.application.Platform;
import javafx.stage.Window;

import java.awt.Desktop;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Tela única: lista de relatórios à esquerda, formulário de parâmetros (gerado a partir dos
 * "-- param:" de cada .sql) e botões de exportação à direita. Não tem lógica de negócio própria:
 * usa exatamente o mesmo AppConfig/ReportLoader/ReportRunner/exportadores da linha de comando.
 */
public final class DesktopApp extends Application {

    private static final Path REPORTS_DIR = Path.of("reports");
    private static final Path CONFIG_FILE = Path.of("config.properties");
    //private static final Path OUT_DIR = Path.of("saida");
    private static final Path OUT_DIR = Path.of("C:/temp"); //carlos.abrantes - salvar na pasta temp

    private final ListView<ReportDefinition> lista = new ListView<>();
    private final VBox formulario = new VBox(10);
    private final Label status = new Label("Selecione um relatório à esquerda.");
    private final CheckBox chkExcel = new CheckBox("Excel (.xlsx)");
    private final CheckBox chkPdf = new CheckBox("PDF");
    private final Button btnGerar = new Button("Gerar relatório");
    private final Button btnAbrirPasta = new Button("Abrir pasta de saída");

    private ReportDefinition atual;
    private final Map<String, Control> campos = new LinkedHashMap<>();
    private AppConfig config;

    private boolean autenticado = false;

    @Override
    public void start(Stage stage) {
        chkExcel.setSelected(true);
        chkPdf.setSelected(true);
        btnGerar.setDisable(true);
        btnGerar.setDefaultButton(true);
        btnAbrirPasta.setDisable(true);

        try {
            config = AppConfig.load(CONFIG_FILE);
        } catch (Exception e) {
            status.setText("Não foi possível ler config.properties: " + e.getMessage());
        }

        lista.setPrefWidth(260);
        lista.getStyleClass().add("lista-relatorios");
        lista.setCellFactory(v -> new ListCell<>() {
            @Override
            protected void updateItem(ReportDefinition r, boolean empty) {
                super.updateItem(r, empty);
                setText(empty || r == null ? null : r.title());
            }
        });
        lista.getSelectionModel().selectedItemProperty()
                .addListener((obs, old, novo) -> {

                    if (novo == null) {
                        mostrarFormulario(null);
                        return;
                    }

                    if (novo.requerLogin() && !autenticado) {

                            Boolean resultadoLogin = pedirLogin();

                            if (resultadoLogin == null) {
                                // Cancelou ou fechou a janela de login.
                                Platform.runLater(() ->
                                        lista.getSelectionModel().clearSelection()
                                );

                                return;
                            }

                            if (!resultadoLogin) {
                                Alert alert = new Alert(
                                        Alert.AlertType.ERROR,
                                        "Usuário ou senha incorretos. Tente novamente.",
                                        ButtonType.OK
                                );
                                alert.setTitle("Erro de Login");
                                alert.setHeaderText(null);
                                aplicarIcone(alert);
                                alert.showAndWait();

                                Platform.runLater(() ->
                                        lista.getSelectionModel().clearSelection()
                                );

                                return;
                            }

                            autenticado = true;

                            Alert alert = new Alert(
                                    Alert.AlertType.INFORMATION,
                                    "Login realizado com sucesso!",
                                    ButtonType.OK
                            );
                            alert.setTitle("Login");
                            alert.setHeaderText(null);
                            aplicarIcone(alert);
                            alert.showAndWait();
                        }

                        mostrarFormulario(novo);
                    });
                    try {
                        List<ReportDefinition> defs = ReportLoader.loadAll(REPORTS_DIR);
                        lista.getItems().setAll(defs);
                        if (defs.isEmpty()) {
                            status.setText("Nenhum relatório encontrado em '" + REPORTS_DIR.toAbsolutePath() + "'.");
                        }
                    } catch (Exception e) {
                        status.setText("Erro ao ler a pasta de relatórios: " + e.getMessage());
                    }

                    // Cabeçalho com a logo da empresa; se o arquivo não existir, cai para texto (nunca quebra a tela).
                    Node marca = criarMarca();
                    Label subtitulo = new Label("");
                    HBox cabecalho = new HBox(14, marca, subtitulo);
                    cabecalho.setAlignment(Pos.CENTER_LEFT);
                    cabecalho.getStyleClass().add("cabecalho");
                    cabecalho.setMaxWidth(Double.MAX_VALUE);

                    ScrollPane scrollForm = new ScrollPane(formulario);
                    scrollForm.setFitToWidth(true);
                    formulario.setPadding(new Insets(4));

                    Label tituloParametros = new Label("Parâmetros");
                    tituloParametros.getStyleClass().add("titulo-parametros");

                    HBox formatos = new HBox(16, chkExcel, chkPdf);
                    HBox botoes = new HBox(10, btnGerar, btnAbrirPasta);
                    botoes.setAlignment(Pos.CENTER_LEFT);
                    status.setWrapText(true);
                    status.getStyleClass().add("status-label");
                    btnGerar.getStyleClass().add("botao-principal");
                    btnAbrirPasta.getStyleClass().add("botao-secundario");

                    VBox direita = new VBox(12, tituloParametros, scrollForm, new Separator(), formatos, botoes, status);
                    direita.getStyleClass().add("painel-direita");
                    direita.setPadding(new Insets(16));
                    VBox.setVgrow(scrollForm, Priority.ALWAYS);

                    btnGerar.setOnAction(e -> gerar());
                    btnAbrirPasta.setOnAction(e -> abrirPasta());

                    SplitPane split = new SplitPane(lista, direita);
                    split.setDividerPositions(0.28);
                    VBox.setVgrow(split, Priority.ALWAYS);

                    VBox raiz = new VBox(cabecalho, split);

                    Scene cena = new Scene(raiz, 940, 600);
                    var folhaEstilo = getClass().getResource("/estilo.css");
                    if (folhaEstilo != null) {
                        cena.getStylesheets().add(folhaEstilo.toExternalForm());
                    } else {
                        status.setText("Aviso: estilo.css não encontrado em src/main/resources — a tela ficou sem estilo próprio.");
                    }

                    stage.setScene(cena);
                    //stage.setTitle("Sunnyvale — Relatórios");
                    //stage.show();
                    stage.setTitle("Sunnyvale — Relatórios");
                    var iconeStream = getClass().getResourceAsStream("/icone.png");
                    if (iconeStream != null) {
                        stage.getIcons().add(new Image(iconeStream));
                    }
                    stage.show();
                }

        private void mostrarFormulario (ReportDefinition def){
            atual = def;
            formulario.getChildren().clear();
            campos.clear();
            btnGerar.setDisable(def == null);
            if (def == null) {
                return;
            }
            if (!def.description().isEmpty()) {
                Label desc = new Label(def.description());
                desc.setWrapText(true);
                desc.getStyleClass().add("descricao-relatorio");
                formulario.getChildren().add(desc);
            }
            for (ReportDefinition.Param p : def.params()) {
                if ("hidden".equalsIgnoreCase(p.type())) {
                    continue; // usa sempre o valor padrão, sem aparecer na tela
                }
                String valorPadrao = Formatters.resolveToken(p.defaultValue(), config);
                Label rotulo = new Label(p.description().isEmpty() ? p.name() : p.description());
                rotulo.getStyleClass().add("rotulo-campo");
                Control campo = criarCampo(p, valorPadrao);
                campos.put(p.name(), campo);
                formulario.getChildren().addAll(rotulo, campo);
            }
        }

        /**
         * Logo em src/main/resources/logo_160.png; se faltar, mostra "Sunnyvale" em texto.
         */
        private Node criarMarca () {
            var recurso = getClass().getResourceAsStream("/logo_160.png");
            if (recurso == null) {
                Label textoLogo = new Label("Sunnyvale");
                textoLogo.getStyleClass().add("marca-texto");
                return textoLogo;
            }
            ImageView iv = new ImageView(new Image(recurso));
            iv.setFitHeight(22);
            iv.setPreserveRatio(true);
            return iv;
        }

        private Control criarCampo (ReportDefinition.Param p, String valorPadrao){
            return switch (p.type().toLowerCase()) {
                case "date" -> {
                    DatePicker dp = new DatePicker();
                    dp.setPromptText("dd/mm/aaaa (vazio = sem limite)");
                    if (!valorPadrao.isBlank()) {
                        try {
                            String ymd = valorPadrao.matches("\\d{8}") ? valorPadrao : Formatters.toDbDate(valorPadrao);
                            dp.setValue(LocalDate.parse(ymd, DateTimeFormatter.ofPattern("yyyyMMdd")));
                        } catch (Exception ignored) {
                            // deixa em branco se o padrão não for uma data válida
                        }
                    }
                    yield dp;
                }
                case "choice" -> {
                    ComboBox<String> cb = new ComboBox<>();
                    cb.getItems().add(""); // opção "todos"
                    cb.getItems().addAll(p.options());
                    cb.setValue(valorPadrao);
                    yield cb;
                }
                default -> {
                    TextField tf = new TextField(valorPadrao);
                    tf.setPromptText("vazio = todos");
                    yield tf;
                }
            };
        }

        private void gerar () {
            if (atual == null || config == null) {
                return;
            }
//        if (atual.requerLogin() && !pedirLogin()) {
//            status.setText("Login necessário para gerar este relatório.");
//            return;
//        }
            if (!chkExcel.isSelected() && !chkPdf.isSelected()) {
                status.setText("Escolha pelo menos um formato (Excel ou PDF).");
                return;
            }
            Map<String, String> valores = new LinkedHashMap<>();
            for (ReportDefinition.Param p : atual.params()) {
                String v;
                if ("hidden".equalsIgnoreCase(p.type()) || !campos.containsKey(p.name())) {
                    v = Formatters.resolveToken(p.defaultValue(), config);
                } else {
                    Control c = campos.get(p.name());
                    if (c instanceof DatePicker dp) {
                        v = dp.getValue() == null ? "" : dp.getValue().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
                    } else if (c instanceof ComboBox<?> cb) {
                        v = cb.getValue() == null ? "" : cb.getValue().toString();
                    } else {
                        v = ((TextField) c).getText() == null ? "" : ((TextField) c).getText().trim();
                    }
                }
                valores.put(p.name(), v);
            }

            btnGerar.setDisable(true);
            btnAbrirPasta.setDisable(true);
            status.setText("Gerando \"" + atual.title() + "\"...");

            Task<String> tarefa = new Task<>() {
                @Override
                protected String call() throws Exception {
                    Files.createDirectories(OUT_DIR);
                    try (Connection con = Db.connect(config)) {
                        ReportResult resultado = ReportRunner.run(con, atual, valores, config.tableSuffix(), 100_000);
                        String stamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
                        StringBuilder gerados = new StringBuilder();
                        if (chkExcel.isSelected()) {
                            Path f = OUT_DIR.resolve(atual.id() + "_" + stamp + ".xlsx");
                            ExcelExporter.export(f, atual, valores, resultado);
                            gerados.append(f.getFileName()).append('\n');
                        }
                        if (chkPdf.isSelected()) {
                            Path f = OUT_DIR.resolve(atual.id() + "_" + stamp + ".pdf");
                            PdfExporter.export(f, atual, valores, resultado);
                            gerados.append(f.getFileName()).append('\n');
                        }
                        return resultado.rows().size() + " linha(s).\n" + gerados
                                + (resultado.truncated() ? "\n(resultado limitado)" : "");
                    }
                }
            };
            tarefa.setOnSucceeded(e -> {
                status.setText("Concluído — " + tarefa.getValue());
                btnGerar.setDisable(false);
                btnAbrirPasta.setDisable(false);
            });
            tarefa.setOnFailed(e -> {
                Throwable ex = tarefa.getException();
                status.setText("Erro: " + (ex == null ? "falha desconhecida" : ex.getMessage()));
                btnGerar.setDisable(false);
            });
            new Thread(tarefa, "gerar-relatorio").start();
        }

        private void abrirPasta () {
            try {
                Files.createDirectories(OUT_DIR);
                Desktop.getDesktop().open(OUT_DIR.toFile());
            } catch (Exception e) {
                status.setText("Não foi possível abrir a pasta: " + e.getMessage());
            }
        }

        private Boolean pedirLogin () {
            Dialog<ButtonType> dialog = new Dialog<>();
            dialog.setTitle("Login necessário");
            TextField usuario = new TextField();
            PasswordField senha = new PasswordField();
            VBox conteudo = new VBox(8, new Label("Usuário:"), usuario, new Label("Senha:"), senha);
            dialog.getDialogPane().setContent(conteudo);
            dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
            var recurso = getClass().getResourceAsStream("/icone.png");

            if (recurso != null) {
                Image icone = new Image(recurso);

                dialog.setOnShown(e -> {
                    var window = dialog.getDialogPane().getScene().getWindow();

                    if (window instanceof Stage stage) {
                        stage.getIcons().add(icone);
                    }
                });
            }

            var resultado = dialog.showAndWait();
            if (resultado.isEmpty() || resultado.get() != ButtonType.OK) {
                return null;
            }
            try {
                return LoginGate.validar(usuario.getText(), senha.getText(), Path.of("login.properties"));
            } catch (Exception e) {
                status.setText("Erro ao validar login: " + e.getMessage());
                return null;
            }
        }

        private void aplicarIcone (Alert alert){
            var recurso = getClass().getResourceAsStream("/icone.png");

            if (recurso != null) {
                Image icone = new Image(recurso);

                alert.setOnShown(e -> {
                    Window window = alert.getDialogPane().getScene().getWindow();

                    if (window instanceof Stage stage) {
                        stage.getIcons().add(icone);
                    }
                });
            }
        }

        public static void main (String[]args){
            launch(args);
        }
    }