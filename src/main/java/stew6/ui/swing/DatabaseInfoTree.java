package stew6.ui.swing;

import static java.awt.EventQueue.invokeLater;
import static java.awt.event.InputEvent.*;
import static java.awt.event.KeyEvent.VK_C;
import static java.util.Collections.*;
import static javax.swing.KeyStroke.getKeyStroke;
import static stew6.ui.swing.AnyActionKey.*;
import static stew6.ui.swing.AnyActionKey.copy;
import static stew6.ui.swing.DatabaseInfoTree.ActionKey.*;
import static stew6.ui.swing.WindowOutputProcessor.showWideMessageDialog;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.sql.*;
import java.text.*;
import java.util.*;
import java.util.List;
import java.util.Map.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.tree.*;
import minestra.text.*;
import stew6.*;
import stew6.sql.*;

/**
 * The Database Information Tree is a tree pane that provides to
 * display database object information from DatabaseMetaData.
 */
final class DatabaseInfoTree extends JTree implements AnyActionListener, TextSearch {

    enum ActionKey {
        copySimpleName,
        copyFullName,
        generateWherePhrase,
        generateSelectPhrase,
        generateUpdateStatement,
        generateInsertStatement,
        jumpToColumnByName,
        toggleShowColumnNumber
    }

    static final Logger log = Logger.getLogger(DatabaseInfoTree.class);

    private static final ResourceSheaf res = WindowLauncher.res.derive().withClass(DatabaseInfoTree.class);

    private static final String TABLE_TYPE_TABLE = "TABLE";
    private static final String TABLE_TYPE_VIEW = "VIEW";
    private static final String TABLE_TYPE_INDEX = "INDEX";
    private static final String TABLE_TYPE_SEQUENCE = "SEQUENCE";
    // @formatter:off
    static final List<String> DEFAULT_TABLE_TYPES = Arrays.asList(TABLE_TYPE_TABLE, TABLE_TYPE_VIEW, TABLE_TYPE_INDEX, TABLE_TYPE_SEQUENCE);
    // @formatter:on

    static volatile boolean showColumnNumber;

    private Connector currentConnector;
    private DatabaseMetaData dbmeta;
    private AnyActionListener anyActionListener;

    DatabaseInfoTree(AnyActionListener anyActionListener) {
        this.anyActionListener = anyActionListener;
        setRootVisible(false);
        setShowsRootHandles(false);
        setScrollsOnExpand(true);
        setCellRenderer(new Renderer());
        setModel(new DefaultTreeModel(null));
        // [Events]
        int sckm = Utilities.getMenuShortcutKeyMask();
        AnyAction aa = new AnyAction(this);
        aa.bindKeyStroke(false, copy, KeyStroke.getKeyStroke(VK_C, sckm));
        aa.bindSelf(copySimpleName, getKeyStroke(VK_C, sckm | ALT_DOWN_MASK));
        aa.bindSelf(copyFullName, getKeyStroke(VK_C, sckm | SHIFT_DOWN_MASK));
    }

    @Override
    protected void processMouseEvent(MouseEvent e) {
        super.processMouseEvent(e);
        if (e.getID() == MouseEvent.MOUSE_CLICKED && e.getClickCount() % 2 == 0) {
            anyActionPerformed(this, jumpToColumnByName);
        }
        if (e.getID() == MouseEvent.MOUSE_CLICKED) {
            if (SwingUtilities.isLeftMouseButton(e)) {
                int sckm = Utilities.getMenuShortcutKeyMask();
                if ((e.getModifiers() & sckm) == sckm) {
                    selectionModel.setSelectionPath(getPathForRow(getRowForLocation(e.getX(), e.getY())));
                    anyActionPerformed(this, showLimitedRecords);
                }
            }
        }
    }

