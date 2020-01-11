package stew6.ui.swing;

import static java.awt.event.KeyEvent.*;
import static javax.swing.KeyStroke.getKeyStroke;
import java.awt.*;
import java.awt.event.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.*;
import javax.swing.*;
import javax.swing.text.*;
import javax.swing.undo.*;
import stew6.*;

/**
 * These class are members of the suite of event-handling as an internal framework.
 */
final class AnyAction extends AbstractAction implements Runnable {

    /** Thread Pool using Daemon Thread */
    private static final ExecutorService threadPool = Executors.newCachedThreadPool(DaemonThreadFactory.getInstance());

    /** AnyActionListener, JComponent, or JTextComponent */
    private Object o;

    /** mappings (a method name and a Method) for dynamic invoking */
    private Map<String, Method> m;

    /** event command string */
    private String eventCommand;

    /** `o' can listen AnyAction (that is, it implemented AnyActionListener) */
    private boolean canListenAnyAction;

    /** `o' has InputMap (that is, it is a JComponent) */
    private boolean hasInputMap;

    AnyAction(Object o) {
        this(o, "");
    }

    AnyAction(Object o, String eventCommand) {
        super(eventCommand);
        if (eventCommand == null) {
            throw new IllegalArgumentException("eventCommand is null");
        }
        this.o = o;
        this.m = new LinkedHashMap<>();
        this.eventCommand = eventCommand;
        this.canListenAnyAction = (o instanceof AnyActionListener);
        this.hasInputMap = (o instanceof JComponent);
    }

    void doNow(String methodName, final Object... args) {
        try {
            resolveMethod(methodName).invoke(o, args);
        } catch (Exception ex) {
            throw new RuntimeException("at AnyAction#doNow", ex);
        }
    }

    void doLater(String methodName, Object... args) {
        final String label = "AnyAction#doLater";
        EventQueue.invokeLater(new Task(label, o, resolveMethod(methodName), args));
    }

    void doParallel(String methodName, final Object... args) {
        final String label = "AnyAction#doParallel";
        doParallel(new Task(label, o, resolveMethod(methodName), args));
    }

    static void doParallel(Runnable runnable) {
        threadPool.execute(runnable);
    }

    private static final class Task implements Runnable {

        private final String label;
        private final Object o;
        private final Method method;
        private final Object[] args;

        Task(String label, Object o, Method method, Object... args) {
            this.label = label;
            this.o = o;
            this.method = method;
            this.args = args;
        }

        @Override
        public void run() {
            try {
                method.invoke(o, args);
            } catch (Exception ex) {
                throw new RuntimeException("at " + label, ex);
            }
        }

    }

    private Method resolveMethod(String methodName) {
        if (m.containsKey(methodName)) {
            return m.get(methodName);
        }
        for (Method method : o.getClass().getDeclaredMethods()) {
            if (method.getName().equals(methodName)) {
                method.setAccessible(true);
                m.put(methodName, method);
                return method;
            }
        }
        throw new IllegalArgumentException("method not found: " + methodName);
    }

    @Override
    public void run() {
        actionPerformed(new ActionEvent(o, 0, eventCommand));
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (!canListenAnyAction) {
            throw new RuntimeException(editErrorMessage(1, e));
        }
        final String cmd;
        if (eventCommand.length() == 0) {
            cmd = e.getActionCommand();
        } else {
            cmd = eventCommand;
        }
        AnyActionEvent aae = new AnyActionEvent(e.getSource(), cmd);
        ((AnyActionListener)o).anyActionPerformed(aae);
    }

    void bind(AnyActionListener dst, Object actionKey, KeyStroke... keyStrokes) {
        bind(dst, false, actionKey, keyStrokes);
    }

    void bind(AnyActionListener dst,
              boolean whenAncestor,
              Object actionKey,
              KeyStroke... keyStrokes) {
        if (!hasInputMap && keyStrokes.length > 0) {
            throw new RuntimeException(editErrorMessage(2, o));
        }
        bind((JComponent)o, dst, whenAncestor, actionKey, keyStrokes);
    }

    void bindKeyStroke(boolean whenAncestor, Object actionKey, KeyStroke... keyStrokes) {
        if (!hasInputMap && keyStrokes.length > 0) {
            throw new RuntimeException(editErrorMessage(2, o));
        }
        bindKeyStroke((JComponent)o, whenAncestor, String.valueOf(actionKey), keyStrokes);
    }

    void bindSelf(Object actionKey, KeyStroke... keyStrokes) {
        if (!canListenAnyAction) {
            throw new RuntimeException(editErrorMessage(1, o));
        }
        if (keyStrokes.length > 0 && !hasInputMap) {
            throw new RuntimeException(editErrorMessage(2, o));
        }
        bind((JComponent)o, (AnyActionListener)o, false, actionKey, keyStrokes);
    }

    UndoManager setUndoAction() {
        if (o instanceof JTextComponent) {
            return setUndoAction((JTextComponent)o);
        }
        throw new RuntimeException(editErrorMessage(3, o));
    }

    static UndoManager setUndoAction(JTextComponent text) {
        final UndoManager um = new UndoManager();
        if (text == null) {
            return um;
        }
        text.getDocument().addUndoableEditListener(um);
        ActionMap amap = text.getActionMap();
        InputMap imap = text.getInputMap();
        final int shortcutKey = Utilities.getMenuShortcutKeyMask();
        amap.put("undo", new UndoAction(um));
        imap.put(getKeyStroke(VK_Z, shortcutKey), "undo");
        amap.put("redo", new RedoAction(um));
        imap.put(getKeyStroke(VK_Y, shortcutKey), "redo");
        return um;
    }

    private static final class UndoAction extends AbstractAction {
        private UndoManager um;
        UndoAction(UndoManager um) {
            this.um = um;
        }
        @Override
        public void actionPerformed(ActionEvent e) {
            if (um.canUndo()) {
                um.undo();
            }
        }
    }

    private static final class RedoAction extends AbstractAction {
        private UndoManager um;
        RedoAction(UndoManager um) {
            this.um = um;
        }
        @Override
        public void actionPerformed(ActionEvent e) {
            if (um.canRedo()) {
                um.redo();
            }
        }
    }

    static String editErrorMessage(int type, Object o) {
        final String cn = getClassName(o);
        switch (type) {
            case 0: // unexpected error
                return "unexpected error";
            case 1: // can not listen AnyAction
                return String.format("%s can not listen AnyAction", cn);
            case 2: // does not have InputMap
                return String.format("%s does not have InputMap", cn);
            case 3: // is not JTextComponent
                return String.format("This is not JTextComponent, but %s", cn);
            default:
                return "";
        }
    }

    private static void bind(JComponent src,
                             AnyActionListener dst,
                             boolean whenAncestor,
                             Object actionKey,
                             KeyStroke... keyStrokes) {
        final String key = String.valueOf(actionKey);
        src.getActionMap().put(key, new AnyAction(dst, key));
        bindKeyStroke(src, whenAncestor, key, keyStrokes);
    }

    private static void bindKeyStroke(JComponent src,
                                      boolean whenAncestor,
                                      String actionKeyString,
                                      KeyStroke... keyStrokes) {
        if (keyStrokes != null) {
            final int focusMode = whenAncestor
                    ? JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT
                    : JComponent.WHEN_FOCUSED;
            for (KeyStroke ks : keyStrokes) {
                src.getInputMap(focusMode).put(ks, actionKeyString);
            }
        }
    }

    private static String getClassName(Object o) {
        return (o == null) ? "null" : o.getClass().getSimpleName();
    }

}
