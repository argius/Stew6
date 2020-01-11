package stew6;

import java.io.*;
import java.util.*;
import java.util.Map.*;
import net.argius.stew.*;

/**
 * Alias Map.
 */
final class AliasMap {

    private final Properties properties;
    private final File file;

    private long timestamp;

    AliasMap(File file) {
        this.properties = new Properties();
        this.file = file;
        this.timestamp = 0L;
    }

    String expand(Parameter p) {
        return expand(p.at(0), p);
    }

    String expand(String command, Parameter p) {
        StringBuilder buffer = new StringBuilder(p.asString());
        String key = command;
        final int limit = 100;
        int limitCount = limit;
        while (containsKey(key)) {
            final String value = getValue(key);
            buffer.replace(0, key.length(), value);
            key = new Parameter(buffer.toString()).at(0);
            if (--limitCount < 0) {
                final String mkey = "e.alias-circulation-reference";
                throw new CommandException(ResourceManager.Default.get(mkey, limit));
            }
        }
        return buffer.toString();
    }

    String getValue(String key) {
        return properties.getProperty(key, "");
    }

    void setValue(String key, String value) {
        properties.setProperty(key, value);
    }

    Object remove(String key) {
        return properties.remove(key);
    }

    boolean containsKey(String key) {
        return properties.containsKey(key);
    }

    boolean isEmpty() {
        return properties.isEmpty();
    }

    Set<String> keys() {
        Set<String> set = new LinkedHashSet<>();
        for (final Object o : Collections.list(properties.propertyNames())) {
            set.add((String)o);
        }
        return set;
    }

    Set<Entry<Object, Object>> entrySet() {
        return properties.entrySet();
    }

    void load() throws IOException {
        InputStream is = new FileInputStream(file);
        try {
            properties.clear();
            properties.load(is);
        } finally {
            is.close();
        }
        timestamp = file.lastModified();
    }

    /**
     * Reloads properties from a file if the file was updated.
     * @throws IOException
     */
    void reload() throws IOException {
        if (updated()) {
            load();
        }
    }

    void save() throws IOException {
        if (isEmpty()) {
            if (file.exists()) {
                if (!file.delete()) {
                    throw new IOException("file couldn't delete: " + file);
                }
            }
            return;
        }
        OutputStream os = new FileOutputStream(file);
        try {
            properties.store(os, "");
        } finally {
            os.close();
        }
        timestamp = file.lastModified();
    }

    boolean updated() {
        return file.lastModified() > timestamp;
    }


}
