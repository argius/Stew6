package stew6;

import static org.junit.Assert.assertEquals;
import org.junit.*;
import org.junit.rules.*;

public final class AnonymousConnectorTest {

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test
    public void testGetConnectorString() {
        Connector c = AnonymousConnector.getConnector("sa/sa@jdbc:h2:mem:test");
        assertEquals("Connector:ANONYMOUS", c.toString());
        Connector c2 = AnonymousConnector.getConnector("sa@jdbc:h2:mem:test");
        assertEquals("Connector:ANONYMOUS", c2.toString());
    }

    @Test
    public void testGetConnectorStringStringString() {
        // do nothing
    }

    @Test
    public void testGetConnectorStringThrowsIllegalArgumentException() {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("jdbc:h2:mem:test");
        AnonymousConnector.getConnector("jdbc:h2:mem:test");
    }

}
