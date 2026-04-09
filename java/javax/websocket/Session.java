package javax.websocket;

import java.io.Closeable;
import java.io.IOException;
import java.net.URI;
import java.security.Principal;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * WebSocket 会话接口。
 * <p>
 * 代表客户端与服务器之间的 WebSocket 连接会话。每个客户端连接都会创建一个 Session 实例，
 * 用于管理该连接的生命周期、消息收发、属性配置等。Session 提供了与远程端点通信的各种方法，
 * 包括发送和接收消息、获取连接信息、管理消息处理器等功能。
 * </p>
 * <p>
 * 会话可以是客户端会话（由客户端发起连接）或服务器端会话（由服务器接受连接）。
 * 会话的生命周期从连接建立开始，到连接关闭结束。在此期间，可以通过 Session 对象
 * 与远程端点进行双向通信。
 * </p>
 *
 * @since WebSocket 1.0
 */
public interface Session extends Closeable {

    /**
     * 获取创建此会话的 WebSocket 容器。
     * <p>
     * WebSocket 容器负责管理 WebSocket 端点的生命周期，包括部署、连接管理和资源配置。
     * 通过此方法可以访问容器的配置信息和功能。
     * </p>
     *
     * @return 创建此会话的 WebSocket 容器实例
     */
    WebSocketContainer getContainer();

    /**
     * 注册一个消息处理器用于接收传入的消息。
     * <p>
     * 每个消息类型（文本、二进制、pong）只能注册一个消息处理器。
     * 消息类型会在运行时从提供的 {@link MessageHandler} 实例中派生。
     * 由于运行时派生并不总是可靠的，建议使用
     * {@link #addMessageHandler(Class, javax.websocket.MessageHandler.Partial)} 或
     * {@link #addMessageHandler(Class, javax.websocket.MessageHandler.Whole)} 方法代替。
     * </p>
     *
     * @param handler 用于处理传入消息的消息处理器
     *
     * @throws IllegalStateException 如果已经为该消息类型注册了消息处理器
     */
    void addMessageHandler(MessageHandler handler) throws IllegalStateException;

    /**
     * 获取当前会话中所有已注册的消息处理器集合。
     * <p>
     * 返回的集合包含所有已注册的消息处理器，包括处理文本消息、二进制消息和 pong 消息的处理器。
     * 该集合是只读的，对其修改不会影响实际注册的消息处理器。
     * </p>
     *
     * @return 当前会话中已注册的消息处理器集合
     */
    Set<MessageHandler> getMessageHandlers();

    /**
     * 移除指定的消息处理器。
     * <p>
     * 从当前会话中移除先前注册的消息处理器。移除后，该处理器将不再接收消息事件。
     * 如果指定的处理器未注册，则此方法不执行任何操作。
     * </p>
     *
     * @param listener 要移除的消息处理器
     */
    void removeMessageHandler(MessageHandler listener);

    /**
     * 获取 WebSocket 协议版本。
     * <p>
     * 返回此会话使用的 WebSocket 协议版本号，例如 "13" 表示 RFC 6455 定义的版本。
     * </p>
     *
     * @return WebSocket 协议版本字符串
     */
    String getProtocolVersion();

    /**
     * 获取协商成功的子协议。
     * <p>
     * 返回在 WebSocket 握手过程中客户端和服务器协商确定的子协议名称。
     * 如果没有协商子协议，则返回空字符串。
     * </p>
     *
     * @return 协商成功的子协议名称，如果没有则返回空字符串
     */
    String getNegotiatedSubprotocol();

    /**
     * 获取协商成功的扩展列表。
     * <p>
     * 返回在 WebSocket 握手过程中客户端和服务器协商确定要使用的扩展列表。
     * 这些扩展用于增强 WebSocket 协议的功能，如压缩、多路复用等。
     * </p>
     *
     * @return 协商成功的扩展列表，如果没有则返回空列表
     */
    List<Extension> getNegotiatedExtensions();

    /**
     * 检查会话是否使用安全连接（WSS）。
     * <p>
     * 判断此 WebSocket 连接是否通过 TLS/SSL 加密。
     * 当使用 wss:// 协议建立连接时返回 true，使用 ws:// 时返回 false。
     * </p>
     *
     * @return 如果使用 WSS（加密连接）返回 true，否则返回 false
     */
    boolean isSecure();

