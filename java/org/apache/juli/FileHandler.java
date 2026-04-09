package org.apache.juli;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.sql.Timestamp;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.ErrorManager;
import java.util.logging.Filter;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.LogRecord;
import java.util.regex.Pattern;

/**
 * Handler 的实现类，将日志消息追加到文件中，文件名为 {prefix}{date}{suffix} 格式，存储在配置的目录中。
 * 支持按日期轮转日志文件。
 * <p>
 * 可用配置属性如下：
 * </p>
 * <ul>
 * <li><code>directory</code> - 创建日志文件的目录。如果路径不是绝对路径，则相对于应用程序的当前工作目录。
 *     Apache Tomcat 配置文件通常为此属性指定绝对路径 <code>${catalina.base}/logs</code>。
 *     默认值：<code>logs</code></li>
 * <li><code>rotatable</code> - 如果为 <code>true</code>，日志文件将在午夜后首次写入时轮转，
 *     文件名为 <code>{prefix}{date}{suffix}</code>，其中 date 为 yyyy-MM-dd 格式。
 *     如果为 <code>false</code>，文件不会轮转，文件名为 <code>{prefix}{suffix}</code>。
 *     默认值：<code>true</code></li>
 * <li><code>prefix</code> - 日志文件名的前缀部分。默认值：<code>juli.</code></li>
 * <li><code>suffix</code> - 日志文件名的后缀部分。默认值：<code>.log</code></li>
 * <li><code>bufferSize</code> - 配置缓冲。<code>0</code> 使用系统默认缓冲（通常使用 8K 缓冲区）。
 *     <code>&lt;0</code> 强制每次日志写入都刷新写入器。<code>&gt;0</code> 使用指定大小的 BufferedOutputStream，
 *     但注意系统默认缓冲也会应用。默认值：<code>-1</code></li>
 * <li><code>encoding</code> - 日志文件使用的字符集。默认值：空字符串，表示使用系统默认字符集。</li>
 * <li><code>level</code> - 此 Handler 的级别阈值。参见 <code>java.util.logging.Level</code> 类了解可能的级别。
 *     默认值：<code>ALL</code></li>
 * <li><code>filter</code> - 此 Handler 的 <code>java.util.logging.Filter</code> 实现类名。
 *     默认值：未设置</li>
 * <li><code>formatter</code> - 此 Handler 的 <code>java.util.logging.Formatter</code> 实现类名。
 *     默认值：<code>org.apache.juli.OneLineFormatter</code></li>
 * <li><code>maxDays</code> - 保留日志文件的最大天数。如果指定值 <code>&lt;=0</code>，
 *     则日志文件将永久保留在文件系统上，否则将保留指定的最大天数。
 *     默认值：<code>-1</code>。</li>
 * </ul>
 */
public class FileHandler extends Handler {


    public static final int DEFAULT_MAX_DAYS = -1;
    public static final int DEFAULT_BUFFER_SIZE = -1;


    private static final ExecutorService DELETE_FILES_SERVICE = Executors
            .newSingleThreadExecutor(new ThreadFactory("FileHandlerLogFilesCleaner-"));

    // ------------------------------------------------------------ Constructor


    public FileHandler() {
        this(null, null, null);
    }


    public FileHandler(String directory, String prefix, String suffix) {
        this(directory, prefix, suffix, null);
    }


    public FileHandler(String directory, String prefix, String suffix, Integer maxDays) {
        this(directory, prefix, suffix, maxDays, null, null);
    }


    public FileHandler(String directory, String prefix, String suffix, Integer maxDays, Boolean rotatable,
            Integer bufferSize) {
        this.directory = directory;
        this.prefix = prefix;
        this.suffix = suffix;
        this.maxDays = maxDays;
        this.rotatable = rotatable;
        this.bufferSize = bufferSize;
        configure();
        openWriter();
        clean();
    }


    // ----------------------------------------------------- Instance Variables


    /**
     * 当前日志文件的日期，如果没有打开的日志文件则为空字符串。
     */
    private volatile String date = "";


