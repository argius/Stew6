package stew6.ui.swing;

import java.awt.*;
import java.util.regex.*;
import javax.swing.text.*;
import javax.swing.text.Highlighter.*;

/**
 * The interface for text search.
 */
public interface TextSearch {

    /**
     * Searches a text.
     * @param matcher
     * @return true if matched, otherwise false
     */
    boolean search(Matcher matcher);

    /**
     * Resets searching.
     */
    void reset();

    /**
     * The Matcher for TextSearch.
     */
    public final class Matcher {

        private Pattern pattern;
        private String string;
        private boolean useRegularExpression;
        private boolean ignoreCase;
        private boolean backward;
        private boolean continuously;
        private int start;
        private int end;

        /**
         * A constructor.
         * @param target
         * @param useRegularExpression
         * @param ignoreCase
         */
        public Matcher(String target, boolean useRegularExpression, boolean ignoreCase) {
            if (useRegularExpression) {
                int option = (ignoreCase) ? Pattern.CASE_INSENSITIVE : 0;
                this.pattern = Pattern.compile(target, option);
                this.string = target;
            } else if (ignoreCase) {
                this.string = target.toUpperCase();
            } else {
                this.string = target;
            }
            this.useRegularExpression = useRegularExpression;
            this.ignoreCase = ignoreCase;
        }

        /**
         * Returns HighlightPainter.
         * @return
         */
        public static HighlightPainter getHighlightPainter() {
            return new DefaultHighlighter.DefaultHighlightPainter(Color.decode("#33dd66"));
        }

        /**
         * Returns backword or not.
         * @return
         */
        public boolean isBackward() {
            return backward;
        }

        /**
         * Sets backword or not.
         * @param backward
         */
        public void setBackward(boolean backward) {
            this.backward = backward;
        }

        /**
         * Returns continuously or not.
         * @return
         */
        public boolean isContinuously() {
            return continuously;
        }

        /**
         * Sets continuously or not.
         * @param continuously
         */
        public void setContinuously(boolean continuously) {
            this.continuously = continuously;
        }

        /**
         * Finds the text.
         * @param value
         * @return true if text was found, otherwise false
         */
        public boolean find(String value) {
            return find(value, 0);
        }

        /**
         * Finds the text.
         * @param value
         * @param start
         * @return true if text was found, otherwise false
         */
        public boolean find(String value, int start) {
            this.start = -1;
            this.end = -1;
            boolean found;
            if (useRegularExpression) {
                java.util.regex.Matcher m = pattern.matcher(value);
                found = m.find(start);
                if (found) {
                    this.start = m.start();
                    this.end = m.end();
                }
            } else {
                String v = (ignoreCase) ? value.toUpperCase() : value;
                int index = v.indexOf(string, start);
                found = index >= start;
                if (found) {
                    this.start = index;
                    this.end = index + string.length();
                }
            }
            return found;
        }

        /**
         * Returns this start position.
         * @return
         */
        public int getStart() {
            return start;
        }

        /**
         * Returns this end position.
         * @return
         */
        public int getEnd() {
            return end;
        }

    }

}
