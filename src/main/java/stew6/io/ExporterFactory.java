package stew6.io;

import java.io.*;

/**
 * A factory to create an Exporter.
 */
final class ExporterFactory {

    private ExporterFactory() {
        // empty
    }

    /**
     * Returns an Exporter.
     * @param file
     * @return
     * @throws IOException
     */
    static Exporter createExporter(File file) throws IOException {
        final String ext = FileUtilities.getExtension(file);
        if (ext.equalsIgnoreCase("xml")) {
            return new XmlExporter(openFile(file));
        } else if (ext.equalsIgnoreCase("htm") || ext.equalsIgnoreCase("html")) {
            return new HtmlExporter(openFile(file), "");
        } else if (ext.equalsIgnoreCase("csv")) {
            return new CsvExporter(openFile(file));
        } else {
            return new CsvExporter(openFile(file), '\t');
        }
    }

    private static OutputStream openFile(File file) throws IOException {
        return new FileOutputStream(file);
    }

}
