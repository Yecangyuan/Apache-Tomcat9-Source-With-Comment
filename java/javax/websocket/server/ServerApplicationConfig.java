package javax.websocket.server;

import java.util.Set;

import javax.websocket.Endpoint;

/**
 * 应用程序可以提供此接口的实现来过滤要部署的已发现 WebSocket 端点。
 * 此类的实现将通过 ServletContainerInitializer 扫描发现。
 */
public interface ServerApplicationConfig {

    /**
     * 使应用程序能够过滤已发现的 {@link ServerEndpointConfig} 实现。
     *
     * @param scanned 应用程序中找到的 {@link Endpoint} 实现
     *
     * @return 应用程序希望部署的端点配置集合
     */
    Set<ServerEndpointConfig> getEndpointConfigs(Set<Class<? extends Endpoint>> scanned);

    /**
     * 使应用程序能够过滤已发现的使用 {@link ServerEndpoint} 注解的类。
     *
     * @param scanned 应用程序中找到的使用 {@link ServerEndpoint} 注解的 POJO
     *
     * @return 应用程序希望部署的 POJO 集合
     */
    Set<Class<?>> getAnnotatedEndpointClasses(Set<Class<?>> scanned);
}
