package stew6.command;

import static stew6.text.TextUtilities.join;
import java.io.*;
import java.sql.*;
import java.util.*;
import net.argius.stew.*;
import stew6.*;
import stew6.io.*;

/**
 * The Load command is used to execute SQL from a file.
 *
 * This command has two mode:
 *   if it gived one argument, it will execute SQL read from a file,
 *   or it it will load data from file.
 *
 * The file type to load will be automatically selected by file's extension:
 * @see Importer
 */
public class Load extends Command {

    private static final Logger log = Logger.getLogger(Load.class);

    @Override
    public void execute(Connection conn, Parameter p) throws CommandException {
        if (!p.has(1)) {
            throw new UsageException(getUsage());
        }
        try {
            final File file = resolvePath(p.at(1));
            if (log.isDebugEnabled()) {
                log.debug("file: " + file.getAbsolutePath());
            }
            if (p.has(2)) {
                final String table = p.at(2);
                final boolean hasHeader = p.at(3).equalsIgnoreCase("HEADER");
                if (log.isDebugEnabled()) {
                    log.debug("table: " + table);
                    log.debug("hasHeader: " + hasHeader);
                }
                loadRecord(conn, file, table, hasHeader);
            } else {
                loadSql(conn, file);
            }
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

    private void loadSql(Connection conn, File file) throws IOException, SQLException {
        final String sql = FileUtilities.readAllBytesAsString(file);
        if (log.isDebugEnabled()) {
            log.debug("sql : " + sql);
        }
        try (Statement stmt = prepareStatement(conn, sql)) {
            if (isSelect(sql)) {
                try (ResultSet rs = executeQuery(stmt, sql)) {
                    ResultSetReference ref = new ResultSetReference(rs, sql);
                    output(ref);
                    outputMessage("i.selected", ref.getRecordCount());
                }
            } else {
                final int count = stmt.executeUpdate(sql);
                outputMessage("i.proceeded", count);
            }
        }
    }

    protected void loadRecord(Connection conn,
                              File file,
                              String tableName,
                              boolean hasHeader) throws IOException, SQLException {
        try (Importer importer = Importer.getImporter(file)) {
            final Object[] header;
            if (hasHeader) {
                header = importer.getHeader();
            } else {
                try (Importer importer2 = Importer.getImporter(file)) {
                    Object[] a = importer2.nextRow();
                    Arrays.fill(a, "");
                    header = a;
                }
            }
            final List<Object> headerList = Arrays.asList(header);
            final String columns = (hasHeader) ? String.format("(%s)", join(",", headerList)) : "";
            final List<Object> valueList = new ArrayList<>(headerList);
            Collections.fill(valueList, "?");
            final String sql = String.format("INSERT INTO %s %s VALUES (%s)", tableName, columns, join(",", valueList));
            if (log.isDebugEnabled()) {
                log.debug("SQL : " + sql);
            }
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                insertRecords(stmt, importer);
            }
        }
    }

    protected List<Class<?>> getTypes(PreparedStatement stmt) {
        final boolean disableConv = App.props.getAsBoolean("disableConversion");
        if (!disableConv) {
            try {
                ParameterMetaData meta = stmt.getParameterMetaData();
                final int n = meta.getParameterCount();
                List<Class<?>> types = new ArrayList<>(n);
                for (int i = 0; i < n; i++) {
                    types.add(SqlTypes.toClass(meta.getParameterType(i + 1)));
                }
                return types;
            } catch (SQLException ex) {
                log.warn("failed to create type list: %s", ex);
            }
        }
        return Collections.emptyList();
    }

    protected void insertRecords(PreparedStatement stmt, Importer importer) throws IOException, SQLException {
        int recordCount = 0;
        int insertedCount = 0;
        int errorCount = 0;
        TypeConverter conv = new TypeConverter(true);
        List<Class<?>> types = getTypes(stmt);
        final boolean autoConversion = !types.isEmpty();
        while (true) {
            Object[] row = importer.nextRow();
            if (row == null || row.length == 0) {
                break;
            }
            ++recordCount;
            if (autoConversion) {
                final int n = Math.min(types.size(), row.length);
                for (int i = 0; i < n; i++) {
                    row[i] = conv.convertWithoutException(row[i], types.get(i));
                }
            }
            try {
                for (int i = 0; i < row.length; i++) {
                    Object o = autoConversion ? conv.convertWithoutException(row[i], types.get(i)) : row[i];
                    stmt.setObject(i + 1, o);
                }
                insertedCount += stmt.executeUpdate();
            } catch (SQLException ex) {
                String message = "error occurred at " + recordCount;
                if (log.isTraceEnabled()) {
                    log.trace(message, ex);
                } else if (log.isDebugEnabled()) {
                    log.debug(message + " : " + ex);
                }
                ++errorCount;
            }
        }
        if (log.isDebugEnabled()) {
            log.debug("record   = " + recordCount);
            log.debug("inserted = " + insertedCount);
            log.debug("error    = " + errorCount);
        }
        outputMessage("i.loaded", insertedCount, recordCount);
    }

}
