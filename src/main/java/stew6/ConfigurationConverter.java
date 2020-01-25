package stew6;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.regex.*;
import stew6.ConnectorsConfig.*;

public final class ConfigurationConverter {

    private static final Logger log = Logger.getLogger(ConfigurationConverter.class);

    /**
     * Converts connectors.properties to connectors.yml if needed.
     */
    public static void convertConnectorsConfigIfNeeded() {
        final Path newConfigPath = ConnectorsConfigFile.getPath();
        final boolean existsNewConfig = Files.exists(newConfigPath);
        if (existsNewConfig) {
            log.info("skip converting: newConfig=(path=%s, exists=%s)", newConfigPath, existsNewConfig);
            return;
        }
        final String OLD_CONNECTOR_PROPERTIES_NAME = "connector.properties";
        final Path oldConfigPath = App.getSystemFile(OLD_CONNECTOR_PROPERTIES_NAME).toPath();
        final boolean existsOldConfig = Files.exists(oldConfigPath);
        if (!existsOldConfig) {
            log.info("skip converting: oldConfig=(path=%s, exists=%s)", oldConfigPath, existsOldConfig);
            return;
        }
        try {
            ConnectorMap oldConnectorConfig = loadOldVersionConnectorConfiguration(oldConfigPath);
            if (oldConnectorConfig.isEmpty()) {
                log.info("skip converting: EMPTY oldConfig=(path=%s, exists=%s)", oldConfigPath, existsOldConfig);
                return;
            }
            log.info("oldConfig=(path=%s, exists=%s), newConfig=(path=%s, exists=%s)", // condition
                     oldConfigPath, existsOldConfig, newConfigPath, existsNewConfig);
            ConnectorsConfig cs = new ConnectorsConfig();
            cs.setConnectors(FunctionalUtils.mapAndToList(oldConnectorConfig.values(),
                                                          x -> convertConnectorToConnectorConfig(x)));
            ConnectorsConfigFile.write(cs);
            log.info("converted");
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static ConnectorMap loadOldVersionConnectorConfiguration(Path oldConfigPath) throws IOException {
        byte[] data = Files.readAllBytes(oldConfigPath);
        ByteArrayInputStream bis = new ByteArrayInputStream(data);
        // create ID list
        List<String> idList = new ArrayList<>();
        final Pattern idPattern = Pattern.compile("^([^\\.]+)\\.name *=");
        try (Scanner scanner = new Scanner(bis)) {
            while (scanner.hasNextLine()) {
                final String line = scanner.nextLine();
                Matcher matcher = idPattern.matcher(line);
                if (matcher.find()) {
                    idList.add(matcher.group(1));
                }
            }
        }
        bis.reset();
        Properties props = new Properties();
        props.load(bis);
        return new ConnectorMap(idList, props);
    }

    static ConnectorConfig convertConnectorToConnectorConfig(Connector c) {
        ConnectorConfig cc = new ConnectorConfig();
        ConnectorConfigSettings st = new ConnectorConfigSettings();
        cc.setId(c.getId());
        cc.setName(c.getName());
        cc.setSettings(st);
        st.setDriver(c.getDriver());
        st.setClasspath(c.getClasspath());
        st.setUrl(c.getUrl());
        st.setUser(c.getUser());
        st.setPassword(c.getPassword().getRawString());
        st.setPasswordClass(c.getPassword().getClass().getName());
        st.setReadOnly(c.isReadOnly());
        st.setEnablesAutoRollback(c.usesAutoRollback());
        return cc;
    }

}
