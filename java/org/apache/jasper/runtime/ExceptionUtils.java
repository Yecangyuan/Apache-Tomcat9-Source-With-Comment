package org.apache.jasper.runtime;

import java.lang.reflect.InvocationTargetException;


/**
 * 异常处理工具类，用于处理 Throwable 和 Exception。
 */
public class ExceptionUtils {

    /**
     * 处理可抛出异常。
     * 检查提供的 Throwable 是否需要重新抛出，其他异常则静默吞掉。
     * @param t 要检查的 Throwable
     */
    public static void handleThrowable(Throwable t) {
        if (t instanceof ThreadDeath) {
            throw (ThreadDeath) t;
        }
        if (t instanceof StackOverflowError) {
            // Swallow silently - it should be recoverable
            return;
        }
        if (t instanceof VirtualMachineError) {
            throw (VirtualMachineError) t;
        }
        // All other instances of Throwable will be silently swallowed
    }

    /**
     * Checks whether the supplied Throwable is an instance of
     * <code>InvocationTargetException</code> and returns the throwable that is
     * wrapped by it, if there is any.
     *
     * @param t the Throwable to check
     * @return <code>t</code> or <code>t.getCause()</code>
     */
    public static Throwable unwrapInvocationTargetException(Throwable t) {
        if (t instanceof InvocationTargetException && t.getCause() != null) {
            return t.getCause();
        }
        return t;
    }
}
