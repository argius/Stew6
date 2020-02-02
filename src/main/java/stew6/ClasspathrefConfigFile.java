package stew6;

import java.io.*;
import stew6.io.*;

public final class ClasspathrefConfigFile extends YamlFile<ClasspathrefConfig> {

    static final String CLASSPATHREF_FILE_NAME = "classpathref.yml";

    public ClasspathrefConfigFile() {
        super(ClasspathrefConfig.class, () -> App.getSystemFile(CLASSPATHREF_FILE_NAME).toPath());
    }

    public static String getClasspath(String key) {
        try {
            return new ClasspathrefConfigFile().read().get(key);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

}