    /**
     * 检查会话是否处于打开状态。
     * <p>
     * 判断此 WebSocket 连接是否仍然打开且可用。
     * 只有在连接完全建立且尚未关闭时返回 true。
     * </p>
     *
     * @return 如果会话处于打开状态返回 true，否则返回 false
     */
    boolean isOpen();

    /**
     * 获取会话的空闲超时时间。
     * <p>
     * 空闲超时时间表示在没有任何消息交换的情况下，会话保持打开状态的最长时间。
     * 超过此时间后，会话将被自动关闭。
     * </p>
     *
     * @return 当前会话的空闲超时时间（毫秒）。
     *         零或负值表示永不超时（无限超时）
     */
    long getMaxIdleTimeout();

    /**
     * 设置会话的空闲超时时间。
     * <p>
     * 设置会话在没有消息交换时的最大空闲时间。如果在此时间内没有收发任何消息，
     * 会话将被自动关闭。设置为 0 或负值表示禁用空闲超时。
     * </p>
     *
     * @param timeout 新的空闲超时时间（毫秒）。
     *                零或负值表示无限超时（永不超时）
     */
    void setMaxIdleTimeout(long timeout);

    /**
     * 设置二进制消息的最大缓冲区大小。
     * <p>
     * 设置用于接收二进制消息的最大缓冲区大小（字节）。
     * 当接收到的二进制消息超过此大小时，可能会被分块处理或导致错误。
     * </p>
     *
     * @param max 新的最大缓冲区大小（字节）
     */
    void setMaxBinaryMessageBufferSize(int max);

    /**
     * 获取二进制消息的最大缓冲区大小。
     * <p>
     * 返回当前用于接收二进制消息的缓冲区大小限制（字节）。
     * </p>
     *
     * @return 当前二进制消息的最大缓冲区大小（字节）
     */
    int getMaxBinaryMessageBufferSize();

    /**
     * 设置文本消息的最大缓冲区大小。
     * <p>
     * 设置用于接收文本消息的最大缓冲区大小（字符）。
     * 当接收到的文本消息超过此大小时，可能会被分块处理或导致错误。
     * </p>
     *
     * @param max 新的最大缓冲区大小（字符数）
     */
    void setMaxTextMessageBufferSize(int max);

    /**
     * 获取文本消息的最大缓冲区大小。
     * <p>
     * 返回当前用于接收文本消息的缓冲区大小限制（字符）。
     * </p>
     *
     * @return 当前文本消息的最大缓冲区大小（字符数）
     */
    int getMaxTextMessageBufferSize();

    /**
     * 获取异步远程端点。
     * <p>
     * 返回用于异步发送消息到远程端点的 RemoteEndpoint.Async 实例。
     * 异步操作不会阻塞调用线程，适合需要高并发或发送大量消息的场景。
     * </p>
     *
     * @return 异步远程端点实例
     */
    RemoteEndpoint.Async getAsyncRemote();

    /**
     * 获取同步远程端点。
     * <p>
     * 返回用于同步发送消息到远程端点的 RemoteEndpoint.Basic 实例。
     * 同步操作会阻塞调用线程直到操作完成，适合简单的消息发送场景。
     * </p>
     *
     * @return 同步远程端点实例
     */
    RemoteEndpoint.Basic getBasicRemote();

    /**
     * 获取会话的唯一标识符。
     * <p>
     * 返回此会话的唯一标识符字符串。此标识符在容器范围内是唯一的，
     * 可用于日志记录、会话管理等用途。
     * 注意：此标识符不应被视为来自安全随机源，不应用于安全敏感的操作。
     * </p>
     *
     * @return 会话的唯一标识符字符串
     */
    String getId();

    /**
     * 关闭与远程端点的连接。
     * <p>
     * 使用 {@link javax.websocket.CloseReason.CloseCodes#NORMAL_CLOSURE} 状态码
     * 和空的原因短语关闭 WebSocket 连接。这是关闭连接的标准方式，
     * 表示连接正常结束。
     * </p>
     *
     * @throws IOException 如果在关闭 WebSocket 会话时发生 I/O 错误
     */
    @Override
    void close() throws IOException;


    /**
     * 使用指定的关闭原因关闭与远程端点的连接。
     * <p>
     * 使用指定的状态码和原因短语关闭 WebSocket 连接。
     * 可以用于正常关闭连接，也可以在出现错误或异常时关闭连接并说明原因。
     * </p>
     *
     * @param closeReason 关闭 WebSocket 会话的原因，包含状态码和关闭消息
     *
     * @throws IOException 如果在关闭 WebSocket 会话时发生 I/O 错误
     */
    void close(CloseReason closeReason) throws IOException;

