package stew6.command;

import java.sql.*;
import java.util.*;
import net.argius.stew.*;
import stew6.*;

/**
 * The Find command is used to search table names.
 * @see DatabaseMetaData#getTables(String, String, String, String[])
 */
public final class Find extends Command {

    private static final Logger log = Logger.getLogger(Find.class);

    @Override
    public void execute(Connection conn, Parameter p) throws CommandException {
        try {
            ResultSetReference ref = getResult(conn, p);
            try (ResultSet rs = ref.getResultSet()) {
                output(ref);
                outputMessage("i.selected", ref.getRecordCount());
            }
        } catch (SQLException ex) {
            throw new CommandException(ex);
        }
    }

    ResultSetReference getResult(Connection conn, Parameter p) throws SQLException {
        if (!p.has(1)) {
            throw new UsageException(getUsage());
        }
        final String p1 = p.at(1);
        final String p2 = p.at(2);
        final String p3 = p.at(3);
        final String p4 = p.at(4);
        final String p5 = p.at(5);
        DatabaseMetaData dbmeta = conn.getMetaData();
        final String tableNamePattern = editNamePattern(p1);
        final String[] tableTypes = editTableType(p2);
        final String schemaNamePattern = editNamePattern(p3);
        final String catalogNamePattern = editNamePattern(p4);
        final boolean isFull = p5.equalsIgnoreCase("FULL");
        if (log.isDebugEnabled()) {
            log.debug("name   : " + tableNamePattern);
            log.debug("types  : " + (tableTypes == null ? null : Arrays.asList(tableTypes)));
            log.debug("schema : " + schemaNamePattern);
            log.debug("catalog: " + catalogNamePattern);
            log.debug("full?  : " + isFull);
        }
        ResultSet rs = dbmeta.getTables(catalogNamePattern, schemaNamePattern, tableNamePattern, tableTypes);
        try {
            ResultSetReference ref = new ResultSetReference(rs, p.asString());
            if (!isFull) {
                ColumnOrder order = ref.getOrder();
                order.addOrder(3, getColumnName("name"));
                order.addOrder(4, getColumnName("type"));
                order.addOrder(2, getColumnName("schema"));
                order.addOrder(1, getColumnName("catalog"));
            }
            return ref;
        } catch (Throwable th) {
            try {
                rs.close();
            } catch (Exception ex) {
                log.warn("%s was thrown on running rs.close()", ex);
            }
            if (th instanceof SQLException) {
                throw (SQLException)th;
            } else if (th instanceof RuntimeException) {
                throw (RuntimeException)th;
            }
            throw new CommandException(th);
        }
    }

    @Override
    public boolean isReadOnly() {
        return true;
    }

    private static String getColumnName(String key) {
        return getMessage("Find.label." + key);
    }

    private static String[] editTableType(String pattern) {
        if (pattern == null || pattern.trim().length() == 0 || pattern.equals("*")) {
            return null;
        }
        return pattern.toUpperCase().split(",");
    }

    private String editNamePattern(String pattern) throws SQLException {
        if (pattern == null || pattern.trim().length() == 0) {
            return null;
        } else if (pattern.equals("''") || pattern.equals("\"\"")) {
            return "";
        }
        final String edited = convertPattern(pattern);
        if (log.isDebugEnabled()) {
            log.debug("table-name-condition : " + edited);
        }
        return edited;
    }

}
