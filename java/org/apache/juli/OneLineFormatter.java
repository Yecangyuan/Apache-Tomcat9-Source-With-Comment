package org.apache.juli;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Formatter;
import java.util.logging.LogManager;
import java.util.logging.LogRecord;

/**
 * 单行日志格式化器。提供与默认日志格式相同的信息，但输出在单行上，便于使用grep搜索日志。
 * 唯一的例外是堆栈跟踪，它们总是以空格开头，以便于跳过。
 */
/*
 * 日期处理基于AccessLogValve实现。
 */
public class OneLineFormatter extends Formatter {

    /** 未知线程名称 */
    private static final String UNKNOWN_THREAD_NAME = "Unknown thread with ID ";
    private static final Object threadMxBeanLock = new Object();
    /** 线程MXBean */
    private static volatile ThreadMXBean threadMxBean = null;
    private static final int THREAD_NAME_CACHE_SIZE = 10000;
    private static final ThreadLocal<ThreadNameCache> threadNameCache = ThreadLocal
            .withInitial(() -> new ThreadNameCache(THREAD_NAME_CACHE_SIZE));

    /** 默认时间格式 */
    private static final String DEFAULT_TIME_FORMAT = "dd-MMM-yyyy HH:mm:ss.SSS";

    /**
     * 全局缓存大小
     */
    private static final int globalCacheSize = 30;

    /**
     * 本地缓存大小
     */
    private static final int localCacheSize = 5;

    /**
     * 线程本地日期格式缓存。
     */
    private ThreadLocal<DateFormatCache> localDateCache;

    private volatile MillisHandling millisHandling = MillisHandling.APPEND;


    public OneLineFormatter() {
        String timeFormat = LogManager.getLogManager().getProperty(OneLineFormatter.class.getName() + ".timeFormat");
        if (timeFormat == null) {
            timeFormat = DEFAULT_TIME_FORMAT;
        }
        setTimeFormat(timeFormat);
    }


    /**
     * 设置时间格式。
     * 指定日志消息中时间戳使用的时间格式。
     *
     * @param timeFormat 使用 {@link java.text.SimpleDateFormat} 语法的格式
     */
    public void setTimeFormat(final String timeFormat) {
        final String cachedTimeFormat;

        if (timeFormat.endsWith(".SSS")) {
            cachedTimeFormat = timeFormat.substring(0, timeFormat.length() - 4);
            millisHandling = MillisHandling.APPEND;
        } else if (timeFormat.contains("SSS")) {
            millisHandling = MillisHandling.REPLACE_SSS;
            cachedTimeFormat = timeFormat;
        } else if (timeFormat.contains("SS")) {
            millisHandling = MillisHandling.REPLACE_SS;
            cachedTimeFormat = timeFormat;
        } else if (timeFormat.contains("S")) {
            millisHandling = MillisHandling.REPLACE_S;
            cachedTimeFormat = timeFormat;
        } else {
            millisHandling = MillisHandling.NONE;
            cachedTimeFormat = timeFormat;
        }

        final DateFormatCache globalDateCache = new DateFormatCache(globalCacheSize, cachedTimeFormat, null);
        localDateCache = ThreadLocal
                .withInitial(() -> new DateFormatCache(localCacheSize, cachedTimeFormat, globalDateCache));
    }


    /**
     * 获取时间格式。
     * 获取当前用于日志消息中时间戳的格式。
     *
     * @return 当前使用 {@link java.text.SimpleDateFormat} 语法的格式
     */
    public String getTimeFormat() {
        return localDateCache.get().getTimeFormat();
    }


    /**
     * 格式化日志记录。
     * 将日志记录格式化为单行字符串。
     */
    @Override
    public String format(LogRecord record) {
        StringBuilder sb = new StringBuilder();

        // Timestamp
        addTimestamp(sb, record.getMillis());

        // Severity
        sb.append(' ');
        sb.append(record.getLevel().getLocalizedName());

        // Thread
        sb.append(' ');
        sb.append('[');
        final String threadName = Thread.currentThread().getName();
        if (threadName != null && threadName.startsWith(AsyncFileHandler.THREAD_PREFIX)) {
            // If using the async handler can't get the thread name from the
            // current thread.
            sb.append(getThreadName(record.getThreadID()));
        } else {
            sb.append(threadName);
        }
        sb.append(']');

        // Source
        sb.append(' ');
        sb.append(record.getSourceClassName());
        sb.append('.');
        sb.append(record.getSourceMethodName());

        // Message
        sb.append(' ');
        sb.append(formatMessage(record));

        // New line for next record
        sb.append(System.lineSeparator());

        // Stack trace
        if (record.getThrown() != null) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new IndentingPrintWriter(sw);
            record.getThrown().printStackTrace(pw);
            pw.close();
            sb.append(sw.getBuffer());
        }

