package stew6.io;

import java.io.*;
import java.net.*;
import java.nio.charset.*;
import java.nio.file.*;
import java.util.*;
import stew6.*;

/**
 * The Exporter for HTML.
 */
public final class HtmlExporter extends Exporter {

    private PrintWriter out;
    private String title;
    private boolean doneWriteBeginning;

    /**
     * A constructor.
     * @param os
     * @param title
     */
    public HtmlExporter(OutputStream os, String title) {
        super(os);
        this.out = new PrintWriter(new OutputStreamWriter(os, StandardCharsets.UTF_8));
        this.title = title;
    }

    private void ensureOpen0() throws IOException {
        ensureOpen();
        if (!doneWriteBeginning) {
            writeBeginning();
            doneWriteBeginning = true;
        }
    }

    private void writeBeginning() {
        String appName = App.res.s(".title");
        out.println("<!DOCTYPE html>");
        out.println("<html>");
        out.println("<head>");
        out.println("<meta charset=\"utf-8\" />");
        out.printf("<meta name=\"generator\" content=\"%s %s\" />%n", appName, App.getVersion());
        out.printf("<title>%s</title>%n", (title != null || title.trim().length() == 0) ? appName : title);
        String cssUri = App.props.get("io.html.css.uri", "");
        if (!cssUri.isEmpty()) {
            boolean requiresInline = App.props.getAsBoolean("io.html.css.inline");
            out.println(createStyleTag(cssUri, requiresInline));
        }
        out.println("</head>");
        out.println("<body>");
        String description = "Exported  Data";
        out.printf("<h1>%s</h1>%n", description);
        out.println("<table>");
        out.flush();
    }

    static String createStyleTag(String cssUri, boolean requiresInline) {
        StringBuilder style = new StringBuilder();
        if (requiresInline) {
            try {
                List<String> lines;
                if (cssUri.startsWith("http")) {
                    Path tmp = Files.createTempFile("stew-", ".tmp");
                    tmp.toFile().deleteOnExit();
                    URL url = new URI(cssUri).toURL();
                    try (InputStream is = url.openStream()) {
                        Files.copy(is, tmp, StandardCopyOption.REPLACE_EXISTING);
                    }
                    lines = Files.readAllLines(tmp, StandardCharsets.UTF_8);
                    Files.deleteIfExists(tmp);
                } else {
                    lines = Files.readAllLines(Paths.get(cssUri), StandardCharsets.UTF_8);
                }
                String eol = String.format("%n");
                style.append(eol).append(String.join(eol, lines)).append(eol);
            } catch (Exception ex) {
                style.append("/* URI=").append(cssUri).append(" */");
                style.append("/* ").append(ex).append(" */");
            }
            style.insert(0, "<style>");
            style.append("</style>");
        } else {
            style.append("<link rel=\"stylesheet\" href=\"");
            style.append(cssUri);
            style.append("\" />");
        }
        return style.toString();
    }

    @Override
    protected void writeHeader(Object[] header) throws IOException {
        ensureOpen0();
        out.println("<tr>");
        for (Object o : header) {
            out.printf("<th>%s</th>%n", o);
        }
        out.println("</tr>");
        out.flush();
    }

    @Override
    public void addRow(Object... values) throws IOException {
        ensureOpen0();
        out.println("<tr>");
        for (Object o : values) {
            out.printf("<td>%s</td>%n", o);
        }
        out.println("</tr>");
        out.flush();
    }

    @Override
    public void close() throws IOException {
        ensureOpen0();
        try {
            if (out != null) {
                out.println("</table>");
                out.println("</body>");
                out.println("</html>");
                out.flush();
                out.close();
            }
        } finally {
            out = null;
            super.close();
        }
    }

}
