package javax.websocket;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 客户端端点配置接口，用于配置客户端WebSocket端点的各种属性，
 * 包括首选子协议、扩展、配置器以及编码器和解码器等。
 */
public interface ClientEndpointConfig extends EndpointConfig {

    /**
     * 获取客户端优先使用的子协议列表。
     *
     * @return 优先子协议列表
     */
    List<String> getPreferredSubprotocols();

    /**
     * 获取客户端支持的WebSocket扩展列表。
     *
     * @return 扩展列表
     */
    List<Extension> getExtensions();

    /**
     * 获取此客户端端点的配置器。
     *
     * @return 配置器实例
     */
    Configurator getConfigurator();

    /**
     * 客户端端点配置的构建器类，用于构建 {@link ClientEndpointConfig} 实例。
     * 采用建造者模式，支持链式调用设置各种配置属性。
     */
    final class Builder {

        private static final Configurator DEFAULT_CONFIGURATOR = new Configurator() {
        };

        /**
         * 创建一个新的 Builder 实例。
         *
         * @return 新的 Builder 实例
         */
        public static Builder create() {
            return new Builder();
        }

        /**
         * 私有构造函数，防止外部直接实例化。
         */
        private Builder() {
            // Hide default constructor
        }

        private Configurator configurator = DEFAULT_CONFIGURATOR;
        private List<String> preferredSubprotocols = Collections.emptyList();
        private List<Extension> extensions = Collections.emptyList();
        private List<Class<? extends Encoder>> encoders = Collections.emptyList();
        private List<Class<? extends Decoder>> decoders = Collections.emptyList();

        /**
         * 构建并返回一个 ClientEndpointConfig 实例。
         *
         * @return 配置完成的 ClientEndpointConfig 实例
         */
        public ClientEndpointConfig build() {
            return new DefaultClientEndpointConfig(preferredSubprotocols, extensions, encoders, decoders, configurator);
        }

        /**
         * 设置此客户端端点的配置器。
         *
         * @param configurator 配置器实例，如果为 null 则使用默认配置器
         * @return 当前 Builder 实例，支持链式调用
         */
        public Builder configurator(Configurator configurator) {
            if (configurator == null) {
                this.configurator = DEFAULT_CONFIGURATOR;
            } else {
                this.configurator = configurator;
            }
            return this;
        }

        /**
         * 设置客户端优先使用的子协议列表。
         *
         * @param preferredSubprotocols 优先子协议列表，如果为 null 或空列表则使用空列表
         * @return 当前 Builder 实例，支持链式调用
         */
        public Builder preferredSubprotocols(List<String> preferredSubprotocols) {
            if (preferredSubprotocols == null || preferredSubprotocols.size() == 0) {
                this.preferredSubprotocols = Collections.emptyList();
            } else {
                this.preferredSubprotocols = Collections.unmodifiableList(preferredSubprotocols);
            }
            return this;
        }

        /**
         * 设置客户端支持的 WebSocket 扩展列表。
         *
         * @param extensions 扩展列表，如果为 null 或空列表则使用空列表
         * @return 当前 Builder 实例，支持链式调用
         */
        public Builder extensions(List<Extension> extensions) {
            if (extensions == null || extensions.size() == 0) {
                this.extensions = Collections.emptyList();
            } else {
                this.extensions = Collections.unmodifiableList(extensions);
            }
            return this;
        }

        /**
         * 设置此端点使用的编码器类列表。
         *
         * @param encoders 编码器类列表，如果为 null 或空列表则使用空列表
         * @return 当前 Builder 实例，支持链式调用
         */
        public Builder encoders(List<Class<? extends Encoder>> encoders) {
            if (encoders == null || encoders.size() == 0) {
                this.encoders = Collections.emptyList();
            } else {
                this.encoders = Collections.unmodifiableList(encoders);
            }
            return this;
        }

        /**
         * 设置此端点使用的解码器类列表。
         *
         * @param decoders 解码器类列表，如果为 null 或空列表则使用空列表
         * @return 当前 Builder 实例，支持链式调用
         */
        public Builder decoders(List<Class<? extends Decoder>> decoders) {
            if (decoders == null || decoders.size() == 0) {
                this.decoders = Collections.emptyList();
            } else {
                this.decoders = Collections.unmodifiableList(decoders);
            }
            return this;
        }
    }

    /**
     * 客户端端点配置器类，允许客户端在 WebSocket 握手过程中
     * 检查和修改发送给服务器的请求头，以及检查服务器返回的握手响应。
     */
    class Configurator {

        /**
         * 为客户端提供一种机制，用于检查和/或修改发送给服务器以启动 WebSocket 握手的请求头。
         *
         * @param headers HTTP 请求头
         */
        public void beforeRequest(Map<String,List<String>> headers) {
            // NO-OP
        }

        /**
         * 为客户端提供一种机制，用于检查从服务器返回的握手响应。
         *
         * @param handshakeResponse 握手响应
         */
        public void afterResponse(HandshakeResponse handshakeResponse) {
            // NO-OP
        }
    }
}
