package stew6.sql;

import java.util.*;
import java.util.stream.*;

public final class Clause implements Element {

    private final Element leftElement;
    private final Operator op;
    private final Element rightElement;

    private Clause(Element left, Operator op, Element right) {
        this.leftElement = left;
        this.op = op;
        this.rightElement = right;
    }

    public static Clause implicit(Column column) {
        return new Clause(column, Operator.EQUAL, Value.asPlaceHolder());
    }

    public static Clause of(Element left, Operator op, Element right) {
        return new Clause(left, op, right);
    }

    public Element getLeftElement() {
        return leftElement;
    }

    public Operator getOp() {
        return op;
    }

    public Element getRightElement() {
        return rightElement;
    }

    public List<Element> asList() {
        return Arrays.asList(leftElement, op, rightElement);
    }

    public List<Column> extractColumns() {
        return asList().stream().filter(x -> x instanceof Column).map(x -> Column.class.cast(x))
                       .collect(Collectors.toList());
    }

    public Clause withoutTable() {
        Element l = (leftElement instanceof Column) ? ((Column)leftElement).withoutTable() : leftElement;
        Element r = (rightElement instanceof Column) ? ((Column)rightElement).withoutTable() : rightElement;
        return new Clause(l, op, r);
    }

    @Override
    public String toSql() {
        return Stream.of(leftElement, op, rightElement).map(Element::toSql).collect(Collectors.joining());
    }

    @Override
    public String toString() {
        return String.format("Clause(leftElement=%s, op=%s, rightElement=%s)", leftElement, op, rightElement);
    }

}
