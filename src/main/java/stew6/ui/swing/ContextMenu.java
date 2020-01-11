package stew6.ui.swing;

import java.awt.event.*;
import java.util.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.text.*;
import javax.swing.undo.*;
import stew6.*;

final class ContextMenu {

    private static final Logger log = Logger.getLogger(ContextMenu.class);
    private static final ResourceManager res = ResourceManager.getInstance(ContextMenu.class);

    private ContextMenu() {
    } // forbidden

    static JPopupMenu create(JComponent target, AnyActionListener dst) {
        return create(target, dst, target.getClass().getSimpleName());
    }

    static JPopupMenu create(AnyActionListener dst) {
        JComponent c = (dst instanceof JComponent) ? (JComponent)dst : null;
        return create(c, dst, dst.getClass().getSimpleName());
    }

    static JPopupMenu create(JComponent target, AnyActionListener dst, String name) {
        log.atEnter("set", dst, name);
        JPopupMenu menu = new JPopupMenu();
        menu.setName("ContextMenuOf" + name);
        Map<String, KeyStroke> keyBounds = extractKeyBinds(target);
        AnyAction aa = new AnyAction(dst);
        for (JMenuItem o : Menu.createJMenuItems(res, name)) {
            if (o == null) {
                menu.add(new JSeparator());
                continue;
            }
            o.addActionListener(aa);
            final String itemId = o.getActionCommand();
            if (keyBounds.containsKey(itemId)) {
                o.setAccelerator(keyBounds.get(itemId));
            }
            menu.add(o);
        }
        if (target == null && dst instanceof JComponent) {
            ((JComponent)dst).setComponentPopupMenu(menu);
        } else if (target != null) {
            target.setComponentPopupMenu(menu);
        }
        if (dst instanceof PopupMenuListener) {
            menu.addPopupMenuListener((PopupMenuListener)dst);
        }
        return log.atExit("set", menu);
    }

    private static Map<String, KeyStroke> extractKeyBinds(JComponent c) {
        Map<String, KeyStroke> m = new HashMap<>();
        if (c != null) {
            InputMap imap = c.getInputMap();
            if (imap != null) {
                KeyStroke[] a = imap.allKeys();
                if (a != null) {
                    for (KeyStroke ks : a) {
                        m.put(String.valueOf(imap.get(ks)), ks);
                    }
                }
            }
        }
        return m;
    }

    static JPopupMenu createForText(JTextComponent text) {
        AnyAction aa = new AnyAction(text);
        return createForText(text, aa.setUndoAction());
    }

    static JPopupMenu createForText(JTextComponent text, UndoManager um) {
        JPopupMenu menu = new JPopupMenu();
        TextPopupMenuListener textPopupListener = new TextPopupMenuListener(text, um);
        for (JMenuItem o : Menu.createJMenuItems(res, "TextComponent")) {
            if (o == null) {
                menu.add(new JSeparator());
                continue;
            }
            menu.add(o);
            o.addActionListener(textPopupListener);
            textPopupListener.putPopupMenuItem(o.getActionCommand(), o);
        }
        menu.addPopupMenuListener(textPopupListener);
        text.setComponentPopupMenu(menu);
        return menu;
    }

    static final class TextPopupMenuListener implements ActionListener, PopupMenuListener {

        private final JTextComponent text;
        private final UndoManager um;
        private final Map<String, JMenuItem> itemMap;

        TextPopupMenuListener(JTextComponent c, UndoManager um) {
            this.text = c;
            this.um = um;
            this.itemMap = new HashMap<>();
        }

        @Override
        public void actionPerformed(ActionEvent ev) {
            final String cmd = ev.getActionCommand();
            final String key;
            if (cmd.equals("cut")) {
                key = "cut-to-clipboard";
            } else if (cmd.equals("copy")) {
                key = "copy-to-clipboard";
            } else if (cmd.equals("paste")) {
                key = "paste-from-clipboard";
            } else if (cmd.equals("selectAll")) {
                key = "select-all";
            } else {
                key = cmd;
            }
            text.getActionMap().get(key).actionPerformed(ev);
        }

        @Override
        public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
            itemMap.get("undo").setEnabled(um.canUndo());
            itemMap.get("redo").setEnabled(um.canRedo());
            final boolean textSelected = text.getSelectionEnd() > text.getSelectionStart();
            itemMap.get("cut").setEnabled(textSelected);
            itemMap.get("copy").setEnabled(textSelected);
        }

        @Override
        public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
            // empty
        }

        @Override
        public void popupMenuCanceled(PopupMenuEvent e) {
            // empty
        }

        void putPopupMenuItem(String key, JMenuItem item) {
            itemMap.put(key, item);
        }

    }

    enum ActionKey {
        // infotree
        copySimpleName,
        copyFullName,
        generateWherePhrase,
        generateSelectPhrase,
        generateUpdateStatement,
        generateInsertStatement,
        jumpToColumnByName,
        toggleShowColumnNumber,
        // rst
        copyWithEscape,
        clearSelectedCellValue,
        setCurrentTimeValue,
        copyColumnName,
        findColumnName,
        addEmptyRow,
        insertFromClipboard,
        duplicateRows,
        linkRowsToDatabase,
        deleteRows,
        adjustColumnWidth,
        sort,
        doNothing,
        // textarea
        submit,
        copyOrBreak,
        addNewLine,
        jumpToHomePosition,
        outputMessage,
        insertText,
        // others
        cut,
        copy,
        paste,
        selectAll,
        undo,
        redo,
        execute,
        refresh,
        newWindow,
        closeWindow,
        quit,
        find,
        toggleFocus,
        clearMessage,
        showStatusBar,
        showInfoTree,
        showColumnNumber,
        showAlwaysOnTop,
        widenColumnWidth,
        narrowColumnWidth,
        executeCommand,
        breakCommand,
        lastHistory,
        nextHistory,
        sendRollback,
        sendCommit,
        connect,
        disconnect,
        inputEcryptionKey,
        editConnectors,
        sortResult,
        importFile,
        exportFile,
        showHelp,
        showAbout
    }

}
