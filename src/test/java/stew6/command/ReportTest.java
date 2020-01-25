package stew6.command;

import static org.junit.Assert.*;
import static stew6.TestUtils.*;
import java.sql.*;
import org.hamcrest.*;
import org.junit.*;
import org.junit.rules.*;
import net.argius.stew.*;
import stew6.*;

public final class ReportTest {

    @Rule
    public TemporaryFolder tmpFolder = new TemporaryFolder();

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    Command cmd = new Report();
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
            TestUtils.setConnectionToEnv(conn, env);
            executeCommand(cmd, conn, "TABLE1");
            assertThat(op.getOutputString(), Matchers.containsString("[1, ID, NO, BIGINT, 19, PUBLIC]"
                                                                     + "[2, NAME, YES, VARCHAR, 32, PUBLIC]"));
            executeCommand(cmd, conn, "-");
            assertThat(op.getOutputString(), Matchers.endsWith("SA@jdbc:h2:mem:test"));
            executeCommand(cmd, conn, "TABLE1 FULL");
            assertThat(op.getOutputString(),
                       Matchers.containsString("[TEST, PUBLIC, TABLE1, ID, -5, BIGINT, 19, 19, 0, 10, 0, , null,"
                                               + " -5, 0, 19, 1, NO, null, null, null, null, NO, NO]"
                                               + "[TEST, PUBLIC, TABLE1, NAME, 12, VARCHAR, 32, 32, 0, 10, 1, , null,"
                                               + " 12, 0, 32, 2, YES, null, null, null, null, NO, NO]"));
            assertEquals("", op.getOutputString());
            executeCommand(cmd, conn, "TABLE1 PK");
            assertThat(op.getOutputString(), Matchers.containsString("[TEST, PUBLIC, TABLE1, 1, ID, CONSTRAINT_9]"));
            executeCommand(cmd, conn, "TABLE1 INDEX");
            assertThat(op.getOutputString(), Matchers.containsString("[TEST, PUBLIC, TABLE1, 1, ID, PRIMARY_KEY_9]"));
        }
    }

    @Test
    public void testIsReadOnly() {
        assertTrue(cmd.isReadOnly());
    }

}
