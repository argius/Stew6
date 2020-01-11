package net.argius.stew;

import java.util.*;

/**
 * Parameter.
 */
public final class Parameter {

    private final String string;
    private final String[] array;
    private final int[] indices;

    /**
     * A constructor.
     * @param string
     */
    public Parameter(String string) {
        char[] chars = string.toCharArray();
        int[] indices = indices(chars);
        String[] array = array(chars, indices);
        this.string = string;
        this.array = array;
        this.indices = indices;
    }

    private static int[] indices(char[] chars) {
        List<Integer> a = new ArrayList<>();
        boolean prev = true;
        boolean quoted = false;
        for (int i = 0; i < chars.length; i++) {
            final char c = chars[i];
            if (c == '"') {
                quoted = !quoted;
            }
            final boolean f = isSpaceChar(c);
            if (!f && f != prev) {
                a.add(i);
            }
            prev = !quoted && f;
        }
        a.add(chars.length);
        int[] indices = new int[a.size()];
        for (int i = 0; i < indices.length; i++) {
            indices[i] = a.get(i);
        }
        return indices;
    }

    private static String[] array(char[] chars, int[] indices) {
        String[] a = new String[indices.length - 1];
        for (int i = 0; i < a.length; i++) {
            final int offset = indices[i];
            int end = indices[i + 1];
            while (end > offset) {
                if (!isSpaceChar(chars[end - 1])) {
                    break;
                }
                --end;
            }
            final String s = String.valueOf(chars, offset, end - offset);
            a[i] = (chars[offset] == '"') ? s.substring(1, s.length() - 1) : s;
        }
        return a;
    }

    private static boolean isSpaceChar(char c) {
        switch (c) {
            case '\t':
            case '\n':
            case '\f':
            case '\r':
            case ' ':
                return true;
            default:
        }
        return false;
    }

    /**
     * Returns the parameter at the position specified index.
     * @param index
     * @return
     */
    public String at(int index) {
        return has(index) ? array[index] : "";
    }

    /**
     * Returns the parameter after the position specified index.
     * @param index
     * @return
     */
    public String after(int index) {
        return has(index) ? string.substring(indices[index]) : "";
    }

    /**
     * Returns whether a parameter exists at the position specified index.
     * @param index
     * @return
     */
    public boolean has(int index) {
        if (index < 0) {
            throw new IndexOutOfBoundsException("index >= 0: " + index);
        }
        return index < array.length;
    }

    /**
     * Returns this parameter as an array.
     * @return
     */
    public String[] asArray() {
        return array.clone();
    }

    /**
     * Returns this parameter as String.
     * is not the same as <code>toString</code>
     * @return
     */
    public String asString() {
        return string;
    }

    @Override
    public String toString() {
        return "Parameter[" + string + "]";
    }

}
