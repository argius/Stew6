package stew6.sql;

import static org.junit.Assert.assertEquals;
import java.util.*;
import org.junit.*;

public final class DeleteTest {

    @Test
    public void testSql() {
        Table t1 = Table.of("table1");
        Clause k1 = Clause.of(Column.of("id"), Operator.EQUAL, Value.asPlaceHolder());
        Clause k2 = Clause.of(Column.of("c1"), Operator.EQUAL, Value.asPlaceHolder());
        Clause k3 = Clause.of(Column.of("c2"), Operator.EQUAL, Value.asPlaceHolder());
        assertEquals("DELETE FROM table1", Delete.sql(t1, Collections.emptyList()));
        assertEquals("DELETE FROM table1 WHERE id=?", Delete.sql(t1, Arrays.asList(k1)));
        assertEquals("DELETE FROM table1 WHERE id=? AND c1=?", Delete.sql(t1, Arrays.asList(k1, k2)));
        assertEquals("DELETE FROM table1 WHERE id=? AND c1=? AND c2=?", Delete.sql(t1, Arrays.asList(k1, k2, k3)));
    }

}
