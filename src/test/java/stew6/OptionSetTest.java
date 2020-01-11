package stew6;

import static org.junit.Assert.*;
import org.apache.commons.cli.*;
import org.junit.*;
import org.junit.rules.*;
import stew6.OptionSet.Parser;

public final class OptionSetTest {

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test
    public void testParseArguments() throws Exception {
        OptionSet opts;
        opts = OptionSet.parseArguments(a("-c", "connector1"));
        assertEquals("connector1", opts.getConnecterName());
        opts = OptionSet.parseArguments(a("-v"));
        assertEquals("", opts.getConnecterName());
        opts = OptionSet.parseArguments(a("--cui", "-c", "connector1", "-e", "-"));
        assertEquals("-e -", opts.getCommandString());
        // getter
        opts = OptionSet.parseArguments(a("--cui", "--gui", "-v", "--help", "--quiet"));
        assertTrue(opts.isCui());
        assertTrue(opts.isGui());
        assertTrue(opts.isShowVersion());
        assertTrue(opts.isHelp());
        assertTrue(opts.isQuiet());
        assertFalse(opts.isEdit());
    }

    @Test
    public void testShowHelp() throws Exception {
        OptionSet.showHelp();
    }

    @Test
    public void testParserGetOptions() throws Exception {
        Parser parser = new Parser();
        Options o = parser.getOptions();
        assertEquals("[cui, connect]", o.getMatchingOptions("c").toString());
        assertEquals("[]", o.getRequiredOptions().toString());
    }

    @Test
    public void testIllegalArgumentExceptionInparseArguments() throws Exception {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("duplicate options: connect");
        OptionSet.parseArguments(a("-c", "connector1", "-c", "connector2"));
    }

    static String[] a(String... args) {
        return args;
    }

}
