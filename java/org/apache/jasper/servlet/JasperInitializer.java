package org.apache.jasper.servlet;

import java.io.IOException;
import java.util.Set;

import javax.servlet.ServletContainerInitializer;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.jsp.JspFactory;

import org.apache.jasper.Constants;
import org.apache.jasper.compiler.Localizer;
import org.apache.jasper.compiler.TldCache;
import org.apache.jasper.runtime.JspFactoryImpl;
import org.apache.jasper.security.SecurityClassLoad;
import org.apache.juli.logging.Log;
import org.apache.juli.logging.LogFactory;
import org.apache.tomcat.InstanceManager;
import org.apache.tomcat.SimpleInstanceManager;
import org.xml.sax.SAXException;

/**
 * JasperInitializer 是 Jasper JSP 引擎的初始化器。
 * <p>
 * 该类实现了 {@link ServletContainerInitializer} 接口，在 Servlet 容器启动时自动被调用，
 * 负责初始化 Jasper JSP 引擎所需的环境和组件。
 * </p>
 * <p>
 * 主要功能包括：
 * <ul>
 *   <li>预加载 JSP Servlet 运行时所需的类，避免安全异常</li>
 *   <li>设置默认的 InstanceManager 实例管理器</li>
 *   <li>扫描应用程序中的 TLD（Tag Library Descriptor）文件</li>
 *   <li>注册 TLD 中定义的监听器</li>
 *   <li>创建并缓存 TLD 信息</li>
 * </ul>
 * </p>
 *
 * @author Apache Software Foundation
 * @see ServletContainerInitializer
 */
public class JasperInitializer implements ServletContainerInitializer {

    private static final String MSG = "org.apache.jasper.servlet.JasperInitializer";
    private final Log log = LogFactory.getLog(JasperInitializer.class); // 不能是静态的，因为需要与具体实例关联

    /**
     * 静态代码块：预加载 JSP Servlet 运行时所需的类。
     * <p>
     * 这样做是为了避免在运行时出现 defineClassInPackage 安全异常。
     * 同时初始化并设置默认的 JspFactory 实例。
     * </p>
     */
    static {
        JspFactoryImpl factory = new JspFactoryImpl();
        SecurityClassLoad.securityClassLoad(factory.getClass().getClassLoader());
        if (JspFactory.getDefaultFactory() == null) {
            JspFactory.setDefaultFactory(factory);
        }
    }

    /**
     * 在 Servlet 容器启动时执行 Jasper JSP 引擎的初始化工作。
     * <p>
     * 此方法由 Servlet 容器自动调用，执行以下初始化任务：
     * <ol>
     *   <li>记录调试日志</li>
     *   <li>设置默认的 InstanceManager（如果尚未设置）</li>
     *   <li>读取 XML 验证和外部实体阻塞的配置参数</li>
     *   <li>扫描应用程序中的所有 TLD 文件</li>
     *   <li>注册 TLD 中定义的所有监听器</li>
     *   <li>创建并缓存 TLD 信息到 ServletContext 属性中</li>
     * </ol>
     * </p>
     *
     * @param types   WebApplicationInitializer 感兴趣的类集合（通过 @HandlesTypes 注解指定），
     *                此实现中未使用该参数
     * @param context 当前 Web 应用的 ServletContext 对象，用于获取配置信息和设置属性
     * @throws ServletException 当初始化过程中发生 I/O 错误或 XML 解析错误时抛出
     */
    @Override
    public void onStartup(Set<Class<?>> types, ServletContext context) throws ServletException {
        if (log.isDebugEnabled()) {
            log.debug(Localizer.getMessage(MSG + ".onStartup", context.getServletContextName()));
        }

        // 设置一个简单的默认 InstanceManager
        if (context.getAttribute(InstanceManager.class.getName()) == null) {
            context.setAttribute(InstanceManager.class.getName(), new SimpleInstanceManager());
        }

        // 从 ServletContext 初始化参数中读取 XML 验证配置
        boolean validate = Boolean.parseBoolean(
                context.getInitParameter(Constants.XML_VALIDATION_TLD_INIT_PARAM));
        // 从 ServletContext 初始化参数中读取是否阻塞外部实体配置
        String blockExternalString = context.getInitParameter(
                Constants.XML_BLOCK_EXTERNAL_INIT_PARAM);
        boolean blockExternal;
        if (blockExternalString == null) {
            blockExternal = true;
        } else {
            blockExternal = Boolean.parseBoolean(blockExternalString);
        }

        // 扫描应用程序中的所有 TLD 文件
        TldScanner scanner = newTldScanner(context, true, validate, blockExternal);
        try {
            scanner.scan();
        } catch (IOException | SAXException e) {
            throw new ServletException(e);
        }

        // 添加 TLD 中定义的所有监听器
        for (String listener : scanner.getListeners()) {
            context.addListener(listener);
        }

        // 创建 TldCache 并设置到 ServletContext 属性中，供后续 JSP 处理使用
        context.setAttribute(TldCache.SERVLET_CONTEXT_ATTRIBUTE_NAME,
                new TldCache(context, scanner.getUriTldResourcePathMap(),
                        scanner.getTldResourcePathTaglibXmlMap()));
    }

    /**
     * 创建并返回一个新的 TldScanner 实例。
     * <p>
     * 该方法为受保护方法，允许子类重写以提供自定义的 TLD 扫描器实现。
     * </p>
     *
     * @param context        当前 Web 应用的 ServletContext 对象，用于访问 Web 应用资源
     * @param namespaceAware 是否启用命名空间感知，true 表示启用 XML 命名空间处理
     * @param validate       是否验证 TLD 文件，true 表示启用 XML 验证
     * @param blockExternal  是否阻塞外部实体引用，true 表示禁止访问外部实体（安全建议）
     * @return 新创建的 TldScanner 实例，用于扫描 TLD 文件
     */
    protected TldScanner newTldScanner(ServletContext context, boolean namespaceAware,
            boolean validate, boolean blockExternal) {
        return new TldScanner(context, namespaceAware, validate, blockExternal);
    }
}
