package stew6.sql;

import static org.junit.Assert.assertEquals;
import java.util.*;
import org.junit.*;
import stew6.*;

public final class InsertTest {

    @Test
    public void testSql() {
        Table t1 = Table.of("table1");
        // Table table, int columnCount
        assertEquals("INSERT INTO table1 VALUES (?, ?, ?)", Insert.sql(Table.of("table1"), 3));
        // String tableName, int columnCount
        assertEquals("INSERT INTO table1 VALUES (?, ?, ?, ?, ?)", Insert.sql("table1", 5));
        assertEquals("INSERT INTO table1 VALUES (?, ?, ?, ?, ?)", Insert.sql("table1", 5));
        assertEquals("INSERT INTO table1 VALUES (?, ?, ?, ?, ?)", Insert.sql("table1", 5));
        // List<Column> columns
        assertEquals("INSERT INTO table1 (c1, c2, id) VALUES (?, ?, ?)",
                     Insert.sql(t1, FunctionalUtils.mapAndToList(Arrays.asList("c1", "c2", "id"), Column::of)));
        // Column... columns
        assertEquals("INSERT INTO table1 (c1, id, c2) VALUES (?, ?, ?)",
                     Insert.sql(t1, Column.of("c1"), Column.of("id"), Column.of("c2")));
    }

}
