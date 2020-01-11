package stew6;

import static org.junit.Assert.*;
import java.math.*;
import java.sql.*;
import java.text.*;
import java.util.*;
import java.util.Date;
import org.junit.*;
import org.junit.rules.*;

public final class TypeConverterTest {

    private static final double DOUBLE_DELTA = 0.0001d;

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    private TypeConverter optim = new TypeConverter(true);
    private TypeConverter pesim = new TypeConverter();

    @Test
    public void testConvert() {
        assertNull(optim.convert(null, Integer.class));
        assertEquals(Integer.valueOf(17), optim.convert(sb("17"), Integer.class));
        assertEquals(Long.valueOf(53), optim.convert(sb(53), Long.class));
        assertEquals(Long.valueOf(67), optim.convert(Integer.valueOf(67), Long.class));
        assertEquals(timestamp("2017051202"), optim.convert(date("2017051202"), Timestamp.class));
        assertEquals(timestamp("20170513"), optim.convert(sb("2017-05-13"), Timestamp.class));
        assertEquals(sqldate("20170526"), optim.convert(date("20170526"), java.sql.Date.class));
        assertEquals(sqltime("125432").toString(), optim.convert(timestamp("20170526125432"), Time.class).toString());
        // dummy date
        class Date0 extends Date { // empty
        }
        Date date0test = date("20170601");
        assertSame(date0test, optim.convert(date0test, Date0.class));
    }

    @Test
    public void testConvertFromString() {
        assertSame(17, optim.convertFromString("17", int.class));
        assertSame(17, optim.convertFromString("17", Integer.class));
        assertSame(53L, optim.convertFromString("53", long.class));
        assertSame(53L, optim.convertFromString("53", Long.class));
        assertSame((short)31, optim.convertFromString("31", short.class));
        assertSame((short)31, optim.convertFromString("31", Short.class));
        assertSame((byte)66, optim.convertFromString("66", byte.class));
        assertSame((byte)66, optim.convertFromString("66", Byte.class));
        assertEquals(1.35f, optim.convertFromString("1.35", float.class));
        assertEquals(1.35f, optim.convertFromString("1.35", Float.class));
        assertEquals(9.61d, optim.convertFromString("9.61", double.class));
        assertEquals(9.61d, optim.convertFromString("9.61", Double.class));
        assertSame("aabbcc", optim.convertFromString("aabbcc", String.class));
        assertEquals(Boolean.TRUE, optim.convert("True", boolean.class));
        assertEquals(Boolean.TRUE, optim.convert("TRUE", Boolean.class));
        assertEquals(BigInteger.valueOf(112233445566L), optim.convertFromString("112233445566", BigInteger.class));
        assertEquals(new BigDecimal("9.876"), optim.convertFromString("9.876", BigDecimal.class));
        assertEquals(date("20120317"), optim.convertFromString("2012/03/17", java.sql.Date.class));
        assertEquals("15:33:42", optim.convertFromString("15:33:42", Time.class).toString());
        assertSame("XYZ", optim.convertFromString("XYZ", Timestamp.class));
        assertSame("&0A", optim.convertFromString("&0A", TypeConverter.class));
    }

    @Test
    public void testConvertNumber() {
        assertSame(13, optim.convertNumber(13, Integer.class));
        assertSame(13, optim.convertNumber(13L, int.class));
        assertSame(13, optim.convertNumber(13L, Integer.class));
        assertSame(17L, optim.convertNumber(17, long.class));
        assertSame(17L, optim.convertNumber(17, Long.class));
        assertSame((short)19, optim.convertNumber(19, short.class));
        assertSame((short)19, optim.convertNumber(19, Short.class));
        assertSame((byte)47, optim.convertNumber(47, byte.class));
        assertSame((byte)49, optim.convertNumber(49, Byte.class));
        assertEquals(3.7f, (float)optim.convertNumber(3.7f, float.class), DOUBLE_DELTA);
        assertEquals(7.9f, (float)optim.convertNumber(7.9f, Float.class), DOUBLE_DELTA);
        assertEquals(13.9d, (double)optim.convertNumber(13.9d, double.class), DOUBLE_DELTA);
        assertEquals(BigInteger.valueOf(9L), optim.convertNumber(BigDecimal.valueOf(9.8d), BigInteger.class));
        assertEquals(BigInteger.valueOf(9L), pesim.convertNumber(BigDecimal.valueOf(9.0d), BigInteger.class));
        assertEquals(BigInteger.valueOf(135L), optim.convertNumber(135L, BigInteger.class));
        assertEquals(BigInteger.valueOf(135L), pesim.convertNumber(BigDecimal.valueOf(135L), BigInteger.class));
        assertEquals(BigDecimal.valueOf(255L), optim.convertNumber(BigInteger.valueOf(255L), BigDecimal.class));
        assertEquals(BigDecimal.valueOf(255L), pesim.convertNumber(BigInteger.valueOf(255L), BigDecimal.class));
        assertEquals(17.3d, optim.convertNumber(17.3f, BigDecimal.class).doubleValue(), DOUBLE_DELTA);
        assertEquals(83.5d, pesim.convertNumber(83.5f, BigDecimal.class).doubleValue(), DOUBLE_DELTA);
    }

