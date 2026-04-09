package javax.websocket;

/**
 * 编码异常类，表示在将Java对象编码为WebSocket消息时发生的错误。
 * <p>
 * 当编码器(Encoder)无法将给定的Java对象转换为其预期的WebSocket消息表示形式时，
 * 将抛出此异常。
 */
public class EncodeException extends Exception {

    private static final long serialVersionUID = 1L;

    /** 尝试编码但失败的Java对象 */
    private Object object;

    /**
     * 使用给定的对象和错误消息构造一个新的编码异常。
     *
     * @param object  尝试编码但失败的Java对象
     * @param message 描述编码失败的详细错误消息
     */
    public EncodeException(Object object, String message) {
        super(message);
        this.object = object;
    }

    /**
     * 使用给定的对象、错误消息和根本原因构造一个新的编码异常。
     *
     * @param object  尝试编码但失败的Java对象
     * @param message 描述编码失败的详细错误消息
     * @param cause   导致编码失败的根本原因异常
     */
    public EncodeException(Object object, String message, Throwable cause) {
        super(message, cause);
        this.object = object;
    }

    /**
     * 获取尝试编码但失败的Java对象。
     *
     * @return 导致编码失败的Java对象
     */
    public Object getObject() {
        return this.object;
    }
}
