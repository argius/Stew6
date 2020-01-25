package net.argius.stew;

import java.io.*;
import java.nio.channels.*;
import java.sql.*;
import java.util.*;
import minestra.text.*;
import stew6.*;
import stew6.io.*;
import stew6.ui.*;

/**
 * The skeletal implementation of the Command.
 */
public abstract class Command implements AutoCloseable {

    protected Environment env;
    protected OutputProcessor op;

    private static final ResourceSheaf res = App.res.derive().withClass(Command.class);

    /**
     * A constructor.
     */
    protected Command() {
        // empty
    }

    /**
     * Initializes this.
     * @throws CommandException
     */
    public void initialize() throws CommandException {
        // empty
    }

    /**
     * Executes this command.
     * @param conn
     * @param p
     * @throws CommandException
     */
    public abstract void execute(Connection conn, Parameter p) throws CommandException;

    /**
     * Closes this command.
     * Overwrite this to tear down and to do post processes.
     * @throws CommandException
     */
    @Override
    public void close() throws CommandException {
        // empty
    }

    /**
     * Returns whether this command is read-only or not.
     * Overwrite this method to change the read-only.
     * @return
     */
    @SuppressWarnings("static-method")
    public boolean isReadOnly() { // overridable
        return false;
    }

    /**
     * Sets the timeout.
     * It only sets when the config value is more than zero.
     * @param stmt Statement
     * @throws SQLException
     * @see Statement#setQueryTimeout(int)
     */
    protected void setTimeout(Statement stmt) throws SQLException {
        final int timeoutSeconds = env.getTimeoutSeconds();
        if (timeoutSeconds >= 0) {
            stmt.setQueryTimeout(timeoutSeconds);
        }
    }

    /**
     * Sets Environment.
     * @param env
     */
    public final void setEnvironment(Environment env) {
        this.env = env;
        this.op = env.getOutputProcessor();
    }

    /**
     * Resolves the path.
     * If the path is relative, this method will convert it to an absolute path to the env's current dir.
     * @param path
     * @return
     */
    protected final File resolvePath(String path) {
        return FileUtilities.resolve(env.getCurrentDirectory(), path);
    }

    /**
     * Resolves the path.
     * If the path is relative, this method will convert it to an absolute path to the env's current dir.
     * @param file
     * @return
     */
    protected final File resolvePath(File file) {
        return FileUtilities.resolve(env.getCurrentDirectory(), file);
    }

    /**
     * Outputs an Object.
     * @param object
     * @throws CommandException
     */
    protected final void output(Object object) throws CommandException {
        op.output(object);
    }

    /**
     * Outputs the message specified by that message-key.
     * @param key
     * @param args
     * @throws CommandException
     */
    protected final void outputMessage(String key, Object... args) throws CommandException {
        output(getMessage(key, args));
    }

    /**
     * Returns the message specified by that message-key.
     * @param key
     * @param args
     * @return
     */
    protected static String getMessage(String key, Object... args) {
        return res.format(key, args);
    }

    /**
     * Converts a pattern string.
     * It converts with considering identifier that depends on each databases.
     * @param pattern
     * @return
     * @throws SQLException
     */
    protected final String convertPattern(String pattern) throws SQLException {
        String edited;
        DatabaseMetaData dbmeta = env.getCurrentConnection().getMetaData();
        if (dbmeta.storesLowerCaseIdentifiers()) {
            edited = pattern.toLowerCase();
        } else if (dbmeta.storesUpperCaseIdentifiers()) {
            edited = pattern.toUpperCase();
        } else {
            edited = pattern;
        }
        return edited.replace('*', '%').replace('?', '_');
    }

    /**
     * Returns USAGE.
     * @return
     */
    protected String getUsage() {
        return getMessage("usage." + getClass().getSimpleName());
    }

    /**
     * Returns whether executing SQL may return ResutSet or not.
     * @param sql
     * @return
     */
    public static boolean mayReturnResultSet(String sql) {
        return startsTableValueConstructor(sql) || isSelect(sql);
    }

    /**
     * Returns whether SQL starts Table Value Constructor or not.
     * @param sql
     * @return
     */
    public static boolean startsTableValueConstructor(String sql) {
        String s = sql.replaceAll("[\r\n]", " ").replaceAll("/\\*.*?\\*/", "");
        return s.matches("(?i)\\s*VALUES\\s*?\\(.*");
    }

    /**
     * Returns whether SQL is SELECT.
     * @param sql
     * @return
     */
    public static boolean isSelect(String sql) {
        try (Scanner scanner = new Scanner(sql)) {
            while (scanner.hasNextLine()) {
                final String line = scanner.nextLine();
                final String s = line.replaceAll("/\\*.*?\\*/", "");
                if (s.matches("\\s*") || s.matches("\\s*--.*")) {
                    continue;
                }
                if (!s.matches("\\s*") && s.matches("(?i)\\s*SELECT.*")) {
                    return true;
                }
                break;
            }
        }
        return false;
    }

    /**
     * Returns the string that read from file.
     * @param file
     * @return
     * @throws IOException
     */
    @Deprecated
    protected static String readFileAsString(File file) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        @SuppressWarnings("resource")
        FileInputStream fis = new FileInputStream(file);
        try {
            fis.getChannel().transferTo(0, file.length(), Channels.newChannel(bos));
        } finally {
            fis.close();
        }
        return bos.toString();
    }

    /**
     * Prepares Statement.
     * @param conn
     * @param sql
     * @return
     * @throws SQLException
     */
    protected final Statement prepareStatement(Connection conn, String sql) throws SQLException {
        final int index = sql.indexOf(';');
        @SuppressWarnings("resource")
        Statement stmt = (index >= 0) ? conn.prepareStatement(sql.substring(0, index)) : conn.createStatement();
        try {
            if (stmt instanceof PreparedStatement) {
                PreparedStatement pstmt = (PreparedStatement)stmt;
                int i = 0;
                for (String p : sql.substring(index + 1).split(",", -1)) {
                    pstmt.setString(++i, p);
                }
            }
            setTimeout(stmt);
            final int limit = App.props.getAsInt("rowcount.limit", Integer.MAX_VALUE);
            if (limit > 0 && limit != Integer.MAX_VALUE) {
                stmt.setMaxRows(limit + 1);
            }
        } catch (Throwable th) {
            try {
                if (th instanceof SQLException) {
                    throw (SQLException)th;
                }
                throw new IllegalStateException(th);
            } finally {
                stmt.close();
            }
        }
        return stmt;
    }

    /**
     * Executes a query.
     * @param stmt
     * @param sql
     * @return
     * @throws SQLException
     */
    @SuppressWarnings("static-method")
    protected ResultSet executeQuery(Statement stmt, String sql) throws SQLException {
        return (stmt instanceof PreparedStatement) ? ((PreparedStatement)stmt).executeQuery() : stmt.executeQuery(sql);
    }

    /**
     * Executes Update(SQL).
     * @param stmt
     * @param sql
     * @return
     * @throws SQLException
     */
    @SuppressWarnings("static-method")
    protected int executeUpdate(Statement stmt, String sql) throws SQLException {
        return (stmt instanceof PreparedStatement) ? ((PreparedStatement)stmt).executeUpdate()
                                                   : stmt.executeUpdate(sql);
    }

}
