package stew6.text;

import java.util.*;

/**
 * Utilities for text.
 */
public final class TextUtilities {

    private TextUtilities() { // empty, forbidden
    }

    @Deprecated
    public static String join(String delimiter, Collection<?> a) {
        if (a.isEmpty()) {
            return "";
        }
        StringBuilder buffer = new StringBuilder();
        for (Object o : a) {
            buffer.append(delimiter);
            buffer.append(o);
        }
        return buffer.substring(delimiter.length());
    }

}
