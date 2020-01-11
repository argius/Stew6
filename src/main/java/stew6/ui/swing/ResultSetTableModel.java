package stew6.ui.swing;

import static java.sql.Types.*;
import static java.util.Collections.nCopies;
import static stew6.text.TextUtilities.join;
import java.sql.*;
import java.util.*;
import java.util.Map.*;
import java.util.concurrent.*;
import java.util.regex.*;
import javax.swing.table.*;
import stew6.*;

/**
 * The TableModel for ResultSetTable.
 * It mainly provides to synchronize with databases.
 */
final class ResultSetTableModel extends DefaultTableModel {

    static final Logger log = Logger.getLogger(ResultSetTableModel.class);

    private static final long serialVersionUID = -8861356207097438822L;
    private static final String PTN1 = "\\s*SELECT\\s.+?\\sFROM\\s+([^\\s]+).*";
    private static final TypeConverter conv = new TypeConverter(true);

    private final int[] types;
    private final String commandString;

    private Connection conn;
    private Object tableName;
    private String[] primaryKeys;
    private boolean updatable;
    private boolean linkable;

    ResultSetTableModel(ResultSetReference ref) throws SQLException {
        super(0, getColumnCount(ref));
        ResultSet rs = ref.getResultSet();
        ColumnOrder order = ref.getOrder();
        final String cmd = ref.getCommandString();
        final boolean orderIsEmpty = order.size() == 0;
        ResultSetMetaData meta = rs.getMetaData();
        final int columnCount = getColumnCount();
        int[] types = new int[columnCount];
        for (int i = 0; i < columnCount; i++) {
            final int type;
            final String name;
            if (orderIsEmpty) {
                type = meta.getColumnType(i + 1);
                name = meta.getColumnName(i + 1);
            } else {
                type = meta.getColumnType(order.getOrder(i));
                name = order.getName(i);
            }
            types[i] = type;
            @SuppressWarnings({"unchecked", "unused"})
            Object o = columnIdentifiers.set(i, name);
        }
        this.types = types;
        this.commandString = cmd;
        try {
            analyzeForLinking(rs, cmd);
        } catch (Exception ex) {
            log.warn(ex);
        }
    }

    /**
     * UnlinkedRow is a marker object which indicates that the row is unlinked.
     */
    private static final class UnlinkedRow extends Vector<Object> {

        UnlinkedRow(int initialCapacity) {
            super(initialCapacity);
        }

        @SuppressWarnings("unused")
        @Deprecated
        UnlinkedRow(Vector<?> rowData) {
            super(rowData);
        }

        @SuppressWarnings("unused")
        @Deprecated
        UnlinkedRow(Object[] rowData) {
            super(rowData.length);
            for (final Object o : rowData) {
                add(o);
            }
        }

    }

    private static int getColumnCount(ResultSetReference ref) throws SQLException {
        final int size = ref.getOrder().size();
        return (size == 0) ? ref.getResultSet().getMetaData().getColumnCount() : size;
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
        switch (types[columnIndex]) {
            case DATE:
            case TIME:
            case TIMESTAMP:
                return Object.class;
            case BIT:
                return Object.class; // XXX ad-hoc workaround
        }
        return SqlTypes.toClass(types[columnIndex]);
    }

    @Override
    public boolean isCellEditable(int row, int column) {
        if (primaryKeys == null || primaryKeys.length == 0) {
            return false;
        }
        return super.isCellEditable(row, column);
    }

    @Override
    public void setValueAt(Object newValue, int row, int column) {
        if (!linkable) {
            return;
        }
        final Object oldValue = getValueAt(row, column);
        final boolean disableConv = App.props.getAsBoolean("disableConversion");
        final Object v;
        if (oldValue == null || disableConv) {
            v = newValue;
        } else {
            v = conv.convert(newValue, SqlTypes.toClass(types[column]));
        }
        final boolean changed = !Objects.deepEquals(oldValue, v);
        log.debug("oldValue=%s, newValue=%s, disableConversion=%s, changed=%s", oldValue, v, disableConv, changed);
        if (changed) {
            if (isLinkedRow(row)) {
                Object[] keys = columnIdentifiers.toArray();
                try {
                    executeUpdate(getRowData(keys, row), keys[column], v);
                } catch (Exception ex) {
                    log.error(ex);
                    throw new RuntimeException(ex);
                }
            } else {
                if (log.isTraceEnabled()) {
                    log.debug("update unlinked row");
                }
            }
        } else {
            if (log.isDebugEnabled()) {
                log.debug("skip to update");
            }
        }
        super.setValueAt(v, row, column);
    }

    void addUnlinkedRow(Object[] rowData) {
        addRow(createUnlinkedRow(Arrays.asList(rowData)));
    }

