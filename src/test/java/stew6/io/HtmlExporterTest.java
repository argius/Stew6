package stew6.io;

import static org.junit.Assert.*;
import static stew6.io.HtmlExporter.createStyleTag;
import java.io.*;
import org.junit.*;
import org.junit.rules.*;
import stew6.*;

public final class HtmlExporterTest {

    @Rule
    public TemporaryFolder tmpFolder = new TemporaryFolder();

    @Test
    public void testHtmlExporter() throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (HtmlExporter exporter = new HtmlExporter(bos, "")) {
            exporter.addHeader("name", "number");
            exporter.addRow("aaa", "123");
        }
        String html = bos.toString("UTF-8").replaceAll("[\r\n]", "");
        assertTrue("actual: "
                   + html,
                   html.matches("<!DOCTYPE html><html><head>"
                                + "<meta charset=\"utf-8\" /><meta name=\"generator\".+?></head>"
                                + "<body><h1>Exported  Data</h1><table>.+</table></body></html>"));
    }

    @Test
    public void testCreateStyleTag() throws IOException {
        assertEquals("<link rel=\"stylesheet\" href=\"\" />", createStyleTag("", false));
        assertEquals("<style>/* URI=dummy *//* java.nio.file.NoSuchFileException: dummy */</style>",
                     createStyleTag("dummy", true));
        File tmpFile = tmpFolder.newFile();
        TestUtils.writeLines(tmpFile.toPath(), "body { color: black }");
        assertEquals("<style>body { color: black }</style>",
                     createStyleTag(tmpFile.getAbsolutePath(), true).replaceAll("[\r\n]", ""));
    }

    @Ignore // surpress
    @Test
    public void testCreateStyleTag2() {
        String uri = "https://raw.githubusercontent.com/argius/Stew4/v4.2.0/markdown.css";
        String tag = createStyleTag(uri, true);
        String tag0 = tag.replaceAll("[\r\n]", "");
        assertTrue("actual: " + tag0, tag0.matches("<style>body \\{ .+; \\}</style>"));
        assertEquals(303, tag.length());
    }

}
