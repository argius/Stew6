package stew6.io;

import static org.junit.Assert.*;
import java.io.*;
import java.util.*;
import org.junit.*;

public final class CsvImporterTest {

    @Test
    public void testNextRow() throws IOException {
        CsvImporter importer = csv("name1,123%nname2,\"456 \"");
        assertArrayEquals0(a("name1", "123"), importer.nextRow());
        assertArrayEquals0(a("name2", "456 "), importer.nextRow());
        assertArrayEquals0(a(), importer.nextRow());
    }

    @Test
    public void testGetHeader() throws IOException {
        CsvImporter csvi = csv("name,number%nname1,135%nname2,246%n");
        assertArrayEquals0(a("name", "number"), csvi.getHeader());
        assertArrayEquals0(a("name1", "135"), csvi.nextRow());
        assertArrayEquals0(a("name2", "246"), csvi.nextRow());
        assertArrayEquals0(a(), csvi.nextRow());
        CsvImporter tsvi = tsv("field1\tfield2%nvalue1\tvalue2%n");
        assertArrayEquals0(a("field1", "field2"), tsvi.getHeader());
        assertArrayEquals0(a("value1", "value2"), tsvi.nextRow());
        assertArrayEquals0(a(), tsvi.nextRow());
    }

    @Test
    public void testClose() {
        CsvImporter csvi1 = csv("");
        try {
            csvi1.close();
        } catch (IOException e) {
            fail(e.toString());
        }
        CsvImporter csvi2 = new CsvImporter(new InputStream() {
            @Override
            public int read() throws IOException {
                return 0;
            }
            @Override
            public void close() throws IOException {
                throw new IOException("test");
            }
        }, ',');
        try {
            csvi2.close();
            fail("expects to fail");
        } catch (IOException e) {
            assertEquals("test", e.getMessage());
        }
    }

    static CsvImporter csv(String s) {
        StringReader r = new StringReader(String.format(s));
        CsvImporter importer = new CsvImporter(r, ',');
        return importer;
    }

    static CsvImporter tsv(String s) {
        StringReader r = new StringReader(String.format(s));
        CsvImporter importer = new CsvImporter(r, '\t');
        return importer;
    }

    @SuppressWarnings("unchecked")
    static <T> T[] a(T... a) {
        return a;
    }

    static <T> void assertArrayEquals0(T[] expected, T[] actual) {
        String msg = String.format("expected=%s, actual=%s", Arrays.asList(expected), Arrays.asList(actual));
        assertArrayEquals(msg, expected, actual);
    }

}