    void addUnlinkedRow(Vector<?> rowData) {
        addRow(createUnlinkedRow(rowData));
    }

    void insertUnlinkedRow(int row, Object[] rowData) {
        insertRow(row, createUnlinkedRow(Arrays.asList(rowData)));
    }

    void insertUnlinkedRow(int row, Vector<?> rowData) {
        insertRow(row, createUnlinkedRow(rowData));
    }

    UnlinkedRow createUnlinkedRow(List<?> rowData) {
        final boolean disableConv = App.props.getAsBoolean("disableConversion");
        final int n = rowData.size();
        UnlinkedRow row = new UnlinkedRow(n);
        for (int i = 0; i < n; i++) {
            Object o = rowData.get(i);
            final Object v;
            if (o == null || disableConv) {
                v = o;
            } else {
                v = conv.convert(o, SqlTypes.toClass(types[i]));
            }
            row.add(v);
        }
        return row;
    }

    /**
     * Links a row with database.
     * @param rowIndex
     * @return true if it successed, false if already linked
     * @throws SQLException failed to link by SQL error
     */
    boolean linkRow(int rowIndex) throws SQLException {
        if (isLinkedRow(rowIndex)) {
            return false;
        }
        executeInsert(getRowData(columnIdentifiers.toArray(), rowIndex));
        @SuppressWarnings("unchecked")
        Vector<Object> rows = getDataVector();
        rows.set(rowIndex, new Vector<>((Vector<?>)rows.get(rowIndex)));
        return true;
    }

    /**
     * Removes a linked row.
     * @param rowIndex
     * @return true if it successed, false if already linked
     * @throws SQLException failed to link by SQL error
     */
    boolean removeLinkedRow(int rowIndex) throws SQLException {
        if (!isLinkedRow(rowIndex)) {
            return false;
        }
        executeDelete(getRowData(columnIdentifiers.toArray(), rowIndex));
        super.removeRow(rowIndex);
        return true;
    }

    private Map<Object, Object> getRowData(Object[] keys, int rowIndex) {
        Map<Object, Object> rowData = new LinkedHashMap<>();
        for (int columnIndex = 0, n = keys.length; columnIndex < n; columnIndex++) {
            rowData.put(keys[columnIndex], getValueAt(rowIndex, columnIndex));
        }
        return rowData;
    }

    /**
     * Sorts this table.
     * @param columnIndex
     * @param descending
     */
    void sort(final int columnIndex, boolean descending) {
        final int f = (descending) ? -1 : 1;
        @SuppressWarnings("unchecked")
        List<List<Object>> dataVector = getDataVector();
        Collections.sort(dataVector, new RowComparator(f, columnIndex));
    }

    private static final class RowComparator implements Comparator<List<Object>> {

        private final int f;
        private final int columnIndex;

        RowComparator(int f, int columnIndex) {
            this.f = f;
            this.columnIndex = columnIndex;
        }

        @Override
        public int compare(List<Object> row1, List<Object> row2) {
            return c(row1, row2) * f;
        }

        private int c(List<Object> row1, List<Object> row2) {
            if (row1 == null || row2 == null) {
                return row1 == null ? row2 == null ? 0 : -1 : 1;
            }
            final Object o1 = row1.get(columnIndex);
            final Object o2 = row2.get(columnIndex);
            if (o1 == null || o2 == null) {
                return o1 == null ? o2 == null ? 0 : -1 : 1;
            }
            if (o1 instanceof Comparable<?> && o1.getClass() == o2.getClass()) {
                @SuppressWarnings("unchecked")
                Comparable<Object> c1 = (Comparable<Object>)o1;
                @SuppressWarnings("unchecked")
                Comparable<Object> c2 = (Comparable<Object>)o2;
                return c1.compareTo(c2);
            }
            return o1.toString().compareTo(o2.toString());
        }

    }

    /**
     * Checks whether this table is updatable.
     * @return
     */
    boolean isUpdatable() {
        return updatable;
    }

    /**
     * Checks whether this table is linkable.
     * @return
     */
    boolean isLinkable() {
        return linkable;
    }

    /**
     * Checks whether the specified row is linked.
     * @param rowIndex
     * @return
     */
    boolean isLinkedRow(int rowIndex) {
        return !(getDataVector().get(rowIndex) instanceof UnlinkedRow);
    }

