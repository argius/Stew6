package stew6.command;

import static org.junit.Assert.assertThat;
import static stew6.TestUtils.*;
import java.sql.*;
import org.hamcrest.*;
import org.junit.*;
import org.junit.rules.*;
import net.argius.stew.*;
import stew6.*;

public class TimeTest {

    @Rule
    public TemporaryFolder tmpFolder = new TemporaryFolder();

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    Command cmd = new Time();
    Environment env = new Environment();
    StringBuilderOutputProcessor op = new StringBuilderOutputProcessor();

    @Before
    public void initEnv() {
        env.setOutputProcessor(op);
        cmd.setEnvironment(env);
    }

    @Test
    public void testExecute() throws SQLException {
        try (Connection conn = connection()) {
            executeCommand(cmd, conn, "select id, substr(name, 0, 2) from table1");
            assertThat(op.getOutputString(), Matchers.matchesPattern(".+0\\.\\d{3}.+"));
            executeCommand(cmd, conn, "insert into table1 values (2, 'Bob')");
            assertThat(op.getOutputString(), Matchers.matchesPattern(".+0\\.\\d{3}.+"));
            executeCommand(cmd, conn, "2 select id, substr(name, 0, 2) from table1");
            assertThat(op.getOutputString(),
                       Matchers.matchesPattern("(?s).+0\\.\\d{3}.+0\\.\\d{3}.+0\\.\\d{3}.+0\\.\\d{3}.+"));
            executeCommand(cmd, conn, "3 update table1 set name = 'test'");
            assertThat(op.getOutputString(),
                       Matchers.matchesPattern("(?s).+0\\.\\d{3}.+0\\.\\d{3}.+0\\.\\d{3}.+0\\.\\d{3}.+"));
            conn.rollback();
        }
    }

    @Test
    public void testUsageException1() throws SQLException {
        try (Connection conn = connection()) {
            thrown.expect(UsageException.class);
            executeCommand(cmd, conn, "");
        }
    }

    @Test
    public void testUsageException2() throws SQLException {
        try (Connection conn = connection()) {
            thrown.expect(UsageException.class);
            executeCommand(cmd, conn, "1");
        }
    }

    @Test
    public void testCommandException() throws SQLException {
        try (Connection conn = connection()) {
            thrown.expect(CommandException.class);
            thrown.expectCause(Matchers.any(SQLException.class));
            thrown.expectMessage("Syntax error");
            executeCommand(cmd, conn, "select from");
        }
    }

}
