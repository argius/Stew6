package stew6;

import java.io.*;
import java.text.*;
import java.util.*;
import java.util.concurrent.*;

/**
 * ResourceManager provides a function like a ResourceBundle used UTF-8 instead Unicode escapes.
 */
public final class ResourceManager {

    public static final ResourceManager Default = ResourceManager.getInstance(ResourceManager0.class);

    private List<Map<String, String>> list;

    private ResourceManager(List<Map<String, String>> list) {
        this.list = list;
    }

    /**
     * Creates an instance.
     * @param o
     * It used as a bundle name.
     * If String, it will use as a bundle name directly.
     * Else if Package, it will use its package name + "messages".
     * Otherwise, it will use as its FQCN.
     * @return
     */
    public static ResourceManager getInstance(Object o) {
        Locale loc = Locale.getDefault();
        String[] suffixes = {"_" + loc, "_" + loc.getLanguage(), ""};
        List<Map<String, String>> a = new ArrayList<>();
        for (final String name : getResourceNames(o)) {
            for (final String suffix : suffixes) {
                final String key = name + suffix;
                Map<String, String> m = (ResourceManager0.map.containsKey(key))
                        ? ResourceManager0.map.get(key)
                        : loadResource(key, "u8p", "utf-8");
                if (m != null) {
                    ResourceManager0.map.put(key, m);
                    a.add(m);
                }
            }
        }
        return new ResourceManager(a);
    }

    private static Set<String> getResourceNames(Object o) {
        Set<String> set = new LinkedHashSet<>();
        String cn = null;
        String pn = null;
        if (o instanceof String) {
            cn = (String)o;
        } else if (o instanceof Package) {
            pn = ((Package)o).getName();
        } else if (o != null) {
            final Class<?> c = (o instanceof Class) ? (Class<?>)o : o.getClass();
            cn = c.getName();
            pn = c.getPackage().getName();
        }
        if (cn != null) {
            set.add(cn);
        }
        if (pn != null) {
            set.add(pn + ".messages");
        }
        set.add(ResourceManager0.getPackageName() + ".messages");
        return set;
    }

    private static Map<String, String> loadResource(String name, String extension, String encname) {
        final String path = "/" + name.replace('.', '/') + '.' + extension;
        InputStream is = ResourceManager0.getResourceAsStream(path);
        if (is == null) {
            return null;
        }
        List<String> lines = new ArrayList<>();
        Scanner r = new Scanner(is, encname);
        try {
            StringBuilder buffer = new StringBuilder();
            while (r.hasNextLine()) {
                final String s = r.nextLine();
                if (s.matches("^\\s*#.*")) {
                    continue;
                }
                buffer.append(s.replace("\\t", "\t").replace("\\n", "\n").replace("\\=", "="));
                if (s.endsWith("\\")) {
                    buffer.setLength(buffer.length() - 1);
                    continue;
                }
                lines.add(buffer.toString());
                buffer.setLength(0);
            }
            if (buffer.length() > 0) {
                lines.add(buffer.toString());
            }
        } finally {
            r.close();
        }
        Map<String, String> m = new HashMap<>();
        for (final String s : lines) {
            if (s.contains("=")) {
                String[] a = s.split("=", 2);
                m.put(a[0].trim(), a[1].trim().replaceFirst("\\\\$", " ").replace("\\ ", " "));
            } else {
                m.put(s.trim(), "");
            }
        }
        return m;
    }

    /**
     * @param path resource's path
     * @param defaultValue defalut value if a resource not found
     * @return
     */
    public String read(String path, String defaultValue) {
        InputStream in = ResourceManager0.getResourceAsStream(path);
        if (in == null) {
            return defaultValue;
        }
        StringBuilder buffer = new StringBuilder();
        Scanner r = new Scanner(in);
        try {
            if (r.hasNextLine()) {
                buffer.append(r.nextLine());
            }
            while (r.hasNextLine()) {
                buffer.append(String.format("%n"));
                buffer.append(r.nextLine());
            }
        } finally {
            r.close();
        }
        return buffer.toString();
    }

    private String s(String key) {
        for (final Map<String, String> m : this.list) {
            final String s = m.get(key);
            if (s != null) {
                return s;
            }
        }
        return "";
    }

    /**
     * Returns true if this resource contains a value specified by key.
     * @param key
     * @return
     */
    public boolean containsKey(String key) {
        for (final Map<String, String> m : this.list) {
            if (m.containsKey(key)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns the value specified by key as a String.
     * @param key
     * @param args
     * @return
     */
    public String get(String key, Object... args) {
        final String s = s(key);
        return (s.length() == 0) ? key : MessageFormat.format(s, args);
    }

    /**
     * Returns the value specified by key as a boolean.
     * @param key
     * @return
     */
    public boolean isTrue(String key) {
        return s(key).matches("(?i)true|on|yes");
    }

    /**
     * Returns the (initial char) value specified by key as a char.
     * @param key
     * @return
     */
    public char getChar(String key) {
        final String s = s(key);
        return (s.length() == 0) ? ' ' : s.charAt(0);
    }

    /**
     * Returns the value specified by key as a int.
     * @param key
     * @return
     */
    public int getInt(String key) {
        return getInt(key, 0);
    }

    /**
     * Returns the value specified by key as a int.
     * @param key
     * @param defaultValue
     * @return
     */
    public int getInt(String key, int defaultValue) {
        final String s = s(key);
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException ex) {
            // ignore
        }
        return defaultValue;
    }

}

class ResourceManager0 {

    static final ConcurrentHashMap<String, Map<String, String>> map = new ConcurrentHashMap<>();

    static String getPackageName() {
        return ResourceManager0.class.getPackage().getName();
    }

    static InputStream getResourceAsStream(String path) {
        return ResourceManager0.class.getResourceAsStream(path);
    }

}
