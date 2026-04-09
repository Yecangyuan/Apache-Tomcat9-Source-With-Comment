package javax.websocket.server;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.websocket.Decoder;
import javax.websocket.Encoder;
import javax.websocket.Extension;

/**
 * 为 WebSocket 服务端端点提供默认配置。
 */
final class DefaultServerEndpointConfig implements ServerEndpointConfig {

    // WebSocket 端点类
    private final Class<?> endpointClass;
    // 端点路径
    private final String path;
    // 子协议列表
    private final List<String> subprotocols;
    // 扩展列表
    private final List<Extension> extensions;
    // 编码器类列表
    private final List<Class<? extends Encoder>> encoders;
    // 解码器类列表
    private final List<Class<? extends Decoder>> decoders;
    // 服务器端点配置器
    private final Configurator serverEndpointConfigurator;
    // 用户属性映射表
    private final Map<String,Object> userProperties = new ConcurrentHashMap<>();

    // 构造方法：初始化所有配置字段
    DefaultServerEndpointConfig(Class<?> endpointClass, String path, List<String> subprotocols,
            List<Extension> extensions, List<Class<? extends Encoder>> encoders,
            List<Class<? extends Decoder>> decoders, Configurator serverEndpointConfigurator) {
        this.endpointClass = endpointClass;
        this.path = path;
        this.subprotocols = subprotocols;
        this.extensions = extensions;
        this.encoders = encoders;
        this.decoders = decoders;
        this.serverEndpointConfigurator = serverEndpointConfigurator;
    }

    // 获取端点类
    @Override
    public Class<?> getEndpointClass() {
        return endpointClass;
    }

    // 获取编码器类列表
    @Override
    public List<Class<? extends Encoder>> getEncoders() {
        return this.encoders;
    }

    // 获取解码器类列表
    @Override
    public List<Class<? extends Decoder>> getDecoders() {
        return this.decoders;
    }

    // 获取端点路径
    @Override
    public String getPath() {
        return path;
    }

    // 获取服务器端点配置器
    @Override
    public Configurator getConfigurator() {
        return serverEndpointConfigurator;
    }

    // 获取用户属性映射表
    @Override
    public Map<String,Object> getUserProperties() {
        return userProperties;
    }

    // 获取子协议列表
    @Override
    public List<String> getSubprotocols() {
        return subprotocols;
    }

    // 获取扩展列表
    @Override
    public List<Extension> getExtensions() {
        return extensions;
    }
}
