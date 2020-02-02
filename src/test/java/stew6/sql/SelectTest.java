package stew6.sql;

import static org.junit.Assert.assertEquals;
import java.util.*;
import org.junit.*;

public final class SelectTest {

    @Test
    public void testSql() {
        Table t1 = Table.of("table1");
        Table t2 = Table.of("table2");
        Column c1 = Column.of("c1", t1);
        Column id = Column.of("id", t1);
        Column t1fk1 = Column.of("fk1", t1);
        Column t2fk1 = Column.of("fk1", t2);
        Clause k0 = Clause.implicit(id);
        Clause k1 = Clause.implicit(t1fk1);
        Clause k2 = Clause.implicit(t2fk1);
        assertEquals("SELECT * FROM table1", Select.sql(Arrays.asList(Column.allOf(t1)), Collections.emptyList()));
        assertEquals("SELECT id, c1 FROM table1", Select.sql(Arrays.asList(id, c1), Collections.emptyList()));
        assertEquals("SELECT table1.id, table1.fk1, table2.fk1, table1.c1 FROM table1, table2",
                     Select.sql(Arrays.asList(id, t1fk1, t2fk1, c1), Collections.emptyList()));
        assertEquals("SELECT table1.id, table1.fk1, table2.fk1, table1.c1 FROM table1, table2 WHERE table1.fk1=table2.fk1 AND table1.id=?",
                     Select.sql(Arrays.asList(id, t1fk1, t2fk1, c1), Arrays.asList(k0)));
        assertEquals("SELECT table1.id, table1.fk1, table2.fk1, table1.c1 FROM table1, table2 WHERE table1.fk1=table2.fk1 AND table1.id=? AND table1.fk1=?",
                     Select.sql(Arrays.asList(id, t1fk1, t2fk1, c1), Arrays.asList(k0, k1)));
        assertEquals("SELECT table1.id, table1.fk1, table2.fk1, table1.c1 FROM table1, table2"
                        + " WHERE table1.fk1=table2.fk1 AND table1.id=? AND table1.fk1=? AND table2.fk1=?",
                     Select.sql(Arrays.asList(id, t1fk1, t2fk1, c1), Arrays.asList(k0, k1, k2)));
        // quote
        assertEquals("SELECT * FROM `table` WHERE `person-name`=?",
                     Select.sql(Arrays.asList(Column.allOf(Table.of("table"))),
                                Arrays.asList(Clause.implicit(Column.of("person-name", Table.of("table"))))));
        assertEquals("SELECT `table`.* FROM `table`, table1 WHERE table1.id=? AND `table`.`person-name`=?",
                     Select.sql(Arrays.asList(Column.allOf(Table.of("table"))),
                                Arrays.asList(k0, Clause.implicit(Column.of("person-name", Table.of("table"))))));
    }

    @Test
    public void testSql_2() {
        Table t1 = Table.of("table1", "a");
        Table t2 = Table.of("table2", "b");
        Column c1 = Column.of("c1", t1);
        Column id = Column.of("id", t1);
        Column t1fk1 = Column.of("fk1", t1);
        Column t2fk1 = Column.of("fk1", t2);
        Clause k0 = Clause.implicit(id);
        Clause k1 = Clause.implicit(t1fk1);
        Clause k2 = Clause.implicit(t2fk1);
        assertEquals("SELECT * FROM table1", Select.sql(Arrays.asList(Column.allOf(t1)), Collections.emptyList()));
        assertEquals("SELECT id, c1 FROM table1", Select.sql(Arrays.asList(id, c1), Collections.emptyList()));
        assertEquals("SELECT a.id, a.fk1, b.fk1, a.c1 FROM table1 a, table2 b",
                     Select.sql(Arrays.asList(id, t1fk1, t2fk1, c1), Collections.emptyList()));
        assertEquals("SELECT a.id, a.fk1, b.fk1, a.c1 FROM table1 a, table2 b WHERE a.fk1=b.fk1 AND a.id=?",
                     Select.sql(Arrays.asList(id, t1fk1, t2fk1, c1), Arrays.asList(k0)));
        assertEquals("SELECT a.id, a.fk1, b.fk1, a.c1 FROM table1 a, table2 b WHERE a.fk1=b.fk1 AND a.id=? AND a.fk1=?",
                     Select.sql(Arrays.asList(id, t1fk1, t2fk1, c1), Arrays.asList(k0, k1)));
        assertEquals("SELECT a.id, a.fk1, b.fk1, a.c1 FROM table1 a, table2 b"
                        + " WHERE a.fk1=b.fk1 AND a.id=? AND a.fk1=? AND b.fk1=?",
                     Select.sql(Arrays.asList(id, t1fk1, t2fk1, c1), Arrays.asList(k0, k1, k2)));
    }

}
