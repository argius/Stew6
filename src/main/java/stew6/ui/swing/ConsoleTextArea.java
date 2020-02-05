package stew6.ui.swing;

import static java.awt.event.InputEvent.ALT_DOWN_MASK;
import static java.awt.event.KeyEvent.*;
import static javax.swing.KeyStroke.getKeyStroke;
import static stew6.ui.swing.AnyActionKey.*;
import static stew6.ui.swing.ConsoleTextArea.ActionKey.*;
import java.awt.datatransfer.*;
import java.awt.dnd.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;
import java.util.stream.*;
import javax.swing.*;
import javax.swing.text.*;
import javax.swing.text.Highlighter.*;
import javax.swing.undo.*;
import stew6.*;

/**
 * The console style text area.
 */
final class ConsoleTextArea extends JTextArea implements AnyActionListener, TextSearch {

    enum ActionKey {
        submit, copyOrBreak, addNewLine, jumpToHomePosition, outputMessage, insertText, doNothing;
    }

    private static final Logger log = Logger.getLogger(ConsoleTextArea.class);
    private static final String BREAK_PROMPT = "[BREAK] > ";

    private final AnyActionListener anyActionListener;
    private final UndoManager undoManager;

    private int homePosition;

    ConsoleTextArea(AnyActionListener anyActionListener) {
        // [Instances]
        this.anyActionListener = anyActionListener;
        this.undoManager = AnyAction.setUndoAction(this);
        ((AbstractDocument)getDocument()).setDocumentFilter(new ConsoleTextAreaDocumentFilter());
        // [Actions]
        final int shortcutKey = Utilities.getMenuShortcutKeyMask();
        AnyAction aa = new AnyAction(this);
        aa.setUndoAction();
        aa.bindSelf(submit, getKeyStroke(VK_ENTER, 0));
        aa.bindSelf(copyOrBreak, getKeyStroke(VK_C, shortcutKey));
        aa.bindSelf(breakCommand, getKeyStroke(VK_B, ALT_DOWN_MASK));
        aa.bindSelf(addNewLine, getKeyStroke(VK_ENTER, shortcutKey));
        aa.bindSelf(jumpToHomePosition, getKeyStroke(VK_HOME, 0));
        // [Events]
        class DropTargetAdapterImpl extends DropTargetAdapter {
            @Override
            public void drop(DropTargetDropEvent dtde) {
                Transferable t = dtde.getTransferable();
                if (t.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                    dtde.acceptDrop(DnDConstants.ACTION_COPY_OR_MOVE);
                    try {
                        @SuppressWarnings("unchecked")
                        List<File> fileList = (List<File>)t.getTransferData(DataFlavor.javaFileListFlavor);
                        final String s = fileList.stream().map(File::getAbsolutePath).collect(Collectors.joining(" "));
                        append(s + " ");
                    } catch (UnsupportedFlavorException | IOException ex) {
                        throw new RuntimeException(ex);
                    }
                }
            }
        }
        setDropTarget(new DropTarget(this, new DropTargetAdapterImpl()));
    }

    private final class ConsoleTextAreaDocumentFilter extends DocumentFilter {

        ConsoleTextAreaDocumentFilter() {
        } // empty

        @Override
        public void insertString(FilterBypass fb, int offset, String string,
                                 AttributeSet attr) throws BadLocationException {
            if (isEditablePosition(offset)) {
                super.insertString(fb, offset, string, attr);
            }
        }

        @Override
        public void remove(FilterBypass fb, int offset, int length) throws BadLocationException {
            if (isEditablePosition(offset)) {
                super.remove(fb, offset, length);
            }
        }

        @Override
        public void replace(FilterBypass fb, int offset, int length, String text,
                            AttributeSet attrs) throws BadLocationException {
            if (isEditablePosition(offset)) {
                super.replace(fb, offset, length, text, attrs);
            }
        }

    }

