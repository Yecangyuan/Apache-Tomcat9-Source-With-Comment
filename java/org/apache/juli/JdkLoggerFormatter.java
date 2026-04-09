package org.apache.juli;

import java.util.logging.Formatter;
import java.util.logging.LogRecord;

/**
 * 更紧凑的日志格式化器，类似于 log4j 的格式。
 *
 * <pre>
 *  log4j.rootCategory=WARN, A1
 *  log4j.appender.A1=org.apache.log4j.ConsoleAppender
 *  log4j.appender.A1.layout=org.apache.log4j.PatternLayout
 *  log4j.appender.A1.Target=System.err
 *  log4j.appender.A1.layout.ConversionPattern=%r %-15.15c{2} %-1.1p %m %n
 * </pre>
 *
 * 示例：1130122891846 Http11BaseProtocol I Initializing Coyote HTTP/1.1 on http-8800
 *
 * @author Costin Manolache
 */
public class JdkLoggerFormatter extends Formatter {

    // JDK 日志级别值
    /** 追踪级别（TRACE）- 400 */
    public static final int LOG_LEVEL_TRACE = 400;
    /** 调试级别（DEBUG）- 500 */
    public static final int LOG_LEVEL_DEBUG = 500;
    /** 信息级别（INFO）- 800 */
    public static final int LOG_LEVEL_INFO = 800;
    /** 警告级别（WARN）- 900 */
    public static final int LOG_LEVEL_WARN = 900;
    /** 错误级别（ERROR）- 1000 */
    public static final int LOG_LEVEL_ERROR = 1000;
    /** 致命级别（FATAL）- 1000 */
    public static final int LOG_LEVEL_FATAL = 1000;

    /**
     * 格式化日志记录，输出格式为：时间戳 + 日志级别 + 日志器名称 + 消息。
     * 如果包含异常，还会追加堆栈跟踪信息。
     *
     * @param record 要格式化的日志记录
     * @return 格式化后的字符串
     */
    @Override
    public String format(LogRecord record) {
        Throwable t = record.getThrown();
        int level = record.getLevel().intValue();
        String name = record.getLoggerName();
        long time = record.getMillis();
        String message = formatMessage(record);


        if (name.indexOf('.') >= 0) {
            name = name.substring(name.lastIndexOf('.') + 1);
        }

        // Use a string buffer for better performance
        StringBuilder buf = new StringBuilder();

        buf.append(time);

        // pad to 8 to make it more readable
        for (int i = 0; i < 8 - buf.length(); i++) {
            buf.append(' ');
        }

        // Append a readable representation of the log level.
        switch (level) {
            case LOG_LEVEL_TRACE:
                buf.append(" T ");
                break;
            case LOG_LEVEL_DEBUG:
                buf.append(" D ");
                break;
            case LOG_LEVEL_INFO:
                buf.append(" I ");
                break;
            case LOG_LEVEL_WARN:
                buf.append(" W ");
                break;
            case LOG_LEVEL_ERROR:
                buf.append(" E ");
                break;
            // case : buf.append(" F "); break;
            default:
                buf.append("   ");
        }


        // Append the name of the log instance if so configured
        buf.append(name);
        buf.append(' ');

        // pad to 20 chars
        for (int i = 0; i < 8 - buf.length(); i++) {
            buf.append(' ');
        }

        // Append the message
        buf.append(message);

        // Append stack trace if not null
        if (t != null) {
            buf.append(System.lineSeparator());

            java.io.StringWriter sw = new java.io.StringWriter(1024);
            java.io.PrintWriter pw = new java.io.PrintWriter(sw);
            t.printStackTrace(pw);
            pw.close();
            buf.append(sw.toString());
        }

        buf.append(System.lineSeparator());
        // Print to the appropriate destination
        return buf.toString();
    }
}
