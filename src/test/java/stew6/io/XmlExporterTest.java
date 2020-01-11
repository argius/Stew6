package stew6.io;

import static org.junit.Assert.fail;
import java.io.*;
import java.util.*;
import javax.xml.*;
import javax.xml.transform.stream.*;
import javax.xml.validation.*;
import org.junit.*;
import org.junit.rules.*;
import org.xml.sax.*;

public final class XmlExporterTest {

    @Rule
    public TemporaryFolder tmpFolder = new TemporaryFolder();

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Ignore
    @Test
    public void testXmlExporter() throws IOException, SAXException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        Date time = new Date(5000000 * 1000L);
        try (XmlExporter exporter = new XmlExporter(bos)) {
            exporter.addHeader("name", "number");
            exporter.addRow("test1", "567");
            exporter.addRow("test2", "890 ");
            exporter.addRow("nulltest", null);
            exporter.addRow("timetest", time);
        }
        // [Validation]
        SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        Validator validator;
        try (InputStream is = getClass().getResourceAsStream("stew-table.xsd")) {
            validator = factory.newSchema(new StreamSource(is)).newValidator();
        }
        // exported XML
        try {
            validator.validate(new StreamSource(new ByteArrayInputStream(bos.toByteArray())));
        } catch (Exception e) {
            e.printStackTrace();
            fail(String.format("%s%n%s", e, bos.toString()));
        }
        // invalid XML 1 (item tag)
        final String x1;
        x1 = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
             + "<!DOCTYPE table >"
             + "<table writer=\"stew5.io.XmlExporter\" /><item />";
        try {
            validator.validate(new StreamSource(new StringReader(x1)));
            fail("invalid XML should be thrown SAXException");
        } catch (SAXException ex) {
            // OK
        }
        // invalid XML 2 (no DOCTYPE)
        final String x2;
        x2 = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
             + "<table writer=\"stew5.io.XmlExporter\">"
             + "<dummy />"
             + "</table>";
        try {
            validator.validate(new StreamSource(new StringReader(x2)));
            fail("invalid XML should be thrown SAXException");
        } catch (SAXException ex) {
            // OK
        }
    }

    @Test
    public void testXmlExporterThrowsNullPointerException() throws IOException {
        thrown.expect(NullPointerException.class);
        try (XmlExporter exp = new XmlExporter(null)) {
            // skip
        }
    }

    @Test
    public void testWriteHeaderThrowsIOException() throws IOException {
        thrown.expect(IOException.class);
        thrown.expectMessage("RuntimeException: thrown by ErrorGenerator");
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        class ErrorGenerator {
            @Override
            public String toString() {
                throw new RuntimeException("thrown by ErrorGenerator");
            }
        }
        try (XmlExporter exp = new XmlExporter(bos)) {
            exp.addHeader(new Object[]{new ErrorGenerator()});
        }
    }

    @Test
    public void testAddRowThrowsIOException() throws IOException {
        thrown.expect(IOException.class);
        // thrown.expectMessage("Exception nor any of its super class is known to this context");
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (XmlExporter exp = new XmlExporter(bos)) {
            exp.addRow(new Object[]{new Exception()});
        }
    }

    @SafeVarargs
    static <T> T[] a(T... a) {
        return a;
    }

}
