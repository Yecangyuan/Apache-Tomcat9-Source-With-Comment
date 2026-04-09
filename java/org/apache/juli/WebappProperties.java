package org.apache.juli;

/**
 * 用于类加载器的接口，提供Web应用信息给JULI。
 * 使与Web应用关联的类加载器能够向JULI提供有关该Web应用的附加信息。
 * 对于任何Web应用，{@link #getWebappName()}、{@link #getHostName()} 和
 * {@link #getServiceName()} 的组合必须是唯一的。
 */
public interface WebappProperties {

    /**
     * 获取Web应用名称。
     *
     * @return 用于Web应用的名称，如果没有可用名称则返回null。
     */
    String getWebappName();

    /**
     * 获取主机名称。
     *
     * @return Web应用部署所在的主机名称，如果没有可用名称则返回null。
     */
    String getHostName();

    /**
     * 获取服务名称。
     *
     * @return Web应用部署所在的服务名称，如果没有可用名称则返回null。
     */
    String getServiceName();

    /**
     * 检查是否有日志配置。
     * 使JULI能够确定Web应用是否包含本地配置，而无需JULI查找文件
     * （在SecurityManager下运行时可能没有权限查找文件）。
     *
     * @return 如果Web应用在标准位置 /WEB-INF/classes/logging.properties
     *         包含日志配置，则返回 {@code true}。
     */
    boolean hasLoggingConfig();
}