    @Override
    public void anyActionPerformed(AnyActionEvent ev) {
        log.atEnter("anyActionPerformed", ev);
        if (ev.isAnyOf(copy)) {
            final String cmd = ev.getActionCommand();
            Action action = getActionMap().get(cmd);
            if (action != null) {
                action.actionPerformed(new ActionEvent(this, 1001, cmd));
            }
        } else if (ev.isAnyOf(copySimpleName)) {
            copySimpleName();
        } else if (ev.isAnyOf(copyFullName)) {
            copyFullName();
        } else if (ev.isAnyOf(refresh)) {
            for (TreePath path : getSelectionPaths()) {
                refresh((InfoNode)path.getLastPathComponent());
            }
        } else if (ev.isAnyOf(generateWherePhrase)) {
            List<ColumnNode> columnNodes = new ArrayList<>();
            for (TreeNode node : getSelectionNodes()) {
                if (node instanceof ColumnNode) {
                    columnNodes.add((ColumnNode)node);
                }
            }
            final String phrase = generateEquivalentJoinClause(columnNodes);
            if (phrase.length() > 0) {
                insertTextIntoTextArea(addCommas(phrase));
            }
        } else if (ev.isAnyOf(generateSelectPhrase)) {
            final String phrase = generateSelectPhrase(getSelectionNodes());
            if (phrase.length() > 0) {
                insertTextIntoTextArea(phrase);
            }
        } else if (ev.isAnyOf(generateUpdateStatement, generateInsertStatement)) {
            final boolean isInsert = ev.isAnyOf(generateInsertStatement);
            try {
                final String phrase = generateUpdateOrInsertPhrase(getSelectionNodes(), isInsert);
                if (phrase.length() > 0) {
                    if (isInsert) {
                        insertTextIntoTextArea(addCommas(phrase));
                    } else {
                        insertTextIntoTextArea(phrase);
                    }
                }
            } catch (IllegalArgumentException ex) {
                showWideMessageDialog(this, ex.getMessage());
            }
        } else if (ev.isAnyOf(jumpToColumnByName)) {
            jumpToColumnByName();
        } else if (ev.isAnyOf(toggleShowColumnNumber)) {
            showColumnNumber = !showColumnNumber;
            repaint();
        } else if (ev.isAnyOf(showLimitedRecords)) {
            try {
                showLimitedRecords();
            } catch (SQLException ex) {
                showWideMessageDialog(this, ex.getMessage());
            }
        } else {
            log.warn("not expected: Event=%s", ev);
        }
        log.atExit("anyActionPerformed");
    }

    @Override
    public TreePath[] getSelectionPaths() {
        TreePath[] a = super.getSelectionPaths();
        if (a == null) {
            return new TreePath[0];
        }
        return a;
    }

    List<TreeNode> getSelectionNodes() {
        List<TreeNode> a = new ArrayList<>();
        for (TreePath path : getSelectionPaths()) {
            a.add((TreeNode)path.getLastPathComponent());
        }
        return a;
    }

    private void insertTextIntoTextArea(String s) {
        anyActionListener.anyActionPerformed(this, ConsoleTextArea.ActionKey.insertText, s);
    }

    private void copySimpleName() {
        TreePath[] paths = getSelectionPaths();
        if (paths == null || paths.length == 0) {
            return;
        }
        List<String> names = new ArrayList<>(paths.length);
        for (TreePath path : paths) {
            if (path == null) {
                continue;
            }
            Object o = path.getLastPathComponent();
            assert o instanceof InfoNode;
            final String name;
            if (o instanceof ColumnNode) {
                name = ((ColumnNode)o).getName();
            } else if (o instanceof TableNode) {
                name = ((TableNode)o).getName();
            } else {
                name = o.toString();
            }
            names.add(name);
        }
        ClipboardHelper.setStrings(names);
    }

    private void copyFullName() {
        TreePath[] paths = getSelectionPaths();
        if (paths == null || paths.length == 0) {
            return;
        }
        List<String> names = new ArrayList<>(paths.length);
        for (TreePath path : paths) {
            if (path == null) {
                continue;
            }
            Object o = path.getLastPathComponent();
            assert o instanceof InfoNode;
            names.add(((InfoNode)o).getNodeFullName());
        }
        ClipboardHelper.setStrings(names);
    }

    private void jumpToColumnByName() {
        TreePath[] paths = getSelectionPaths();
        if (paths == null || paths.length == 0) {
            return;
        }
        final TreePath path = paths[0];
        Object o = path.getLastPathComponent();
        if (o instanceof ColumnNode) {
            ColumnNode node = (ColumnNode)o;
            anyActionListener.anyActionPerformed(this, ResultSetTable.ActionKey.jumpToColumn, node.getName());
        }
    }

    private static String addCommas(String phrase) {
        int c = 0;
        for (final char ch : phrase.toCharArray()) {
            if (ch == '?') {
                ++c;
            }
        }
        if (c >= 1) {
            return String.format("%s;%s", phrase, String.join("", nCopies(c - 1, ",")));
        }
        return phrase;
    }

