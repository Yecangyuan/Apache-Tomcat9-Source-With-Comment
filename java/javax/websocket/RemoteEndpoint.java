package javax.websocket;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.nio.ByteBuffer;
import java.util.concurrent.Future;


/**
 * WebSocket远程端点接口，表示与对等端的连接。
 * 用于向远程端点发送消息，包含异步(Async)和同步(Basic)两种发送方式。
 */
public interface RemoteEndpoint {

    /**
     * 异步发送消息的远程端点接口。
     * 所有发送操作都是非阻塞的，通过Future或SendHandler回调通知发送完成状态。
     */
    interface Async extends RemoteEndpoint {

        /**
         * 获取异步发送消息的超时时间（毫秒）。
         * 默认值由 {@link WebSocketContainer#getDefaultAsyncSendTimeout()} 决定。
         *
         * @return 当前发送超时时间（毫秒）。非正值表示无限超时。
         */
        long getSendTimeout();

        /**
         * 设置异步发送消息的超时时间（毫秒）。
         * 默认值由 {@link WebSocketContainer#getDefaultAsyncSendTimeout()} 决定。
         *
         * @param timeout 新的异步发送消息超时时间（毫秒）。非正值表示无限超时。
         */
        void setSendTimeout(long timeout);

        /**
         * 异步发送文本消息，使用SendHandler在消息发送完成后通知客户端。
         *
         * @param text       要发送的文本消息
         * @param completion 用于在消息发送完成后通知客户端的处理器
         */
        void sendText(String text, SendHandler completion);

        /**
         * 异步发送文本消息，使用Future在消息发送完成后通知客户端。
         *
         * @param text 要发送的文本消息
         * @return 一个Future对象，在消息发送完成时发出信号
         */
        Future<Void> sendText(String text);

        /**
         * 异步发送二进制消息，使用Future在消息发送完成后通知客户端。
         *
         * @param data 要发送的二进制消息数据
         * @return 一个Future对象，在消息发送完成时发出信号
         * @throws IllegalArgumentException 如果 {@code data} 为 {@code null}
         */
        Future<Void> sendBinary(ByteBuffer data);

        /**
         * 异步发送二进制消息，使用SendHandler在消息发送完成后通知客户端。
         *
         * @param data       要发送的二进制消息数据
         * @param completion 用于在消息发送完成后通知客户端的处理器
         * @throws IllegalArgumentException 如果 {@code data} 或 {@code completion} 为 {@code null}
         */
        void sendBinary(ByteBuffer data, SendHandler completion);

        /**
         * 将对象编码为消息并异步发送，使用Future在消息发送完成后通知客户端。
         *
         * @param obj 要发送的对象
         * @return 一个Future对象，在消息发送完成时发出信号
         * @throws IllegalArgumentException 如果 {@code obj} 为 {@code null}
         */
        Future<Void> sendObject(Object obj);

        /**
         * 将对象编码为消息并异步发送，使用SendHandler在消息发送完成后通知客户端。
         *
         * @param obj        要发送的对象
         * @param completion 用于在消息发送完成后通知客户端的处理器
         * @throws IllegalArgumentException 如果 {@code obj} 或 {@code completion} 为 {@code null}
         */
        void sendObject(Object obj, SendHandler completion);

    }

    /**
     * 同步发送消息的远程端点接口。
     * 所有发送操作都是阻塞的，直到消息发送完成。
     */
    interface Basic extends RemoteEndpoint {

        /**
         * 发送文本消息，阻塞直到消息发送完成。
         *
         * @param text 要发送的文本消息
         * @throws IllegalArgumentException 如果 {@code text} 为 {@code null}
         * @throws IOException              如果在发送消息过程中发生I/O错误
         */
        void sendText(String text) throws IOException;

        /**
         * 发送二进制消息，阻塞直到消息发送完成。
         *
         * @param data 要发送的二进制消息数据
         * @throws IllegalArgumentException 如果 {@code data} 为 {@code null}
         * @throws IOException              如果在发送消息过程中发生I/O错误
         */
        void sendBinary(ByteBuffer data) throws IOException;

