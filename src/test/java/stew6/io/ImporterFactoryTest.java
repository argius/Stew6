package stew6.io;

import static org.junit.Assert.assertEquals;
import java.io.*;
import java.util.function.*;
import org.junit.*;
import org.junit.rules.*;

public final class ImporterFactoryTest {

    @Rule
    public TemporaryFolder tmpFolder = new TemporaryFolder();

    @Test
    public void testCreateImporter() {
        Function<String, Class<? extends Importer>> f = x -> {
            try {
                return ImporterFactory.createImporter(tmpFolder.newFile(x)).getClass();
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        };
        assertEquals(CsvImporter.class, f.apply("c1.csv"));
        assertEquals(CsvImporter.class, f.apply("c2.CSV"));
        assertEquals(XmlImporter.class, f.apply("x1.xml"));
        assertEquals(XmlImporter.class, f.apply("x2.XML"));
        assertEquals(CsvImporter.class, f.apply("a1.tsv"));
        assertEquals(CsvImporter.class, f.apply("a2.dat"));
    }

}
