package org.apache.juli;

import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.LogRecord;

/**
 * 使用日志条目队列的 {@link FileHandler} 实现，支持异步写入。
 * <p>
 * 配置属性继承自 {@link FileHandler} 类。此类没有为自己的日志配置添加额外的配置属性，
 * 而是依赖以下系统属性：
 * </p>
 * <ul>
 * <li><code>org.apache.juli.AsyncOverflowDropType</code> 默认值：<code>1</code></li>
 * <li><code>org.apache.juli.AsyncMaxRecordCount</code> 默认值：<code>10000</code></li>
 * </ul>
 * <p>
 * 请参阅 Tomcat 配置参考中的系统属性页面。
 * </p>
 */
public class AsyncFileHandler extends FileHandler {

    static final String THREAD_PREFIX = "AsyncFileHandlerWriter-";

    /** 队列溢出处理策略：丢弃最新的日志条目 */
    public static final int OVERFLOW_DROP_LAST = 1;
    /** 队列溢出处理策略：丢弃最早的日志条目 */
    public static final int OVERFLOW_DROP_FIRST = 2;
    /** 队列溢出处理策略：等待队列有空间后再入队 */
    public static final int OVERFLOW_DROP_FLUSH = 3;
    /** 队列溢出处理策略：丢弃当前的日志条目 */
    public static final int OVERFLOW_DROP_CURRENT = 4;

    /** 默认的队列溢出处理类型（丢弃最新的） */
    public static final int DEFAULT_OVERFLOW_DROP_TYPE = 1;
    /** 默认的最大日志记录数 */
    public static final int DEFAULT_MAX_RECORDS = 10000;

    public static final int OVERFLOW_DROP_TYPE = Integer.parseInt(
            System.getProperty("org.apache.juli.AsyncOverflowDropType", Integer.toString(DEFAULT_OVERFLOW_DROP_TYPE)));
    public static final int MAX_RECORDS = Integer
            .parseInt(System.getProperty("org.apache.juli.AsyncMaxRecordCount", Integer.toString(DEFAULT_MAX_RECORDS)));

    private static final LoggerExecutorService LOGGER_SERVICE = new LoggerExecutorService(OVERFLOW_DROP_TYPE,
            MAX_RECORDS);

    private final Object closeLock = new Object();
    protected volatile boolean closed = false;
    private final LoggerExecutorService loggerService;

    public AsyncFileHandler() {
        this(null, null, null);
    }

    public AsyncFileHandler(String directory, String prefix, String suffix) {
        this(directory, prefix, suffix, null);
    }

    public AsyncFileHandler(String directory, String prefix, String suffix, Integer maxDays) {
        this(directory, prefix, suffix, maxDays, LOGGER_SERVICE);
    }

    AsyncFileHandler(String directory, String prefix, String suffix, Integer maxDays,
            LoggerExecutorService loggerService) {
        super(directory, prefix, suffix, maxDays);
        this.loggerService = loggerService;
        open();
    }

    /**
     * 关闭处理器。
     * 同步处理以确保线程安全，并注销处理器。
     */
    @Override
    public void close() {
        if (closed) {
            return;
        }
        synchronized (closeLock) {
            if (closed) {
                return;
            }
            closed = true;
        }
        loggerService.deregisterHandler();
        super.close();
    }

    /**
     * 打开处理器。
     * 同步处理以确保线程安全，并注册处理器。
     */
    @Override
    protected void open() {
        if (!closed) {
            return;
        }
        synchronized (closeLock) {
            if (!closed) {
                return;
            }
            closed = false;
        }
        loggerService.registerHandler();
        super.open();
    }

