package javax.websocket;

/**
 * 表示 WebSocket 异步发送操作的结果。
 * 当使用 {@link RemoteEndpoint.Async} 的异步发送方法时，可以通过此类获取发送操作是否成功，
 * 或者在失败时获取异常信息。
 */
public final class SendResult {
    
    /** 发送过程中发生的异常，如果发送成功则为 null */
    private final Throwable exception;
    
    /** 发送是否成功的标志，true 表示发送成功，false 表示发送失败 */
    private final boolean ok;

    /**
     * 使用指定的异常构造 SendResult 实例。
     * 如果异常为 null，则表示发送成功；否则表示发送失败。
     *
     * @param exception 发送过程中发生的异常，成功时为 null
     */
    public SendResult(Throwable exception) {
        this.exception = exception;
        this.ok = (exception == null);
    }

    /**
     * 构造一个表示发送成功的 SendResult 实例。
     * 此构造方法等同于调用 {@link #SendResult(Throwable)} 并传入 null。
     */
    public SendResult() {
        this(null);
    }

    /**
     * 获取发送过程中发生的异常。
     *
     * @return 发送异常，如果发送成功则返回 null
     */
    public Throwable getException() {
        return exception;
    }

    /**
     * 判断发送操作是否成功。
     *
     * @return true 表示发送成功，false 表示发送失败
     */
    public boolean isOK() {
        return ok;
    }
}
