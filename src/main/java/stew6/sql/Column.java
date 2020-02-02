package stew6.sql;

public final class Column implements Element {

    private String name;
    private Table table;

    private Column(String name, Table table) {
        this.name = name;
        this.table = table;
    }

    public static Column of(String name) {
        return new Column(name, Table.NONE);
    }

    public static Column of(String name, Table table) {
        return new Column(name, table);
    }

    public static Column allOf(Table table) {
        return new Column("*", table);
    }

    public Column withoutTable() {
        return of(name);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Table getTable() {
        return table;
    }

    public void setTable(Table table) {
        this.table = table;
    }

    @Override
    public String toSql() {
        String s = Sql.quoteIfNeeded(name);
        if (table == Table.NONE) {
            return s;
        }
        return String.format("%s.%s", table.getAliasOrNot().orElseGet(table::getNameForSql), s);
    }

    @Override
    public String toString() {
        return String.format("Column(name=%s, table=%s)", name, table);
    }

}
