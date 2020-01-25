package stew6.command;

import java.sql.*;
import net.argius.stew.*;
import stew6.*;

/**
 * The Report command used to show database informations.
 */
@SuppressWarnings("resource")
public final class Report extends Command {

    private static final Logger log = Logger.getLogger(Report.class);

    @Override
    public void execute(Connection conn, Parameter p) throws CommandException {
        if (!p.has(1)) {
            throw new UsageException(getUsage());
        }
        try {
            final String p1 = p.at(1);
            if (p1.equals("-")) {
                reportDBInfo(conn);
            } else {
                try (ResultSetReference ref = getResult(conn, p)) {
                    output(ref);
                    outputMessage("i.selected", ref.getRecordCount());
                }
            }
        } catch (SQLException ex) {
            throw new CommandException(ex);
        }
    }

    @Override
    public boolean isReadOnly() {
        return true;
    }

    ResultSetReference getResult(Connection conn, Parameter p) throws SQLException {
        if (!p.has(1)) {
            throw new UsageException(getUsage());
        }
        final String cmd = p.asString();
        final String tableName = p.at(1);
        final String option = p.at(2);
        try {
            DatabaseMetaData dbmeta = conn.getMetaData();
            if (option.equalsIgnoreCase("FULL")) {
                return getTableFullDescription(dbmeta, tableName, cmd);
            } else if (option.equalsIgnoreCase("PK")) {
                return getPrimaryKeyInfo(dbmeta, tableName, cmd);
            } else if (option.equalsIgnoreCase("INDEX")) {
                return getIndexInfo(dbmeta, tableName, cmd);
            }
            return getTableDescription(dbmeta, tableName, cmd);
        } catch (Throwable th) {
            if (th instanceof SQLException) {
                throw (SQLException)th;
            } else if (th instanceof RuntimeException) {
                throw (RuntimeException)th;
            }
            throw new CommandException(th);
        }
    }

    private ResultSetReference getTableFullDescription(DatabaseMetaData dbmeta, String tableName,
                                                       String cmd) throws Throwable {
        if (log.isDebugEnabled()) {
            log.debug("report table-full-description of : " + tableName);
        }
        ResultSet rs = dbmeta.getColumns(null, null, convertPattern(tableName), null);
        try {
            return new ResultSetReference(rs, cmd);
        } catch (Throwable th) {
            rs.close();
            throw th;
        }
    }

    private ResultSetReference getTableDescription(DatabaseMetaData dbmeta, String tableName,
                                                   String cmd) throws Throwable {
        if (log.isDebugEnabled()) {
            log.debug("report table-description of : " + tableName);
        }
        ResultSet rs = dbmeta.getColumns(null, null, convertPattern(tableName), null);
        try {
            ResultSetReference ref = new ResultSetReference(rs, cmd);
            ColumnOrder order = ref.getOrder();
            order.addOrder(17, getColumnName("sequence"));
            order.addOrder(4, getColumnName("columnname"));
            order.addOrder(18, getColumnName("nullable"));
            order.addOrder(6, getColumnName("type"));
            order.addOrder(7, getColumnName("size"));
            order.addOrder(2, getColumnName("schema"));
            return ref;
        } catch (Throwable th) {
            rs.close();
            throw th;
        }
    }

    private ResultSetReference getPrimaryKeyInfo(DatabaseMetaData dbmeta, String tableName,
                                                 String cmd) throws Throwable {
        if (log.isDebugEnabled()) {
            log.debug("report primary-key of : " + tableName);
        }
        ResultSet rs = dbmeta.getPrimaryKeys(null, null, convertPattern(tableName));
        try {
            ResultSetReference ref = new ResultSetReference(rs, cmd);
            ColumnOrder order = ref.getOrder();
            order.addOrder(1, getColumnName("catalog"));
            order.addOrder(2, getColumnName("schema"));
            order.addOrder(3, getColumnName("tablename"));
            order.addOrder(5, getColumnName("sequence"));
            order.addOrder(4, getColumnName("columnname"));
            order.addOrder(6, getColumnName("keyname"));
            return ref;
        } catch (Throwable th) {
            rs.close();
            throw th;
        }
    }

    private ResultSetReference getIndexInfo(DatabaseMetaData dbmeta, String tableName, String cmd) throws Throwable {
        if (log.isDebugEnabled()) {
            log.debug("report index of : " + tableName);
        }
        ResultSet rs = dbmeta.getIndexInfo(null, null, convertPattern(tableName), false, false);
        try {
            ResultSetReference ref = new ResultSetReference(rs, cmd);
            ColumnOrder order = ref.getOrder();
            order.addOrder(1, getColumnName("catalog"));
            order.addOrder(2, getColumnName("schema"));
            order.addOrder(3, getColumnName("tablename"));
            order.addOrder(8, getColumnName("sequence"));
            order.addOrder(9, getColumnName("columnname"));
            order.addOrder(6, getColumnName("keyname"));
            return ref;
        } catch (Throwable th) {
            rs.close();
            throw th;
        }
    }

    private void reportDBInfo(Connection conn) throws SQLException {
        if (log.isDebugEnabled()) {
            log.debug("report dbinfo");
        }
        DatabaseMetaData meta = conn.getMetaData();
        final String userName = meta.getUserName();
        outputMessage("Report.dbinfo", meta.getDatabaseProductName(), meta.getDatabaseProductVersion(),
                      meta.getDriverName(), meta.getDriverVersion(), (userName == null) ? "" : userName, meta.getURL());
    }

    private static String getColumnName(String key) {
        return getMessage("Report.label." + key);
    }

}
