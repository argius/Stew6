package stew6;

import static stew6.App.getSystemFile;
import java.io.*;
import java.nio.file.*;
import java.sql.*;
import javax.script.*;
import org.apache.commons.lang3.exception.*;
import net.argius.stew.*;
import stew6.ui.*;

/**
 * Environment.
 */
public final class Environment {

    static final String ALIAS_PROPERTIES_NAME = "alias.properties";

    private static final Logger log = Logger.getLogger(Environment.class);

    private OutputProcessor outputProcessor;
    private ConnectorMap connectorMap;
    private Connector connector;
    private Connection conn;

    private int timeoutSeconds;
    private File currentDirectory;
    private long connectorTimestamp;
    private AliasMap aliasMap;
    private ScriptContext scriptContext;
    private int exitStatus;

    private Environment(ConnectorMap connectorMap, File currentDirectory) {
        this.connectorMap = connectorMap;
        this.currentDirectory = currentDirectory;
        // init alias
        final File aliasPropFile = getSystemFile(ALIAS_PROPERTIES_NAME);
        this.aliasMap = new AliasMap(aliasPropFile);
        if (aliasPropFile.exists()) {
            try {
                aliasMap.load();
            } catch (IOException ex) {
                log.warn(ex);
            }
        }
    }

    /**
     * A constructor.
     */
    public Environment() {
        this(new ConnectorMap(), getInitialCurrentDirectory()); // init directories
        initializeQueryTimeout();
        loadConnectorMap();
        initializeScriptContext();
    }

    /**
     * A constructor (for copy).
     * @param src
     */
    public Environment(Environment src) {
        // never copy coconnector,conn,op,aliasMap,scriptContext into this
        this(new ConnectorMap(src.connectorMap), src.currentDirectory);
        this.timeoutSeconds = src.timeoutSeconds;
    }

    /**
     * Releases resouces it keeps.
     */
    public void release() {
        try {
            releaseConnection();
            log.debug("released connection");
        } catch (SQLException ex) {
            log.error(ex, "release error");
        } finally {
            outputProcessor = null;
            connectorMap = null;
            connector = null;
            conn = null;
            currentDirectory = null;
        }
        log.debug("released internal state of Environment");
    }

    /**
     * Establishes a connection.
     * @param connector
     * @throws SQLException
     */
    void establishConnection(Connector connector) throws SQLException {
        @SuppressWarnings("resource")
        Connection conn = connector.getConnection();
        try {
            if (connector.isReadOnly()) {
                conn.setReadOnly(true);
            }
        } catch (RuntimeException ex) {
            log.warn(ex);
        }
        boolean isAutoCommitAvailable;
        try {
            conn.setAutoCommit(false);
            isAutoCommitAvailable = conn.getAutoCommit();
        } catch (RuntimeException ex) {
            log.warn(ex);
            isAutoCommitAvailable = false;
        }
        if (isAutoCommitAvailable) {
            outputMessage("w.auto-commit-not-available");
        }
        setCurrentConnection(conn);
        setCurrentConnector(connector);
        outputMessage("i.connected");
        log.debug("connected %s (conn=%08x, env=%08x)", connector.getId(), conn.hashCode(), hashCode());
        if (App.props.getAsBoolean("print-connected-time")) {
            outputMessage("i.now", System.currentTimeMillis());
        }
    }

    /**
     * Releases the connection.
     * @throws SQLException
     */
    void releaseConnection() throws SQLException {
        if (conn == null) {
            log.debug("not connected");
            return;
        }
        try {
            if (connector != null && connector.usesAutoRollback()) {
                try {
                    conn.rollback();
                    outputMessage("i.rollbacked");
                    log.debug("rollbacked %s (%s)", connector.getId(), conn);
                } catch (SQLException ex) {
                    log.warn(ex);
                    outputMessage("e.database", ExceptionUtils.getStackTrace(ex));
                    outputMessage("w.error-occurred-on-auto-rollback", ex);
                }
            }
            try {
                conn.close();
                if (log.isDebugEnabled()) {
                    final String id = (connector == null) ? "null" : connector.getId();
                    log.debug("disconnected %s (conn=%08x, env=%08x)", id, conn.hashCode(), hashCode());
                }
                if (App.props.getAsBoolean("print-disconnected-time")) {
                    outputMessage("i.now", System.currentTimeMillis());
                }
            } catch (SQLException ex) {
                log.warn(ex);
                throw ex;
            }
        } finally {
            conn = null;
            connector = null;
        }
    }

    private void outputMessage(String id, Object... args) throws CommandException {
        if (outputProcessor != null) {
            outputProcessor.output(App.res.format(id, args));
        }
    }

    private static File getInitialCurrentDirectory() {
        final String propkey = "directory";
        if (App.props.hasKey(propkey)) {
            File directory = new File(App.props.get(propkey, ""));
            if (directory.isDirectory()) {
                return directory;
            }
        }
        return new File(".");
    }

    private void initializeQueryTimeout() {
        this.timeoutSeconds = App.props.getAsInt("query.timeout", -1);
        if (log.isDebugEnabled()) {
            log.debug("timeout: " + this.timeoutSeconds);
        }
    }

    void initializeScriptContext() {
        this.scriptContext = new SimpleScriptContext();
    }

    /**
     * Loads and refreshes connector map.
     */
    public void loadConnectorMap() {
        ConnectorsConfigFile cf = new ConnectorsConfigFile();
        if (!Files.exists(cf.getPath())) {
            ConfigurationConverter.convertConnectorsConfigIfNeeded();
        }
        ConnectorMap m;
        try {
            m = ConnectorMap.createFromFile();
        } catch (UncheckedIOException ex) {
            m = new ConnectorMap();
        }
        synchronized (connectorMap) {
            if (connectorMap.size() > 0) {
                connectorMap.clear();
            }
            connectorMap.putAll(m);
            connectorTimestamp = cf.getLastModified();
        }
    }

    /**
     * Updates connector map.
     * When file was updated, it calls loadConnectorMap().
     * @return whether updated or not
     */
    public boolean updateConnectorMap() {
        if (new ConnectorsConfigFile().getLastModified() > connectorTimestamp) {
            loadConnectorMap();
            return true;
        }
        return false;
    }

    public OutputProcessor getOutputProcessor() {
        return outputProcessor;
    }

    public void setOutputProcessor(OutputProcessor outputProcessor) {
        this.outputProcessor = outputProcessor;
    }

    public ConnectorMap getConnectorMap() {
        return connectorMap;
    }

    public Connector getCurrentConnector() {
        return connector;
    }

    void setCurrentConnector(Connector connector) {
        this.connector = connector;
    }

    public Connection getCurrentConnection() {
        return conn;
    }

    void setCurrentConnection(Connection conn) {
        this.conn = conn;
    }

    public int getTimeoutSeconds() {
        return timeoutSeconds;
    }

    public File getCurrentDirectory() {
        return currentDirectory;
    }

    public void setCurrentDirectory(File currentDirectory) {
        this.currentDirectory = currentDirectory;
    }

    public AliasMap getAliasMap() {
        return aliasMap;
    }

    public ScriptContext getScriptContext() {
        return scriptContext;
    }

    public int getExitStatus() {
        return exitStatus;
    }

    public void setExitStatus(int exitStatus) {
        this.exitStatus = exitStatus;
    }

}
