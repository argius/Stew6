package stew6.io;

import java.io.*;

/**
 * A factory of Importer.
 */
final class ImporterFactory {

    private ImporterFactory() {
        // empty
    }

    /**
     * Returns an Importer.
     * @param file
     * @return
     * @throws IOException
     */
    static Importer createImporter(File file) throws IOException {
        final String ext = FileUtilities.getExtension(file);
        if (ext.equalsIgnoreCase("xml")) {
            return new XmlImporter(openFile(file));
        } else if (ext.equalsIgnoreCase("csv")) {
            return new CsvImporter(openFile(file));
        } else {
            return new CsvImporter(openFile(file), '\t');
        }
    }

    private static InputStream openFile(File file) throws IOException {
        return new FileInputStream(file);
    }

}
