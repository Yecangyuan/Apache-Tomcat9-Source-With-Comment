package javax.websocket;

import java.io.IOException;
import java.net.URI;
import java.util.Set;

/**
 * WebSocket 容器接口。
 * <p>
 * 该接口定义了与 WebSocket 服务器建立连接、管理配置参数以及获取扩展信息的方法。
 * 它是 WebSocket 客户端 API 的核心组件，提供了创建和管理 WebSocket 会话的能力。
 *
 * @author JSR 356 专家组
 * @since 1.0
 */
public interface WebSocketContainer {

    /**
     * 获取异步发送消息的默认超时时间。
     *
     * @return 当前的默认超时时间，单位为毫秒。非正值表示无限超时。
     */
    long getDefaultAsyncSendTimeout();

    /**
     * 设置异步发送消息的默认超时时间。
     *
     * @param timeout 新的默认超时时间，单位为毫秒。非正值表示无限超时。
     */
    void setAsyncSendTimeout(long timeout);

    /**
     * 连接到 WebSocket 服务器。
     * <p>
     * 使用编程式端点实例建立与指定 WebSocket 端点的连接。
     *
     * @param endpoint 处理服务器响应的端点实例
     * @param path     WebSocket 端点的完整 URL 路径
     * @return 连接的 WebSocket 会话
     * @throws DeploymentException 如果无法建立连接
     * @throws IOException         如果尝试建立连接时发生 I/O 错误
     */
    Session connectToServer(Object endpoint, URI path) throws DeploymentException, IOException;

    /**
     * 连接到 WebSocket 服务器。
     * <p>
     * 使用注解式端点类建立与指定 WebSocket 端点的连接。
     * 将创建该类的新实例来处理服务器响应。
     *
     * @param annotatedEndpointClass 端点类，将创建其实例来处理服务器响应
     * @param path                   WebSocket 端点的完整 URL 路径
     * @return 连接的 WebSocket 会话
     * @throws DeploymentException 如果无法建立连接
     * @throws IOException         如果尝试建立连接时发生 I/O 错误
     */
    Session connectToServer(Class<?> annotatedEndpointClass, URI path) throws DeploymentException, IOException;

    /**
     * 创建与 WebSocket 的新连接。
     *
     * @param endpoint                    处理服务器响应的端点实例
     * @param clientEndpointConfiguration 用于配置新连接的客户端端点配置
     * @param path                        WebSocket 端点的完整 URL
     * @return 连接的 WebSocket 会话
     * @throws DeploymentException 如果无法建立连接
     * @throws IOException         如果尝试建立连接时发生 I/O 错误
     */
    Session connectToServer(Endpoint endpoint, ClientEndpointConfig clientEndpointConfiguration, URI path)
            throws DeploymentException, IOException;

    /**
     * 创建与 WebSocket 的新连接。
     *
     * @param endpoint                    将创建该类的实例来处理服务器响应
     * @param clientEndpointConfiguration 用于配置新连接的客户端端点配置
     * @param path                        WebSocket 端点的完整 URL
     * @return 连接的 WebSocket 会话
     * @throws DeploymentException 如果无法建立连接
     * @throws IOException         如果尝试建立连接时发生 I/O 错误
     */
    Session connectToServer(Class<? extends Endpoint> endpoint, ClientEndpointConfig clientEndpointConfiguration,
            URI path) throws DeploymentException, IOException;

    /**
     * 获取当前的默认会话空闲超时时间。
     *
     * @return 当前的默认会话空闲超时时间，单位为毫秒。零或负值表示无限超时。
     */
    long getDefaultMaxSessionIdleTimeout();

    /**
     * 设置默认会话空闲超时时间。
     *
     * @param timeout 新的默认会话空闲超时时间，单位为毫秒。零或负值表示无限超时。
     */
    void setDefaultMaxSessionIdleTimeout(long timeout);

    /**
     * 获取二进制消息的默认最大缓冲区大小。
     *
     * @return 当前的默认最大缓冲区大小，单位为字节
     */
    int getDefaultMaxBinaryMessageBufferSize();

    /**
     * 设置二进制消息的默认最大缓冲区大小。
     *
     * @param max 新的默认最大缓冲区大小，单位为字节
     */
    void setDefaultMaxBinaryMessageBufferSize(int max);

    /**
     * 获取文本消息的默认最大缓冲区大小。
     *
     * @return 当前的默认最大缓冲区大小，单位为字符
     */
    int getDefaultMaxTextMessageBufferSize();

    /**
     * 设置文本消息的默认最大缓冲区大小。
     *
     * @param max 新的默认最大缓冲区大小，单位为字符
     */
    void setDefaultMaxTextMessageBufferSize(int max);

    /**
     * 获取已安装的扩展。
     *
     * @return 此 WebSocket 实现支持的扩展集合
     */
    Set<Extension> getInstalledExtensions();
}
