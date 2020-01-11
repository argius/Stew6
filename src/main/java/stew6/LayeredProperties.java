package stew6;

import java.io.*;
import java.util.*;
import java.util.Map.*;

/**
 * LayeredProperties manages layered properties which Stew uses and provides useful accessors.
 */
public final class LayeredProperties {

    private static final String prefix1 = "net.argius.stew";
    private static final String prefix2 = "stew";

    private final List<Map<String, String>> maps;

    LayeredProperties(Map<?, ?>... arrayOfProperties) {
        List<Map<String, String>> a = new ArrayList<>();
        for (Map<?, ?> props : arrayOfProperties) {
            a.add(toStringMap(props));
        }
        this.maps = a;
    }

    String dump() {
        StringWriter sw = new StringWriter();
        PrintWriter out = new PrintWriter(sw);
        out.println();
        List<Map<String, String>> a = maps;
        int i = 0;
        for (Map<String, String> m : a) {
            out.printf("--- properties layer %d ---%n", ++i);
            List<String> keys = new ArrayList<>(m.keySet());
            Collections.sort(keys);
            for (String key : keys) {
                out.println(key + '=' + m.get(key));
            }
        }
        return sw.toString();
    }

    public boolean hasKey(String key) {
        String[] keys = getExternalKeys(key);
        for (Map<String, String> m : maps) {
            for (int i = 0; i < keys.length; i++) {
                if (m.containsKey(keys[i])) {
                    return true;
                }
            }
        }
        return false;
    }

    public String get(String key) {
        return get(key, "");
    }

    public String get(String key, String defaultValue) {
        String[] keys = getExternalKeys(key);
        for (Map<String, String> m : maps) {
            for (int i = 0; i < keys.length; i++) {
                String k = keys[i];
                if (m.containsKey(k)) {
                    return m.get(k);
                }
            }
        }
        return defaultValue;
    }

    public int getAsInt(String key, int defaultValue) {
        String v = get(key);
        if (v != null) {
            try {
                return Integer.parseInt(v);
            } catch (NumberFormatException ex) {
                // ignore
            }
        }
        return defaultValue;
    }

    public boolean getAsBoolean(String key) {
        return Boolean.valueOf(get(key, ""));
    }

    static String[] getExternalKeys(String key) {
        if (key.startsWith(prefix1) || key.startsWith(prefix2)) {
            String msg = "LayeredProperties now allows " + prefix1 + " or " + prefix2 + " as prefix, key = " + key;
            throw new IllegalArgumentException(msg);
        }
        return new String[]{prefix1 + '.' + key, prefix2 + '.' + key};
    }

    private static Map<String, String> toStringMap(Map<?, ?> map) {
        Map<String, String> m = new LinkedHashMap<>();
        for (Entry<?, ?> entry : map.entrySet()) {
            Object key = entry.getKey();
            Object value = entry.getValue();
            String k = (key instanceof String) ? (String)key : String.valueOf(key);
            String v = (value instanceof String) ? (String)value : String.valueOf(value);
            m.put(k, v);
        }
        return m;
    }

}
