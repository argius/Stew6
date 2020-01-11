package stew6.ui.swing;

import java.sql.*;
import stew6.*;

/**
 * It is used for sending and receiving an object.
 * It called at {@link WindowOutputProcessor#outputResult(ResultSetReference)} to fetch
 * and ResultSetTableModel#executeSql (update sql) to upload.
 */
class ValueTransporter {

    protected ValueTransporter() {
        // empty
    }

    static ValueTransporter getInstance(String className) {
        if (className != null && className.trim().length() > 0) {
            try {
                return DynamicLoader.newInstance(className);
            } catch (DynamicLoadingException ex) {
                // ignore
            }
        }
        return new ValueTransporter();
    }

    /**
     * Returns the object received from ResultSet.
     * @param rs
     * @param index
     * @return
     * @throws SQLException
     */
    @SuppressWarnings("static-method")
    Object getObject(ResultSet rs, int index) throws SQLException {
        return rs.getObject(index);
    }

    /**
     * Sets the object to Statement.
     * @param stmt
     * @param index
     * @param o
     * @throws SQLException
     */
    @SuppressWarnings("static-method")
    void setObject(PreparedStatement stmt, int index, Object o) throws SQLException {
        stmt.setObject(index, o);
    }

}
