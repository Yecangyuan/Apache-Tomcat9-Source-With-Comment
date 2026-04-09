package javax.websocket;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 默认的客户端端点配置实现类。
 * 该类实现了 {@link ClientEndpointConfig} 接口，提供了 WebSocket 客户端端点的默认配置。
 */
final class DefaultClientEndpointConfig implements ClientEndpointConfig {

    /** 首选的子协议列表 */
    private final List<String> preferredSubprotocols;
    /** WebSocket 扩展列表 */
    private final List<Extension> extensions;
    /** 编码器类列表 */
    private final List<Class<? extends Encoder>> encoders;
    /** 解码器类列表 */
    private final List<Class<? extends Decoder>> decoders;
    /** 用户属性映射表，用于存储与端点相关的自定义属性 */
    private final Map<String,Object> userProperties = new ConcurrentHashMap<>();
    /** 配置器对象，用于自定义端点配置 */
    private final Configurator configurator;


    /**
     * 构造方法。
     * 使用指定的参数创建一个新的客户端端点配置实例。
     *
     * @param preferredSubprotocols 首选的子协议列表
     * @param extensions WebSocket 扩展列表
     * @param encoders 编码器类列表
     * @param decoders 解码器类列表
     * @param configurator 配置器对象
     */
    DefaultClientEndpointConfig(List<String> preferredSubprotocols, List<Extension> extensions,
            List<Class<? extends Encoder>> encoders, List<Class<? extends Decoder>> decoders,
            Configurator configurator) {
        this.preferredSubprotocols = preferredSubprotocols;
        this.extensions = extensions;
        this.decoders = decoders;
        this.encoders = encoders;
        this.configurator = configurator;
    }


    /**
     * 获取首选的子协议列表。
     *
     * @return 首选子协议列表
     */
    @Override
    public List<String> getPreferredSubprotocols() {
        return preferredSubprotocols;
    }


    /**
     * 获取 WebSocket 扩展列表。
     *
     * @return 扩展列表
     */
    @Override
    public List<Extension> getExtensions() {
        return extensions;
    }


    /**
     * 获取编码器类列表。
     *
     * @return 编码器类列表
     */
    @Override
    public List<Class<? extends Encoder>> getEncoders() {
        return encoders;
    }


    /**
     * 获取解码器类列表。
     *
     * @return 解码器类列表
     */
    @Override
    public List<Class<? extends Decoder>> getDecoders() {
        return decoders;
    }


    /**
     * 获取用户属性映射表。
     * 该映射表可用于存储与端点相关的自定义属性。
     *
     * @return 用户属性映射表
     */
    @Override
    public Map<String,Object> getUserProperties() {
        return userProperties;
    }


    /**
     * 获取配置器对象。
     *
     * @return 配置器对象
     */
    @Override
    public Configurator getConfigurator() {
        return configurator;
    }
}
