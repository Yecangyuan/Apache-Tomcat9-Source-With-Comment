package javax.websocket;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.websocket.ClientEndpointConfig.Configurator;

/**
 * 用于标识一个类作为 WebSocket 客户端端点的注解。
 * 被标注的类必须满足以下要求：
 * - 必须是 public 的
 * - 必须有一个 public 的无参构造方法
 * - 类不能被声明为 abstract 或 final
 * - 必须包含至少一个被 {@link OnOpen}、{@link OnMessage}、{@link OnError} 或 {@link OnClose} 注解的方法
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface ClientEndpoint {
    /**
     * 返回此端点支持的子协议名称数组。
     *
     * @return 子协议名称数组，默认为空数组
     */
    String[] subprotocols() default {};

    /**
     * 返回此类使用的解码器类数组。
     * 这些解码器用于将 WebSocket 消息解码为 Java 对象。
     *
     * @return 解码器类数组，默认为空数组
     */
    Class<? extends Decoder>[] decoders() default {};

    /**
     * 返回此类使用的编码器类数组。
     * 这些编码器用于将 Java 对象编码为 WebSocket 消息。
     *
     * @return 编码器类数组，默认为空数组
     */
    Class<? extends Encoder>[] encoders() default {};

    /**
     * 返回用于配置此端点的配置器类。
     * 配置器可以用于自定义端点的创建和配置过程。
     *
     * @return 配置器类，默认为 {@link Configurator} 类本身
     */
    Class<? extends Configurator> configurator() default Configurator.class;
}
