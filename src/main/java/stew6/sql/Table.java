package stew6.sql;

import java.util.*;

public final class Table implements Element {

    static final Table NONE = new Table("", "");

    private String name;
    private String alias;

    private Table(String name) {
        this.name = name;
        this.alias = "";
    }

    public Table(String name, String alias) {
        this.name = name;
        this.alias = alias;
    }

    public static Table of(String name) {
        return new Table(name);
    }

    public static Table of(String name, String alias) {
        return new Table(name, alias);
    }

    public Table withAlias(String alias) {
        return new Table(name, alias);
    }

    public Table withoutAlias() {
        return new Table(name);
    }

    public Column column(String name) {
        return Column.of(name, this);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAlias() {
        return alias;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }

    public Optional<String> getAliasOrNot() {
        return isBlank(alias) ? Optional.empty() : Optional.of(alias);
    }

    @Override
    public String toSql() {
        String s = getNameForSql();
        return isBlank(alias) ? s : String.format("%s %s", s, alias);
    }

    public String getNameForSql() {
        return Sql.quoteIfNeeded(name);
    }

    static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    public static void attachAlias(Table... tables) {
        attachAlias(Arrays.asList(tables));
    }

    public static void attachAlias(Collection<Table> tables) {
        final int n = 26;
        if (tables.size() > n) {
            throw new IllegalArgumentException("number of table > " + n);
        }
        int i = 0;
        for (Table t : tables) {
            char c = (char)('a' + i);
            t.setAlias(String.valueOf(c));
            ++i;
        }
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Table other = (Table)obj;
        if (name == null) {
            if (other.name != null)
                return false;
        } else if (!name.equals(other.name))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return String.format("Table(%s)", name);
    }

}
