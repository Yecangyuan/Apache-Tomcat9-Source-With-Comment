package javax.websocket.server;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.ServiceLoader;

import javax.websocket.Decoder;
import javax.websocket.Encoder;
import javax.websocket.EndpointConfig;
import javax.websocket.Extension;
import javax.websocket.HandshakeResponse;

/**
 * 为发布到服务器的 WebSocket 端点提供配置信息。应用程序可以提供自己的实现或使用 {@link Builder}。
 */
public interface ServerEndpointConfig extends EndpointConfig {

    Class<?> getEndpointClass();

    /**
     * 返回此 WebSocket 服务端端点注册的路径。可能是路径或 0 级 URI 模板。
     *
     * @return 注册的路径
     */
    String getPath();

    List<String> getSubprotocols();

    List<Extension> getExtensions();

    Configurator getConfigurator();


    /**
     * 用于构建 ServerEndpointConfig 的构建器类
     */
    final class Builder {

        /**
         * 创建构建器实例
         *
         * @param endpointClass 端点类
         * @param path          路径
         * @return 构建器实例
         */
        public static Builder create(Class<?> endpointClass, String path) {
            return new Builder(endpointClass, path);
        }


        private final Class<?> endpointClass;
        private final String path;
        private List<Class<? extends Encoder>> encoders = Collections.emptyList();
        private List<Class<? extends Decoder>> decoders = Collections.emptyList();
        private List<String> subprotocols = Collections.emptyList();
        private List<Extension> extensions = Collections.emptyList();
        private Configurator configurator = Configurator.fetchContainerDefaultConfigurator();


        private Builder(Class<?> endpointClass, String path) {
            if (endpointClass == null) {
                throw new IllegalArgumentException("Endpoint class may not be null");
            }
            if (path == null) {
                throw new IllegalArgumentException("Path may not be null");
            }
            if (path.isEmpty()) {
                throw new IllegalArgumentException("Path may not be empty");
            }
            if (path.charAt(0) != '/') {
                throw new IllegalArgumentException("Path must start with '/'");
            }
            this.endpointClass = endpointClass;
            this.path = path;
        }

        /**
         * 构建 ServerEndpointConfig 实例
         *
         * @return ServerEndpointConfig 实例
         */
        public ServerEndpointConfig build() {
            return new DefaultServerEndpointConfig(endpointClass, path, subprotocols, extensions, encoders, decoders,
                    configurator);
        }


        /**
         * 设置编码器列表
         *
         * @param encoders 编码器类列表
         * @return 构建器实例
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
         * 设置解码器列表
         *
         * @param decoders 解码器类列表
         * @return 构建器实例
         */
        public Builder decoders(List<Class<? extends Decoder>> decoders) {
            if (decoders == null || decoders.size() == 0) {
                this.decoders = Collections.emptyList();
            } else {
                this.decoders = Collections.unmodifiableList(decoders);
            }
            return this;
        }


        /**
         * 设置子协议列表
         *
         * @param subprotocols 子协议列表
         * @return 构建器实例
         */
        public Builder subprotocols(List<String> subprotocols) {
            if (subprotocols == null || subprotocols.size() == 0) {
                this.subprotocols = Collections.emptyList();
            } else {
                this.subprotocols = Collections.unmodifiableList(subprotocols);
            }
            return this;
        }


        /**
         * 设置扩展列表
         *
         * @param extensions 扩展列表
         * @return 构建器实例
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
         * 设置配置器
         *
         * @param serverEndpointConfigurator 服务端端点配置器
         * @return 构建器实例
         */
        public Builder configurator(Configurator serverEndpointConfigurator) {
            if (serverEndpointConfigurator == null) {
                this.configurator = Configurator.fetchContainerDefaultConfigurator();
            } else {
                this.configurator = serverEndpointConfigurator;
            }
            return this;
        }
    }


    /**
     * 配置器类，用于配置 WebSocket 端点的握手和端点实例创建
     */
    class Configurator {

        private static volatile Configurator defaultImpl = null;
        private static final Object defaultImplLock = new Object();

        private static final String DEFAULT_IMPL_CLASSNAME =
                "org.apache.tomcat.websocket.server.DefaultServerEndpointConfigurator";

        /**
         * 获取容器默认配置器
         *
         * @return 默认配置器实例
         */
        static Configurator fetchContainerDefaultConfigurator() {
            if (defaultImpl == null) {
                synchronized (defaultImplLock) {
                    if (defaultImpl == null) {
                        if (System.getSecurityManager() == null) {
                            defaultImpl = loadDefault();
                        } else {
                            defaultImpl = AccessController.doPrivileged(new PrivilegedLoadDefault());
                        }
                    }
                }
            }
            return defaultImpl;
        }


        private static Configurator loadDefault() {
            Configurator result = null;

            ServiceLoader<Configurator> serviceLoader = ServiceLoader.load(Configurator.class);

            Iterator<Configurator> iter = serviceLoader.iterator();
            while (result == null && iter.hasNext()) {
                result = iter.next();
            }

            // Fall-back. Also used by unit tests
            if (result == null) {
                try {
                    @SuppressWarnings("unchecked")
                    Class<Configurator> clazz = (Class<Configurator>) Class.forName(DEFAULT_IMPL_CLASSNAME);
                    result = clazz.getConstructor().newInstance();
                } catch (ReflectiveOperationException | IllegalArgumentException | SecurityException e) {
                    // No options left. Just return null.
                }
            }
            return result;
        }


        private static class PrivilegedLoadDefault implements PrivilegedAction<Configurator> {

            @Override
            public Configurator run() {
                return Configurator.loadDefault();
            }
        }


        /**
         * 获取协商后的子协议
         *
         * @param supported 服务器支持的子协议列表
         * @param requested 客户端请求的子协议列表
         * @return 协商后的子协议
         */
        public String getNegotiatedSubprotocol(List<String> supported, List<String> requested) {
            return fetchContainerDefaultConfigurator().getNegotiatedSubprotocol(supported, requested);
        }

        /**
         * 获取协商后的扩展列表
         *
         * @param installed 服务器已安装的扩展列表
         * @param requested 客户端请求的扩展列表
         * @return 协商后的扩展列表
         */
        public List<Extension> getNegotiatedExtensions(List<Extension> installed, List<Extension> requested) {
            return fetchContainerDefaultConfigurator().getNegotiatedExtensions(installed, requested);
        }

        /**
         * 检查请求来源是否允许
         *
         * @param originHeaderValue Origin 请求头值
         * @return 如果允许返回 true，否则返回 false
         */
        public boolean checkOrigin(String originHeaderValue) {
            return fetchContainerDefaultConfigurator().checkOrigin(originHeaderValue);
        }

        /**
         * 修改握手信息
         *
         * @param sec      服务端端点配置
         * @param request  握手请求
         * @param response 握手响应
         */
        public void modifyHandshake(ServerEndpointConfig sec, HandshakeRequest request, HandshakeResponse response) {
            fetchContainerDefaultConfigurator().modifyHandshake(sec, request, response);
        }

        /**
         * 获取端点实例
         *
         * @param clazz 端点类
         * @param <T>   端点类型
         * @return 端点实例
         * @throws InstantiationException 如果实例化失败
         */
        public <T extends Object> T getEndpointInstance(Class<T> clazz) throws InstantiationException {
            return fetchContainerDefaultConfigurator().getEndpointInstance(clazz);
        }
    }
}
