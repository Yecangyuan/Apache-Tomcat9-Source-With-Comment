package javax.websocket;

import java.nio.ByteBuffer;

/**
 * 当传入的消息无法被解码时抛出的异常。
 * 此异常可能由 {@link Decoder} 实现抛出。
 */
public class DecodeException extends Exception {

    private static final long serialVersionUID = 1L;

    /** 导致解码失败的字节缓冲区 */
    private ByteBuffer bb;
    /** 导致解码失败的编码字符串 */
    private String encodedString;

    /**
     * 使用给定的字节缓冲区、错误消息和原因构造解码异常。
     *
     * @param bb      导致解码失败的字节缓冲区
     * @param message 错误消息
     * @param cause   原始异常
     */
    public DecodeException(ByteBuffer bb, String message, Throwable cause) {
        super(message, cause);
        this.bb = bb;
    }

    /**
     * 使用给定的编码字符串、错误消息和原因构造解码异常。
     *
     * @param encodedString 导致解码失败的编码字符串
     * @param message       错误消息
     * @param cause         原始异常
     */
    public DecodeException(String encodedString, String message, Throwable cause) {
        super(message, cause);
        this.encodedString = encodedString;
    }

    /**
     * 使用给定的字节缓冲区和错误消息构造解码异常。
     *
     * @param bb      导致解码失败的字节缓冲区
     * @param message 错误消息
     */
    public DecodeException(ByteBuffer bb, String message) {
        super(message);
        this.bb = bb;
    }

    /**
     * 使用给定的编码字符串和错误消息构造解码异常。
     *
     * @param encodedString 导致解码失败的编码字符串
     * @param message       错误消息
     */
    public DecodeException(String encodedString, String message) {
        super(message);
        this.encodedString = encodedString;
    }

    /**
     * 获取导致解码失败的字节缓冲区。
     *
     * @return 字节缓冲区
     */
    public ByteBuffer getBytes() {
        return bb;
    }

    /**
     * 获取导致解码失败的编码字符串。
     *
     * @return 编码字符串
     */
    public String getText() {
        return encodedString;
    }
}
