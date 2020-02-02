package stew6.sql;

import java.util.*;

public final class Sql {

    public static String joinElements(Element... elements) {
        if (elements.length == 0) {
            return "";
        }
        Queue<Element> q = new LinkedList<>();
        Collections.addAll(q, elements);
        return joinElements(q);
    }

    public static String joinElements(List<? extends Element> elements) {
        if (elements.isEmpty()) {
            return "";
        }
        Queue<Element> q = new LinkedList<>(elements);
        return joinElements(q);
    }

    static String joinElements(Queue<? extends Element> elements) {
        if (elements.isEmpty()) {
            return "";
        }
        LinkedList<Element> q = new LinkedList<>(elements);
        Element prev = q.poll();
        StringBuilder sb = new StringBuilder();
        sb.append(prev.toSql());
        for (Element o : q) {
            Class<?> c = o.getClass();
            // before
            if (prev != Operator.LPAREN && o != Operator.RPAREN) {
                if (c.equals(prev.getClass()) && !c.equals(Keyword.class)) {
                    sb.append(",");
                }
                sb.append(" ");
            }
            // main
            sb.append(o.toSql());
            // after
            prev = o;
        }
        return sb.toString();
    }

    public static List<Value> generatePlaceHolders(int count) {
        return Collections.nCopies(count, Value.asPlaceHolder());
    }

    static String quoteIfNeeded(String rawString) {
        if (shouldQuote(rawString)) {
            return "`" + rawString + "`";
        }
        return rawString;
    }

    static boolean shouldQuote(String s) {
        if (s.matches("(?i)table|.*-.*")) {
            return true;
        }
        return false;
    }

    public static final class Builder {

        private Queue<Element> elements;

        public Builder() {
            this.elements = new LinkedList<>();
        }

        public String build() {
            return Sql.joinElements(elements);
        }

        Builder add(Element element) {
            elements.add(element);
            return this;
        }

        Builder add(Element... elements) {
            Collections.addAll(this.elements, elements);
            return this;
        }

        Builder add(List<? extends Element> elements) {
            this.elements.addAll(elements);
            return this;
        }

        Builder joinAndAdd(Element conjunction, List<Clause> clauses) {
            if (clauses.isEmpty()) {
                return this;
            }
            if (clauses.size() == 1) {
                add(clauses);
                return this;
            }
            boolean isNotFirst = false;
            for (Clause clause : clauses) {
                if (isNotFirst) {
                    add(conjunction);
                } else {
                    isNotFirst = true;
                }
                add(clause);
            }
            return this;
        }

        public Queue<? extends Element> getElements() {
            return elements;
        }

    }

}
