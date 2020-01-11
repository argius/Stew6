package stew6.ui;

import stew6.*;

public final class Prompt {

    private final Environment env;

    public Prompt(Environment env) {
        this.env = env;
    }

    @Override
    public String toString() {
        Connector connector = env.getCurrentConnector();
        final String currentConnectorName = (connector == null) ? "" : connector.getName();
        return currentConnectorName + " > ";
    }

}
