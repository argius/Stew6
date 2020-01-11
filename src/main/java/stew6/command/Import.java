package stew6.command;

import java.io.*;
import java.sql.*;
import java.util.*;
import net.argius.stew.*;
import stew6.*;
import stew6.io.*;

/**
 * Import command used to import a file into database.
 *
 * The export type will be automatically selected by file's extension:
 * *.csv as CSV, otherwise as TSV.
 *
 * Unlike Load command, this uses "executeBatch".
 */
public final class Import extends Load {

    private static final Logger log = Logger.getLogger(Import.class);
    private static final int DEFAULT_BATCH_LIMIT = 10000;

    @Override
    public void execute(Connection conn, Parameter p) throws CommandException {
        if (!p.has(2)) {
            throw new UsageException(getUsage());
        }
        final File file = resolvePath(p.at(1));
        final String table = p.at(2);
        final boolean hasHeader = p.at(3).equalsIgnoreCase("HEADER");
        if (log.isDebugEnabled()) {
            log.debug("file: " + file.getAbsolutePath());
            log.debug("table: " + table);
            log.debug("hasHeader: " + hasHeader);
        }
        try {
            loadRecord(conn, file, table, hasHeader);
        } catch (IOException ex) {
            throw new CommandException(ex);
        } catch (SQLException ex) {
            SQLException next = ex.getNextException();
            if (next != null && next != ex) {
                log.error(next, "next exception: ");
            }
            throw new CommandException(ex);
        }
    }

    @Override
    protected void insertRecords(PreparedStatement stmt, Importer importer) throws IOException, SQLException {
        final int batchLimit = App.props.getAsInt("command.Import.batch.limit", DEFAULT_BATCH_LIMIT);
        if (log.isDebugEnabled()) {
            log.debug("batch limit = " + batchLimit);
        }
        int recordCount = 0;
        int insertedCount = 0;
        int errorCount = 0;
        TypeConverter conv = new TypeConverter(true);
        List<Class<?>> types = getTypes(stmt);
        final boolean autoConversion = !types.isEmpty();
        while (true) {
            Object[] row = importer.nextRow();
            final boolean eof = row.length == 0;
            if (!eof) {
                ++recordCount;
                if (autoConversion) {
                    final int n = Math.min(types.size(), row.length);
                    for (int i = 0; i < n; i++) {
                        row[i] = conv.convertWithoutException(row[i], types.get(i));
                    }
                }
                try {
                    for (int i = 0; i < row.length; i++) {
                        stmt.setObject(i + 1, row[i]);
                    }
                    stmt.addBatch();
                } catch (SQLException ex) {
                    String message = "error occurred at " + recordCount;
                    if (log.isDebugEnabled()) {
                        log.debug(message + " : " + ex);
                    }
                    if (log.isTraceEnabled()) {
                        log.trace(ex);
                    }
                    ++errorCount;
                }
            }
            if (recordCount % batchLimit == 0 || eof) {
                int inserted = executeBatch(stmt);
                insertedCount += inserted;
                if (log.isDebugEnabled()) {
                    log.debug("record/inserted = " + recordCount + "/" + insertedCount);
                }
                if (eof) {
                    break;
                }
            }
        }
        if (errorCount > 0) {
            log.warn("error count = " + errorCount);
        }
        outputMessage("i.loaded", insertedCount, recordCount);
    }

    /**
     * Executes batch.
     * @param stmt
     * @return updated record count
     * @throws SQLException
     */
    private static int executeBatch(PreparedStatement stmt) throws SQLException {
        int[] results = stmt.executeBatch();
        int resultCount = 0;
        int noInfoCount = 0;
        int failedCount = 0;
        for (int i = 0; i < results.length; i++) {
            int result = results[i];
            switch (result) {
                case 1:
                    ++resultCount;
                    break;
                case Statement.SUCCESS_NO_INFO:
                    ++noInfoCount;
                    ++resultCount;
                    break;
                case Statement.EXECUTE_FAILED:
                    ++failedCount;
                    break;
                default:
                    throw new IllegalStateException("result=" + result);
            }
        }
        if (failedCount > 0) {
            log.warn("failedCount = " + failedCount);
        }
        if (noInfoCount > 0) {
            log.warn("noInfoCount = " + noInfoCount);
        }
        if (resultCount != results.length) {
            log.warn("array size = " + results.length + ", but result count = " + resultCount);
        }
        stmt.clearBatch();
        return resultCount;
    }

}
