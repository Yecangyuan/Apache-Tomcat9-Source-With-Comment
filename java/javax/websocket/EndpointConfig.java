package javax.websocket;

import java.util.List;
import java.util.Map;

/**
 * WebSocket 端点配置的接口。该接口提供了获取编码器、解码器以及与端点关联的用户属性的方法。
 */
public interface EndpointConfig {

    /**
     * 获取用于编码从端点发送的消息的编码器类列表。
     * 这些编码器将消息对象转换为 WebSocket 消息（如文本或二进制格式）。
     *
     * @return 继承自 Encoder 的类的列表，如果没有配置编码器则返回空列表
     */
    List<Class<? extends Encoder>> getEncoders();

    /**
     * 获取用于解码传入端点的消息的解码器类列表。
     * 这些解码器将接收到的 WebSocket 消息（文本或二进制）转换为应用程序可以处理的对象。
     *
     * @return 继承自 Decoder 的类的列表，如果没有配置解码器则返回空列表
     */
    List<Class<? extends Decoder>> getDecoders();

    /**
     * 获取与此端点配置关联的用户属性映射。
     * 用户属性用于在端点实例之间共享数据，可用于存储与连接相关的状态信息。
     *
     * @return 包含用户属性的 Map，键为属性名（String），值为属性对象（Object）
     */
    Map<String,Object> getUserProperties();
}
