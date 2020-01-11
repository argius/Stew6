package stew6.ui;

import static org.junit.Assert.assertEquals;
import java.lang.reflect.*;
import java.util.*;
import org.junit.*;
import stew6.*;

public final class PromptTest {

    @Test
    public void testToString() {
        Environment env = new Environment();
        Prompt prompt = new Prompt(env);
        assertEquals(" > ", prompt.toString());
        Properties props = new Properties();
        props.setProperty("name", "testConnectorNameXXX");
        props.setProperty("url", "jdbc:h2:mem:test");
        props.setProperty("driver", "org.h2.Driver");
        props.setProperty("user", "sa");
        props.setProperty("password", "sa");
        setConnectorToEnv(new Connector("test1", props), env);
        assertEquals("testConnectorNameXXX > ", prompt.toString());
    }

    static void setConnectorToEnv(Connector connector, Environment env) {
        try {
            Method m = env.getClass().getDeclaredMethod("establishConnection", Connector.class);
            m.setAccessible(true);
            m.invoke(env, connector);
        } catch (Exception ex) {
            throw new IllegalArgumentException(ex);
        }
    }

}
