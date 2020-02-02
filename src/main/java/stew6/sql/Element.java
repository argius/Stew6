package stew6.sql;

public interface Element {

    default String toSql() {
        return toString();
    }

}
