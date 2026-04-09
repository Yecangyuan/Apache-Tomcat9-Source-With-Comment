package org.apache.juli.logging;

import java.util.logging.ConsoleHandler;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 硬编码的 java.util.logging commons-logging 实现。
 * 该类直接封装 JDK 自带的日志框架，实现 commons-logging 接口。
 */
class DirectJDKLog implements Log {
    // 没有隐藏的理由 - 但有很好的理由不隐藏
    // JDK Logger 实例
    public final Logger logger;

    // 替代的配置读取器和控制台格式
    // 格式化器相关常量
    // 简单格式化器类名
    private static final String SIMPLE_FMT="java.util.logging.SimpleFormatter";
    // 格式化器系统属性名
    private static final String FORMATTER="org.apache.juli.formatter";

    static {
        if (System.getProperty("java.util.logging.config.class") == null  &&
                System.getProperty("java.util.logging.config.file") == null) {
            // 默认配置 - 它很糟糕。让我们至少覆盖控制台的格式化器
            try {
                Formatter fmt= (Formatter) Class.forName(System.getProperty(
                        FORMATTER, SIMPLE_FMT)).getConstructor().newInstance();
                // 也有可能用户修改了 jre/lib/logging.properties -
                // 但在大多数情况下这真的很愚蠢
                Logger root=Logger.getLogger("");
                for (Handler handler : root.getHandlers()) {
                    // 我只关心控制台 - 反正默认配置中使用的就是它
                    if (handler instanceof  ConsoleHandler) {
                        handler.setFormatter(fmt);
                    }
                }
            } catch (Throwable t) {
                // 也许它没有被包含 - 将使用丑陋的默认设置。
            }

        }
    }

    /**
     * 构造方法，使用指定名称创建 JDK Logger 实例
     * @param name 日志记录器名称
     */
    DirectJDKLog(String name ) {
        logger=Logger.getLogger(name);
    }

    /**
     * 检查是否启用了 ERROR 级别的日志记录
     * ERROR 级别对应 JDK 的 SEVERE 级别
     * @return 如果启用了 ERROR 级别则返回 true
     */
    @Override
    public final boolean isErrorEnabled() {
        return logger.isLoggable(Level.SEVERE);
    }

    /**
     * 检查是否启用了 WARN 级别的日志记录
     * WARN 级别对应 JDK 的 WARNING 级别
     * @return 如果启用了 WARN 级别则返回 true
     */
    @Override
    public final boolean isWarnEnabled() {
        return logger.isLoggable(Level.WARNING);
    }

    /**
     * 检查是否启用了 INFO 级别的日志记录
     * @return 如果启用了 INFO 级别则返回 true
     */
    @Override
    public final boolean isInfoEnabled() {
        return logger.isLoggable(Level.INFO);
    }

    /**
     * 检查是否启用了 DEBUG 级别的日志记录
     * DEBUG 级别对应 JDK 的 FINE 级别
     * @return 如果启用了 DEBUG 级别则返回 true
     */
    @Override
    public final boolean isDebugEnabled() {
        return logger.isLoggable(Level.FINE);
    }

    /**
     * 检查是否启用了 FATAL 级别的日志记录
     * FATAL 级别对应 JDK 的 SEVERE 级别
     * @return 如果启用了 FATAL 级别则返回 true
     */
    @Override
    public final boolean isFatalEnabled() {
        return logger.isLoggable(Level.SEVERE);
    }

    /**
     * 检查是否启用了 TRACE 级别的日志记录
     * TRACE 级别对应 JDK 的 FINER 级别
     * @return 如果启用了 TRACE 级别则返回 true
     */
    @Override
    public final boolean isTraceEnabled() {
        return logger.isLoggable(Level.FINER);
    }

    /**
     * 记录 DEBUG 级别的日志消息
     * @param message 日志消息对象
     */
    @Override
    public final void debug(Object message) {
        log(Level.FINE, String.valueOf(message), null);
    }

