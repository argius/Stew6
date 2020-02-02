package stew6.io;

import java.io.*;
import java.nio.file.*;
import java.util.function.*;
import org.yaml.snakeyaml.*;
import org.yaml.snakeyaml.DumperOptions.*;
import org.yaml.snakeyaml.nodes.*;
import org.yaml.snakeyaml.representer.*;
import stew6.*;
import stew6.ConnectorsConfig.*;

/**
 * YAML file.
 * @param <T>
 */
public class YamlFile<T> {

    private Class<? extends T> c;
    private Supplier<? extends Path> pathSupplier;

    protected YamlFile(Class<? extends T> c, Supplier<? extends Path> pathSupplier) {
        this.c = c;
        this.pathSupplier = pathSupplier;
    }

    public static <T> YamlFile<T> forClass(Class<? extends T> c, Supplier<? extends Path> pathSupplier) {
        return new YamlFile<>(c, pathSupplier);
    }

    public T read() throws IOException {
        return read(c, pathSupplier);
    }

    public static <T> T read(Class<T> c, Supplier<? extends Path> pathSupplier) throws IOException {
        try (InputStream is = Files.newInputStream(pathSupplier.get())) {
            Yaml yaml = new Yaml();
            T o = yaml.loadAs(is, c);
            if (o != null) {
                return o;
            }
        } catch (NoSuchFileException e) {
            Files.createFile(pathSupplier.get());
        }
        try {
            return c.newInstance();
        } catch (InstantiationException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    public void write(T o) throws IOException {
        try (Writer out = Files.newBufferedWriter(pathSupplier.get())) {
            writeTo(out, o);
        }
    }

    public static <T> void writeTo(Writer out, T o) throws IOException {
        Representer repr = new Representer();
        repr.addClassTag(ConnectorsConfig.class, Tag.MAP);
        repr.addClassTag(ConnectorConfig.class, Tag.MAP);
        DumperOptions op = new DumperOptions();
        op.setDefaultFlowStyle(FlowStyle.BLOCK);
        op.setIndicatorIndent(1);
        op.setIndent(3);
        Yaml yaml = new Yaml(repr, op);
        yaml.dump(o, out);
        out.flush();
    }

    public long getLastModified() {
        try {
            return Files.getLastModifiedTime(pathSupplier.get()).toMillis();
        } catch (NoSuchFileException e) {
            return 0L;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public Path getPath() {
        return pathSupplier.get();
    }

}