    /**
     * Checks whether this table has unlinked rows.
     * @return
     */
    boolean hasUnlinkedRows() {
        for (final Object row : getDataVector()) {
            if (row instanceof UnlinkedRow) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks whether specified connection is same as the connection it has.
     * @return
     */
    boolean isSameConnection(Connection conn) {
        return conn == this.conn;
    }

    /**
     * Returns the command string that creates this.
     * @return
     */
    String getCommandString() {
        return commandString;
    }

    private void executeUpdate(Map<Object, Object> keyMap, Object targetKey, Object targetValue) throws SQLException {
        final String sql = String.format("UPDATE %s SET %s=? WHERE %s",
                                         tableName,
                                         quoteIfNeeds(targetKey),
                                         toKeyPhrase(primaryKeys));
        List<Object> a = new ArrayList<>();
        a.add(targetValue);
        for (Object pk : primaryKeys) {
            a.add(keyMap.get(pk));
        }
        executeSql(sql, a.toArray());
    }

    private void executeInsert(Map<Object, Object> rowData) throws SQLException {
        final int dataSize = rowData.size();
        List<Object> keys = new ArrayList<>(dataSize);
        List<Object> values = new ArrayList<>(dataSize);
        for (Entry<?, ?> entry : rowData.entrySet()) {
            keys.add(quoteIfNeeds(String.valueOf(entry.getKey())));
            values.add(entry.getValue());
        }
        final String sql = String.format("INSERT INTO %s (%s) VALUES (%s)",
                                         tableName,
                                         join(",", keys),
                                         join(",", nCopies(dataSize, "?")));
        executeSql(sql, values.toArray());
    }

    private void executeDelete(Map<Object, Object> keyMap) throws SQLException {
        final String sql = String.format("DELETE FROM %s WHERE %s", tableName, toKeyPhrase(primaryKeys));
        List<Object> a = new ArrayList<>();
        for (Object pk : primaryKeys) {
            a.add(keyMap.get(pk));
        }
        executeSql(sql, a.toArray());
    }

    private void executeSql(final String sql, final Object[] parameters) throws SQLException {
        if (log.isDebugEnabled()) {
            log.debug("SQL: " + sql);
            log.debug("parameters: " + Arrays.asList(parameters));
        }
        final CountDownLatch latch = new CountDownLatch(1);
        final List<SQLException> errors = new ArrayList<>();
        final Connection conn = this.conn;
        final int[] types = this.types;
        // asynchronous execution
        class SqlTask implements Runnable {
            @Override
            public void run() {
                try {
                    if (conn.isClosed()) {
                        throw new SQLException(ResourceManager.Default.get("e.not-connect"));
                    }
                    try (final PreparedStatement stmt = conn.prepareStatement(sql)) {
                        ValueTransporter transporter = ValueTransporter.getInstance("");
                        int index = 0;
                        for (Object o : parameters) {
                            boolean isNull = false;
                            if (o == null || String.valueOf(o).length() == 0) {
                                if (getColumnClass(index) != String.class) {
                                    isNull = true;
                                }
                            }
                            ++index;
                            if (isNull) {
                                stmt.setNull(index, types[index - 1]);
                            } else {
                                transporter.setObject(stmt, index, o);
                            }
                        }
                        final int updatedCount = stmt.executeUpdate();
                        if (updatedCount != 1) {
                            throw new SQLException("updated count is not 1, but " + updatedCount);
                        }
                    }
                } catch (SQLException ex) {
                    log.error(ex);
                    errors.add(ex);
                } catch (Throwable th) {
                    log.error(th);
                    SQLException ex = new SQLException();
                    ex.initCause(th);
                    errors.add(ex);
                }
                latch.countDown();
            }
        }
        AnyAction.doParallel(new SqlTask());
        try {
            // waits for a task to stop
            latch.await(3L, TimeUnit.SECONDS);
        } catch (InterruptedException ex) {
            throw new RuntimeException(ex);
        }
        if (latch.getCount() != 0) {
            class SqlTaskErrorHandler implements Runnable {
                @Override
                public void run() {
                    try {
                        latch.await();
                    } catch (InterruptedException ex) {
                        log.warn(ex);
                    }
                    if (!errors.isEmpty()) {
                        WindowOutputProcessor.showErrorDialog(null, errors.get(0));
                    }
                }
            }
            AnyAction.doParallel(new SqlTaskErrorHandler());
        } else if (!errors.isEmpty()) {
            if (log.isDebugEnabled()) {
                for (final Exception ex : errors) {
                    log.debug("", ex);
                }
            }
            throw new SQLException(errors.get(0));
        }
    }

    private static String toKeyPhrase(Object[] keys) {
        List<String> a = new ArrayList<>(keys.length);
        for (final Object key : keys) {
            a.add(String.format("%s=?", key));
        }
        return join(" AND ", a);
    }

    private static String quoteIfNeeds(Object o) {
        final String s = String.valueOf(o);
        if (s.matches(".*\\W.*")) {
            return String.format("\"%s\"", s);
        }
        return s;
    }

    private void analyzeForLinking(ResultSet rs, String cmd) throws SQLException {
        if (rs == null) {
            return;
        }
        Statement stmt = rs.getStatement();
        if (stmt == null) {
            return;
        }
        Connection conn = stmt.getConnection();
        if (conn == null) {
            return;
        }
        this.conn = conn;
        if (conn.isReadOnly()) {
            return;
        }
        final String tableName = findTableName(cmd);
        if (tableName.length() == 0) {
            return;
        }
        this.tableName = tableName;
        this.updatable = true;
        List<String> pkList = findPrimaryKeys(conn, tableName);
        if (pkList.isEmpty()) {
            return;
        }
        @SuppressWarnings("unchecked")
        final Collection<Object> columnIdentifiers = this.columnIdentifiers;
        if (!columnIdentifiers.containsAll(pkList)) {
            return;
        }
        if (findUnion(cmd)) {
            return;
        }
        this.primaryKeys = pkList.toArray(new String[pkList.size()]);
        this.linkable = true;
    }

    /**
     * Finds a table name.
     * @param cmd command string or SQL
     * @return table name if it found only a table, or empty string
     */
    static String findTableName(String cmd) {
        if (cmd != null) {
            StringBuilder buffer = new StringBuilder();
            try (Scanner scanner = new Scanner(cmd)) {
                while (scanner.hasNextLine()) {
                    final String line = scanner.nextLine();
                    buffer.append(line.replaceAll("/\\*.*?\\*/|//.*", ""));
                    buffer.append(' ');
                }
            }
            Pattern p = Pattern.compile(PTN1, Pattern.CASE_INSENSITIVE);
            Matcher m = p.matcher(buffer);
            if (m.matches()) {
                String afterFrom = m.group(1);
                String[] words = afterFrom.split("\\s");
                boolean foundComma = false;
                for (int i = 0; i < 2 && i < words.length; i++) {
                    String word = words[i];
                    if (word.indexOf(',') >= 0) {
                        foundComma = true;
                    }
                }
                if (!foundComma) {
                    String word = words[0];
                    if (word.matches("[A-Za-z0-9_\\.]+")) {
                        return word;
                    }
                }
            }
        }
        return "";
    }

    private static List<String> findPrimaryKeys(Connection conn, String tableName) throws SQLException {
        DatabaseMetaData dbmeta = conn.getMetaData();
        final String cp0;
        final String sp0;
        final String tp0;
        if (tableName.contains(".")) {
            String[] splitted = tableName.split("\\.");
            if (splitted.length >= 3) {
                cp0 = splitted[0];
                sp0 = splitted[1];
                tp0 = splitted[2];
            } else {
                cp0 = null;
                sp0 = splitted[0];
                tp0 = splitted[1];
            }
        } else {
            cp0 = null;
            sp0 = dbmeta.getUserName();
            tp0 = tableName;
        }
        final String cp;
        final String sp;
        final String tp;
        if (dbmeta.storesLowerCaseIdentifiers()) {
            cp = (cp0 == null) ? null : cp0.toLowerCase();
            sp = (sp0 == null) ? null : sp0.toLowerCase();
            tp = tp0.toLowerCase();
        } else if (dbmeta.storesUpperCaseIdentifiers()) {
            cp = (cp0 == null) ? null : cp0.toUpperCase();
            sp = (sp0 == null) ? null : sp0.toUpperCase();
            tp = tp0.toUpperCase();
        } else {
            cp = cp0;
            sp = sp0;
            tp = tp0;
        }
        if (cp == null && sp == null) {
            return getPrimaryKeys(dbmeta, null, null, tp);
        }
        List<String> a = getPrimaryKeys(dbmeta, cp, sp, tp);
        if (a.isEmpty()) {
            return getPrimaryKeys(dbmeta, null, null, tp);
        }
        return a;
    }

    private static List<String> getPrimaryKeys(DatabaseMetaData dbmeta,
                                               String catalog,
                                               String schema,
                                               String table) throws SQLException {
        try (ResultSet rs = dbmeta.getPrimaryKeys(catalog, schema, table)) {
            List<String> pkList = new ArrayList<>();
            Set<String> schemaSet = new HashSet<>();
            while (rs.next()) {
                pkList.add(rs.getString(4));
                schemaSet.add(rs.getString(2));
            }
            if (schemaSet.size() != 1) {
                return Collections.emptyList();
            }
            return pkList;
        }
    }

    private static boolean findUnion(String sql) {
        String s = sql;
        if (s.indexOf('\'') >= 0) {
            if (s.indexOf("\\'") >= 0) {
                s = s.replaceAll("\\'", "");
            }
            s = s.replaceAll("'[^']+'", "''");
        }
        StringTokenizer tokenizer = new StringTokenizer(s);
        while (tokenizer.hasMoreTokens()) {
            if (tokenizer.nextToken().equalsIgnoreCase("UNION")) {
                return true;
            }
        }
        return false;
    }

}
