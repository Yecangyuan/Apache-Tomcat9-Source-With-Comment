package javax.websocket.server;

import javax.websocket.DeploymentException;
import javax.websocket.WebSocketContainer;

/**
 * 提供以编程方式部署端点的能力。
 */
public interface ServerContainer extends WebSocketContainer {

    /**
     * 添加端点类，通过类对象部署 WebSocket 端点
     *
     * @param clazz WebSocket 端点类对象
     * @throws DeploymentException 如果部署失败
     */
    void addEndpoint(Class<?> clazz) throws DeploymentException;

    /**
     * 添加端点配置，通过配置对象部署 WebSocket 端点
     *
     * @param sec WebSocket 端点配置对象
     * @throws DeploymentException 如果部署失败
     */
    void addEndpoint(ServerEndpointConfig sec) throws DeploymentException;
}
