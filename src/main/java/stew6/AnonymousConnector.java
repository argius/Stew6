package stew6;

import java.util.*;

/**
 * A factory class that creates anonymous connector.
 */
final class AnonymousConnector {

    private static final String PREFIX_JDBC = "jdbc:";

    private AnonymousConnector() { // empty, forbidden
    }

    /**
     * Gets an anonymous connector with a URI.
     * @param uri user/password@URL
     * @return an anonymous connector
     */
    public static Connector getConnector(String uri) {
        int index1 = uri.indexOf('@');
        if (index1 < 0) {
            throw new IllegalArgumentException(uri);
        }
        String userInfo = uri.substring(0, index1);
        String url = uri.substring(index1 + 1);
        String user;
        String password;
        int index2 = userInfo.indexOf('/');
        if (index2 >= 0) {
            user = userInfo.substring(0, index2);
            password = userInfo.substring(index2 + 1);
        } else {
            user = userInfo;
            password = "";
        }
        return getConnector(url, user, password);
    }

    /**
     * Gets an anonymous connector with a URI, a user name and a password.
     * @param url the JDBC URL
     * @param user the database user to connect with it
     * @param password the user's password
     * @return an anonymous connector
     */
    public static Connector getConnector(String url, String user, String password) {
        Properties props = new Properties();
        props.setProperty("name", getName(url, user));
        props.setProperty("driver", "");
        props.setProperty("classpath", "");
        props.setProperty("url", url);
        props.setProperty("user", user);
        props.setProperty("readonly", Boolean.FALSE.toString());
        props.setProperty("rollback", Boolean.FALSE.toString());
        Connector connector = new Connector("ANONYMOUS", props);
        connector.getPassword().setRawString(password);
        return connector;
    }

    private static String getName(String url, String user) {
        final String url0 = url.startsWith(PREFIX_JDBC) ? url.substring(PREFIX_JDBC.length()) : url;
        return String.format("%s@%s", user, url0);
    }

}
