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
        } catch (UnsupportedFlavorException ex) {
            throw new RuntimeException("at ClipboardHelper.getString", ex);
        } catch (IOException ex) {
            throw new RuntimeException("at ClipboardHelper.getString", ex);
        }
    }

    static void setStrings(Iterable<String> rows) {
        StringWriter buffer = new StringWriter();
        PrintWriter out = new PrintWriter(buffer);
        try {
            for (String s : rows) {
                out.println(s);
            }
        } finally {
            out.close();
        }
        setString(buffer.toString().replaceFirst("[\\r\\n]+$", ""));
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
        } catch (UnsupportedFlavorException ex) {
            throw new RuntimeException("at ClipboardHelper.getReaderForText", ex);
        } catch (IOException ex) {
            throw new RuntimeException("at ClipboardHelper.getReaderForText", ex);
        }
    }

}