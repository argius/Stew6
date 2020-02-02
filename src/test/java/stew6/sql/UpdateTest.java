package stew6.sql;

import static org.junit.Assert.assertEquals;
import java.util.*;
import org.junit.*;

public final class UpdateTest {

    @Test
    public void testSql() {
        Table t1 = Table.of("table1");
        Clause v1 = Clause.of(Column.of("c1"), Operator.EQUAL, Value.asPlaceHolder());
        Clause v2 = Clause.of(Column.of("c2"), Operator.EQUAL, Value.asPlaceHolder());
        Clause k1 = Clause.of(Column.of("id"), Operator.EQUAL, Value.asPlaceHolder());
        assertEquals("UPDATE table1 SET c1=?", Update.sql(t1, Arrays.asList(v1), Collections.emptyList()));
        assertEquals("UPDATE table1 SET c1=?, c2=? WHERE id=?",
                     Update.sql(t1, Arrays.asList(v1, v2), Arrays.asList(k1)));
    }

}
