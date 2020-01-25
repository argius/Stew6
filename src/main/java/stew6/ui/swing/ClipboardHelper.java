package stew6.ui.swing;

import java.awt.*;
import java.awt.datatransfer.*;
import java.io.*;
import stew6.*;

final class ClipboardHelper {

    private static final Logger log = Logger.getLogger(ClipboardHelper.class);

    private ClipboardHelper() {
        // ignore
    }

    static String getString() {
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        try {
            Object o = clipboard.getData(DataFlavor.stringFlavor);
            if (log.isTraceEnabled()) {
                log.trace("received from clipboard: [%s]", o);
            }
            return (String)o;
        } catch (UnsupportedFlavorException | IOException ex) {
            throw new RuntimeException("at ClipboardHelper.getString", ex);
        }
    }

    static void setStrings(Iterable<String> rows) {
        setString(String.join(String.format("%n"), rows));
    }

    static void setString(String s) {
        if (log.isTraceEnabled()) {
            log.trace("sending to clipboard: [%s]", s);
        }
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        StringSelection sselection = new StringSelection(s);
        clipboard.setContents(sselection, sselection);
    }

    static Reader getReaderForText() {
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        Transferable content = clipboard.getContents(null);
        try {
            return DataFlavor.stringFlavor.getReaderForText(content);
        } catch (UnsupportedFlavorException | IOException ex) {
            throw new RuntimeException("at ClipboardHelper.getReaderForText", ex);
        }
    }

}