        /**
         * 向远程端点发送文本消息的一部分。
         * 一旦消息的第一部分被发送，在该消息的所有剩余部分发送完成之前，不能发送其他文本或二进制消息。
         *
         * @param fragment 要发送的部分消息
         * @param isLast   如果这是消息的最后一部分则为 <code>true</code>，否则为 <code>false</code>
         * @throws IllegalArgumentException 如果 {@code fragment} 为 {@code null}
         * @throws IOException              如果在发送消息过程中发生I/O错误
         */
        void sendText(String fragment, boolean isLast) throws IOException;

        /**
         * 向远程端点发送二进制消息的一部分。
         * 一旦消息的第一部分被发送，在该消息的所有剩余部分发送完成之前，不能发送其他文本或二进制消息。
         *
         * @param partialByte 要发送的部分消息
         * @param isLast      如果这是消息的最后一部分则为 <code>true</code>，否则为 <code>false</code>
         * @throws IllegalArgumentException 如果 {@code partialByte} 为 {@code null}
         * @throws IOException              如果在发送消息过程中发生I/O错误
         */
        void sendBinary(ByteBuffer partialByte, boolean isLast) throws IOException;

        /**
         * 获取用于发送二进制消息的输出流。
         *
         * @return 用于发送二进制消息的OutputStream
         * @throws IOException 如果获取输出流时发生I/O错误
         */
        OutputStream getSendStream() throws IOException;

        /**
         * 获取用于发送文本消息的写入器。
         *
         * @return 用于发送文本消息的Writer
         * @throws IOException 如果获取写入器时发生I/O错误
         */
        Writer getSendWriter() throws IOException;

        /**
         * 将对象编码为消息并发送到远程端点。
         *
         * @param data 要发送的对象
         * @throws EncodeException          如果将 {@code data} 对象编码为WebSocket消息时出现问题
         * @throws IllegalArgumentException 如果 {@code data} 为 {@code null}
         * @throws IOException              如果在发送消息过程中发生I/O错误
         */
        void sendObject(Object data) throws IOException, EncodeException;

    }

    /**
     * 启用或禁用此端点的出站消息批处理。
     * 如果批处理被禁用而它之前是启用的，则此方法将阻塞，直到当前批处理的消息被写入。
     *
     * @param batchingAllowed 新的批处理设置
     * @throws IOException 如果更改值导致调用 {@link #flushBatch()} 且该调用抛出 {@link IOException}
     */
    void setBatchingAllowed(boolean batchingAllowed) throws IOException;

    /**
     * 获取端点当前的批处理状态。
     *
     * @return 如果启用了批处理则为 <code>true</code>，否则为 <code>false</code>
     */
    boolean getBatchingAllowed();

    /**
     * 将当前批处理的所有消息刷新到远程端点。
     * 此方法将阻塞直到刷新完成。
     *
     * @throws IOException 如果刷新过程中发生I/O错误
     */
    void flushBatch() throws IOException;

    /**
     * 发送ping消息，阻塞直到消息发送完成。
     * 注意：如果消息正在异步发送过程中，此方法将阻塞直到该消息和此ping消息都被发送。
     *
     * @param applicationData ping消息的负载数据
     * @throws IOException              如果发送ping时发生I/O错误
     * @throws IllegalArgumentException 如果应用数据对于控制消息来说太大（最大125字节）
     */
    void sendPing(ByteBuffer applicationData) throws IOException, IllegalArgumentException;

    /**
     * 发送pong消息，阻塞直到消息发送完成。
     * 注意：如果消息正在异步发送过程中，此方法将阻塞直到该消息和此pong消息都被发送。
     *
     * @param applicationData pong消息的负载数据
     * @throws IOException              如果发送pong时发生I/O错误
     * @throws IllegalArgumentException 如果应用数据对于控制消息来说太大（最大125字节）
     */
    void sendPong(ByteBuffer applicationData) throws IOException, IllegalArgumentException;
}
