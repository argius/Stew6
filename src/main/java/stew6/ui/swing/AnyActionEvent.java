package stew6.ui.swing;

import java.awt.event.*;
import java.util.*;
import java.util.stream.*;

/**
 * @see AnyAction
 */
final class AnyActionEvent extends ActionEvent {

    private final Object[] args;

    AnyActionEvent(Object source, Object actionKey, Object... args) {
        this(source, System.currentTimeMillis(), String.valueOf(actionKey), args);
    }

    private AnyActionEvent(Object source, long when, String actionKeystring, Object... args) {
        super(source, ACTION_PERFORMED, actionKeystring, when, 0);
        this.args = args;
    }

    Object[] getArgs() {
        return args.clone();
    }

    List<String> getArgsAsStringList() {
        return Stream.of(args).map(String::valueOf).collect(Collectors.toList());
    }

    void validate() {
        // the 2 second rule
        if (getWhen() < System.currentTimeMillis() - 2000L) {
            final String msg = String.format("timeout: command [%s] cancelled", getActionCommand());
            throw new RuntimeException(msg);
        }
    }

    boolean isAnyOf(Object... a) {
        final String cmd = getActionCommand();
        for (Object o : a) {
            final String key = String.valueOf(o);
            if (key.equals(cmd)) {
                return true;
            }
        }
        return false;
    }

}
