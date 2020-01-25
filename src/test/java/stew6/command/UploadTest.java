package stew6.command;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;
import static stew6.TestUtils.*;
import static stew6.command.Upload.getModeOption;
import java.io.*;
import java.nio.file.*;
import java.sql.*;
import java.util.*;
import org.hamcrest.*;
import org.junit.*;
import org.junit.rules.*;
import net.argius.stew.*;
import stew6.*;
import stew6.ui.console.*;

public class UploadTest {

    @Rule
    public TemporaryFolder tmpFolder = new TemporaryFolder();

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    Command cmd = new Upload();
    Environment env = new Environment();

    @Before
    public void initEnv() {
        env.setOutputProcessor(new ConsoleOutputProcessor());
        cmd.setEnvironment(env);
    }

    @Test
    public void testExecute() throws Exception {
        final String testName = TestUtils.getCurrentMethodString(new Exception());
        try (Connection conn = TestUtils.connection()) {
            File dir = tmpFolder.newFolder(testName);
            File textFile = new File(dir, "t.txt");
            TestUtils.writeLines(textFile.toPath(), "uploadx");
            executeCommand(cmd, conn, " " + textFile + " update table1 set name=? where id=1");
            assertEquals("uploadx%n", select(conn, "select name from table1 where id = 1").replaceAll("\r?\n", "%n"));
            File binFile = new File(dir, "b.bin");
            byte[] binData = new byte[]{0x01, 0x02};
            Files.write(binFile.toPath(), binData);
            executeCommand(cmd, conn, " " + binFile + " B update table2 set filedata=? where id=1");
            @SuppressWarnings("resource")
            Download download = new Download();
            download.setEnvironment(env);
            executeCommand(download, conn, " " + dir + " select filedata, id, '.bin' from table2 where id=1");
            assertArrayEquals(binData, Files.readAllBytes(new File(dir, "1.bin").toPath()));
        }
    }

    @Test
    public void testUploadFile() throws Exception {
        final String testName = TestUtils.getCurrentMethodString(new Exception());
        try (Connection conn = TestUtils.connection()) {
            Upload cmdUpload = (Upload)cmd;
            File dir = tmpFolder.newFolder(testName);
            File textFile = new File(dir, "t.txt");
            TestUtils.writeLines(textFile.toPath(), "uploadx");
            File binFile = new File(dir, "b.txt");
            byte[] binData = "upload*test".getBytes();
            Files.write(binFile.toPath(), binData);
            try (PreparedStatement stmt = conn.prepareStatement("update table1 set name=? where id=1")) {
                cmdUpload.uploadFile(stmt, textFile, 2);
                String fetchSql = "select name from table1 where id = 1";
                assertEquals("uploadx%n", select(conn, fetchSql).replaceAll("\r?\n", "%n"));
                cmdUpload.uploadFile(stmt, binFile, 1);
                assertEquals("75706c6f61642a74657374", select(conn, fetchSql));
                cmdUpload.uploadFile(stmt, binFile, 0);
                assertEquals("75706c6f61642a74657374", select(conn, fetchSql));
            }
        }
    }

    @Test
    public void testGetModeOption() {
        for (String word : generateLetterCases("T", "TEXT")) {
            assertEquals(2, getModeOption(word));
        }
        for (String word : generateLetterCases("B", "BIN")) {
            assertEquals(1, getModeOption(word));
        }
        for (String word : generateLetterCases("UPDATE", "INSERT")) {
            assertEquals(0, getModeOption(word));
        }
        for (String word : generateLetterCases("X", "ABC")) {
            assertEquals(-1, getModeOption(word));
        }
    }

    static List<String> generateLetterCases(String... words) {
        List<String> a = new ArrayList<>();
        for (String word : words) {
            a.add(word.toUpperCase());
            a.add(word.toLowerCase());
            a.add(word.toUpperCase().substring(0, 1) + word.toLowerCase().substring(1));
        }
        return a;
    }

    @Test
    public void testExecuteUsageException1() throws SQLException {
        try (Connection conn = connection()) {
            thrown.expect(UsageException.class);
            executeCommand(cmd, conn, "");
        }
    }

    @Test
    public void testExecuteUsageException2() throws SQLException {
        try (Connection conn = connection()) {
            thrown.expect(UsageException.class);
            executeCommand(cmd, conn, "  1");
        }
    }

    @Ignore // this test does not pass on Unix-like OS
    @Test
    public void testExecuteIOException() throws SQLException {
        try (Connection conn = connection()) {
            thrown.expect(CommandException.class);
            thrown.expectCause(Matchers.<IOException> allOf(instanceOf(IOException.class),
                                                            hasProperty("message", containsString("("))));
            executeCommand(cmd, conn, " : select id from table1");
        }
    }

    @Test
    public void testExecuteSQLException() throws IOException, SQLException {
        File f = new File(tmpFolder.getRoot(), "1.txt");
        try (FileWriter out = new FileWriter(f)) {
            out.write("uploadx");
        }
        try (Connection conn = connection()) {
            thrown.expect(CommandException.class);
            thrown.expectCause(Matchers.<SQLException> allOf(instanceOf(SQLException.class),
                                                             hasProperty("message", containsString("Syntax error"))));
            executeCommand(cmd, conn, " " + f + " update table1 where");
        }
    }

}