    /**
     * 获取 WebSocket 端点的请求 URI。
     * <p>
     * 返回客户端用于连接到此端点的 URI。
     * 对于服务器端会话，这是连接请求的 URI；
     * 对于客户端会话，这是连接时使用的目标 URI。
     * </p>
     *
     * @return WebSocket 端点的 URI
     */
    URI getRequestURI();

    /**
     * 获取请求参数映射。
     * <p>
     * 返回 WebSocket 握手请求中的查询参数映射。
     * 键是参数名，值是该参数的所有值的列表（一个参数可能有多个值）。
     * </p>
     *
     * @return 请求参数的映射表，键为参数名，值为参数值列表
     */
    Map<String,List<String>> getRequestParameterMap();

    /**
     * 获取查询字符串。
     * <p>
     * 返回 WebSocket 握手请求 URL 中的查询字符串部分（即 ? 后面的内容）。
     * 如果没有查询字符串，则返回 null。
     * </p>
     *
     * @return 查询字符串，如果没有则返回 null
     */
    String getQueryString();

    /**
     * 获取路径参数映射。
     * <p>
     * 返回 WebSocket 端点路径模板中提取的路径参数。
     * 例如，对于路径模板 "/chat/{roomId}" 和实际路径 "/chat/123"，
     * 将返回包含 "roomId" -> "123" 的映射。
     * </p>
     *
     * @return 路径参数的映射表，键为参数名，值为参数值
     */
    Map<String,String> getPathParameters();

    /**
     * 获取用户属性映射。
     * <p>
     * 返回与此会话关联的用户属性映射。这些属性用于在会话的生命周期内
     * 存储用户特定的数据。属性只对当前会话可见，不同会话之间不共享。
     * </p>
     *
     * @return 用户属性的映射表，可用于存储会话级别的数据
     */
    Map<String,Object> getUserProperties();

    /**
     * 获取用户主体。
     * <p>
     * 返回与当前 WebSocket 连接关联的用户主体（Principal），
     * 用于表示经过身份验证的用户。如果用户未经过身份验证，则返回 null。
     * </p>
     *
     * @return 用户主体实例，如果未认证则返回 null
     */
    Principal getUserPrincipal();

    /**
     * 获取与此会话关联的本地端点的所有开放会话。
     * <p>
     * 返回与当前会话使用相同本地端点（@ServerEndpoint 注解的类或
     * ClientEndpoint 实现类）的所有当前处于打开状态的会话集合。
     * 这对于实现广播消息功能或跟踪连接数很有用。
     * </p>
     *
     * @return 与本地端点关联的当前所有开放会话的集合
     */
    Set<Session> getOpenSessions();

    /**
     * 注册一个用于处理部分（分块）传入消息的消息处理器。
     * <p>
     * 每个消息类型（文本或二进制）只能注册一个消息处理器。
     * Pong 消息永远不会以部分消息的形式呈现。
     * 部分消息处理器允许处理大型消息而无需等待完整消息到达，
     * 适用于流式处理或内存受限的场景。
     * </p>
     *
     * @param <T>     给定处理器要处理的消息类型
     * @param clazz   实现 T 的 Class 对象
     * @param handler 用于处理传入部分消息的消息处理器
     *
     * @throws IllegalStateException 如果已经为该消息类型注册了消息处理器
     *
     * @since WebSocket 1.1
     */
    <T> void addMessageHandler(Class<T> clazz, MessageHandler.Partial<T> handler) throws IllegalStateException;

    /**
     * 注册一个用于处理完整传入消息的消息处理器。
     * <p>
     * 每个消息类型（文本、二进制、pong）只能注册一个消息处理器。
     * 完整消息处理器只有在整条消息完全接收后才会被调用，
     * 适合处理可以放入内存的小型消息。
     * </p>
     *
     * @param <T>     给定处理器要处理的消息类型
     * @param clazz   实现 T 的 Class 对象
     * @param handler 用于处理传入完整消息的消息处理器
     *
     * @throws IllegalStateException 如果已经为该消息类型注册了消息处理器
     *
     * @since WebSocket 1.1
     */
    <T> void addMessageHandler(Class<T> clazz, MessageHandler.Whole<T> handler) throws IllegalStateException;
}
