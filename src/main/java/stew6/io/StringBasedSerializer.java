package stew6.io;

import java.io.*;
import java.math.*;
import java.util.*;

/**
 * A serializer with String.
 */
public final class StringBasedSerializer {

    private StringBasedSerializer() {
        // empty
    }

    /**
     * Serializes an object.
     * @param object
     * @return
     * @throws IOException
     */
    public static Element serialize(Object object) throws IOException {
        String name;
        String value;
        if (object == null) {
            name = Element.NULL;
            value = null;
        } else if (object instanceof String) {
            name = Element.STRING;
            value = (String)object;
        } else if (object instanceof Boolean) {
            name = Element.BOOLEAN;
            value = object.toString();
        } else if (object instanceof Byte) {
            name = Element.BYTE;
            value = object.toString();
        } else if (object instanceof Short) {
            name = Element.SHORT;
            value = object.toString();
        } else if (object instanceof Integer) {
            name = Element.INT;
            value = object.toString();
        } else if (object instanceof Long) {
            name = Element.LONG;
            value = object.toString();
        } else if (object instanceof Float) {
            name = Element.FLOAT;
            value = object.toString();
        } else if (object instanceof Double) {
            name = Element.DOUBLE;
            value = object.toString();
        } else if (object instanceof BigDecimal) {
            name = Element.DECIMAL;
            value = object.toString();
        } else if (object instanceof Date) {
            name = Element.TIME;
            Date date = (Date)object;
            value = Long.toString(date.getTime());
        } else {
            name = Element.OBJECT;
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            try (ObjectOutputStream oos = new ObjectOutputStream(bos)) {
                oos.writeObject(object);
                value = toHexString(bos.toByteArray());
            }
        }
        return new Element(name, value);
    }

    /**
     * Deserializes an object.
     * @param element
     * @return
     * @throws IOException
     */
    public static Object deserialize(Element element) throws IOException {
        return deserialize(element.getType(), element.getValue());
    }

    /**
     * Deserializes an object.
     * @param name
     * @param value
     * @return
     * @throws IOException
     */
    public static Object deserialize(String name, String value) throws IOException {
        Object o;
        if (name.equals(Element.NULL)) {
            o = null;
        } else if (name.equals(Element.STRING)) {
            o = value;
        } else if (name.equals(Element.BOOLEAN)) {
            o = Boolean.valueOf(value);
        } else if (name.equals(Element.BYTE)) {
            o = Byte.valueOf(value);
        } else if (name.equals(Element.SHORT)) {
            o = Short.valueOf(value);
        } else if (name.equals(Element.INT)) {
            o = Integer.valueOf(value);
        } else if (name.equals(Element.LONG)) {
            o = Long.valueOf(value);
        } else if (name.equals(Element.FLOAT)) {
            o = Float.valueOf(value);
        } else if (name.equals(Element.DOUBLE)) {
            o = Double.valueOf(value);
        } else if (name.equals(Element.DECIMAL)) {
            o = new BigDecimal(value);
        } else if (name.equals(Element.TIME)) {
            o = new Date(Long.parseLong(value));
        } else if (name.equals(Element.OBJECT)) {
            ByteArrayInputStream bis = new ByteArrayInputStream(toBytes(value));
            try (ObjectInputStream ois = new ObjectInputStream(bis)) {
                o = ois.readObject();
            } catch (ClassNotFoundException ex) {
                IOException ioe = new IOException(ex.getMessage());
                ioe.initCause(ex);
                throw ioe;
            }
        } else {
            throw new IOException("unknown element : " + name);
        }
        return o;
    }

    private static String toHexString(byte[] bytes) {
        StringBuilder buffer = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            buffer.append(String.format("%02X", b & 0xFF));
        }
        return buffer.toString();
    }

    private static byte[] toBytes(String hexString) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        for (int i = 0; i < hexString.length(); i += 2) {
            String s = hexString.substring(i, i + 2);
            bos.write(Integer.parseInt(s, 16));
        }
        return bos.toByteArray();
    }

    /**
     * Element (type).
     */
    public static final class Element {

        /**
         * <code>NULL</code>
         */
        public static final String NULL = "null";
        /**
         * <code>STRING</code>
         */
        public static final String STRING = "string";
        /**
         * <code>BOOLEAN</code>
         */
        public static final String BOOLEAN = "boolean";
        /**
         * <code>BYTE</code>
         */
        public static final String BYTE = "byte";
        /**
         * <code>SHORT</code>
         */
        public static final String SHORT = "short";
        /**
         * <code>INT</code>
         */
        public static final String INT = "int";
        /**
         * <code>LONG</code>
         */
        public static final String LONG = "long";
        /**
         * <code>FLOAT</code>
         */
        public static final String FLOAT = "float";
        /**
         * <code>DOUBLE</code>
         */
        public static final String DOUBLE = "double";
        /**
         * <code>DECIMAL</code>
         */
        public static final String DECIMAL = "decimal";
        /**
         * <code>TIME</code>
         */
        public static final String TIME = "time";
        /**
         * <code>OBJECT</code>
         */
        public static final String OBJECT = "object";

        private final String type;
        private final String value;

        Element(String type, String value) {
            this.type = (type == null) ? NULL : type;
            this.value = value;
        }

        /**
         * Returns this type.
         * @return
         */
        public String getType() {
            return type;
        }

        /**
         * Returns this value.
         * @return
         */
        public String getValue() {
            return value;
        }

        /**
         * Tests whether this element is null.
         * @return
         */
        public boolean isNull() {
            return (type == NULL || value == null);
        }

        @Override
        public String toString() {
            return type + ":" + value;
        }

    }

}
