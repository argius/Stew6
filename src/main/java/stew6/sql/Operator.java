package stew6.sql;

public enum Operator implements Element {

    COMMA(","), EQUAL("="), LT("<"), LE("<="), GT(">"), GE(">="), LPAREN("("), RPAREN(")");

    private String s;

    private Operator(String s) {
        this.s = s;
    }

    @Override
    public String toSql() {
        return s;
    }

}
