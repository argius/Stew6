package stew6;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import org.yaml.snakeyaml.*;
import org.yaml.snakeyaml.DumperOptions.*;
import org.yaml.snakeyaml.nodes.*;
import org.yaml.snakeyaml.representer.*;
import stew6.ConnectorsConfig.*;

final class ConnectorsConfigFile {

    static final String CONNECTORS_FILE_NAME = "connectors.yml";

    static ConnectorsConfig read() throws IOException {
        Yaml yaml = new Yaml();
        try (InputStream is = Files.newInputStream(getPath())) {
            ConnectorsConfig cfg = yaml.loadAs(is, ConnectorsConfig.class);
            return Optional.ofNullable(cfg).orElseGet(() -> new ConnectorsConfig());
        } catch (NoSuchFileException e) {
            Files.createFile(getPath());
            return new ConnectorsConfig();
        }
    }

    static void write(ConnectorsConfig config) throws IOException {
        try (Writer out = Files.newBufferedWriter(getPath())) {
            writeTo(out, config);
        }
    }

    static void writeTo(Writer out, ConnectorsConfig config) throws IOException {
        Representer repr = new Representer();
        repr.addClassTag(ConnectorsConfig.class, Tag.MAP);
        repr.addClassTag(ConnectorConfig.class, Tag.MAP);
        DumperOptions op = new DumperOptions();
        op.setDefaultFlowStyle(FlowStyle.BLOCK);
        op.setIndicatorIndent(1);
        op.setIndent(3);
        Yaml yml = new Yaml(repr, op);
        out.append(yml.dump(config));
        out.flush();
    }

    static long getLastModified() {
        try {
            return Files.getLastModifiedTime(getPath()).toMillis();
        } catch (NoSuchFileException e) {
            return 0L;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    static Path getPath() {
        return App.getSystemFile(CONNECTORS_FILE_NAME).toPath();
    }

}