        return sb.toString();
    }

    /**
     * 添加时间戳。
     * 将格式化的时间戳添加到字符串构建器中。
     */
    protected void addTimestamp(StringBuilder buf, long timestamp) {
        String cachedTimeStamp = localDateCache.get().getFormat(timestamp);
        if (millisHandling == MillisHandling.NONE) {
            buf.append(cachedTimeStamp);
        } else if (millisHandling == MillisHandling.APPEND) {
            buf.append(cachedTimeStamp);
            long frac = timestamp % 1000;
            buf.append('.');
            if (frac < 100) {
                if (frac < 10) {
                    buf.append('0');
                    buf.append('0');
                } else {
                    buf.append('0');
                }
            }
            buf.append(frac);
        } else {
            // Some version of replace
            long frac = timestamp % 1000;
            // Formatted string may vary in length so the insert point may vary
            int insertStart = cachedTimeStamp.indexOf(DateFormatCache.MSEC_PATTERN);
            buf.append(cachedTimeStamp.subSequence(0, insertStart));
            if (frac < 100 && millisHandling == MillisHandling.REPLACE_SSS) {
                buf.append('0');
                if (frac < 10) {
                    buf.append('0');
                }
            } else if (frac < 10 && millisHandling == MillisHandling.REPLACE_SS) {
                buf.append('0');
            }
            buf.append(frac);
            if (millisHandling == MillisHandling.REPLACE_SSS) {
                buf.append(cachedTimeStamp.substring(insertStart + 3));
            } else if (millisHandling == MillisHandling.REPLACE_SS) {
                buf.append(cachedTimeStamp.substring(insertStart + 2));
            } else {
                buf.append(cachedTimeStamp.substring(insertStart + 1));
            }
        }
    }


    /**
     * 获取线程名称。
     * LogRecord有threadID但没有线程名称。LogRecord使用int表示线程ID但线程ID是long类型。
     * 如果真实线程ID > (Integer.MAXVALUE / 2)，LogRecord会使用自己的ID以避免溢出冲突。
     * <p>
     * 无法用言语形容我对在LogRecord中使用int表示long值的设计决策以及随之而来的混乱的看法。
     */
    private static String getThreadName(int logRecordThreadId) {
        Map<Integer, String> cache = threadNameCache.get();
        String result = cache.get(Integer.valueOf(logRecordThreadId));

        if (result != null) {
            return result;
        }

        if (logRecordThreadId > Integer.MAX_VALUE / 2) {
            result = UNKNOWN_THREAD_NAME + logRecordThreadId;
        } else {
            // Double checked locking OK as threadMxBean is volatile
            if (threadMxBean == null) {
                synchronized (threadMxBeanLock) {
                    if (threadMxBean == null) {
                        threadMxBean = ManagementFactory.getThreadMXBean();
                    }
                }
            }
            ThreadInfo threadInfo = threadMxBean.getThreadInfo(logRecordThreadId);
            if (threadInfo == null) {
                return Long.toString(logRecordThreadId);
            }
            result = threadInfo.getThreadName();
        }

        cache.put(Integer.valueOf(logRecordThreadId), result);

        return result;
    }


    /*
     * 线程名称缓存。这是一个LRU缓存。
     */
    private static class ThreadNameCache extends LinkedHashMap<Integer, String> {

        private static final long serialVersionUID = 1L;

        private final int cacheSize;

        ThreadNameCache(int cacheSize) {
            super(cacheSize, 0.75f, true);
            this.cacheSize = cacheSize;
        }

        @Override
        protected boolean removeEldestEntry(Map.Entry<Integer, String> eldest) {
            return (size() > cacheSize);
        }
    }


    /*
     * 缩进打印写入器。用于缩进打印堆栈跟踪的最小实现。
     */
    private static class IndentingPrintWriter extends PrintWriter {

        IndentingPrintWriter(Writer out) {
            super(out);
        }

        @Override
        public void println(Object x) {
            super.print('\t');
            super.println(x);
        }
    }


    /** 毫秒处理方式。定义时间戳中毫秒部分的处理方式。 */
    private enum MillisHandling {
        NONE, APPEND, REPLACE_S, REPLACE_SS, REPLACE_SSS,
    }
}
