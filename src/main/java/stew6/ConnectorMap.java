package stew6;

import java.util.*;
import net.argius.stew.*;

/**
 * ConnectorMap provides a mapping to associate an Connector with its own ID. 
 */
public final class ConnectorMap extends LinkedHashMap<String, Connector> {

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
