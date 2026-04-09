package javax.websocket;

import java.util.List;
import java.util.Map;

/**
 * 表示 WebSocket 握手响应的接口。
 *
 * @since WebSocket 1.0
 * @version WebSocket 1.1
 */
public interface HandshakeResponse {

    /**
     * WebSocket 接受 HTTP 头的名称。
     */
    String SEC_WEBSOCKET_ACCEPT = "Sec-WebSocket-Accept";

    /**
     * 返回 HTTP 响应头。
     *
     * @return HTTP 响应头的映射，其中键为头名称，值为头值列表
     */
    Map<String,List<String>> getHeaders();
}