    @Test
    public void testDetectDatetimeFormat() {
        assertEquals("yyyy-MM-dd HH:mm:ss.SSS", optim.detectDatetimeFormat(""));
        assertEquals("", pesim.detectDatetimeFormat(""));
        assertEquals("yyyy/MM/dd HH:mm:ss", optim.detectDatetimeFormat("2017/12/31 12:31:55"));
        assertEquals("yyyy/MM/dd", optim.detectDatetimeFormat("2017/12/31"));
        assertEquals("yyyy-MM-dd", optim.detectDatetimeFormat("2017-12-31"));
        assertEquals("HH:mm:ss", optim.detectDatetimeFormat("10:53:27"));
    }

    @Test
    public void testDetectDateFormat() {
        assertEquals("yyyy-MM-dd", optim.detectDateFormat(""));
        assertEquals("", pesim.detectDateFormat(""));
        assertEquals("yyyy-MM-dd", optim.detectDateFormat("2017-12-31"));
        assertEquals("dd-MM-yyyy", optim.detectDateFormat("31-12-2017"));
        assertEquals("MM-dd-yyyy", optim.detectDateFormat("12-31-2017"));
        assertEquals("yyyy/MM/dd", optim.detectDateFormat("2016/10/27"));
        assertEquals("dd/MM/yyyy", optim.detectDateFormat("27/10/2016"));
        assertEquals("MM/dd/yyyy", optim.detectDateFormat("10/27/2016"));
        assertEquals("yyyyMMdd", optim.detectDateFormat("20161027"));
    }

    @Test
    public void testDetectTimeFormat() {
        assertEquals("HH:mm:ss.SSS", optim.detectTimeFormat(""));
        assertEquals("", pesim.detectTimeFormat(""));
        assertEquals("HH:mm:ss.SSS", optim.detectTimeFormat("12:34:56.789"));
        assertEquals("HH:mm:ss.SS", optim.detectTimeFormat("12:34:56.01"));
        assertEquals("HH:mm:ss.S", optim.detectTimeFormat("12:34:56.0"));
        assertEquals("HH:mm:ss", optim.detectTimeFormat("12:34:56"));
        assertEquals("HH:mm", optim.detectTimeFormat("12:34"));
        assertEquals("HHmmss", optim.detectTimeFormat("123456"));
        assertEquals("HHmm", optim.detectTimeFormat("1234"));
        assertEquals("HH:mm:ss.SSS", optim.detectTimeFormat("23:59:59.999"));
        assertEquals("HH:mm:ss", optim.detectTimeFormat("03:33:33"));
    }

    @Test
    public void testConvertWithoutException() {
        Object o1 = new Object();
        assertSame(o1, optim.convertWithoutException(o1, Integer.class));
        assertSame(o1, pesim.convertWithoutException(o1, Integer.class));
        assertSame(12, optim.convertWithoutException("12", Integer.class));
        assertSame(12, pesim.convertWithoutException("12", Integer.class));
    }

    @Test
    public void testConvertThrowsIllegalArgumentException() {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("cannot convert from ");
        pesim.convert(new Object(), Integer.class);
    }

    @Test
    public void testConvertNumberThrowsArithmeticException() {
        thrown.expect(ArithmeticException.class);
        thrown.expectMessage("Rounding necessary");
        assertEquals(BigInteger.valueOf(9L), pesim.convertNumber(BigDecimal.valueOf(9.8d), BigInteger.class));
    }

    static Date date(String s) {
        DateFormat fmt = new SimpleDateFormat("yyyyMMddHHmmss".substring(0, s.length()));
        try {
            return fmt.parse(s);
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
    }

    static Date sqldate(String s) {
        return new java.sql.Date(date(s).getTime());
    }

    static Date sqltime(String s) {
        DateFormat fmt = new SimpleDateFormat("HHmmss".substring(0, s.length()));
        try {
            Date d = fmt.parse(s);
            Calendar c = Calendar.getInstance();
            c.setTime(d);
            c.clear(Calendar.MILLISECOND);
            return new Time(c.getTimeInMillis());
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
    }

    static Timestamp timestamp(String s) {
        return new Timestamp(date(s).getTime());
    }

    static StringBuilder sb(Object o) {
        return new StringBuilder(String.valueOf(o));
    }

}