    /**
     * 创建日志文件的目录。
     */
    private String directory;


    /**
     * 添加到日志文件名的前缀。
     */
    private String prefix;


    /**
     * 添加到日志文件名的后缀。
     */
    private String suffix;


    /**
     * 决定是否轮转日志文件。
     */
    private Boolean rotatable;


    /**
     * 保留日志文件的最大天数。
     */
    private Integer maxDays;


    /**
     * 当前日志写入的输出写入器（如果有）。
     */
    private volatile PrintWriter writer = null;


    /**
     * 用于控制对写入器访问的锁。
     */
    protected final ReadWriteLock writerLock = new ReentrantReadWriteLock();


    /**
     * 日志缓冲区大小。
     */
    private Integer bufferSize;


    /**
     * 表示 {prefix}{date}{suffix} 类型的文件名模式。日期格式为 YYYY-MM-DD。
     */
    private Pattern pattern;


    // --------------------------------------------------------- Public Methods


    /**
     * 格式化并发布日志记录。
     *
     * @param record 日志事件的描述
     */
    @Override
    public void publish(LogRecord record) {

        if (!isLoggable(record)) {
            return;
        }

        // 构建时间戳
        Timestamp ts = new Timestamp(System.currentTimeMillis());
        String tsDate = ts.toString().substring(0, 10);

        writerLock.readLock().lock();
        try {
            // 如果日期已更改，切换日志文件
            if (rotatable.booleanValue() && !date.equals(tsDate)) {
                // 在切换前升级为写锁
                writerLock.readLock().unlock();
                writerLock.writeLock().lock();
                try {
                    // 确保其他线程尚未完成此操作
                    if (!date.equals(tsDate)) {
                        closeWriter();
                        date = tsDate;
                        openWriter();
                        clean();
                    }
                } finally {
                    // 降级为读锁。这确保在写入日志消息前写入器保持有效
                    writerLock.readLock().lock();
                    writerLock.writeLock().unlock();
                }
            }

            String result = null;
            try {
                result = getFormatter().format(record);
            } catch (Exception e) {
                reportError(null, e, ErrorManager.FORMAT_FAILURE);
                return;
            }

            try {
                if (writer != null) {
                    writer.write(result);
                    if (bufferSize.intValue() < 0) {
                        writer.flush();
                    }
                } else {
                    reportError("FileHandler is closed or not yet initialized, unable to log [" + result + "]", null,
                            ErrorManager.WRITE_FAILURE);
                }
            } catch (Exception e) {
                reportError(null, e, ErrorManager.WRITE_FAILURE);
            }
        } finally {
            writerLock.readLock().unlock();
        }
    }


    // -------------------------------------------------------- Private Methods


    /**
     * 关闭当前打开的日志文件（如果有）。
     */
    @Override
    public void close() {
        closeWriter();
    }

    /**
     * 关闭写入器。
     */
    protected void closeWriter() {

        writerLock.writeLock().lock();
        try {
            if (writer == null) {
                return;
            }
            writer.write(getFormatter().getTail(this));
            writer.flush();
            writer.close();
            writer = null;
            date = "";
        } catch (Exception e) {
            reportError(null, e, ErrorManager.CLOSE_FAILURE);
        } finally {
            writerLock.writeLock().unlock();
        }
    }


    /**
     * 刷新缓冲区。
     */
    @Override
    public void flush() {

        writerLock.readLock().lock();
        try {
            if (writer == null) {
                return;
            }
            writer.flush();
        } catch (Exception e) {
            reportError(null, e, ErrorManager.FLUSH_FAILURE);
        } finally {
            writerLock.readLock().unlock();
        }

    }


