package stew6;

import static org.junit.Assert.*;
import static stew6.LayeredProperties.getExternalKeys;
import java.util.*;
import org.junit.*;

public final class LayeredPropertiesTest {

    @Test
    public void testLayeredProperties() {
        Properties layer1 = new Properties();
        Map<String, String> layer2 = new HashMap<>();
        layer1.setProperty("stew.key1", "AAA");
        layer2.put("stew.key1", "BBB");
        layer2.put("stew.key2", "CCC");
        LayeredProperties o = new LayeredProperties(layer1, layer2);
        assertEquals("AAA", o.get("key1"));
        assertEquals("CCC", o.get("key2"));
    }

    @Test
    public void testDump() {
        LayeredProperties o = createSample();
        String expected = "%n--- properties layer 1 ---%nstew.key1=abc%n--- properties layer 2 ---%nstew.key1=xyz%nstew.key2=XYZ%n";
        assertEquals(expected, o.dump().replaceAll("\\r?\\n", "%n"));
    }

    @Test
    public void testHasKey() {
        LayeredProperties o = createSample();
        assertTrue(o.hasKey("key1"));
        assertTrue(o.hasKey("key2"));
        assertFalse(o.hasKey("key3"));
    }

    @Test
    public void testGetString() {
        // do nothing
    }

    @Test
    public void testGetStringString() {
        LayeredProperties o = createSample();
        assertEquals("abc", o.get("key1", "123"));
        assertEquals("XYZ", o.get("key2", "123"));
        assertEquals("123", o.get("key3", "123"));
    }

    @Test
    public void testGetAsInt() {
        Map<String, String> layer1 = new HashMap<>();
        Map<String, String> layer2 = new HashMap<>();
        layer1.put("stew.key1", "123");
        layer2.put("stew.key1", "000");
        layer2.put("stew.key2", "456");
        layer1.put("stew.notint", "abc");
        LayeredProperties o = new LayeredProperties(layer1, layer2);
        assertEquals(123, o.getAsInt("key1", 555));
        assertEquals(456, o.getAsInt("key2", 555));
        assertEquals(555, o.getAsInt("key3", 555));
        assertEquals(666, o.getAsInt("notint", 666));
    }

    @Test
    public void testGetAsBoolean() {
        Map<String, String> layer1 = new HashMap<>();
        Map<String, String> layer2 = new HashMap<>();
        layer1.put("stew.key1", "true");
        layer1.put("stew.key2", "TRUE");
        layer1.put("stew.key3", "True");
        layer1.put("stew.key4", "false");
        layer1.put("stew.key5", "FALSE");
        layer1.put("stew.key6", "False");
        layer1.put("stew.key7", "XXX");
        layer2.put("stew.key8", "true");
        layer2.put("stew.key9", "false");
        LayeredProperties o = new LayeredProperties(layer1, layer2);
        assertTrue(o.getAsBoolean("key1"));
        assertTrue(o.getAsBoolean("key2"));
        assertTrue(o.getAsBoolean("key3"));
        assertFalse(o.getAsBoolean("key4"));
        assertFalse(o.getAsBoolean("key5"));
        assertFalse(o.getAsBoolean("key6"));
        assertFalse(o.getAsBoolean("key7"));
        assertTrue(o.getAsBoolean("key8"));
        assertFalse(o.getAsBoolean("key9"));
    }

    @Test
    public void testGetExternalKeys() {
        assertArrayEquals(new String[]{"net.argius.stew.key1", "stew.key1"}, getExternalKeys("key1"));
        try {
            getExternalKeys("net.argius.stew.key1");
            fail("this test must occur error when calling getExternalKeys with prefix stew");
        } catch (IllegalArgumentException e) {
            assertEquals("LayeredProperties now allows net.argius.stew or stew as prefix, key = net.argius.stew.key1",
                         e.getMessage());
        }
        try {
            getExternalKeys("stew.key1");
            fail("this test must occur error when calling getExternalKeys with prefix stew");
        } catch (IllegalArgumentException e) {
            assertEquals("LayeredProperties now allows net.argius.stew or stew as prefix, key = stew.key1",
                         e.getMessage());
        }
    }

    private static LayeredProperties createSample() {
        Map<String, String> layer1 = new HashMap<>();
        Map<String, String> layer2 = new HashMap<>();
        layer1.put("stew.key1", "abc");
        layer2.put("stew.key1", "xyz");
        layer2.put("stew.key2", "XYZ");
        return new LayeredProperties(layer1, layer2);
    }

}