    @Override
    public void anyActionPerformed(AnyActionEvent ev) {
        log.atEnter("anyActionPerformed", ev);
        if (ev.isAnyOf(submit)) {
            final int ep = getEndPosition();
            if (getCaretPosition() == ep) {
                anyActionListener.anyActionPerformed(this, execute);
            } else {
                setCaretPosition(ep);
            }
        } else if (ev.isAnyOf(copyOrBreak)) {
            if (getSelectedText() == null) {
                sendBreak();
            } else {
                Action copyAction = new DefaultEditorKit.CopyAction();
                copyAction.actionPerformed(ev);
            }
        } else if (ev.isAnyOf(breakCommand)) {
            sendBreak();
        } else if (ev.isAnyOf(addNewLine)) {
            insert("\n", getCaretPosition());
        } else if (ev.isAnyOf(jumpToHomePosition)) {
            setCaretPosition(getHomePosition());
        } else if (ev.isAnyOf(insertText)) {
            if (!isEditablePosition(getCaretPosition())) {
                setCaretPosition(getEndPosition());
            }
            replaceSelection(String.join(" ", ev.getArgsAsStringList()));
            requestFocus();
        } else if (ev.isAnyOf(outputMessage)) {
            for (String s : ev.getArgsAsStringList()) {
                output(s);
            }
        } else if (ev.isAnyOf(doNothing)) {
            // do nothing
        } else {
            log.warn("not expected: Event=%s", ev);
        }
        log.atExit("anyActionPerformed");
    }

    boolean canUndo() {
        return undoManager.canUndo();
    }

    boolean canRedo() {
        return undoManager.canRedo();
    }

    /**
     * Appends text.
     * @param s
     * @param movesCaretToEnd true if it moves Caret to the end, otherwise false
     */
    void append(String s, boolean movesCaretToEnd) {
        super.append(s);
        if (movesCaretToEnd) {
            setCaretPosition(getEndPosition());
        }
    }

    /**
     * Outputs text.
     * @param s
     */
    void output(String s) {
        super.append(s);
        undoManager.discardAllEdits();
        homePosition = getEndPosition();
        setCaretPosition(homePosition);
    }

    /**
     * Replaces text from prompt to the end.
     * @param s
     */
    void replace(String s) {
        replaceRange(s, homePosition, getEndPosition());
    }

    /**
     * Prepares submitting.
     * Clears selection, moves cursor to end, and focuses this.
     */
    void prepareSubmitting() {
        final int ep = getEndPosition();
        setSelectionStart(ep);
        moveCaretPosition(ep);
        requestFocus();
    }

    /**
     * Clears text.
     */
    void clear() {
        homePosition = 0;
        setText("");
    }

    /**
     * Returns the text that is editable.
     * @return
     */
    String getEditableText() {
        try {
            return getText(homePosition, getEndPosition() - homePosition);
        } catch (BadLocationException ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     * Tests whether the specified position is editable.
     * @param position
     * @return
     */
    boolean isEditablePosition(int position) {
        return (position >= homePosition);
    }

    int getHomePosition() {
        return homePosition;
    }

    int getEndPosition() {
        Document document = getDocument();
        Position position = document.getEndPosition();
        return position.getOffset() - 1;
    }

    void resetHomePosition() {
        undoManager.discardAllEdits();
        homePosition = getEndPosition();
    }

    void sendBreak() {
        append(BREAK_PROMPT);
        resetHomePosition();
        validate();
    }

    @Override
    public void updateUI() {
        if (getCaret() == null) {
            super.updateUI();
        } else {
            final int p = getCaretPosition();
            super.updateUI();
            setCaretPosition(p);
        }
    }

    // text search

    @Override
    public boolean search(Matcher matcher) {
        removeHighlights();
        try {
            Highlighter highlighter = getHighlighter();
            HighlightPainter painter = TextSearch.Matcher.getHighlightPainter();
            final String text = getText();
            int start = 0;
            boolean matched = false;
            while (matcher.find(text, start)) {
                matched = true;
                int matchedIndex = matcher.getStart();
                highlighter.addHighlight(matchedIndex, matcher.getEnd(), painter);
                start = matchedIndex + 1;
            }
            addKeyListener(new TextSearchKeyListener());
            return matched;
        } catch (BadLocationException ex) {
            throw new RuntimeException(ex);
        }
    }

    private final class TextSearchKeyListener extends KeyAdapter {
        TextSearchKeyListener() {
        } // empty
        @Override
        public void keyTyped(KeyEvent e) {
            removeKeyListener(this);
            removeHighlights();
        }
    }

    @Override
    public void reset() {
        removeHighlights();
    }

    void removeHighlights() {
        for (Highlight highlight : getHighlighter().getHighlights()) {
            getHighlighter().removeHighlight(highlight);
        }
    }

}
