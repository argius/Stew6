package stew6.ui.fx;

import java.sql.*;
import java.util.*;
import javafx.application.*;
import javafx.beans.property.*;
import javafx.beans.value.*;
import javafx.collections.*;
import javafx.event.*;
import javafx.scene.*;
import javafx.scene.control.*;
import javafx.scene.control.TableColumn.*;
import javafx.scene.input.*;
import javafx.scene.layout.*;
import javafx.scene.text.*;
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

    private Environment env;
    private TableView<Map<String, Object>> resultTable;
    private TextArea textArea;
    private int promptPosition;

    @Override
    public void start(Stage stage) throws Exception {
        stage.setTitle(res.s(".title"));
        stage.setOnCloseRequest(new EventHandler<WindowEvent>() {
            @Override
            public void handle(WindowEvent e) {
                close();
            }
        });
        BorderPane pane = new BorderPane();
        pane.setCenter(this.resultTable = new TableView<>());
        pane.setBottom(this.textArea = buildOutputArea());
        stage.setScene(new Scene(pane, 600, 400));
        stage.show();

        // Font
        this.textArea.setFont(Font.font("Monospaced"));

        Environment env = new Environment();
        env.setOutputProcessor(this);
        launch(env);
    }

    private TextArea buildOutputArea() {
        final TextArea o = new TextArea();
        o.setStyle(res.s("style"));
        o.setOnKeyPressed(new EventHandler<KeyEvent>() {
            @Override
            public void handle(KeyEvent e) {
                switch (e.getCode()) {
                    case ENTER:
                        onSubmit();
                        break;
                    case C:
                        if (e.isControlDown()) {
                            o.appendText(" [BREAK] ");
                            outputPrompt();
                        }
                        break;
                    default:
                }
            }
            @SuppressWarnings("synthetic-access")
            private void onSubmit() {
                o.end();
                final int endp = o.getCaretPosition();
                if (endp >= promptPosition) {
                    sendCommand(o.getText(promptPosition, endp));
                }
            }
        });
        return o;
    }

    private void sendCommand(final String cmd) {
        Platform.runLater(new Runnable() {
            @SuppressWarnings("synthetic-access")
            @Override
            public void run() {
                if (!Commands.invoke(env, cmd)) {
                    close();
                }
                outputPrompt();
            }
        });
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
            outputPrompt();
        } else {
            outputLine(o);
        }
    }

    void outputPrompt() {
        printf("%s", new Prompt(env));
        promptPosition = textArea.getCaretPosition();
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

    private void outputLine(Object... a) {
        if (a.length == 0) {
            printf("%n");
        } else {
            for (final Object o : a) {
                printf("%s%n", o);
            }
        }
    }

    private void printf(String format, Object... a) {
        textArea.appendText(String.format(format, a));
    }

    @Override
    public void close() {
        env.release();
        Platform.exit();
    }

    public static void main(String[] args) {
        launch(CLASS, args);
    }

}
