package stew6.ui.swing;

import java.util.*;
import stew6.*;

/**
 * ConnectorEntry.
 */
final class ConnectorEntry {

    private final String id;
    private final Connector connector;

    /**
     * A constructor.
     * @param id
     * @param connector
     */
    ConnectorEntry(String id, Connector connector) {
        this.id = id;
        this.connector = connector;
    }

    /**
     * Creates the list from a connector list.
     * @param iterable
     * @return
     */
    static List<ConnectorEntry> toList(Iterable<Connector> iterable) {
        List<ConnectorEntry> a = new ArrayList<>();
        for (Connector c : iterable) {
            a.add(new ConnectorEntry(c.getId(), c));
        }
        return a;
    }

    /**
     * Returns this ID.
     * @return
     */
    public String getId() {
        return id;
    }

    /**
     * Returns this connector.
     * @return
     */
    public Connector getConnector() {
        return connector;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((id == null) ? 0 : id.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof ConnectorEntry)) {
            return false;
        }
        ConnectorEntry other = (ConnectorEntry)obj;
        if (id == null) {
            if (other.id != null) {
                return false;
            }
        } else if (!id.equals(other.id)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        final String name = connector.getName();
        if (name == null || name.length() == 0) {
            return id;
        }
        return String.format("%s (%s)", id, name);
    }

}
