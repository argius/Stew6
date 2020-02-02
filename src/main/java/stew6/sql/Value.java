package stew6.sql;

public final class Value implements Element {

    enum Type {
        STRING, NUMERIC, PLACE_HOLDER;
    }

    private Object value;
    private Type type;

    private Value(Object o, Type type) {
        this.value = o;
        this.type = type;
    }

    public static Value asString(String s) {
        return new Value(s, Type.STRING);
    }

    public static Value asNumeric(Number n) {
        return new Value(n, Type.NUMERIC);
    }

    public static Value asPlaceHolder() {
        return new Value("?", Type.PLACE_HOLDER);
    }

    @Override
    public String toSql() {
        switch (type) {
            case STRING:
                break;
            case NUMERIC:
            default:
        }
        return Element.super.toSql();
    }

    @Override
    public String toString() {
        return String.valueOf(value);
    }

}
