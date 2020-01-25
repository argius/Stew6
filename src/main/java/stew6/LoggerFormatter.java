package stew6;

import java.text.*;
import java.util.logging.*;
import org.apache.commons.lang3.exception.*;

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
        Throwable th = record.getThrown();
        final String sts = (th == null) ? "" : ExceptionUtils.getStackTrace(th);
        final String msg = MessageFormat.format(record.getMessage(), record.getParameters()) + sts;
        return String.format(format, record.getMillis(), record.getLevel().getName(), record.getSourceClassName(),
                             record.getSourceMethodName(), msg);
    }

}
