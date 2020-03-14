package stew6.io;

import java.io.*;

/**
 * A factory to create an Exporter.
 */
final class ExporterFactory {

    private ExporterFactory() { // empty, forbidden
    }

    /**
     * Returns an Exporter.
     * @param file the file to export
     * @return an instance of Exporter
     * @throws IOException
     */
    static Exporter createExporter(File file) throws IOException {
        switch (FileUtilities.getExtension(file).toLowerCase()) {
            case "xml":
                return new XmlExporter(openFile(file));
            case "htm":
            case "html":
                return new HtmlExporter(openFile(file), "");
            case "csv":
                return new CsvExporter(openFile(file));
            default:
                return new CsvExporter(openFile(file), '\t');
        }
    }

    private static OutputStream openFile(File file) throws IOException {
        return new FileOutputStream(file);
    }

}
