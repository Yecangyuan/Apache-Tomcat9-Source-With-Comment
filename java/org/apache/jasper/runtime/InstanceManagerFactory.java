package org.apache.jasper.runtime;

import javax.servlet.ServletConfig;

import org.apache.jasper.compiler.Localizer;
import org.apache.tomcat.InstanceManager;

/**
 * InstanceManager 工厂类。
 * <p>
 * 用于从 ServletContext 中获取 InstanceManager 实例的工厂类。
 * InstanceManager 负责管理 JSP 页面中使用的对象实例的创建和销毁。
 * </p>
 *
 * @author 作者未详
 * @since Tomcat 版本未详
 */
public class InstanceManagerFactory {

    /**
     * 私有构造方法，防止外部实例化。
     * <p>
     * 这是一个工具类/工厂类，不需要创建实例。
     * </p>
     */
    private InstanceManagerFactory() {
    }

    /**
     * 从 ServletConfig 中获取 InstanceManager 实例。
     * <p>
     * 该方法从 ServletContext 的属性中检索 InstanceManager 实例。
     * 如果找不到 InstanceManager，则抛出 IllegalStateException 异常。
     * </p>
     *
     * @param config ServletConfig 对象，用于访问 ServletContext
     * @return InstanceManager 实例
     * @throws IllegalStateException 如果 ServletContext 中没有设置 InstanceManager
     */
    public static InstanceManager getInstanceManager(ServletConfig config) {
        InstanceManager instanceManager =
                (InstanceManager) config.getServletContext().getAttribute(InstanceManager.class.getName());
        if (instanceManager == null) {
            throw new IllegalStateException(Localizer.getMessage("jsp.error.noInstanceManager"));
        }
        return instanceManager;
    }

}
