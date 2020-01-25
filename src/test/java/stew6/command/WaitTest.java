package stew6.command;

import static org.junit.Assert.assertThat;
import static stew6.TestUtils.*;
import java.sql.*;
import java.util.concurrent.*;
import org.hamcrest.*;
import org.junit.*;
import org.junit.rules.*;
import minestra.text.*;
import net.argius.stew.*;
import stew6.*;
import stew6.ui.console.*;

public final class WaitTest {

    private static final ResourceSheaf res = App.res.derive().withClass(Command.class);

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    Command cmd = new Wait();

    @Before
    public void initEnv() {
        Environment env = new Environment();
        env.setOutputProcessor(new ConsoleOutputProcessor());
        cmd.setEnvironment(env);
    }

    @Test
    public void testAll() throws SQLException {
        try (Connection conn = connection()) {
            executeCommand(cmd, conn, "0.1");
        }
    }

    @Ignore
    @Test
    public void testExecute() throws SQLException {
        try (Connection conn = connection()) {
            long t = System.currentTimeMillis();
            executeCommand(cmd, conn, "3.1");
            t = System.currentTimeMillis() - t;
            assertThat(t, Matchers.greaterThan(3000L));
            assertThat(t, Matchers.lessThan(3300L));
        }
    }

    @Test
    public void testUsageException() throws SQLException {
        try (Connection conn = connection()) {
            thrown.expect(UsageException.class);
            thrown.expectMessage(res.s("usage." + cmd.getClass().getSimpleName()));
            executeCommand(cmd, conn, "X");
        }
    }

    @Test
    public void testInterruptedException() throws SQLException {
        ScheduledExecutorService es = Executors.newSingleThreadScheduledExecutor();
        final Thread currThread = Thread.currentThread();
        es.schedule(() -> {
            currThread.interrupt();
        }, 500L, TimeUnit.MILLISECONDS);
        try (Connection conn = connection()) {
            thrown.expect(CommandException.class);
            thrown.expectCause(Matchers.any(InterruptedException.class));
            executeCommand(cmd, conn, "3");
        }
    }

}