    /**
     * 从 LogManager 属性中配置。
     */
    private void configure() {

        Timestamp ts = new Timestamp(System.currentTimeMillis());
        date = ts.toString().substring(0, 10);

        String className = this.getClass().getName(); // 允许类覆盖

        ClassLoader cl = ClassLoaderLogManager.getClassLoader();

        // 检索日志文件名的配置
        if (rotatable == null) {
            rotatable = Boolean.valueOf(getProperty(className + ".rotatable", "true"));
        }
        if (directory == null) {
            directory = getProperty(className + ".directory", "logs");
        }
        if (prefix == null) {
            prefix = getProperty(className + ".prefix", "juli.");
        }
        if (suffix == null) {
            suffix = getProperty(className + ".suffix", ".log");
        }

        // https://bz.apache.org/bugzilla/show_bug.cgi?id=61232
        boolean shouldCheckForRedundantSeparator = !rotatable.booleanValue() && !prefix.isEmpty() && !suffix.isEmpty();
        // 假设分隔符只是一个字符，如果有更多用例，可能需要引入分隔符的概念
        if (shouldCheckForRedundantSeparator && (prefix.charAt(prefix.length() - 1) == suffix.charAt(0))) {
            suffix = suffix.substring(1);
        }

        pattern = Pattern
                .compile("^(" + Pattern.quote(prefix) + ")\\d{4}-\\d{1,2}-\\d{1,2}(" + Pattern.quote(suffix) + ")$");

        if (maxDays == null) {
            String sMaxDays = getProperty(className + ".maxDays", String.valueOf(DEFAULT_MAX_DAYS));
            try {
                maxDays = Integer.valueOf(sMaxDays);
            } catch (NumberFormatException ignore) {
                maxDays = Integer.valueOf(DEFAULT_MAX_DAYS);
            }
        }

        if (bufferSize == null) {
            String sBufferSize = getProperty(className + ".bufferSize", String.valueOf(DEFAULT_BUFFER_SIZE));
            try {
                bufferSize = Integer.valueOf(sBufferSize);
            } catch (NumberFormatException ignore) {
                bufferSize = Integer.valueOf(DEFAULT_BUFFER_SIZE);
            }
        }

        // 获取日志文件的编码
        String encoding = getProperty(className + ".encoding", null);
        if (encoding != null && encoding.length() > 0) {
            try {
                setEncoding(encoding);
            } catch (UnsupportedEncodingException ex) {
                // 忽略
            }
        }

        // 获取处理器的日志级别
        setLevel(Level.parse(getProperty(className + ".level", "" + Level.ALL)));

        // 获取过滤器配置
        String filterName = getProperty(className + ".filter", null);
        if (filterName != null) {
            try {
                setFilter((Filter) cl.loadClass(filterName).getConstructor().newInstance());
            } catch (Exception e) {
                // 忽略
            }
        }

        // 设置格式化器
        String formatterName = getProperty(className + ".formatter", null);
        if (formatterName != null) {
            try {
                setFormatter((Formatter) cl.loadClass(formatterName).getConstructor().newInstance());
            } catch (Exception e) {
                // 忽略并回退到默认值
                setFormatter(new OneLineFormatter());
            }
        } else {
            setFormatter(new OneLineFormatter());
        }

        // 设置错误管理器
        setErrorManager(new ErrorManager());
    }


    private String getProperty(String name, String defaultValue) {
        String value = LogManager.getLogManager().getProperty(name);
        if (value == null) {
            value = defaultValue;
        } else {
            value = value.trim();
        }
        return value;
    }


    /**
     * 为 <code>date</code> 指定的日期打开新的日志文件。
     */
    protected void open() {
        openWriter();
    }

