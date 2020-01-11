package stew6;

import java.io.*;
import java.text.*;
import java.util.logging.*;

public final class LoggerFormatter extends Formatter {

    private static final String format;
    private static final String defaultFormat = "%1$tF %1$tT [%2$-6s] %3$s %4$s: %5$s %n";

    static {
        String p = LogManager.getLogManager().getProperty(LoggerFormatter.class.getName() + ".format");
        if (p == null) {
            p = defaultFormat;
        }
        format = p;
    }

    @Override
    public String format(LogRecord record) {
        String stackTraceString;
        Throwable th = record.getThrown();
        if (th == null) {
            stackTraceString = "";
        } else {
            StringWriter sw = new StringWriter();
            PrintWriter out = new PrintWriter(sw);
            out.println();
            record.getThrown().printStackTrace(out);
            out.close();
            stackTraceString = sw.toString();
        }
        final String msg = MessageFormat.format(record.getMessage(), record.getParameters()) + stackTraceString;
        return String.format(format,
                             record.getMillis(),
                             record.getLevel().getName(),
                             record.getSourceClassName(),
                             record.getSourceMethodName(),
                             msg);
    }

}
