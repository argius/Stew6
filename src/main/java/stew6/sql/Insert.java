package stew6.sql;

import java.util.*;

public final class Insert {

    private Insert() { // empty, forbidden
    }

    public static String sql(Table table, List<Column> columns) {
        return sql(table, Optional.of(columns), Collections.nCopies(columns.size(), Value.asPlaceHolder()));
    }

    public static String sql(Table table, Column... columns) {
        return sql(table, Arrays.asList(columns));
    }

    public static String sql(Table table, int columnCount) {
        return sql(table, Optional.empty(), Sql.generatePlaceHolders(columnCount));
    }

    public static String sql(String tableName, int columnCount) {
        return sql(Table.of(tableName), columnCount);
    }

    public static String sql(List<Column> columns) {
        Optional<Table> tableOpt = columns.stream().map(Column::getTable).distinct().findFirst();
        Table t = tableOpt.orElseThrow(() -> new IllegalArgumentException("table.size is not 1"));
        return sql(t, Optional.of(columns), Collections.nCopies(columns.size(), Value.asPlaceHolder()));
    }

    public static String sql(Table table, Optional<List<Column>> columnsOpt, List<Value> values) {
        Sql.Builder b = new Sql.Builder();
        b.add(Keyword.INSERT, Keyword.INTO, table);
        columnsOpt.ifPresent(x -> {
            b.add(Operator.LPAREN);
            x.stream().map(Column::withoutTable).forEachOrdered(b::add);
            b.add(Operator.RPAREN);
        });
        b.add(Keyword.VALUES);
        b.add(Operator.LPAREN);
        b.add(values);
        b.add(Operator.RPAREN);
        return b.build();
    }

}
