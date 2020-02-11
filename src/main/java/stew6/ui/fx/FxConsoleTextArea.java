package stew6.ui.fx;

import org.apache.commons.lang3.*;
import javafx.application.*;
import javafx.event.*;
import javafx.scene.control.*;
import javafx.scene.input.*;
import javafx.scene.text.*;
import minestra.text.*;

final class FxConsoleTextArea extends TextArea {

    private static final ResourceSheaf res = FxLauncher.res.derive().withClass(FxConsoleTextArea.class);

    private FxLauncher launcher;
    private int promptPosition;
    private CommandHistory history;

    FxConsoleTextArea(FxLauncher launcher) {
        this.launcher = launcher;
        this.history = new CommandHistory();
        setStyle(res.s("style"));
        setFont(Font.font("Monospaced"));
        setOnKeyPressed(new EventHandler<KeyEvent>() {
            @Override
            public void handle(KeyEvent e) {
                switch (e.getCode()) {
                    case ENTER:
                        onSubmit();
                        break;
                    case C:
                        if (e.isShortcutDown()) {
                            appendText(" [BREAK] ");
                            outputPrompt();
                        }
                        break;
                    case UP:
                        if (e.isShortcutDown()) {
                            String cmd = history.prev();
                            if (!StringUtils.isBlank(cmd)) {
                                replaceText(cmd);
                            }
                        }
                        break;
                    case DOWN:
                        if (e.isShortcutDown()) {
                            String cmd = history.next();
                            if (!StringUtils.isBlank(cmd)) {
                                replaceText(cmd);
                            }
                        }
                        break;
                    default:
                }
            }
        });
    }

    void onSubmit() {
        end();
        final int endp = getCaretPosition();
        if (endp >= promptPosition) {
            String cmd = getText(promptPosition, endp);
            launcher.sendCommand(cmd);
            history.add(cmd);
            Platform.runLater(() -> end());
        }
    }

    void replaceText(String text) {
        replaceText(promptPosition, getText().length(), text);
        Platform.runLater(() -> end());
    }

    void outputPrompt() {
        printf("%s", launcher.getPrompt());
        promptPosition = getCaretPosition();
    }

    void outputLine(Object... a) {
        if (a.length == 0) {
            printf("%n");
        } else {
            for (final Object o : a) {
                printf("%s%n", o);
            }
        }
    }

    void printf(String format, Object... a) {
        appendText(String.format(format, a));
    }

}
