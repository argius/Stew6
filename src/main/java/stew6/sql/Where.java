package stew6.sql;

import java.util.*;
import java.util.function.*;
import java.util.stream.*;

public final class Where {

    private Where() { // empty, forbidden
    }

    public static String sql(List<Column> columns, List<Clause> clauses) {
        Sql.Builder b = new Sql.Builder();
        addWhere(b, columns, clauses);
        return b.build();
    }

    static Sql.Builder addWhere(Sql.Builder builder, List<Column> columns, List<Clause> clauses) {
        List<Column> columnsInClauses = collectColumn(clauses);
        List<Table> tables = Stream.concat(columns.stream(), columnsInClauses.stream()).map(Column::getTable).distinct()
                                   .collect(Collectors.toList());
        List<Clause> whereClauses = new ArrayList<>();
        if (tables.size() > 1) {
            whereClauses.addAll(createJoinClauses(columns));
            whereClauses.addAll(clauses);
        } else {
            whereClauses.addAll(map(clauses, Clause::withoutTable));
        }
        if (!whereClauses.isEmpty()) {
            builder.add(Keyword.WHERE).joinAndAdd(Keyword.AND, whereClauses);
        }
        return builder;
    }

    static List<Clause> createJoinClauses(List<Column> columns) {
        List<Clause> a = new ArrayList<>();
        Column[] arr = columns.toArray(new Column[0]);
        for (int i = 0; i < arr.length; i++) {
            Column x = arr[i];
            for (int j = i; j < arr.length; j++) {
                if (j == i) {
                    continue;
                }
                Column y = arr[j];
                if (x.getName().equals(y.getName())) {
                    a.add(Clause.of(x, Operator.EQUAL, y));
                }
            }
        }
        return a;
    }

    static <T, R> List<R> map(List<T> list, Function<T, R> f) {
        return list.stream().map(x -> f.apply(x)).collect(Collectors.toList());
    }

    static List<Column> collectColumn(List<Clause> clauses) {
        return clauses.stream().flatMap(x -> x.asList().stream().filter(y -> (y instanceof Column)))
                      .map(x -> Column.class.cast(x)).collect(Collectors.toList());
    }

}
