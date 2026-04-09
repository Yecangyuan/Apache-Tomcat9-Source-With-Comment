package javax.websocket;

import java.util.Iterator;
import java.util.ServiceLoader;

/**
 * 使用 {@link ServiceLoader} 机制来提供 WebSocket 客户端容器实例。
 */
public abstract class ContainerProvider {

    // 默认的 WebSocket 容器提供类名，用于回退机制
    private static final String DEFAULT_PROVIDER_CLASS_NAME = "org.apache.tomcat.websocket.WsWebSocketContainer";

    /**
     * 创建一个新的容器，用于创建传出的 WebSocket 连接。
     *
     * @return 新创建的容器。
     */
    public static WebSocketContainer getWebSocketContainer() {
        WebSocketContainer result = null;

        ServiceLoader<ContainerProvider> serviceLoader = ServiceLoader.load(ContainerProvider.class);
        Iterator<ContainerProvider> iter = serviceLoader.iterator();
        while (result == null && iter.hasNext()) {
            result = iter.next().getContainer();
        }

        // Fall-back. Also used by unit tests
        if (result == null) {
            try {
                @SuppressWarnings("unchecked")
                Class<WebSocketContainer> clazz =
                        (Class<WebSocketContainer>) Class.forName(DEFAULT_PROVIDER_CLASS_NAME);
                result = clazz.getConstructor().newInstance();
            } catch (ReflectiveOperationException | IllegalArgumentException | SecurityException e) {
                // No options left. Just return null.
            }
        }
        return result;
    }

    protected abstract WebSocketContainer getContainer();
}
