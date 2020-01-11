package stew6.text;

import static org.junit.Assert.assertEquals;
import static stew6.text.PrintFormat.format;
import org.junit.*;

public final class PrintFormatTest {

    @Test
    public void testFormat() {
        assertEquals("     abc", format("%8s", "abc"));
        assertEquals("  あいう", format("%8s", "あいう"));
        assertEquals("あいう  ", format("%-8s", "あいう"));
        assertEquals("  \u2000", format("%3s", '\u2000'));
        assertEquals("  \uFF60", format("%3s", '\uFF60'));
        assertEquals("  \uFF61", format("%3s", '\uFF61'));
        assertEquals("  \uFF9F", format("%3s", '\uFF9F'));
        assertEquals("  \uFFA0", format("%3s", '\uFFA0'));
    }

}
