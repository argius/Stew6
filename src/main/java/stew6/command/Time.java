package stew6.command;

import java.sql.*;
import net.argius.stew.*;
import stew6.*;

/**
 * The Time command used to measure execution times.
 */
public final class Time extends Command {

    private static final Logger log = Logger.getLogger(Time.class);

    @Override
    public void execute(Connection conn, Parameter p) throws CommandException {
        if (!p.has(1)) {
            throw new UsageException(getUsage());
        }
        int argsIndex = 0;
        final String p1 = p.at(++argsIndex);
        final int times;
        if (p1.matches("\\d+")) {
            ++argsIndex;
            int number = 1;
            try {
                number = Integer.parseInt(p1);
            } catch (NumberFormatException ex) {
                log.warn("", ex);
            }
            times = number;
        } else {
            times = 1;
        }
        if (!p.has(argsIndex)) {
            throw new UsageException(getUsage());
        }
        final String sql = p.after(argsIndex);
        try {
            if (times > 1) {
                tryManyTimes(conn, sql, times);
            } else {
                tryOnce(conn, sql);
            }
        } catch (SQLException ex) {
            throw new CommandException(ex);
        }
    }

    private void tryOnce(Connection conn, String sql) throws SQLException {
        log.debug("tryOnce");
        try (Statement stmt = prepareStatement(conn, sql)) {
            final long beginningTime;
            final long endTime;
            if (isSelect(sql)) {
                beginningTime = System.currentTimeMillis();
                try (ResultSet rs = executeQuery(stmt, sql)) {
                    endTime = System.currentTimeMillis();
                }
            } else {
                beginningTime = System.currentTimeMillis();
                stmt.executeUpdate(sql);
                endTime = System.currentTimeMillis();
            }
            if (log.isDebugEnabled()) {
                log.debug("beginning: " + beginningTime);
                log.debug("      end: " + endTime);
            }
            outputMessage("Time.once", (endTime - beginningTime) / 1000f);
        }
    }

    private void tryManyTimes(Connection conn, String sql, int times) throws SQLException {
        log.debug("tryManyTimes");
        final boolean isSelect = isSelect(sql);
        try (Statement stmt = prepareStatement(conn, sql)) {
            long total = 0;
            long maximum = 0;
            long minimun = Long.MAX_VALUE;
            for (int i = 1; i <= times; i++) {
                final long beginningTime;
                final long endTime;
                log.trace("beginning: %d", i);
                if (isSelect) {
                    beginningTime = System.currentTimeMillis();
                    try (ResultSet rs = executeQuery(stmt, sql)) {
                        endTime = System.currentTimeMillis();
                    }
                } else {
                    beginningTime = System.currentTimeMillis();
                    stmt.executeUpdate(sql);
                    endTime = System.currentTimeMillis();
                }
                log.trace("      end: %d", i);
                final long result = endTime - beginningTime;
                total += result;
                maximum = Math.max(result, maximum);
                minimun = Math.min(result, minimun);
            }
            outputMessage("Time.summary", total / 1000f, total / 1000f / times, maximum / 1000f, minimun / 1000f);
        }
    }

}
