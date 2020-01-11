package stew6;

import java.util.*;

/**
 * A (list of) column order.
 */
public final class ColumnOrder {

    private final List<Entry> list;

    /**
     * A constructor.
     */
    public ColumnOrder() {
        this.list = new ArrayList<>();
    }

    /**
     * Adds an order.
     * @param order
     */
    public void addOrder(int order) {
        addOrder(order, "");
    }

    /**
     * Adds an order with a name.
     * @param order
     * @param name
     */
    public void addOrder(int order, String name) {
        Entry entry = new Entry(order, name);
        list.add(entry);
    }

    /**
     * Returns the size of this column order's list.
     * @return size
     */
    public int size() {
        return list.size();
    }

    /**
     * Returns the number of the order at the specified index.
     * @param index
     * @return the order
     */
    public int getOrder(int index) {
        return list.get(index).order;
    }

    /**
     * Returns the name of the order at the specified index.
     * @param index
     * @return the name of the order
     */
    public String getName(int index) {
        return list.get(index).name;
    }

    /**
     * An order entry.
     */
    private static final class Entry {

        int order;
        String name;

        Entry(int order, String name) {
            this.order = order;
            this.name = name;
        }

    }

}
