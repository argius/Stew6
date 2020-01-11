package stew6;

import java.math.*;
import java.sql.*;
import java.text.*;
import java.util.Date;

/**
 * TypeConverter provides conversion for incompatible types.
 * This class is thread-safe.
 */
public final class TypeConverter {

    private final boolean optimistic;

    public TypeConverter() {
        this(false);
    }

    public TypeConverter(boolean optimistic) {
        this.optimistic = optimistic;
    }

    /**
     * Converts the type of object to the specified type.
     * @param o object
     * @param destType the desired type to convert
     * @return converted value
     */
    public Object convert(Object o, Class<?> destType) {
        if (o == null) {
            return o;
        }
        if (o instanceof CharSequence) {
            return convertFromString(o.toString(), destType);
        }
        if (o instanceof Number) {
            @SuppressWarnings("unchecked")
            Class<? extends Number> numberType = (Class<? extends Number>)destType;
            return convertNumber((Number)o, numberType);
        }
        if (Date.class.isInstance(o)) {
            Date d = (Date)o;
            if (destType == java.sql.Date.class) {
                return new java.sql.Date(d.getTime());
            }
            if (destType == Time.class) {
                return new Time(d.getTime());
            }
            if (destType == Timestamp.class) {
                return new Timestamp(d.getTime());
            }
        }
        if (optimistic) {
            return o;
        }
        throw new IllegalArgumentException("cannot convert from " + o.getClass() + " to " + destType);
    }

    /**
     * Converts from string to the type of specified object.
     * @param s string
     * @param destType the desired type to convert
     * @return converted value
     */
    public Object convertFromString(String s, Class<?> destType) {
        if (destType == String.class) {
            return s;
        }
        if (destType == int.class || destType == Integer.class) {
            return Integer.valueOf(s);
        }
        if (destType == long.class || destType == Long.class) {
            return Long.valueOf(s);
        }
        if (destType == short.class || destType == Short.class) {
            return Short.valueOf(s);
        }
        if (destType == byte.class || destType == Byte.class) {
            return Byte.valueOf(s);
        }
        if (destType == float.class || destType == Float.class) {
            return Float.valueOf(s);
        }
        if (destType == double.class || destType == Double.class) {
            return Double.valueOf(s);
        }
        if (destType == boolean.class || destType == Boolean.class) {
            return Boolean.valueOf(s);
        }
        if (destType == BigInteger.class) {
            return new BigInteger(s);
        }
        if (destType == BigDecimal.class) {
            return new BigDecimal(s);
        }
        try {
            if (destType == java.sql.Date.class) {
                return new java.sql.Date(convertToDate(s, detectDateFormat(s)).getTime());
            }
            if (destType == Time.class) {
                return new Time(convertToDate(s, detectTimeFormat(s)).getTime());
            }
            if (destType == Timestamp.class) {
                return new Timestamp(convertToDate(s, detectDatetimeFormat(s)).getTime());
            }
        } catch (ParseException ex) {
            if (optimistic) {
                return s;
            } else {
                throw new IllegalArgumentException(ex);
            }
        }
        if (optimistic) {
            return s;
        }
        throw new IllegalArgumentException("cannot convert from String to " + destType);
    }

    /**
     * Converts the type of specified Number object.
     * @param n object of Number
     * @param destType the desired type to convert
     * @return converted value
     */
    public Number convertNumber(Number n, Class<? extends Number> destType) {
        Class<? extends Number> srcType = n.getClass();
        if (srcType == destType) {
            return n;
        }
        if (destType == int.class || destType == Integer.class) {
            return n.intValue();
        }
        if (destType == long.class || destType == Long.class) {
            return n.longValue();
        }
        if (destType == short.class || destType == Short.class) {
            return n.shortValue();
        }
        if (destType == byte.class || destType == Byte.class) {
            return n.byteValue();
        }
        if (destType == float.class || destType == Float.class) {
            return n.floatValue();
        }
        if (destType == double.class || destType == Double.class) {
            return n.doubleValue();
        }
        if (destType == BigInteger.class) {
            if (srcType == BigDecimal.class) {
                BigDecimal bd = (BigDecimal)n;
                return optimistic ? bd.toBigInteger() : bd.toBigIntegerExact();
            }
            if (optimistic) {
                return BigInteger.valueOf(n.longValue());
            }
        }
        if (destType == BigDecimal.class) {
            if (srcType == BigInteger.class) {
                return new BigDecimal((BigInteger)n, 0);
            }
            return BigDecimal.valueOf(n.doubleValue());
        }
        if (optimistic) {
            return n;
        }
        throw new IllegalArgumentException("cannot convert from " + srcType + " to " + destType);
    }

    public static Date convertToDate(String dateString, String dateFormatString) throws ParseException {
        DateFormat fmt = new SimpleDateFormat(dateFormatString);
        return fmt.parse(dateString);
    }

    public String detectDatetimeFormat(String datetimeString) {
        final int index1 = datetimeString.indexOf(' ');
        final int index = (index1 >= 0) ? index1 : datetimeString.indexOf('T');
        if (index >= 0) {
            final char separator = datetimeString.charAt(index);
            return detectDateFormat(datetimeString.substring(0, index))
                   + separator
                   + detectTimeFormat(datetimeString.substring(index + 1));
        } else if (datetimeString.contains("/") || datetimeString.contains("-")) {
            return detectDateFormat(datetimeString);
        } else if (datetimeString.contains(":")) {
            return detectTimeFormat(datetimeString);
        }
        return optimistic ? detectDateFormat("") + ' ' + detectTimeFormat("") : "";
    }

    public String detectDateFormat(String dateString) {
        if (dateString.matches("\\d{4}-\\d{2}-\\d{2}")) {
            return "yyyy-MM-dd";
        }
        if (dateString.matches("[23]\\d-[01]\\d-\\d{4}")) {
            return "dd-MM-yyyy";
        }
        if (dateString.matches("\\d{2}-\\d{2}-\\d{4}")) {
            return "MM-dd-yyyy";
        }
        if (dateString.matches("\\d{4}/\\d{2}/\\d{2}")) {
            return "yyyy/MM/dd";
        }
        if (dateString.matches("[23]\\d/[01]\\d/\\d{4}")) {
            return "dd/MM/yyyy";
        }
        if (dateString.matches("\\d{2}/\\d{2}/\\d{4}")) {
            return "MM/dd/yyyy";
        }
        if (dateString.matches("\\d{2}\\d{2}\\d{4}")) {
            return "yyyyMMdd";
        }
        return optimistic ? "yyyy-MM-dd" : "";
    }

    public String detectTimeFormat(String timeString) {
        final String pattern = "HH:mm:ss.SSS";
        if (timeString.matches("\\d{2}:\\d{2}:\\d{2}\\.\\d{1,3}")
            || timeString.matches("\\d{2}:\\d{2}:\\d{2}")
            || timeString.matches("\\d{2}:\\d{2}")) {
            return pattern.substring(0, timeString.length());
        }
        if (timeString.matches("\\d{6}")) {
            return "HHmmss";
        }
        if (timeString.matches("\\d{4}")) {
            return "HHmm";
        }
        return optimistic ? pattern : "";
    }

    public Object convertWithoutException(Object o, Class<?> destType) {
        try {
            return convert(o, destType);
        } catch (Exception ex) {
            return o;
        }
    }

}
