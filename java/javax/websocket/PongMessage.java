package javax.websocket;

import java.nio.ByteBuffer;

/**
 * 表示 WebSocket Pong 消息，用于接收 pong 响应的接口。
 * 消息处理器使用此接口使应用程序能够处理其发送的任何 Ping 的响应。
 */
public interface PongMessage {
    /**
     * 获取 Pong 消息的有效负载。
     *
     * @return Pong 消息的有效负载。
     */
    ByteBuffer getApplicationData();
}
