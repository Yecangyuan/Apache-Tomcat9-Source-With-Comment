package javax.websocket;

/**
 * 与WebSocket会话相关的异常类。
 * 当WebSocket会话操作出现问题时抛出此异常。
 */
public class SessionException extends Exception {

    private static final long serialVersionUID = 1L;

    /** 与此异常关联的WebSocket会话 */
    private final Session session;


    /**
     * 构造一个新的SessionException实例。
     *
     * @param message 异常的详细消息
     * @param cause 异常的原因（可以为null）
     * @param session 与此异常关联的WebSocket会话
     */
    public SessionException(String message, Throwable cause, Session session) {
        super(message, cause);
        this.session = session;
    }


    /**
     * 获取与此异常关联的WebSocket会话。
     *
     * @return 与此异常关联的WebSocket会话
     */
    public Session getSession() {
        return session;
    }
}
