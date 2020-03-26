package stew6.io;

import static org.junit.Assert.assertEquals;
import java.io.*;
import java.util.function.*;
import org.junit.*;
import org.junit.rules.*;

public final class ExporterFactoryTest {

    @Rule
    public TemporaryFolder tmpFolder = new TemporaryFolder();

    @Test
    public void testCreateExporter() {
        Function<String, Class<? extends Exporter>> f = x -> {
            try {
                return ExporterFactory.createExporter(tmpFolder.newFile(x)).getClass();
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        };
        assertEquals(CsvExporter.class, f.apply("c1.csv"));
        assertEquals(CsvExporter.class, f.apply("c2.CSV"));
        assertEquals(HtmlExporter.class, f.apply("h1.htm"));
        assertEquals(HtmlExporter.class, f.apply("h2.html"));
        assertEquals(HtmlExporter.class, f.apply("h3.html"));
        assertEquals(XmlExporter.class, f.apply("x1.xml"));
        assertEquals(XmlExporter.class, f.apply("x2.XML"));
        assertEquals(CsvExporter.class, f.apply("a1.tsv"));
        assertEquals(CsvExporter.class, f.apply("a2.dat"));
    }

}
