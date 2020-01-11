package net.argius.stew;

/**
 * A plain-text password.
 */
public final class PlainTextPassword implements Password {

    private String rowString;

    @Override
    public String getTransformedString() {
        return getRawString();
    }

    @Override
    public void setTransformedString(String transformedString) {
        setRawString(transformedString);
    }

    @Override
    public String getRawString() {
        return (hasPassword()) ? rowString : "";
    }

    @Override
    public void setRawString(String rowString) {
        if (rowString != null) {
            this.rowString = rowString;
        }
    }

    @Override
    public boolean hasPassword() {
        return rowString != null;
    }

}
