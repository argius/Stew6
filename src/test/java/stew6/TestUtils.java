package stew6;

import java.awt.*;
import java.io.*;
import java.nio.charset.*;
import java.nio.file.*;
import java.sql.*;
import java.util.*;
import java.util.List;
import net.argius.stew.*;
import stew6.ui.*;

public final class TestUtils {

    public static Parameter p(String paramString) {
        return new Parameter(paramString);
    }

    public static Connection connection() throws SQLException {
        return initOrReuseConnection();
//        Connection conn = DriverManager.getConnection("jdbc:h2:mem:test", "sa", "sa");
//        if (!inited) {
//            try (Statement stmt = conn.createStatement()) {
//                stmt.executeUpdate("create table table1 (id bigint primary key, name varchar(32))");
//                stmt.executeUpdate("insert into table1 values (1, 'argius')");
//                stmt.executeUpdate("create table table2 (id bigint primary key, filedata blob)");
//                stmt.executeUpdate("insert into table2 (id) values (1)");
//            }
//            conn.commit();
////            inited = true;
//        }
//        return conn;
    }

    private static final Object guardian = new Object();
    static Connection conn;

    private static Connection initOrReuseConnection() throws SQLException {
//        if (conn != null) {
//            return conn;
//        }
        synchronized (guardian) {
            Connection c;
            c = DriverManager.getConnection("jdbc:h2:mem:test", "sa", "sa");
            try (Statement stmt = c.createStatement()) {
                stmt.executeUpdate("create table table1 (id bigint primary key, name varchar(32))");
                stmt.executeUpdate("insert into table1 values (1, 'argius')");
            } catch (SQLException e) {
                // ignore
            }
            try (Statement stmt = c.createStatement()) {
                stmt.executeUpdate("create table table2 (id bigint primary key, filedata blob)");
                stmt.executeUpdate("insert into table2 (id) values (1)");
            } catch (SQLException e) {
                // ignore
            }
            c.commit();
            conn = c;
        }
        return conn;
    }

    public static String select(Connection conn, String sql) throws SQLException {
        try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                return rs.getString(1);
            }
        }
        return "";
    }

    public static void setConnectionToEnv(Connection conn, Environment env) {
        env.setCurrentConnection(conn);
        env.setCurrentConnector(new Connector("dummy", new Properties()));
    }

    public static String getCurrentMethodString(Throwable th) {
        StackTraceElement st = th.getStackTrace()[0];
        return String.format("%s.%s", st.getClassName(), st.getMethodName());
    }

    public static List<String> readAllLines(Path path) throws IOException {
        return Files.readAllLines(path, StandardCharsets.UTF_8);
    }

    @SafeVarargs
    public static <T extends CharSequence> void writeLines(Path path, T... a) throws IOException {
        Files.write(path, Arrays.asList(a), StandardCharsets.UTF_8);
    }

    public static void executeCommand(Command cmd, Connection conn, String parameterString) {
        cmd.execute(conn, p(cmd.getClass().getSimpleName() + " " + parameterString));
    }

    public static boolean isInHeadless() {
        if (App.props.getAsBoolean("debug.headless")) {
            return true;
        }
        return GraphicsEnvironment.isHeadless();
    }

    public static final class StringBuilderOutputProcessor implements OutputProcessor {

        private StringBuilder buffer = new StringBuilder();

        @Override
        public void output(Object o) {
            ensureOpen();
            if (o instanceof ResultSetReference) {
                outputResultSetReference((ResultSetReference)o);
            } else if (o instanceof ResultSet) {
                outputResultSetReference(new ResultSetReference((ResultSet)o, ""));
            } else {
                buffer.append(o);
            }
        }

        void outputResultSetReference(ResultSetReference ref) {
            try {
                ResultSet rs = ref.getResultSet();
                ColumnOrder order = ref.getOrder();
                final boolean needsOrderChange = order.size() > 0;
                ResultSetMetaData meta = rs.getMetaData();
                final int columnCount = (needsOrderChange) ? order.size() : meta.getColumnCount();
                final int limit = App.props.getAsInt("rowcount.limit", Integer.MAX_VALUE);
                int rowCount = 0;
                while (rs.next()) {
                    if (rowCount >= limit) {
                        break;
                    }
                    ++rowCount;
                    List<Object> a = new ArrayList<>();
                    for (int i = 0; i < columnCount; i++) {
                        final int index = needsOrderChange ? order.getOrder(i) : i + 1;
                        a.add(rs.getObject(index));
                    }
                    buffer.append(a);
                }
                ref.setRecordCount(rowCount);
            } catch (SQLException e) {
                buffer.append(e);
            }
        }

        @Override
        public void close() {
            clearBuffer();
            buffer = null;
        }

        public void clearBuffer() {
            ensureOpen();
            buffer.setLength(0);
        }

        public String getOutputString() {
            ensureOpen();
            String s = buffer.toString();
            clearBuffer();
            return s;
        }

        private void ensureOpen() {
            if (buffer == null) {
                throw new IllegalStateException("OutputProcessor already closed");
            }
        }

    }

}