    /**
     * 发布日志记录。
     * 将日志记录提交到异步执行队列中处理。
     *
     * @param record 要发布的日志记录
     */
    @Override
    public void publish(LogRecord record) {
        if (!isLoggable(record)) {
            return;
        }
        // 在将记录交给其他线程之前填充源条目，
        // 因为其他线程可能使用不同的类加载器
        record.getSourceMethodName();
        loggerService.execute(new Runnable() {

            @Override
            public void run() {
                /*
                 * 在 Tomcat 关闭期间，处理器会在执行器队列被刷新之前关闭，
                 * 因此如果执行器正在关闭，则忽略 closed 标志。
                 */
                if (!closed || loggerService.isTerminating()) {
                    publishInternal(record);
                }
            }
        });
    }

    /**
     * 内部发布日志记录。
     * 实际调用父类的 publish 方法写入日志。
     *
     * @param record 要发布的日志记录
     */
    protected void publishInternal(LogRecord record) {
        super.publish(record);
    }


    /**
     * 日志执行器服务。
     * 继承自 ThreadPoolExecutor，提供异步日志写入的执行环境。
     */
    static class LoggerExecutorService extends ThreadPoolExecutor {

        private static final ThreadFactory THREAD_FACTORY = new ThreadFactory(THREAD_PREFIX);

        /*
         * 实现说明：此计数器的使用可以扩展为启动/停止 LoggerExecutorService，
         * 但这需要仔细的锁定，因为队列的当前大小也需要考虑，
         * 并且在快速启动和停止处理器时会有很多边缘情况。
         */
        private final AtomicInteger handlerCount = new AtomicInteger();

        LoggerExecutorService(final int overflowDropType, final int maxRecords) {
            super(1, 1, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingDeque<>(maxRecords), THREAD_FACTORY);
            switch (overflowDropType) {
                case OVERFLOW_DROP_LAST:
                default:
                    setRejectedExecutionHandler(new DropLastPolicy());
                    break;
                case OVERFLOW_DROP_FIRST:
                    setRejectedExecutionHandler(new DiscardOldestPolicy());
                    break;
                case OVERFLOW_DROP_FLUSH:
                    setRejectedExecutionHandler(new DropFlushPolicy());
                    break;
                case OVERFLOW_DROP_CURRENT:
                    setRejectedExecutionHandler(new DiscardPolicy());
            }
        }

        @Override
        public LinkedBlockingDeque<Runnable> getQueue() {
            return (LinkedBlockingDeque<Runnable>) super.getQueue();
        }

        public void registerHandler() {
            handlerCount.incrementAndGet();
        }

        public void deregisterHandler() {
            int newCount = handlerCount.decrementAndGet();
            if (newCount == 0) {
                try {
                    Thread dummyHook = new Thread();
                    Runtime.getRuntime().addShutdownHook(dummyHook);
                    Runtime.getRuntime().removeShutdownHook(dummyHook);
                } catch (IllegalStateException ise) {
                    // JVM 正在关闭。
                    // 允许最多 10 秒来清空队列
                    shutdown();
                    try {
                        awaitTermination(10, TimeUnit.SECONDS);
                    } catch (InterruptedException e) {
                        // 忽略
                    }
                    shutdownNow();
                }
            }
        }
    }


    /**
     * 队列溢出策略：等待队列有空间后再入队。
     * 当队列已满时，会循环尝试将任务加入队列，直到成功或执行器关闭。
     */
    private static class DropFlushPolicy implements RejectedExecutionHandler {

        @Override
        public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
            while (true) {
                if (executor.isShutdown()) {
                    break;
                }
                try {
                    if (executor.getQueue().offer(r, 1000, TimeUnit.MILLISECONDS)) {
                        break;
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new RejectedExecutionException("Interrupted", e);
                }
            }
        }
    }

    /**
     * 队列溢出策略：丢弃最新的日志条目。
     * 当队列已满时，移除队列中最后一个元素，然后将新任务加入队列。
     */
    private static class DropLastPolicy implements RejectedExecutionHandler {

        @Override
        public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
            if (!executor.isShutdown()) {
                ((LoggerExecutorService) executor).getQueue().pollLast();
                executor.execute(r);
            }
        }
    }
}
