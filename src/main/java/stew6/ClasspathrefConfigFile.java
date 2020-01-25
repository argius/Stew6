package stew6;

import java.io.*;
import java.nio.file.*;
import org.yaml.snakeyaml.*;
import org.yaml.snakeyaml.DumperOptions.*;
import org.yaml.snakeyaml.nodes.*;
import org.yaml.snakeyaml.representer.*;
import stew6.ConnectorsConfig.*;

public final class ClasspathrefConfigFile {

    static final String CLASSPATHREF_FILE_NAME = "classpathref.yml";

    public static ClasspathrefConfig read() throws IOException {
        Yaml yaml = new Yaml();
        try (InputStream is = Files.newInputStream(getPath())) {
            return yaml.loadAs(is, ClasspathrefConfig.class);
        }
    }

    static void write(ClasspathrefConfig config) throws IOException {
        try (Writer out = Files.newBufferedWriter(getPath())) {
            writeTo(out, config);
        }
    }

    static void writeTo(Writer out, ClasspathrefConfig config) throws IOException {
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
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    static Path getPath() {
        return App.getSystemFile(CLASSPATHREF_FILE_NAME).toPath();
    }

}
