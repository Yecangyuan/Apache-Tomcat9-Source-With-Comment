package org.apache.juli.logging;


/**
 * <p>当日志工厂（LogFactory）或日志实例（Log）无法通过相应的工厂方法创建时抛出此异常。</p>
 *
 * @author Craig R. McClanahan
 */
public class LogConfigurationException extends RuntimeException {


    private static final long serialVersionUID = 1L;


    /**
     * 构造一个新的异常，将详细消息设置为 null。
     */
    public LogConfigurationException() {
        super();
    }


    /**
     * 构造一个新的异常，使用指定的详细消息。
     *
     * @param message 详细消息
     */
    public LogConfigurationException(String message) {
        super(message);
    }


    /**
     * 构造一个新的异常，使用指定的异常原因和派生的详细消息。
     *
     * @param cause 底层异常原因
     */
    public LogConfigurationException(Throwable cause) {
        super(cause);
    }


    /**
     * 构造一个新的异常，使用指定的详细消息和异常原因。
     *
     * @param message 详细消息
     * @param cause 底层异常原因
     */
    public LogConfigurationException(String message, Throwable cause) {
        super(message, cause);
    }
}
