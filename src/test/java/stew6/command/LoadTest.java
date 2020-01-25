package stew6.command;

import static org.junit.Assert.assertThat;
import static stew6.TestUtils.*;
import java.io.*;
import java.sql.*;
import org.hamcrest.*;
import org.junit.*;
import org.junit.rules.*;
import net.argius.stew.*;
import stew6.*;
import stew6.io.*;

public class LoadTest {

    @Rule
    public TemporaryFolder tmpFolder = new TemporaryFolder();

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    Command cmd = new Load();
    Environment env = new Environment();
    StringBuilderOutputProcessor op = new StringBuilderOutputProcessor();

    @Before
    public void initEnv() {
        env.setOutputProcessor(op);
        cmd.setEnvironment(env);
    }

    @Test
    public void testExecute() throws Exception {
        final String testName = TestUtils.getCurrentMethodString(new Exception());
        File f1 = tmpFolder.newFile(testName + ".sql");
        File f2 = tmpFolder.newFile(testName + ".csv");
        try (Connection conn = connection()) {
            TestUtils.setConnectionToEnv(conn, env); // for using Commands.invoke
            // SQL select file
            TestUtils.writeLines(f1.toPath(), "select id || '+' || name from table1");
            executeCommand(cmd, conn, f1.getAbsolutePath());
            assertThat(op.getOutputString(), Matchers.startsWith("[1+argius]"));
            // data file
            TestUtils.writeLines(f2.toPath(), "2,Bob", "3,Chris");
            executeCommand(cmd, conn, f2.getAbsolutePath() + " table1");
            op.clearBuffer();
            Commands.invoke(env, "select id || '+' || name from table1 order by id");
            assertThat(op.getOutputString(), Matchers.containsString("[1+argius][2+Bob][3+Chris]"));
            // SQL update file
            TestUtils.writeLines(f1.toPath(), "update table1 set name='Davis' where id=3");
            executeCommand(cmd, conn, f1.getAbsolutePath());
            op.clearBuffer();
            Commands.invoke(env, "select id || '+' || name from table1 order by id");
            assertThat(op.getOutputString(), Matchers.containsString("[1+argius][2+Bob][3+Davis]"));
            conn.rollback();
        }
    }

    @Test
    public void testInsertRecords() throws Exception {
        final String testName = TestUtils.getCurrentMethodString(new Exception());
        Load cmdLoad = (Load)cmd;
        File f = tmpFolder.newFile(testName + ".csv");
        try (Connection conn = connection(); PreparedStatement stmt = conn.prepareStatement("insert into table1 values (?, ?)")) {
            TestUtils.setConnectionToEnv(conn, env); // for using Commands.invoke
            TestUtils.writeLines(f.toPath(), "2,Bob", "3,Chris");
            try (Importer importer = Importer.getImporter(f)) {
                cmdLoad.insertRecords(stmt, importer);
            }
            op.clearBuffer();
            Commands.invoke(env, "select id || '+' || name from table1 order by id");
            assertThat(op.getOutputString(), Matchers.containsString("[1+argius][2+Bob][3+Chris]"));
            // in case of error
            TestUtils.writeLines(f.toPath(), "1");
            try (Importer importer = Importer.getImporter(f)) {
                cmdLoad.insertRecords(stmt, importer);
            }
            assertThat(op.getOutputString(), Matchers.matchesPattern(".+0.+"));
            conn.rollback();
        }
    }

    @Test
    public void testUsageException() throws SQLException {
        try (Connection conn = connection()) {
            thrown.expect(UsageException.class);
            executeCommand(cmd, conn, "");
        }
    }

    @Test
    public void testIOException() throws Exception {
        try (Connection conn = connection()) {
            thrown.expect(CommandException.class);
            thrown.expectCause(Matchers.any(IOException.class));
            executeCommand(cmd, conn, "/");
        }
    }

    @Test
    public void testSQLException() throws Exception {
        final String testName = TestUtils.getCurrentMethodString(new Exception());
        File f1 = tmpFolder.newFile(testName + ".sql");
        try (Connection conn = connection()) {
            thrown.expect(CommandException.class);
            thrown.expectCause(Matchers.any(SQLException.class));
            TestUtils.setConnectionToEnv(conn, env); // for using Commands.invoke
            // SQL select file
            TestUtils.writeLines(f1.toPath(), "select id || '+' || name from tableX");
            executeCommand(cmd, conn, f1.getAbsolutePath());
        }
    }

}