    /**
     * 打开写入器。
     */
    protected void openWriter() {

        // 如有必要，创建目录
        File dir = new File(directory);
        if (!dir.mkdirs() && !dir.isDirectory()) {
            reportError("Unable to create [" + dir + "]", null, ErrorManager.OPEN_FAILURE);
            writer = null;
            return;
        }

        // 打开当前日志文件
        writerLock.writeLock().lock();
        FileOutputStream fos = null;
        OutputStream os = null;
        try {
            File pathname = new File(dir.getAbsoluteFile(), prefix + (rotatable.booleanValue() ? date : "") + suffix);
            File parent = pathname.getParentFile();
            if (!parent.mkdirs() && !parent.isDirectory()) {
                reportError("Unable to create [" + parent + "]", null, ErrorManager.OPEN_FAILURE);
                writer = null;
                return;
            }
            String encoding = getEncoding();
            fos = new FileOutputStream(pathname, true);
            os = bufferSize.intValue() > 0 ? new BufferedOutputStream(fos, bufferSize.intValue()) : fos;
            writer = new PrintWriter(
                    (encoding != null) ? new OutputStreamWriter(os, encoding) : new OutputStreamWriter(os), false);
            writer.write(getFormatter().getHead(this));
        } catch (Exception e) {
            reportError(null, e, ErrorManager.OPEN_FAILURE);
            writer = null;
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e1) {
                    // 忽略
                }
            }
            if (os != null) {
                try {
                    os.close();
                } catch (IOException e1) {
                    // 忽略
                }
            }
        } finally {
            writerLock.writeLock().unlock();
        }
    }

    /**
     * 清理旧日志文件。
     */
    private void clean() {
        if (maxDays.intValue() <= 0 || Files.notExists(getDirectoryAsPath())) {
            return;
        }
        DELETE_FILES_SERVICE.submit(() -> {
            try (DirectoryStream<Path> files = streamFilesForDelete()) {
                for (Path file : files) {
                    Files.delete(file);
                }
            } catch (IOException e) {
                reportError("Unable to delete log files older than [" + maxDays + "] days", null,
                        ErrorManager.GENERIC_FAILURE);
            }
        });
    }

    private DirectoryStream<Path> streamFilesForDelete() throws IOException {
        LocalDate maxDaysOffset = LocalDate.now().minus(maxDays.intValue(), ChronoUnit.DAYS);
        return Files.newDirectoryStream(getDirectoryAsPath(), path -> {
            boolean result = false;
            String date = obtainDateFromPath(path);
            if (date != null) {
                try {
                    LocalDate dateFromFile = LocalDate.from(DateTimeFormatter.ISO_LOCAL_DATE.parse(date));
                    result = dateFromFile.isBefore(maxDaysOffset);
                } catch (DateTimeException e) {
                    // 无操作
                }
            }
            return result;
        });
    }

    private Path getDirectoryAsPath() {
        return FileSystems.getDefault().getPath(directory);
    }

    private String obtainDateFromPath(Path path) {
        Path fileName = path.getFileName();
        if (fileName == null) {
            return null;
        }
        String date = fileName.toString();
        if (pattern.matcher(date).matches()) {
            date = date.substring(prefix.length());
            return date.substring(0, date.length() - suffix.length());
        } else {
            return null;
        }
    }

    /**
     * 线程工厂类，用于创建日志文件清理线程。
     */
    protected static final class ThreadFactory implements java.util.concurrent.ThreadFactory {
        private final String namePrefix;
        private final boolean isSecurityEnabled;
        private final ThreadGroup group;
        private final AtomicInteger threadNumber = new AtomicInteger(1);

        public ThreadFactory(final String namePrefix) {
            this.namePrefix = namePrefix;
            SecurityManager s = System.getSecurityManager();
            if (s == null) {
                this.isSecurityEnabled = false;
                this.group = Thread.currentThread().getThreadGroup();
            } else {
                this.isSecurityEnabled = true;
                this.group = s.getThreadGroup();
            }
        }

        @Override
        public Thread newThread(Runnable r) {
            Thread t = new Thread(group, r, namePrefix + threadNumber.getAndIncrement());
            // 线程不应将 Webapp 类加载器作为上下文类加载器
            if (isSecurityEnabled) {
                AccessController.doPrivileged((PrivilegedAction<Void>) () -> {
                    t.setContextClassLoader(ThreadFactory.class.getClassLoader());
                    return null;
                });
            } else {
                t.setContextClassLoader(ThreadFactory.class.getClassLoader());
            }
            t.setDaemon(true);
            return t;
        }
    }
}
