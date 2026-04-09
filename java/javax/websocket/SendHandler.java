package javax.websocket;

/**
 * 异步发送结果的回调接口。
 * 当使用 {@link RemoteEndpoint.Async} 的异步发送方法发送消息时，
 * 可以通过此接口接收发送操作的结果通知（成功或失败）。
 *
 * @author 翻译者
 * @see javax.websocket.RemoteEndpoint.Async
 * @see javax.websocket.SendResult
 */
public interface SendHandler {

    /**
     * 当异步消息发送完成时调用此方法。
     * 无论发送成功还是失败，都会触发此回调。
     *
     * @param result 发送结果对象，包含发送是否成功的状态以及可能的异常信息
     */
    void onResult(SendResult result);
}
