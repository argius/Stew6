package stew6;

import stew6.io.*;

final class ConnectorsConfigFile extends YamlFile<ConnectorsConfig> {

    static final String CONNECTORS_FILE_NAME = "connectors.yml";

    public ConnectorsConfigFile() {
        super(ConnectorsConfig.class, () -> App.getSystemFile(CONNECTORS_FILE_NAME).toPath());
    }

}
