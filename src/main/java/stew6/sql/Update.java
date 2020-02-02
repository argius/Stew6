package stew6.sql;

import java.util.*;
import java.util.stream.*;

public final class Update {

    private Update() { // empty, forbidden
    }

    public static String sql(List<Clause> updationValues, List<Clause> clauses) {
        List<Table> tables = Stream.concat(updationValues.stream(), clauses.stream())
                                   .flatMap(x -> x.extractColumns().stream()).map(Column::getTable).distinct()
                                   .collect(Collectors.toList());
        if (tables.size() != 1) {
            throw new IllegalArgumentException("table.size=" + tables.size());
        }
        return sql(tables.get(0), updationValues, clauses);
    }

    public static String sql(Table table, List<Clause> updationValues, List<Clause> clauses) {
        Sql.Builder b = new Sql.Builder();
        b.add(Keyword.UPDATE, table, Keyword.SET);
        b.add(updationValues);
        if (clauses.isEmpty()) {
            return b.build();
        } else {
            return Where.addWhere(b, Arrays.asList(), clauses).build();
        }
    }

}
