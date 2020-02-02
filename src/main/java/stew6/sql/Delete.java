package stew6.sql;

import java.util.*;

public final class Delete {

    private Delete() { // empty, forbidden
    }

    public static String sql(Table table, List<Clause> clauses) {
        Sql.Builder b = new Sql.Builder();
        b.add(Keyword.DELETE, Keyword.FROM, table);
        if (clauses.isEmpty()) {
            return b.build();
        } else {
            return Where.addWhere(b, Arrays.asList(), clauses).build();
        }
    }

}
