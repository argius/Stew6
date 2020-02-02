package stew6.sql;

import static org.junit.Assert.*;
import org.junit.*;

public final class SqlTest {

    @Test
    public void testShouldQuote() {
        String[] shouldQuote = {"table", "person-name",};
        String[] notQuotes = {"table1", "id",};
        for (String s : shouldQuote) {
            assertTrue(s, Sql.shouldQuote(s));
            assertTrue(s.toUpperCase(), Sql.shouldQuote(s.toUpperCase()));
        }
        for (String s : notQuotes) {
            assertFalse(s.toUpperCase(), Sql.shouldQuote(s.toUpperCase()));
        }
    }

}
