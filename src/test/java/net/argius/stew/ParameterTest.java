package net.argius.stew;

import static org.junit.Assert.*;
import org.junit.*;

public final class ParameterTest {

    private static final String S1 = "-f a.xls select  *\nfrom test";
    private static final String S2 = "-fa.xlsselect*fromtest";
    private static final String S3 = " export  \"my data.xml\" select\t* from test";

    private Parameter p1;
    private Parameter p2;
    private Parameter p3;

    public ParameterTest() {
        this.p1 = new Parameter(S1);
        this.p2 = new Parameter(S2);
        this.p3 = new Parameter(S3);
    }

    @Test
    public void testParameter() {
        // empty
    }

    @Test
    public void testAt() {
        assertEquals("-f", p1.at(0));
        assertEquals("a.xls", p1.at(1));
        assertEquals("select", p1.at(2));
        assertEquals("*", p1.at(3));
        assertEquals("from", p1.at(4));
        assertEquals("test", p1.at(5));
        assertEquals("", p1.at(6));
        assertEquals(p2.asString(), p2.at(0));
        assertEquals("", p2.at(1));
        assertEquals("", p2.at(2));
        assertEquals("export", p3.at(0));
        assertEquals("my data.xml", p3.at(1));
        assertEquals("select", p3.at(2));
        assertEquals("*", p3.at(3));
        assertEquals("from", p3.at(4));
        assertEquals("test", p3.at(5));
        // error
        try {
            p1.at(-1);
            fail();
        } catch (IndexOutOfBoundsException ex) {
            assertEquals("index >= 0: -1", ex.getMessage());
        }
        try {
            p2.at(-1);
            fail();
        } catch (IndexOutOfBoundsException ex) {
            assertEquals("index >= 0: -1", ex.getMessage());
        }
    }

    @Test
    public void testAfter() {
        assertEquals(S1, p1.after(0));
        assertEquals("a.xls select  *\nfrom test", p1.after(1));
        assertEquals("select  *\nfrom test", p1.after(2));
        assertEquals(S2, p2.after(0));
        assertEquals("", p2.after(1));
        assertEquals("", p2.after(2));
        assertEquals(S3.substring(1), p3.after(0));
        assertEquals(S3.substring(9), p3.after(1));
        assertEquals(S3.substring(23), p3.after(2));
        // error
        try {
            p1.after(-1);
            fail();
        } catch (IndexOutOfBoundsException ex) {
            assertEquals("index >= 0: -1", ex.getMessage());
        }
        try {
            p2.after(-1);
            fail();
        } catch (IndexOutOfBoundsException ex) {
            assertEquals("index >= 0: -1", ex.getMessage());
        }
    }

    @Test
    public void testHas() {
        assertTrue(p1.has(0));
        assertTrue(p1.has(1));
        assertTrue(p1.has(2));
        assertTrue(p1.has(3));
        assertTrue(p1.has(4));
        assertTrue(p1.has(5));
        assertTrue(!p1.has(6));
        assertTrue(!p1.has(7));
        assertTrue(p2.has(0));
        assertTrue(!p2.has(1));
        assertTrue(!p2.has(2));
        assertTrue(p3.has(0));
        assertTrue(p3.has(1));
        assertTrue(p3.has(2));
        assertTrue(p3.has(3));
        assertTrue(p3.has(4));
        assertTrue(p3.has(5));
        assertTrue(!p3.has(6));
    }

    @Test
    public void testAsArray() {
        assertArrayEquals(a(S1), p1.asArray());
        assertArrayEquals(a(S2), p2.asArray());
        assertArrayEquals(a(S3), p3.asArray());
    }

    @Test
    public void testAsString() {
        assertEquals(S1, p1.asString());
        assertEquals(S2, p2.asString());
        assertEquals(S3, p3.asString());
    }

    @Test
    public void testToString() {
        assertEquals(String.format("Parameter[%s]", S1), p1.toString());
        assertEquals(String.format("Parameter[%s]", S2), p2.toString());
    }

    private String[] a(String s) {
        Parameter p = new Parameter(s);
        return p.asArray();
    }

}
