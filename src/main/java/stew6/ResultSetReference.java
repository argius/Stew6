package stew6;

import java.sql.*;

/**
 * This object holds the reference of ResultSet.
 */
public final class ResultSetReference implements AutoCloseable {

    private final ResultSet rs;
    private final ColumnOrder order;
    private final String commandString;

    private int recordCount;

    /**
     * A constructor.
     * @param rs ResultSet
     * @param commandString
     */
    public ResultSetReference(ResultSet rs, String commandString) {
        this.rs = rs;
        this.order = new ColumnOrder();
        this.commandString = commandString;
    }

    /**
     * Returns the ResultSet.
     * @return
     */
    public ResultSet getResultSet() {
        return rs;
    }

    /**
     * Returns the ColumnOrder.
     * @return
     */
    public ColumnOrder getOrder() {
        return order;
    }

    /**
     * Returns the command string.
     * @return
     */
    public String getCommandString() {
        return commandString;
    }

    /**
     * Returns the count of records.
     * @return
     */
    public int getRecordCount() {
        return recordCount;
    }

    /**
     * Sets the count of records.
     * @param recordCount
     */
    public void setRecordCount(int recordCount) {
        this.recordCount = recordCount;
    }

    @Override
    public void close() throws SQLException {
        if (rs.isClosed()) {
            throw new IllegalStateException("already closed");
        }
        rs.close();
    }

}
