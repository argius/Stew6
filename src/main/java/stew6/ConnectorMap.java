package stew6;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import net.argius.stew.*;
import stew6.ConnectorsConfig.*;

/**
 * ConnectorMap provides a mapping to associate an Connector with its own ID.
 */
public final class ConnectorMap extends LinkedHashMap<String, Connector> {

    public static final List<String> propertyKeys = Arrays.asList("name", "driver", "classpath", "classpathref", "url",
                                                                  "user", "password", "password.class", "readonly",
                                                                  "rollback");

    /**
     * A constructor.
     */
    public ConnectorMap() {
        // empty
    }

    /**
     * A constructor to create from a Properties.
     * @param idList
     * @param props
     */
    public ConnectorMap(List<String> idList, Properties props) {
        for (String id : idList) {
            Properties p = new Properties();
            copyPropertyById(id, "name", props, p);
            copyPropertyById(id, "driver", props, p);
            copyPropertyById(id, "classpath", props, p);
            copyPropertyById(id, "classpathref", props, p);
            copyPropertyById(id, "url", props, p);
            copyPropertyById(id, "user", props, p);
            copyPropertyById(id, "password", props, p);
            copyPropertyById(id, "password.class", props, p);
            copyPropertyById(id, "readonly", props, p);
            copyPropertyById(id, "rollback", props, p);
            Connector connector = new Connector(id, p);
            put(id, connector);
        }
    }

    /**
     * A copy constructor.
     * @param src
     */
    public ConnectorMap(ConnectorMap src) {
        putAll(src);
    }

    /**
     * A factory method which creates from Connectors config file.
     */
    public static ConnectorMap createFromFile() {
        try {
            return createFrom(new ConnectorsConfigFile().read());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * A factory method for ConnectorsConfig.
     * @param src ConnectorsConfig
     */
    public static ConnectorMap createFrom(ConnectorsConfig src) {
        ConnectorMap m = new ConnectorMap();
        for (ConnectorConfig cc : src.getConnectors()) {
            Properties p = new Properties();
            final String id = cc.getId();
            p.setProperty("name", cc.getName());
            ConnectorConfigSettings o = cc.getSettings();
            p.setProperty("driver", o.getDriver());
            p.setProperty("classpath", o.getClasspath());
            p.setProperty("classpathref", o.getClasspathref());
            p.setProperty("url", o.getUrl());
            p.setProperty("user", o.getUser());
            p.setProperty("password", o.getPassword());
            p.setProperty("password.class", o.getPasswordClass());
            p.setProperty("readonly", String.valueOf(o.isReadOnly()));
            p.setProperty("rollback", String.valueOf(o.isEnablesAutoRollback()));
            m.put(id, new Connector(id, p));
        }
        return m;
    }

    public static boolean existsFile() {
        return Files.exists(new ConnectorsConfigFile().getPath());
    }

    public ConnectorsConfig toConnectorConfig() {
        List<ConnectorConfig> a = new ArrayList<>();
        for (Connector c : values()) {
            ConnectorConfig cc = new ConnectorConfig();
            cc.setId(c.getId());
            cc.setName(c.getName());
            ConnectorConfigSettings o = new ConnectorConfigSettings();
            o.setDriver(c.getDriver());
            o.setClasspath(c.getClasspath());
            o.setClasspathref(c.getClasspathref());
            o.setUrl(c.getUrl());
            o.setUser(c.getUser());
            o.setPassword(c.getPassword().getRawString());
            o.setPasswordClass(c.getPassword().getClass().getName());
            o.setReadOnly(c.isReadOnly());
            o.setEnablesAutoRollback(c.usesAutoRollback());
            cc.setSettings(o);
            a.add(cc);
        }
        final ConnectorsConfig cs = new ConnectorsConfig();
        cs.setConnectors(a);
        return cs;
    }

    public void saveToFile() {
        try {
            new ConnectorsConfigFile().write(toConnectorConfig());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static void copyPropertyById(String id, String key, Properties src, Properties dst) {
        String fullKey = id + '.' + key;
        String value = src.getProperty(fullKey, "");
        dst.setProperty(key, value);
    }

    /**
     * Returns the connector specified by ID.
     * @param id
     * @return
     */
    public Connector getConnector(String id) {
        return get(id);
    }

    /**
     * Sets a connector.
     * @param id
     * @param connector
     */
    public void setConnector(String id, Connector connector) {
        put(id, connector);
    }

    /**
     * Returns this map as Properties.
     * @return
     */
    public Properties toProperties() {
        Properties props = new Properties();
        for (String id : keySet()) {
            Connector connector = getConnector(id);
            Password password = connector.getPassword();
            props.setProperty(id + ".name", connector.getName());
            props.setProperty(id + ".driver", connector.getDriver());
            props.setProperty(id + ".classpath", connector.getClasspath());
            props.setProperty(id + ".classpathref", connector.getClasspathref());
            props.setProperty(id + ".url", connector.getUrl());
            props.setProperty(id + ".user", connector.getUser());
            props.setProperty(id + ".password", password.getTransformedString());
            props.setProperty(id + ".password.class", password.getClass().getName());
            props.setProperty(id + ".readonly", Boolean.toString(connector.isReadOnly()));
            props.setProperty(id + ".rollback", Boolean.toString(connector.usesAutoRollback()));
        }
        return props;
    }

}