    /**
     * 记录 DEBUG 级别的日志消息和异常
     * @param message 日志消息对象
     * @param t 异常对象
     */
    @Override
    public final void debug(Object message, Throwable t) {
        log(Level.FINE, String.valueOf(message), t);
    }

    /**
     * 记录 TRACE 级别的日志消息
     * @param message 日志消息对象
     */
    @Override
    public final void trace(Object message) {
        log(Level.FINER, String.valueOf(message), null);
    }

    /**
     * 记录 TRACE 级别的日志消息和异常
     * @param message 日志消息对象
     * @param t 异常对象
     */
    @Override
    public final void trace(Object message, Throwable t) {
        log(Level.FINER, String.valueOf(message), t);
    }

    /**
     * 记录 INFO 级别的日志消息
     * @param message 日志消息对象
     */
    @Override
    public final void info(Object message) {
        log(Level.INFO, String.valueOf(message), null);
    }

    /**
     * 记录 INFO 级别的日志消息和异常
     * @param message 日志消息对象
     * @param t 异常对象
     */
    @Override
    public final void info(Object message, Throwable t) {
        log(Level.INFO, String.valueOf(message), t);
    }

    /**
     * 记录 WARN 级别的日志消息
     * @param message 日志消息对象
     */
    @Override
    public final void warn(Object message) {
        log(Level.WARNING, String.valueOf(message), null);
    }

    /**
     * 记录 WARN 级别的日志消息和异常
     * @param message 日志消息对象
     * @param t 异常对象
     */
    @Override
    public final void warn(Object message, Throwable t) {
        log(Level.WARNING, String.valueOf(message), t);
    }

    /**
     * 记录 ERROR 级别的日志消息
     * @param message 日志消息对象
     */
    @Override
    public final void error(Object message) {
        log(Level.SEVERE, String.valueOf(message), null);
    }

    /**
     * 记录 ERROR 级别的日志消息和异常
     * @param message 日志消息对象
     * @param t 异常对象
     */
    @Override
    public final void error(Object message, Throwable t) {
        log(Level.SEVERE, String.valueOf(message), t);
    }

    /**
     * 记录 FATAL 级别的日志消息
     * @param message 日志消息对象
     */
    @Override
    public final void fatal(Object message) {
        log(Level.SEVERE, String.valueOf(message), null);
    }

    /**
     * 记录 FATAL 级别的日志消息和异常
     * @param message 日志消息对象
     * @param t 异常对象
     */
    @Override
    public final void fatal(Object message, Throwable t) {
        log(Level.SEVERE, String.valueOf(message), t);
    }

    // 来自 commons logging。这是我为什么认为 java.util.logging
    // 不好的首要原因 - 委员会设计可能真的很糟糕！使用 java.util.logging 对性能的影响 -
    // 以及如果你需要包装它时的丑陋 - 远比日志不友好和不常见的默认格式更糟糕。

    /**
     * 内部日志记录方法。
     * 使用堆栈跟踪获取调用者信息（类名和方法名）。
     * 创建一个虚拟异常来获取堆栈跟踪，调用者将是堆栈的第三个元素。
     * @param level 日志级别
     * @param msg 日志消息
     * @param ex 异常对象，可为 null
     */
    private void log(Level level, String msg, Throwable ex) {
        if (logger.isLoggable(level)) {
            // Hack (?) 来获取堆栈跟踪。
            Throwable dummyException=new Throwable();
            StackTraceElement locations[]=dummyException.getStackTrace();
            // 调用者将是第三个元素
            String cname = "unknown";
            String method = "unknown";
            if (locations != null && locations.length >2) {
                StackTraceElement caller = locations[2];
                cname = caller.getClassName();
                method = caller.getMethodName();
            }
            if (ex==null) {
                logger.logp(level, cname, method, msg);
            } else {
                logger.logp(level, cname, method, msg, ex);
            }
        }
    }

    static Log getInstance(String name) {
        return new DirectJDKLog( name );
    }
}