    static String generateEquivalentJoinClause(List<ColumnNode> nodes) {
        if (nodes.isEmpty()) {
            return "";
        }
        List<Column> columns = new ArrayList<>();
        List<Clause> clauses = new ArrayList<>();
        for (ColumnNode node : nodes) {
            final String tableName = node.getTableNode().getNodeFullName();
            final String columnName = node.getName();
            Column c = Column.of(columnName, Table.of(tableName));
            columns.add(c);
            clauses.add(Clause.implicit(c));
        }
        String sql = Where.sql(columns, clauses);
        return sql.length() >= 6 ? sql.substring(6) : sql;
    }

    static String generateSelectPhrase(List<TreeNode> nodes) {
        Set<String> tableNames = new LinkedHashSet<>();
        ListMap columnMap = new ListMap();
        for (TreeNode node : nodes) {
            if (node instanceof TableNode) {
                final String tableFullName = ((TableNode)node).getNodeFullName();
                tableNames.add(tableFullName);
                columnMap.add(tableFullName);
            } else if (node instanceof ColumnNode) {
                ColumnNode cn = (ColumnNode)node;
                final String tableFullName = cn.getTableNode().getNodeFullName();
                tableNames.add(tableFullName);
                columnMap.add(tableFullName, cn.getName());
            }
        }
        if (tableNames.isEmpty()) {
            return "";
        }
        List<Column> columns = new ArrayList<>();
        for (Entry<String, List<String>> entry : columnMap.entrySet()) {
            final List<String> columnsInTable = entry.getValue();
            Table table = Table.of(entry.getKey());
            if (columnsInTable.isEmpty()) {
                columns.add(Column.allOf(table));
            } else {
                columns.addAll(FunctionalUtils.mapAndToList(columnsInTable, x -> Column.of(x, table)));
            }
        }
        return Select.sql(columns, emptyList()) + " " + Keyword.WHERE.toSql() + " ";
    }

    static String generateUpdateOrInsertPhrase(List<TreeNode> nodes, boolean isInsert) {
        Set<String> tableNames = new LinkedHashSet<>();
        ListMap columnMap = new ListMap();
        for (TreeNode node : nodes) {
            if (node instanceof TableNode) {
                final String tableFullName = ((TableNode)node).getNodeFullName();
                tableNames.add(tableFullName);
                columnMap.add(tableFullName);
            } else if (node instanceof ColumnNode) {
                ColumnNode cn = (ColumnNode)node;
                final String tableFullName = cn.getTableNode().getNodeFullName();
                tableNames.add(tableFullName);
                columnMap.add(tableFullName, cn.getName());
            }
        }
        if (tableNames.isEmpty()) {
            return "";
        }
        if (tableNames.size() >= 2) {
            throw new IllegalArgumentException(res.s("e.enables-select-just-1-table"));
        }
        String tableName = new LinkedList<>(tableNames).poll();
        List<String> columnsInTable = columnMap.get(tableName);
        if (columnsInTable.isEmpty()) {
            if (isInsert) {
                List<TableNode> tableNodes = new ArrayList<>();
                for (TreeNode node : nodes) {
                    if (node instanceof TableNode) {
                        tableNodes.add((TableNode)node);
                        break;
                    }
                }
                TableNode tableNode = tableNodes.get(0);
                if (tableNode.getChildCount() == 0) {
                    throw new IllegalArgumentException(res.s("i.can-only-use-after-tablenode-expanded"));
                }
                for (int i = 0, n = tableNode.getChildCount(); i < n; i++) {
                    ColumnNode child = (ColumnNode)tableNode.getChildAt(i);
                    columnsInTable.add(child.getName());
                }
            } else {
                return "";
            }
        }
        return getSqlPhrase(tableName, columnsInTable, isInsert);
    }

    private static String getSqlPhrase(String tableName, List<String> columnNames, boolean isInsert) {
        Table table = Table.of(tableName);
        if (isInsert) {
            List<Column> columns = FunctionalUtils.mapAndToList(columnNames, x -> Column.of(x, table));
            return Insert.sql(table, columns);
        } else {
            List<Clause> clauses = FunctionalUtils.mapAndToList(columnNames, x -> Clause.implicit(Column.of(x)));
            if (clauses.isEmpty()) {
                return "";
            }
            return Update.sql(table, clauses, Collections.emptyList()) + " " + Keyword.WHERE.toSql() + " ";
        }
    }

