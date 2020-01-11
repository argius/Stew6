package stew6.command;

import java.io.*;
import java.sql.*;
import java.util.*;
import net.argius.stew.*;
import stew6.*;
import stew6.io.*;

/**
 * The Export command is used to export data to a file.
 * The data is the output of a command which is Select, Find, or Report.
 *
 * The export type will be automatically selected by file's extension.
 * @see Exporter
 */
public final class Export extends Command {

    private static final Logger log = Logger.getLogger(Export.class);

    @Override
    public void execute(Connection conn, Parameter p) throws CommandException {
        if (!p.has(2)) {
            throw new UsageException(getUsage());
        }
        final String path = p.at(1);
        int argsIndex = 2;
        final boolean withHeader = p.at(argsIndex).equalsIgnoreCase("HEADER");
        if (withHeader) {
            ++argsIndex;
        }
        final String cmd = p.after(argsIndex);
        if (log.isDebugEnabled()) {
            log.debug(String.format("file: [%s]", path));
            log.debug("withHeader: " + withHeader);
            log.debug(String.format("command: [%s]", cmd));
        }
        try {
            final File file = resolvePath(path);
            if (file.exists()) {
                throw new CommandException(getMessage("e.file-already-exists", file));
            }
            Parameter p2 = new Parameter(cmd);
            final String subCommand = p2.at(0);
            final ResultSetReference ref;
            if (subCommand.equalsIgnoreCase("SELECT")) {
                try (Statement stmt = prepareStatement(conn, cmd)) {
                    ref = new ResultSetReference(executeQuery(stmt, cmd), "");
                    export(file, ref, withHeader);
                }
            } else if (subCommand.equalsIgnoreCase("FIND")) {
                try (Find find = new Find()) {
                    find.setEnvironment(env);
                    ref = find.getResult(conn, p2);
                } catch (UsageException ex) {
                    throw new UsageException(getMessage("Export.command.usage",
                                                        getMessage("usage.Export"),
                                                        cmd,
                                                        ex.getMessage()));
                }
                try (ResultSet rs = ref.getResultSet()) {
                    export(file, ref, withHeader);
                }
            } else if (subCommand.equalsIgnoreCase("REPORT") && !p2.at(1).equals("-")) {
                try (Report report = new Report()) {
                    report.setEnvironment(env);
                    ref = report.getResult(conn, p2);
                } catch (UsageException ex) {
                    throw new UsageException(getMessage("Export.command.usage",
                                                        getMessage("usage.Export"),
                                                        cmd,
                                                        ex.getMessage()));
                }
                try (ResultSet rs = ref.getResultSet()) {
                    export(file, ref, withHeader);
                }
            } else {
                throw new UsageException(getUsage());
            }
            outputMessage("i.selected", ref.getRecordCount());
            outputMessage("i.exported");
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
    public boolean isReadOnly() {
        return true;
    }

    private static void export(File file, ResultSetReference ref, boolean withHeader) throws IOException, SQLException {
        try (Exporter exporter = Exporter.getExporter(file)) {
            ResultSet rs = ref.getResultSet();
            ColumnOrder order = ref.getOrder();
            boolean needOrderChange = order.size() > 0;
            int columnCount;
            List<String> header = new ArrayList<>();
            if (needOrderChange) {
                columnCount = order.size();
                for (int i = 0; i < columnCount; i++) {
                    header.add(order.getName(i));
                }
            } else {
                ResultSetMetaData m = rs.getMetaData();
                columnCount = m.getColumnCount();
                for (int i = 0; i < columnCount; i++) {
                    header.add(m.getColumnName(i + 1));
                }
            }
            if (withHeader) {
                exporter.addHeader(header.toArray());
            }
            int count = 0;
            while (rs.next()) {
                ++count;
                Object[] row = new Object[columnCount];
                for (int i = 0; i < columnCount; i++) {
                    int index = (needOrderChange) ? order.getOrder(i) : i + 1;
                    row[i] = rs.getObject(index);
                }
                exporter.addRow(row);
            }
            ref.setRecordCount(count);
        }
    }

}
