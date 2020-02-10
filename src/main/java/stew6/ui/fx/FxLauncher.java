package stew6.ui.fx;

import java.io.*;
import java.sql.*;
import java.util.*;
import java.util.function.*;
import javafx.application.*;
import javafx.beans.property.*;
import javafx.beans.value.*;
import javafx.collections.*;
import javafx.scene.*;
import javafx.scene.control.*;
import javafx.scene.control.Alert.*;
import javafx.scene.control.TableColumn.*;
import javafx.scene.input.*;
import javafx.scene.layout.*;
import javafx.stage.*;
import javafx.util.*;
import minestra.text.*;
import stew6.*;
import stew6.ui.*;

/**
 * GUI with JavaFX (experimental).
 */
public final class FxLauncher extends Application implements Launcher, OutputProcessor {

    static final Class<FxLauncher> CLASS = FxLauncher.class;
    static final ResourceSheaf res = App.res.derive().withClass(CLASS).withMessages();

    private static final Logger log = Logger.getLogger(FxLauncher.class);

    private Environment env;
    private Stage stage;
    private TableView<Map<String, Object>> resultTable;
    private FxConsoleTextArea textArea;

    @Override
    public void start(Stage stage) throws Exception {
        this.stage = stage;
        this.resultTable = new TableView<>();
        this.textArea = new FxConsoleTextArea(this);
        stage.setTitle(res.s(".title"));
        stage.setOnCloseRequest(e -> requestClose());
        BorderPane pane = new BorderPane();
        pane.setCenter(resultTable);
        pane.setBottom(textArea);
        Scene scene = createScene(pane);
        scene.setOnKeyPressed(this::handleKeyEvent);
        stage.setScene(scene);
        stage.show();
        textArea.requestFocus();
        launchWith(this);
    }

    Scene createScene(Parent root) {
        Supplier<FxWindowConfig> loader = () -> {
            try {
                FxWindowConfigFile f = new FxWindowConfigFile();
                return f.read();
            } catch (IOException e) {
                log.error(e);
            }
            return new FxWindowConfig();
        };
        FxWindowConfig cfg = loader.get();
        try {
            FxWindowConfigFile f = new FxWindowConfigFile();
            cfg = f.read();
        } catch (IOException e) {
            log.error(e);
        }
        if (cfg.getX() <= 0) {
            cfg.setX(100);
        }
        if (cfg.getY() <= 0) {
            cfg.setY(100);
        }
        if (cfg.getWidth() <= 0) {
            cfg.setWidth(600);
        }
        if (cfg.getHeight() <= 0) {
            cfg.setHeight(400);
        }
        Scene scene = new Scene(root, cfg.getWidth(), cfg.getHeight());
        return scene;
    }

    void saveWindowConfig() {
        FxWindowConfig cfg = loadWindowConfig();
        cfg.setX((int)stage.getX());
        cfg.setY((int)stage.getY());
        cfg.setWidth((int)stage.getWidth());
        cfg.setHeight((int)stage.getHeight());
        FxWindowConfigFile f = new FxWindowConfigFile();
        try {
            f.write(cfg);
        } catch (IOException e) {
            log.error(e);
        }
    }

    static FxWindowConfig loadWindowConfig() {
        try {
            FxWindowConfigFile f = new FxWindowConfigFile();
            return f.read();
        } catch (IOException e) {
            log.error(e);
        }
        return new FxWindowConfig();
    }

    void handleKeyEvent(KeyEvent e) {
        switch (e.getCode()) {
            case W:
                if (e.isShortcutDown()) {
                    requestClose();
                }
                break;
            default:
        }
    }

    void sendCommand(String cmd) {
        try {
            Platform.runLater(() -> {
                setDisable(true);
            });
            Platform.runLater(() -> {
                if (!Commands.invoke(env, cmd)) {
                    close();
                }
                outputPrompt();
            });
        } finally {
            Platform.runLater(() -> {
                setDisable(false);
                textArea.requestFocus();
            });
        }
    }

    void setDisable(boolean value) {
        resultTable.setDisable(value);
        textArea.setDisable(value);
    }

    @Override
    public void launch(Environment env) {
        this.env = env;
        output(res.format(".about", App.getVersion()));
        outputPrompt();
    }

    @SuppressWarnings("resource")
    @Override
    public void output(Object o) {
        if ((o instanceof ResultSetReference) || (o instanceof ResultSet)) {
            ResultSetReference ref = (o instanceof ResultSetReference) ? (ResultSetReference)o
                                                                       : new ResultSetReference((ResultSet)o, "");
            try {
                outputResult(ref);
            } catch (SQLException ex) {
                throw new RuntimeException(ex);
            }
        } else if (o instanceof Prompt) {
            textArea.outputPrompt();
        } else {
            textArea.outputLine(o);
        }
    }

    void outputPrompt() {
        textArea.outputPrompt();
    }

    Prompt getPrompt() {
        return new Prompt(env);
    }

    private void outputResult(ResultSetReference ref) throws SQLException {
        @SuppressWarnings("resource")
        ResultSet rs = ref.getResultSet();
        ColumnOrder order = ref.getOrder();
        final boolean needsOrderChange = order.size() > 0;
        ResultSetMetaData meta = rs.getMetaData();
        final int columnCount = (needsOrderChange) ? order.size() : meta.getColumnCount();
        final int limit = App.props.getAsInt("rowcount.limit", Integer.MAX_VALUE);
        int rowCount = 0;
        final ObservableList<TableColumn<Map<String, Object>, ?>> columns = resultTable.getColumns();
        columns.clear();
        for (int i = 0; i < columnCount; i++) {
            final String name = (needsOrderChange) ? order.getName(i) : meta.getColumnName(i + 1);
            TableColumn<Map<String, Object>, Object> column = new TableColumn<>(name);
            column.setCellValueFactory(new Callback<TableColumn.CellDataFeatures<Map<String, Object>, Object>, ObservableValue<Object>>() {
                @Override
                public ObservableValue<Object> call(CellDataFeatures<Map<String, Object>, Object> m) {
                    return new SimpleObjectProperty<>(m.getValue().get(name));
                }
            });
            columns.add(column);
        }
        final ObservableList<Map<String, Object>> data = FXCollections.observableArrayList();
        while (rs.next()) {
            if (++rowCount >= limit) {
                continue;
            }
            Map<String, Object> row = new HashMap<>();
            for (int i = 0; i < columnCount; i++) {
                final int index = needsOrderChange ? order.getOrder(i) : i + 1;
                row.put(columns.get(i).getText(), rs.getObject(index));
            }
            data.add(row);
        }
        resultTable.setItems(data);
        ref.setRecordCount(rowCount);
    }

    void requestClose() {
        Alert dialog = new Alert(AlertType.CONFIRMATION);
        dialog.setHeaderText("");
        dialog.setContentText(res.s("i.confirm-close"));
        dialog.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                Platform.runLater(this::close);
            }
        });
    }

    @Override
    public void close() {
        env.release();
        saveWindowConfig();
        stage.close();
    }

    public static void main(String[] args) {
        launch(CLASS, args);
    }

}
