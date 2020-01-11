package net.argius.stew;

/**
 * The Password interface that is used by Connector.
 */
public interface Password {

    /**
     * Returns the transformed string.
     * @return
     */
    String getTransformedString();

    /**
     * Sets the transformed string.
     * @param transformedString
     */
    void setTransformedString(String transformedString);

    /**
     * Returns the raw string.
     * @return
     */
    String getRawString();

    /**
     * Sets the raw string.
     * @param rowString
     */
    void setRawString(String rowString);

    /**
     * Returns true if the password was already set.
     * @return
     */
    boolean hasPassword();

}
