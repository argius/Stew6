package stew6.sql;

import java.util.*;
import java.util.function.*;
import java.util.stream.*;

public final class Select {

    public static String sql(List<Column> columns, List<Clause> clauses) {
        List<Column> columnsInClauses = collectColumn(clauses);
        List<Table> tables = Stream.concat(columns.stream(), columnsInClauses.stream()).map(Column::getTable).distinct()
                                   .collect(Collectors.toList());
        Sql.Builder b = new Sql.Builder();
        b.add(Keyword.SELECT);
        b.add(tables.size() > 1 ? columns : map(columns, Column::withoutTable));
        b.add(Keyword.FROM);
        b.add(tables.size() > 1 ? tables : map(tables, Table::withoutAlias));
        if (clauses.isEmpty()) {
            return b.build();
        } else {
            return Where.addWhere(b, columns, clauses).build();
        }
    }

    static <T, R> List<R> map(List<T> list, Function<T, R> f) {
        return list.stream().map(x -> f.apply(x)).collect(Collectors.toList());
    }

    static List<Column> collectColumn(List<Clause> clauses) {
        return clauses.stream().flatMap(x -> x.asList().stream().filter(y -> (y instanceof Column)))
                      .map(x -> Column.class.cast(x)).collect(Collectors.toList());
    }

}
