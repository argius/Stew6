package stew6.text;

import static java.util.FormattableFlags.LEFT_JUSTIFY;
import java.util.*;

/**
 * The text formatter supports the width of
 *  multi-byte characters and full-width characters.
 */
public final class PrintFormat {

    /**
     * Formats text.
     * @param format
     * @param args
     * @return
     */
    public static String format(String format, Object... args) {
        List<Object> a = new ArrayList<>();
        for (Object arg : args) {
            final Object o;
            if (arg == null || arg instanceof CharSequence) {
                o = new FullwidthFormatter((arg == null) ? "" : arg.toString());
            } else {
                o = arg;
            }
            a.add(o);
        }
        try {
            return String.format(format, a.toArray());
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    private static final class FullwidthFormatter implements Formattable {

        private static final char SPACE = ' ';

        private final char[] chars;
        private final int length;

        FullwidthFormatter(String string) {
            this.chars = string.toCharArray();
            this.length = chars.length;
        }

        @Override
        public void formatTo(Formatter formatter, int flags, int width, int precision) {
            final boolean leftJustify = (flags & LEFT_JUSTIFY) == LEFT_JUSTIFY;
            StringBuilder buffer = new StringBuilder(width);
            int count = 0;
            for (int i = 0; i < length; i++) {
                final int index = leftJustify ? i : length - 1 - i;
                char c = chars[index];
                if (leftJustify) {
                    buffer.append(c);
                } else {
                    buffer.insert(0, c);
                }
                final int w = getWidth(c);
                if (++count >= width) {
                    if (w == 2) {
                        buffer.setCharAt(index, SPACE);
                    }
                    break;
                }
                if (w == 2) {
                    ++count;
                }
            }
            if (count < width) {
                char[] chars = new char[width - count];
                Arrays.fill(chars, SPACE);
                if (leftJustify) {
                    buffer.append(chars);
                } else {
                    buffer.insert(0, chars);
                }
            }
            formatter.format(buffer.toString());
        }

        private static int getWidth(char ch) {
            // U+0000 <= C <= U+00FF: half-width character
            // U+FF61 <= C <= U+FF9F: half-width katakana (Japanese)
            // otherwise            : full-width character
            return (ch > '\u00FF' && (ch < '\uFF61' || ch > '\uFF9F')) ? 2 : 1;
        }

    }

}
