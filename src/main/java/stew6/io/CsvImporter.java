package stew6.io;

import java.io.*;
import java.util.*;
import org.apache.commons.csv.*;
import stew6.*;

/**
 * CSV Importer implemented with commons-csv.
 */
public final class CsvImporter extends Importer {

    private CSVParser parser;
    private Iterator<CSVRecord> iterator;

    public CsvImporter(InputStream is) {
        this(is, ',');
    }

    public CsvImporter(InputStream is, char delimiter) {
        super(is);
        this.parser = parser(new InputStreamReader(is), delimiter);
        this.iterator = parser.iterator();
    }

    public CsvImporter(Reader r) {
        this(r, ',');
    }

    public CsvImporter(Reader r, char delimiter) {
        super(new InputStream() {
            @Override
            public int read() throws IOException {
                throw new IllegalStateException("cannot use");
            }
        });
        this.parser = parser(r, delimiter);
        this.iterator = parser.iterator();
    }

    static CSVParser parser(Reader r, char delimiter) {
        String formatName = App.props.get("io.csvformat", "Default");
        try {
            return new CSVParser(r, CSVFormat.valueOf(formatName).withDelimiter(delimiter));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Object[] nextRow() throws IOException {
        ensureOpen();
        if (iterator.hasNext()) {
            CSVRecord record = iterator.next();
            Object[] a = new Object[record.size()];
            for (int i = 0; i < a.length; i++) {
                a[i] = record.get(i);
            }
            return a;
        }
        return new Object[0];
    }

    @Override
    public void close() throws IOException {
        ensureOpen();
        try {
            parser.close();
        } finally {
            parser = null;
            super.close();
        }
    }

}