    void showLimitedRecords() throws SQLException {
        if (!App.props.getAsBoolean("sql.limitRecords.enabled")) {
            return;
        }
        List<TreeNode> nodes = getSelectionNodes();
        if (nodes.size() != 1) {
            return;
        }
        TreeNode node = nodes.get(0);
        if (!(node instanceof TableNode)) {
            return;
        }
        final String keyPrefix = "sql.limitRecords.sqlPattern.";
        String key = keyPrefix + dbmeta.getDatabaseProductName();
        String keyDefault = keyPrefix + "default";
        final String sqlPattern;
        if (App.props.hasKey(key)) {
            sqlPattern = App.props.get(key);
        } else if (App.props.hasKey(keyDefault)) {
            sqlPattern = App.props.get(keyDefault);
        } else {
            return; // cancel
        }
        TableNode t = (TableNode)node;
        final String sql = MessageFormat.format(sqlPattern, t.getNodeFullName());
        anyActionListener.anyActionPerformed(this, ConsoleTextArea.ActionKey.insertText, sql);
        anyActionListener.anyActionPerformed(this, executeCommand);
    }

    // text-search

    @Override
    public boolean search(Matcher matcher) {
        return search(resolveTargetPath(getSelectionPath()), matcher);
    }

    private static TreePath resolveTargetPath(TreePath path) {
        if (path != null) {
            TreePath parent = path.getParentPath();
            if (parent != null) {
                return parent;
            }
        }
        return path;
    }

    private boolean search(TreePath path, Matcher matcher) {
        if (path == null) {
            return false;
        }
        TreeNode node = (TreeNode)path.getLastPathComponent();
        if (node == null) {
            return false;
        }
        boolean found = false;
        found = matcher.find(node.toString());
        if (found) {
            addSelectionPath(path);
        } else {
            removeSelectionPath(path);
        }
        if (!node.isLeaf() && node.getChildCount() >= 0) {
            @SuppressWarnings("unchecked")
            Iterable<DefaultMutableTreeNode> children = Collections.list(node.children());
            for (DefaultMutableTreeNode child : children) {
                if (search(path.pathByAddingChild(child), matcher)) {
                    found = true;
                }
            }
        }
        return found;
    }

    @Override
    public void reset() {
        // empty
    }

    // node expansion

    /**
     * Refreshes the root and its children.
     * @param env Environment
     * @throws SQLException
     */
    void refreshRoot(Environment env) throws SQLException {
        Connector c = env.getCurrentConnector();
        if (c == null) {
            if (log.isDebugEnabled()) {
                log.debug("not connected");
            }
            currentConnector = null;
            return;
        }
        if (c == currentConnector && getModel().getRoot() != null) {
            if (log.isDebugEnabled()) {
                log.debug("not changed");
            }
            return;
        }
        if (log.isDebugEnabled()) {
            log.debug("updating");
        }
        // initializing models
        ConnectorNode connectorNode = new ConnectorNode(c.getName());
        DefaultTreeModel model = new DefaultTreeModel(connectorNode);
        setModel(model);
        final DefaultTreeSelectionModel m = new DefaultTreeSelectionModel();
        m.setSelectionMode(TreeSelectionModel.DISCONTIGUOUS_TREE_SELECTION);
        setSelectionModel(m);
        // initializing nodes
        final DatabaseMetaData dbmeta = env.getCurrentConnection().getMetaData();
        final Set<InfoNode> createdStatusSet = new HashSet<>();
        expandNode(connectorNode, dbmeta);
        createdStatusSet.add(connectorNode);
        // events
        class TreeWillExpandListenerImpl implements TreeWillExpandListener {
            @Override
            public void treeWillExpand(TreeExpansionEvent event) throws ExpandVetoException {
                TreePath path = event.getPath();
                final Object lastPathComponent = path.getLastPathComponent();
                if (!createdStatusSet.contains(lastPathComponent)) {
                    InfoNode node = (InfoNode)lastPathComponent;
                    if (node.isLeaf()) {
                        return;
                    }
                    createdStatusSet.add(node);
                    try {
                        expandNode(node, dbmeta);
                    } catch (SQLException ex) {
                        throw new RuntimeException(ex);
                    }
                }
            }
            @Override
            public void treeWillCollapse(TreeExpansionEvent event) throws ExpandVetoException {
                // ignore
            }
        }
        addTreeWillExpandListener(new TreeWillExpandListenerImpl());
        this.dbmeta = dbmeta;
        // showing
        model.reload();
        setRootVisible(true);
        this.currentConnector = c;
        // auto-expansion
        try {
            File confFile = App.getSystemFile("autoexpansion.tsv");
            if (confFile.exists() && confFile.length() > 0) {
                AnyAction aa = new AnyAction(this);
                try (Scanner r = new Scanner(confFile)) {
                    while (r.hasNextLine()) {
                        final String line = r.nextLine();
                        if (line.matches("^\\s*#.*")) {
                            continue;
                        }
                        aa.doParallel("expandNodes", Arrays.asList(line.split("\t")));
                    }
                }
            }
        } catch (IOException ex) {
            throw new IllegalStateException(ex);
        }
    }

