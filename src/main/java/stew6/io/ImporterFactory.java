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
     * @param file the file to import
     * @return an instance of Importer
     * @throws IOException
     */
    static Importer createImporter(File file) throws IOException {
        switch (FileUtilities.getExtension(file).toLowerCase()) {
            case "xml":
                return new XmlImporter(openFile(file));
            case "csv":
                return new CsvImporter(openFile(file));
            default:
                return new CsvImporter(openFile(file), '\t');
        }
    }

    private static InputStream openFile(File file) throws IOException {
        return new FileInputStream(file);
    }

}
