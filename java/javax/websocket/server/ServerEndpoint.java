package javax.websocket.server;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.websocket.Decoder;
import javax.websocket.Encoder;

/**
 * 用于标注 WebSocket 服务端端点的注解
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface ServerEndpoint {

    /**
     * 端点应映射到的 URI 或 URI 模板
     *
     * @return 端点应映射到的 URI 或 URI 模板
     */
    String value();

    /**
     * 支持的子协议列表，默认为空
     */
    String[] subprotocols() default {};

    /**
     * 解码器类列表，用于将消息解码为 Java 对象，默认为空
     */
    Class<? extends Decoder>[] decoders() default {};

    /**
     * 编码器类列表，用于将 Java 对象编码为消息，默认为空
     */
    Class<? extends Encoder>[] encoders() default {};

    /**
     * 配置器类，用于自定义端点配置，默认为容器提供的配置器
     */
    Class<? extends ServerEndpointConfig.Configurator> configurator() default ServerEndpointConfig.Configurator.class;
}