    void expandNodes(List<String> a) {
        long startTime = System.currentTimeMillis();
        AnyAction aa = new AnyAction(this);
        int index = 1;
        while (index < a.size()) {
            final String s = a.subList(0, index + 1).toString();
            for (int i = 0, n = getRowCount(); i < n; i++) {
                TreePath target;
                try {
                    target = getPathForRow(i);
                } catch (IndexOutOfBoundsException ex) {
                    // FIXME when IndexOutOfBoundsException was thrown at expandNodes
                    assert false : "FIXME: when IndexOutOfBoundsException was thrown at expandNodes, Exception=" + ex;
                    log.warn(ex);
                    break;
                }
                if (target != null && target.toString().equals(s)) {
                    if (!isExpanded(target)) {
                        aa.doLater("expandLater", target);
                        App.sleepMillis(200L);
                    }
                    index++;
                    break;
                }
            }
            if (System.currentTimeMillis() - startTime > 5000L) {
                break; // timeout
            }
        }
    }

    // called by expandNodes
    @SuppressWarnings("unused")
    private void expandLater(TreePath parent) {
        expandPath(parent);
    }

    /**
     * Refreshes a node and its children.
     * @param node
     */
    void refresh(InfoNode node) {
        if (dbmeta == null) {
            return;
        }
        node.removeAllChildren();
        final DefaultTreeModel model = (DefaultTreeModel)getModel();
        model.reload(node);
        try {
            expandNode(node, dbmeta);
        } catch (SQLException ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     * Expands a node.
     * @param parent
     * @param dbmeta
     * @throws SQLException
     */
    void expandNode(final InfoNode parent, final DatabaseMetaData dbmeta) throws SQLException {
        if (parent.isLeaf()) {
            return;
        }
        final DefaultTreeModel model = (DefaultTreeModel)getModel();
        final DefaultMutableTreeNode tmpNode = new DefaultMutableTreeNode(res.s("i.paren-in-processing"));
        // asynchronous
        class NodeExpansionTask implements Runnable {
            @Override
            public void run() {
                class Task1 implements Runnable {
                    @Override
                    public void run() {
                        model.insertNodeInto(tmpNode, parent, 0);
                    }
                }
                invokeLater(new Task1());
                final List<InfoNode> children;
                try {
                    children = new ArrayList<>(parent.createChildren(dbmeta));
                } catch (SQLException ex) {
                    try {
                        if (dbmeta.getConnection().isClosed())
                            return;
                    } catch (SQLException exx) {
                        ex.setNextException(exx);
                    }
                    throw new RuntimeException(ex);
                }
                class Task2 implements Runnable {
                    @Override
                    public void run() {
                        for (InfoNode child : children) {
                            model.insertNodeInto(child, parent, parent.getChildCount());
                        }
                        model.removeNodeFromParent(tmpNode);
                    }
                }
                invokeLater(new Task2());
            }
        }
        AnyAction.doParallel(new NodeExpansionTask());
    }

    /**
     * Clears (root).
     */
    void clear() {
        for (TreeWillExpandListener listener : getListeners(TreeWillExpandListener.class).clone()) {
            removeTreeWillExpandListener(listener);
        }
        setModel(new DefaultTreeModel(null));
        currentConnector = null;
        dbmeta = null;
        if (log.isDebugEnabled()) {
            log.debug("cleared");
        }
    }

    // subclasses

    private static final class ListMap extends LinkedHashMap<String, List<String>> {

        ListMap() {
            // empty
        }

        void add(String key, String... values) {
            if (get(key) == null) {
                put(key, new ArrayList<String>());
            }
            for (String value : values) {
                get(key).add(value);
            }
        }

    }

    private static class Renderer extends DefaultTreeCellRenderer {

        Renderer() {
            // empty
        }

        @Override
        public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel, boolean expanded,
                                                      boolean leaf, int row, boolean hasFocus) {
            super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);
            if (value instanceof InfoNode) {
                ImageIcon icon = Utilities.getImageIcon(((InfoNode)value).getIconName());
                icon.setImage(icon.getImage().getScaledInstance(18, 18, Image.SCALE_SMOOTH));
                setIcon(icon);
            }
            if (value instanceof ColumnNode) {
                if (showColumnNumber) {
                    TreePath path = tree.getPathForRow(row);
                    if (path != null) {
                        TreePath parent = path.getParentPath();
                        if (parent != null) {
                            final int index = row - tree.getRowForPath(parent);
                            setText(String.format("%d %s", index, getText()));
                        }
                    }
                }
            }
            return this;
        }

    }

