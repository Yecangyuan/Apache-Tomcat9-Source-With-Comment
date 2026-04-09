package javax.websocket;

/**
 * WebSocket端点的基类，定义了WebSocket连接生命周期中的回调方法。
 * 开发者需要继承此类并实现onOpen方法来处理新的WebSocket连接。
 */
public abstract class Endpoint {

    /**
     * 当新的WebSocket会话建立时触发此方法。
     * 这是WebSocket连接生命周期的第一个阶段，开发者需要在此方法中处理新连接，
     * 例如保存会话对象、设置消息处理器等。
     *
     * @param session 新建立的会话对象
     * @param config  端点的配置信息
     */
    public abstract void onOpen(Session session, EndpointConfig config);

    /**
     * 当WebSocket会话关闭时触发此方法。
     * 可以在此方法中执行清理工作，如释放资源、记录日志等。
     * 默认实现为空操作（NO-OP）。
     *
     * @param session     即将关闭的会话对象
     * @param closeReason 会话关闭的原因
     */
    public void onClose(Session session, CloseReason closeReason) {
        // NO-OP by default
    }

    /**
     * 当WebSocket通信过程中发生协议错误时触发此方法。
     * 可以在此方法中处理异常情况，如记录错误日志、通知客户端等。
     * 默认实现为空操作（NO-OP）。
     *
     * @param session   发生错误的会话对象
     * @param throwable 异常对象
     */
    public void onError(Session session, Throwable throwable) {
        // NO-OP by default
    }
}
