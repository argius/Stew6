package stew6.ui.console;

import java.sql.*;
import java.util.*;
import stew6.*;
import stew6.text.*;
import stew6.ui.*;

/**
 * This is the implementation of OutputProcessor for console.
 */
public final class ConsoleOutputProcessor implements OutputProcessor {

    private static final int WIDTH_LIMIT = 30;

    private boolean quiet;

    @SuppressWarnings("resource")
    @Override
    public void output(Object o) {
        if (o instanceof ResultSetReference) {
            outputResult((ResultSetReference)o);
        } else if (o instanceof ResultSet) {
            outputResult(new ResultSetReference((ResultSet)o, ""));
        } else if (o instanceof Prompt) {
            if (!quiet) {
                System.err.print(o);
            }
        } else {
            if (!quiet) {
                System.out.println(o);
            }
        }
    }

    private static void outputResult(ResultSetReference ref) {
        try {
            // result
            @SuppressWarnings("resource")
            ResultSet rs = ref.getResultSet();
            ColumnOrder order = ref.getOrder();
            ResultSetMetaData rsmeta = rs.getMetaData();
            final boolean needsOrderChange = order.size() > 0;
            System.err.println();
            // column info
            final int columnCount = (needsOrderChange) ? order.size() : rsmeta.getColumnCount();
            int maxWidth = 1;
            StringBuilder borderFormat = new StringBuilder();
            for (int i = 0; i < columnCount; i++) {
                final int index = (needsOrderChange) ? order.getOrder(i) : i + 1;
                int size = rsmeta.getColumnDisplaySize(index);
                if (size > WIDTH_LIMIT) {
                    size = WIDTH_LIMIT;
                } else if (size < 1) {
                    size = 1;
                }
                maxWidth = Math.max(maxWidth, size);
                final int widthExpression = (SqlTypes.isRightAlign(rsmeta.getColumnType(index))) ? size : -size;
                final String format = "%" + widthExpression + "s";
                borderFormat.append(" " + format);
                if (i != 0) {
                    System.out.print(' ');
                }
                final String name = (needsOrderChange) ? order.getName(i) : rsmeta.getColumnName(index);
                System.out.print(PrintFormat.format(format, name));
            }
            System.out.println();
            // border
            String format = borderFormat.substring(1);
            char[] borderChars = new char[maxWidth];
            Arrays.fill(borderChars, '-');
            Object[] borders = new String[columnCount];
            Arrays.fill(borders, String.valueOf(borderChars));
            System.out.println(PrintFormat.format(format, borders));
            // beginning of loop
            Object[] a = new Object[columnCount];
            final int limit = App.props.getAsInt("rowcount.limit", Integer.MAX_VALUE);
            int count = 0;
            while (rs.next()) {
                if (count >= limit) {
                    System.err.println(ConsoleLauncher.res.format("w.exceeded-limit", limit));
                    break;
                }
                ++count;
                for (int i = 0; i < columnCount; i++) {
                    final int index = (needsOrderChange) ? order.getOrder(i) : i + 1;
                    a[i] = rs.getString(index);
                }
                System.out.println(PrintFormat.format(format, a));
            }
            System.out.println();
            // end of loop
            ref.setRecordCount(count);
        } catch (SQLException ex) {
            ex.printStackTrace(System.err);
        }
    }

    public void setQuiet(boolean quiet) {
        this.quiet = quiet;
    }

    @Override
    public void close() {
        // do nothing
    }

}