    private abstract static class InfoNode extends DefaultMutableTreeNode {

        InfoNode(Object userObject) {
            super(userObject, true);
        }

        @Override
        public boolean isLeaf() {
            return false;
        }

        abstract protected List<InfoNode> createChildren(DatabaseMetaData dbmeta) throws SQLException;

        String getIconName() {
            final String className = getClass().getName();
            final String nodeType = className.replaceFirst(".+?([^\\$]+)Node$", "$1");
            return "node-" + nodeType.toLowerCase() + ".png";
        }

        protected String getNodeFullName() {
            return String.valueOf(userObject);
        }

        static List<TableTypeNode> getTableTypeNodes(DatabaseMetaData dbmeta, String catalog,
                                                     String schema) throws SQLException {
            List<String> tableTypes = new ArrayList<>(DEFAULT_TABLE_TYPES);
            try (ResultSet rs = dbmeta.getTableTypes()) {
                while (rs.next()) {
                    final String tableType = rs.getString(1);
                    if (!DEFAULT_TABLE_TYPES.contains(tableType)) {
                        tableTypes.add(tableType);
                    }
                }
            } catch (SQLException ex) {
                log.warn("getTableTypes at getTableTypeNodes", ex);
            }
            List<TableTypeNode> a = new ArrayList<>();
            for (final String tableType : tableTypes) {
                TableTypeNode typeNode = new TableTypeNode(catalog, schema, tableType);
                if (typeNode.hasItems(dbmeta)) {
                    a.add(typeNode);
                }
            }
            return a;
        }

        static void arrangeSchemaNodes(List<InfoNode> nodes, String schemaName) {
            if (App.props.getAsBoolean("ui.swing.tree.moveToTopOwnSchema")) {
                final int index = getNodeIndex(nodes, schemaName);
                if (index >= 0) {
                    Collections.swap(nodes, 0, index);
                }
            }
        }

        private static int getNodeIndex(List<InfoNode> nodes, String schemaName) {
            for (InfoNode node : nodes) {
                if (node instanceof SchemaNode && Objects.equals(((SchemaNode)node).schema, schemaName)) {
                    return nodes.indexOf(node);
                }
            }
            return -1;
        }

    }

    private static class ConnectorNode extends InfoNode {

        ConnectorNode(String name) {
            super(name);
        }

        @Override
        protected List<InfoNode> createChildren(DatabaseMetaData dbmeta) throws SQLException {
            List<InfoNode> a = new ArrayList<>();
            if (dbmeta.supportsCatalogsInDataManipulation()) {
                try (ResultSet rs = dbmeta.getCatalogs()) {
                    while (rs.next()) {
                        a.add(new CatalogNode(rs.getString(1)));
                    }
                }
            } else if (dbmeta.supportsSchemasInDataManipulation()) {
                try (ResultSet rs = dbmeta.getSchemas()) {
                    while (rs.next()) {
                        a.add(new SchemaNode(null, rs.getString(1)));
                    }
                }
                arrangeSchemaNodes(a, dbmeta.getUserName());
            } else {
                a.addAll(getTableTypeNodes(dbmeta, null, null));
            }
            return a;
        }

    }

    private static final class CatalogNode extends InfoNode {

        private final String name;

        CatalogNode(String name) {
            super(name);
            this.name = name;
        }

