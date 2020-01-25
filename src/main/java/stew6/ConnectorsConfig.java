package stew6;

import java.util.*;

public final class ConnectorsConfig {

    private List<ConnectorConfig> connectors;

    public ConnectorsConfig() {
        this.connectors = new ArrayList<>();
    }

    public List<ConnectorConfig> getConnectors() {
        return connectors;
    }

    public void setConnectors(List<ConnectorConfig> connectors) {
        this.connectors = connectors;
    }

    public static final class ConnectorConfig {

        private String id;
        private String name;
        private ConnectorConfigSettings settings;

        public ConnectorConfig() {
            this.id = "";
            this.name = "";
            this.settings = new ConnectorConfigSettings();
        }

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public ConnectorConfigSettings getSettings() {
            return settings;
        }

        public void setSettings(ConnectorConfigSettings settings) {
            this.settings = settings;
        }

        @Override
        public String toString() {
            return String.format("ConnectorConfig(id=%s, name=%s, settings=%s)", id, name, settings);
        }

    }

    public static final class ConnectorConfigSettings {

        private String driver;
        private String classpath;
        private String classpathref;
        private String url;
        private String user;
        private String password;
        private String passwordClass;
        private boolean isReadOnly;
        private boolean enablesAutoRollback;

        public ConnectorConfigSettings() {
            this.driver = "";
            this.classpath = "";
            this.classpathref = "";
            this.url = "";
            this.user = "";
            this.password = "";
            this.passwordClass = "";
            this.isReadOnly = false;
            this.enablesAutoRollback = false;
        }

        public String getDriver() {
            return driver;
        }

        public void setDriver(String driver) {
            this.driver = driver;
        }

        public String getClasspath() {
            return classpath;
        }

        public void setClasspath(String classpath) {
            this.classpath = classpath;
        }

        public String getClasspathref() {
            return classpathref;
        }

        public void setClasspathref(String classpathref) {
            this.classpathref = classpathref;
        }

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public String getUser() {
            return user;
        }

        public void setUser(String user) {
            this.user = user;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public String getPasswordClass() {
            return passwordClass;
        }

        public void setPasswordClass(String passwordClass) {
            this.passwordClass = passwordClass;
        }

        public boolean isReadOnly() {
            return isReadOnly;
        }

        public void setReadOnly(boolean isReadOnly) {
            this.isReadOnly = isReadOnly;
        }

        public boolean isEnablesAutoRollback() {
            return enablesAutoRollback;
        }

        public void setEnablesAutoRollback(boolean enablesAutoRollback) {
            this.enablesAutoRollback = enablesAutoRollback;
        }

        @Override
        public String toString() {
            return String.format("ConnectorConfigSettings(driver=%s, classpath=%s, classpathref=%s, url=%s, user=%s, password=%s, passwordClass=%s, isReadOnly=%s, enablesAutoRollback=%s)",
                                 driver, classpath, classpathref, url, user, password, passwordClass, isReadOnly,
                                 enablesAutoRollback);
        }

    }

}
