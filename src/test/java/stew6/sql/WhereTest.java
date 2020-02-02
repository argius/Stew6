package stew6.sql;

import static org.junit.Assert.*;
import java.util.*;
import org.junit.*;

public final class WhereTest {

    @Test
    public void testSql() {
        Table t1 = Table.of("table1");
        Table t2 = Table.of("table2");
        Column c1 = Column.of("c1", t1);
        Column id = Column.of("id", t1);
        Column t1fk1 = Column.of("fk1", t1);
        Column t2fk1 = Column.of("fk1", t2);
//        assertEquals("", Where.sql(Arrays.asList(Column.allOf(t1)), Collections.emptyList()));
//        assertEquals("", Where.sql(Arrays.asList(id, c1), Collections.emptyList()));
        assertEquals("WHERE table1.fk1=table2.fk1",
                     Where.sql(Arrays.asList(id, t1fk1, t2fk1, c1), Collections.emptyList()));
    }

}