        @Override
        protected List<InfoNode> createChildren(DatabaseMetaData dbmeta) throws SQLException {
            List<InfoNode> a = new ArrayList<>();
            if (dbmeta.supportsSchemasInDataManipulation()) {
                try (ResultSet rs = dbmeta.getSchemas()) {
                    while (rs.next()) {
                        a.add(new SchemaNode(name, rs.getString(1)));
                    }
                }
                arrangeSchemaNodes(a, dbmeta.getUserName());
            } else {
                a.addAll(getTableTypeNodes(dbmeta, name, null));
            }
            return a;
        }

    }

    private static final class SchemaNode extends InfoNode {

        private final String catalog;
        private final String schema;

        SchemaNode(String catalog, String schema) {
            super(schema);
            this.catalog = catalog;
            this.schema = schema;
        }

        @Override
        protected List<InfoNode> createChildren(DatabaseMetaData dbmeta) throws SQLException {
            List<InfoNode> a = new ArrayList<>();
            a.addAll(getTableTypeNodes(dbmeta, catalog, schema));
            return a;
        }

    }

    private static final class TableTypeNode extends InfoNode {

        private static final String ICON_NAME_FORMAT = "node-tabletype-%s.png";

        private final String catalog;
        private final String schema;
        private final String tableType;

        TableTypeNode(String catalog, String schema, String tableType) {
            super(tableType);
            this.catalog = catalog;
            this.schema = schema;
            this.tableType = tableType;
        }

        @Override
        protected List<InfoNode> createChildren(DatabaseMetaData dbmeta) throws SQLException {
            List<InfoNode> a = new ArrayList<>();
            try (ResultSet rs = dbmeta.getTables(catalog, schema, null, new String[]{tableType})) {
                while (rs.next()) {
                    final String table = rs.getString(3);
                    final String type = rs.getString(4);
                    a.add(new TableNode(catalog, schema, table, type));
                }
            }
            return a;
        }

        @Override
        String getIconName() {
            final String name = String.format(ICON_NAME_FORMAT, getUserObject());
            if (getClass().getResource("icon/" + name) == null) {
                return String.format(ICON_NAME_FORMAT, "");
            }
            return name;
        }

        boolean hasItems(DatabaseMetaData dbmeta) throws SQLException {
            try (ResultSet rs = dbmeta.getTables(catalog, schema, null, new String[]{tableType})) {
                return rs.next();
            }
        }

    }

    static final class TableNode extends InfoNode {

        private final String catalog;
        private final String schema;
        private final String name;
        private final String tableType;

        TableNode(String catalog, String schema, String name, String tableType) {
            super(name);
            this.catalog = catalog;
            this.schema = schema;
            this.name = name;
            this.tableType = tableType;
        }

        @Override
        protected List<InfoNode> createChildren(DatabaseMetaData dbmeta) throws SQLException {
            List<InfoNode> a = new ArrayList<>();
            try (ResultSet rs = dbmeta.getColumns(catalog, schema, name, null)) {
                while (rs.next()) {
                    a.add(new ColumnNode(rs.getString(4), rs.getString(6), rs.getInt(7), rs.getString(18), this));
                }
            }
            return a;
        }

        @Override
        public boolean isLeaf() {
            if (TABLE_TYPE_SEQUENCE.equals(tableType)) {
                return true;
            }
            return false;
        }

        @Override
        protected String getNodeFullName() {
            List<String> a = new ArrayList<>();
            if (catalog != null) {
                a.add(catalog);
            }
            if (schema != null) {
                a.add(schema);
            }
            a.add(name);
            return String.join(".", a);
        }

        String getName() {
            return name;
        }

    }

    static final class ColumnNode extends InfoNode {

        private final String name;
        private final TableNode tableNode;

        ColumnNode(String name, String type, int size, String nulls, TableNode tableNode) {
            super(format(name, type, size, nulls));
            setAllowsChildren(false);
            this.name = name;
            this.tableNode = tableNode;
        }

        String getName() {
            return name;
        }

        TableNode getTableNode() {
            return tableNode;
        }

        private static String format(String name, String type, int size, String nulls) {
            final String nonNull = "NO".equals(nulls) ? " NOT NULL" : "";
            return String.format("%s [%s(%d)%s]", name, type, size, nonNull);
        }

        @Override
        public boolean isLeaf() {
            return true;
        }

        @Override
        protected List<InfoNode> createChildren(DatabaseMetaData dbmeta) throws SQLException {
            return emptyList();
        }

        @Override
        protected String getNodeFullName() {
            return String.format("%s.%s", tableNode.getNodeFullName(), name);
        }

    }

}
