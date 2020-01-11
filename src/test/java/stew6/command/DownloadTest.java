package stew6.command;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;
import static stew6.TestUtils.*;
import static stew6.command.Download.getSizeString;
import java.io.*;
import java.nio.file.*;
import java.sql.*;
import org.hamcrest.*;
import org.junit.*;
import org.junit.rules.*;
import net.argius.stew.*;
import stew6.*;
import stew6.ui.console.*;

public final class DownloadTest {

    private static final String TBL = "download1"; // table name

    @Rule
    public TemporaryFolder tmpFolder = new TemporaryFolder();

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    Command cmd = new Download();

    @Before
    public void initEnv() {
        Environment env = new Environment();
        env.setOutputProcessor(new ConsoleOutputProcessor());
        cmd.setEnvironment(env);
    }

    @Test
    public void testExecute() throws Exception {
        final String testName = TestUtils.getCurrentMethodString(new Exception());
        String tmpFolderPath = tmpFolder.newFolder(testName).getAbsolutePath();
        try (Connection conn = connection()) {
            final String dir1 = tmpFolderPath + "/1";
            executeCommand(cmd, conn, dir1 + " select id, 'a.txt' from table1");
            final String filePathA = dir1 + "/a.txt";
            assertTrue("file does not exists: " + filePathA, Paths.get(filePathA).toFile().exists());
            final String dir2 = tmpFolderPath + "/2";
            executeCommand(cmd, conn, dir2 + " select name, 'b.txt' from table1");
            final String filePathB = dir2 + "/b.txt";
            assertTrue("file does not exists: " + filePathB, Paths.get(filePathB).toFile().exists());
            // data type check
            prepareTable(conn);
            final String dir3 = tmpFolderPath + "/3";
            executeCommand(cmd, conn, dir3 + " select content, id || '.txt' from " + TBL);
            assertEquals("abcde123", TestUtils.readAllLines(Paths.get(dir3 + "/1.txt")).get(0));
            assertEquals("fgh456", TestUtils.readAllLines(Paths.get(dir3 + "/2.txt")).get(0));
            final String dir4 = tmpFolderPath + "/4";
            executeCommand(cmd, conn, dir4 + " select updated_at, id || '.txt' from " + TBL);
            assertEquals("" + new Timestamp(1200000000000L), TestUtils.readAllLines(Paths.get(dir4 + "/1.txt")).get(0));
            assertEquals("" + new Timestamp(1300000000000L), TestUtils.readAllLines(Paths.get(dir4 + "/2.txt")).get(0));
            final String dir5 = tmpFolderPath + "/5";
            executeCommand(cmd, conn, dir5 + " select lob, id || '.txt' from " + TBL);
            assertEquals("LOB1", TestUtils.readAllLines(Paths.get(dir5 + "/1.txt")).get(0));
            assertEquals("LOB2", TestUtils.readAllLines(Paths.get(dir5 + "/2.txt")).get(0));
            conn.rollback();
        }
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
            executeCommand(cmd, conn, "1");
        }
    }

    @Ignore // this test does not pass on Unix-like OS
    @Test
    public void testExecuteIOException1() throws SQLException {
        // invalid path
        try (Connection conn = connection()) {
            thrown.expect(CommandException.class);
            thrown.expectCause(Matchers.<IOException> allOf(instanceOf(IOException.class),
                                                            hasProperty("message", containsString("("))));
            executeCommand(cmd, conn, " : select id from table1");
        }
    }

    @Test
    public void testExecuteIOException2() throws Exception {
        final String testName = TestUtils.getCurrentMethodString(new Exception());
        String tmpFolderPath = tmpFolder.newFolder(testName).getAbsolutePath();
        try (Connection conn = connection()) {
            // constant file name
            prepareTable(conn);
            final String dir1 = tmpFolderPath + "/io2/1";
            thrown.expect(CommandException.class);
            thrown.expectCause(Matchers.<IOException> any(IOException.class));
            executeCommand(cmd, conn, dir1 + " select id, 'a.txt' from " + TBL);
            conn.rollback();
        }
    }

    @Test
    public void testExecuteIOException3() throws Exception {
        final String testName = TestUtils.getCurrentMethodString(new Exception());
        try (Connection conn = connection()) {
            // fail to mkdirs
            File f = tmpFolder.newFile(testName);
            thrown.expect(CommandException.class);
            thrown.expectCause(Matchers.<IOException> any(IOException.class));
            executeCommand(cmd, conn, f.getAbsolutePath() + " select id, 'a.txt' from table1");
            conn.rollback();
        }
    }

    @Test
    public void testExecuteSQLException() throws SQLException {
        try (Connection conn = connection()) {
            thrown.expect(CommandException.class);
            thrown.expectCause(Matchers.<SQLException> allOf(instanceOf(SQLException.class),
                                                             hasProperty("message", containsString("Syntax error"))));
            executeCommand(cmd, conn, "1 select id from table1 where");
        }
    }

    @Test
    public void testExecuteCommandException() throws SQLException {
        try (Connection conn = connection()) {
            prepareTable(conn);
            thrown.expect(CommandException.class);
            thrown.expectMessage("unsupported type");
            executeCommand(cmd, conn, "1 select tags from " + TBL + " where id=1");
        }
    }

    @Test
    public void testGetSizeString() {
        assertEquals("0byte", getSizeString(0));
        assertEquals("1byte", getSizeString(1));
        assertEquals("2bytes", getSizeString(2));
        assertEquals("511bytes", getSizeString(511));
        assertEquals("0.5KB", getSizeString(512));
        assertEquals("0.502KB", getSizeString(514));
        assertEquals("0.515KB", getSizeString(527));
        assertEquals("0.999KB", getSizeString(1023));
        assertEquals("1KB", getSizeString(1024));
        assertEquals("1.001KB", getSizeString(1025));
        assertEquals("2KB", getSizeString(2048));
        assertEquals("10.001KB", getSizeString(10241));
        assertEquals("511.999KB", getSizeString(524287));
        assertEquals("0.5MB", getSizeString(524288));
        assertEquals("0.501MB", getSizeString(525289));
        assertEquals("0.501MB", getSizeString(525335));
        assertEquals("0.999MB", getSizeString(1047946));
        assertEquals("0.999MB", getSizeString(1047947));
        assertEquals("1MB", getSizeString(1048571));
        assertEquals("4.768MB", getSizeString(5000000));
        assertEquals("0.999GB", getSizeString(1073204953));
        assertEquals("1GB", getSizeString(1073204964));
    }

    static void prepareTable(Connection conn) throws SQLException {
        final String sql = "create table "
                           + TBL
                           + " (id bigint primary key, content varchar(256), updated_at timestamp, lob text,"
                           + " tags array)";
        int index;
        try (Statement stmt = conn.createStatement()) {
            stmt.executeUpdate(sql);
        }
        try (PreparedStatement stmt = conn.prepareStatement("insert into " + TBL + " values(?, ?, ?, ?, ?)")) {
            index = 0;
            stmt.setLong(++index, 1);
            stmt.setString(++index, "abcde123");
            stmt.setTimestamp(++index, new Timestamp(1200000000000L));
            stmt.setString(++index, "LOB1");
            stmt.setNull(++index, Types.ARRAY);
            stmt.executeUpdate();
            stmt.clearParameters();
            index = 0;
            stmt.setLong(++index, 2);
            stmt.setString(++index, "fgh456");
            stmt.setTimestamp(++index, new Timestamp(1300000000000L));
            stmt.setString(++index, "LOB2");
            stmt.setNull(++index, Types.ARRAY);
            stmt.executeUpdate();
            conn.commit();
        }
    }

}
