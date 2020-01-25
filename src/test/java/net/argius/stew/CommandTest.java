package net.argius.stew;

import static org.junit.Assert.*;
import org.junit.*;

public final class CommandTest {

    @Test
    public void testMayReturnResultSet() {
        assertTrue(Command.mayReturnResultSet("values('aaa')"));
        assertTrue(Command.isSelect("select * from table1"));
        assertFalse(Command.isSelect("/* select * from table1 */ update"));
    }

    @Test
    public void testIsOnlyValuesConstructor() {
        assertTrue(Command.startsTableValueConstructor("values('aaa')"));
        assertFalse(Command.startsTableValueConstructor("select * from table1"));
        assertTrue(Command.startsTableValueConstructor("values()"));
        assertTrue(Command.startsTableValueConstructor(" VALUES('xyz', '123')"));
        assertTrue(Command.startsTableValueConstructor("\nvalues(\n '' \n) "));
        assertTrue(Command.startsTableValueConstructor("/**/\nvalues(\n '' \n) "));
        assertFalse(Command.startsTableValueConstructor("insert into table1 values (2, 'Bob')"));
    }

    @Test
    public void testIsSelect() {
        assertTrue(Command.isSelect("select * from table1"));
        assertFalse(Command.isSelect("/* select * from table1 */ update"));
    }

}
