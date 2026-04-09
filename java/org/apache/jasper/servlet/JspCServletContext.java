package org.apache.jasper.servlet;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Enumeration;
import java.util.EventListener;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;

import javax.servlet.Filter;
import javax.servlet.FilterRegistration;
import javax.servlet.FilterRegistration.Dynamic;
import javax.servlet.RequestDispatcher;
import javax.servlet.Servlet;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRegistration;
import javax.servlet.SessionCookieConfig;
import javax.servlet.SessionTrackingMode;
import javax.servlet.descriptor.JspConfigDescriptor;

import org.apache.jasper.Constants;
import org.apache.jasper.JasperException;
import org.apache.jasper.compiler.Localizer;
import org.apache.jasper.runtime.ExceptionUtils;
import org.apache.tomcat.Jar;
import org.apache.tomcat.JarScanType;
import org.apache.tomcat.util.descriptor.web.FragmentJarScannerCallback;
import org.apache.tomcat.util.descriptor.web.WebXml;
import org.apache.tomcat.util.descriptor.web.WebXmlParser;
import org.apache.tomcat.util.scan.JarFactory;
import org.apache.tomcat.util.scan.StandardJarScanFilter;
import org.apache.tomcat.util.scan.StandardJarScanner;


/**
 * JspC（JSP 预编译器）使用的简单 ServletContext 实现。
 * <p>
 * 此类实现了 ServletContext 接口，但仅提供 JspC 在命令行环境中预编译 JSP 所需的基本功能。
 * 它不包含 HTTP 特定的方法，主要用于在非 Web 容器环境中提供 Servlet 上下文支持。
 * <p>
 * 主要功能：
 * <ul>
 *   <li>管理 Servlet 上下文属性</li>
 *   <li>管理初始化参数</li>
 *   <li>提供资源访问功能</li>
 *   <li>解析和合并 web.xml 配置</li>
 *   <li>扫描 JAR 文件中的 web-fragment.xml</li>
 * </ul>
 *
 * @author Peter Rossbach (pr@webapp.de)
 */

public class JspCServletContext implements ServletContext {


    // ----------------------------------------------------- 实例变量


    /**
     * Servlet 上下文属性映射表。
     * 用于存储和检索 Servlet 上下文范围内的属性对象。
     */
    private final Map<String,Object> myAttributes;


    /**
     * Servlet 上下文初始化参数映射表。
     * 存储从 web.xml 或编程方式设置的初始化参数。
     */
    private final Map<String,String> myParameters = new ConcurrentHashMap<>();


    /**
     * 日志写入器，用于输出日志信息。
     */
    private final PrintWriter myLogWriter;


    /**
     * 资源基础 URL（文档根目录）。
     * 用于定位 Web 应用程序的资源文件。
     */
    private final URL myResourceBaseURL;


    /**
     * 合并后的 WebXml 配置对象。
     * 包含 web.xml 和 web-fragment.xml 合并后的配置信息。
     */
    private WebXml webXml;


    /**
     * 资源 JAR 文件列表。
     * 包含包含 META-INF/resources/ 目录的 JAR 文件 URL。
     */
    private List<URL> resourceJARs;


    /**
     * JSP 配置描述符。
     * 从 web.xml 中提取的 JSP 配置信息。
     */
    private JspConfigDescriptor jspConfigDescriptor;


    /**
     * Web 应用程序类加载器。
     */
    private final ClassLoader loader;


    // ----------------------------------------------------------- 构造方法

    /**
     * 创建此类的新实例。
     *
     * @param aLogWriter    用于 log() 方法的 PrintWriter
     * @param aResourceBaseURL 资源基础 URL
     * @param classLoader   此 ServletContext 的类加载器
     * @param validate      是否使用验证解析器解析 web.xml
     * @param blockExternal 解析 web.xml 时是否阻止外部实体
     * @throws JasperException 构建合并 web.xml 时发生错误
     */
    public JspCServletContext(PrintWriter aLogWriter, URL aResourceBaseURL,
            ClassLoader classLoader, boolean validate, boolean blockExternal)
            throws JasperException {

        myAttributes = new HashMap<>();
        myParameters.put(Constants.XML_BLOCK_EXTERNAL_INIT_PARAM,
                String.valueOf(blockExternal));
        myLogWriter = aLogWriter;
        myResourceBaseURL = aResourceBaseURL;
        this.loader = classLoader;
        this.webXml = buildMergedWebXml(validate, blockExternal);
        jspConfigDescriptor = webXml.getJspConfigDescriptor();
    }

    /**
     * 构建合并后的 WebXml 配置。
     * <p>
     * 此方法解析 web.xml 文件，并根据需要扫描和合并 web-fragment.xml。
     *
     * @param validate      是否使用验证解析器
     * @param blockExternal 是否阻止外部实体
     * @return 合并后的 WebXml 对象
     * @throws JasperException 解析或合并过程中发生错误
     */
    private WebXml buildMergedWebXml(boolean validate, boolean blockExternal)
            throws JasperException {
        WebXml webXml = new WebXml();
        WebXmlParser webXmlParser = new WebXmlParser(validate, validate, blockExternal);
        // 使用此类的类加载器，因为 Ant 会将 TCCL 设置为它自己的类加载器
        webXmlParser.setClassLoader(getClass().getClassLoader());

        try {
            URL url = getResource(
                    org.apache.tomcat.util.descriptor.web.Constants.WEB_XML_LOCATION);
            if (!webXmlParser.parseWebXml(url, webXml, false)) {
                throw new JasperException(Localizer.getMessage("jspc.error.invalidWebXml"));
            }
        } catch (IOException e) {
            throw new JasperException(e);
        }

        // 如果应用程序是 metadata-complete 的，则可以跳过片段处理
        if (webXml.isMetadataComplete()) {
            return webXml;
        }

        // 如果存在空的绝对排序元素，则可以跳过片段处理
        Set<String> absoluteOrdering = webXml.getAbsoluteOrdering();
        if (absoluteOrdering != null && absoluteOrdering.isEmpty()) {
            return webXml;
        }

        Map<String, WebXml> fragments = scanForFragments(webXmlParser);
        Set<WebXml> orderedFragments = WebXml.orderWebFragments(webXml, fragments, this);

        // 查找资源 JAR
        this.resourceJARs = scanForResourceJARs(orderedFragments, fragments.values());

        // JspC 不受注解影响，因此跳过注解处理，直接进行合并
        webXml.merge(orderedFragments);
        return webXml;
    }


    /**
     * 扫描资源 JAR 文件。
     * <p>
     * 查找包含 META-INF/resources/ 目录的 JAR 文件，这些 JAR 可以作为资源提供者。
     *
     * @param orderedFragments 已排序的 WebXml 片段集合
     * @param fragments        所有 WebXml 片段的集合
     * @return 资源 JAR 文件的 URL 列表
     * @throws JasperException 扫描过程中发生错误
     */
    private List<URL> scanForResourceJARs(Set<WebXml> orderedFragments, Collection<WebXml> fragments)
            throws JasperException {
        List<URL> resourceJars = new ArrayList<>();
        // 构建潜在资源 JAR 列表，使用与 ContextConfig 相同的排序
        Set<WebXml> resourceFragments = new LinkedHashSet<>(orderedFragments);
        for (WebXml fragment : fragments) {
            if (!resourceFragments.contains(fragment)) {
                resourceFragments.add(fragment);
            }
        }

        for (WebXml resourceFragment : resourceFragments) {
            try (Jar jar = JarFactory.newInstance(resourceFragment.getURL())) {
                if (jar.exists("META-INF/resources/")) {
                    // 这是一个资源 JAR
                    resourceJars.add(resourceFragment.getURL());
                }
            } catch (IOException ioe) {
                throw new JasperException(ioe);
            }
        }

        return resourceJars;
    }


    /**
     * 扫描 JAR 文件中的 web-fragment.xml。
     * <p>
     * 使用 StandardJarScanner 扫描类路径中的 JAR 文件，查找其中的 web-fragment.xml。
     *
     * @param webXmlParser WebXml 解析器
     * @return 名称到 WebXml 片段的映射
     * @throws JasperException 扫描过程中发生错误
     */
    private Map<String, WebXml> scanForFragments(WebXmlParser webXmlParser) throws JasperException {
        StandardJarScanner scanner = new StandardJarScanner();
        // TODO - 启用此选项意味着需要在 JspC 中首先初始化类加载器
        scanner.setScanClassPath(false);
        // TODO - 从 Ant 配置过滤器规则而不是系统属性
        scanner.setJarScanFilter(new StandardJarScanFilter());

        FragmentJarScannerCallback callback =
                new FragmentJarScannerCallback(webXmlParser, false, true);
        scanner.scan(JarScanType.PLUGGABILITY, this, callback);
        if (!callback.isOk()) {
            throw new JasperException(Localizer.getMessage("jspc.error.invalidFragment"));
        }
        return callback.getFragments();
    }


    // --------------------------------------------------------- 公共方法

    /**
     * 获取指定名称的 Servlet 上下文属性。
     *
     * @param name 属性名称
     * @return 属性对象，如果不存在则返回 null
     */
    @Override
    public Object getAttribute(String name) {
        return myAttributes.get(name);
    }


    /**
     * 获取所有 Servlet 上下文属性的名称枚举。
     *
     * @return 属性名称的枚举
     */
    @Override
    public Enumeration<String> getAttributeNames() {
        return Collections.enumeration(myAttributes.keySet());
    }


    /**
     * 获取指定 URI 路径的 ServletContext。
     * <p>
     * 此方法在此实现中始终返回 null，因为 JspC 不支持多上下文。
     *
     * @param uripath URI 路径
     * @return null
     */
    @Override
    public ServletContext getContext(String uripath) {
        return null;
    }


    /**
     * 获取 Servlet 上下文的上下文路径。
     * <p>
     * 此方法在此实现中始终返回 null。
     *
     * @return null
     */
    @Override
    public String getContextPath() {
        return null;
    }


    /**
     * 获取指定名称的初始化参数值。
     *
     * @param name 参数名称
     * @return 参数值，如果不存在则返回 null
     */
    @Override
    public String getInitParameter(String name) {
        return myParameters.get(name);
    }


    /**
     * 获取所有初始化参数名称的枚举。
     *
     * @return 参数名称的枚举
     */
    @Override
    public Enumeration<String> getInitParameterNames() {
        return Collections.enumeration(myParameters.keySet());
    }


    /**
     * 获取 Servlet 规范的主版本号。
     *
     * @return 主版本号，此处返回 4
     */
    @Override
    public int getMajorVersion() {
        return 4;
    }


    /**
     * 获取指定文件的 MIME 类型。
     * <p>
     * 此方法在此实现中始终返回 null。
     *
     * @param file 文件名
     * @return null
     */
    @Override
    public String getMimeType(String file) {
        return null;
    }


    /**
     * 获取 Servlet 规范的次版本号。
     *
     * @return 次版本号，此处返回 0
     */
    @Override
    public int getMinorVersion() {
        return 0;
    }


    /**
     * 获取指定名称的 RequestDispatcher。
     * <p>
     * 此方法在此实现中始终返回 null。
     *
     * @param name 名称
     * @return null
     */
    @Override
    public RequestDispatcher getNamedDispatcher(String name) {
        return null;
    }


    /**
     * 获取指定路径的真实文件系统路径。
     * <p>
     * 将虚拟路径转换为本地文件系统中的绝对路径。
     *
     * @param path 虚拟路径，必须以 "/" 开头
     * @return 绝对路径，如果无法转换则返回 null
     */
    @Override
    public String getRealPath(String path) {
        if (!myResourceBaseURL.getProtocol().equals("file")) {
            return null;
        }
        if (!path.startsWith("/")) {
            return null;
        }
        try {
            URL url = getResource(path);
            if (url == null) {
                return null;
            }
            File f = new File(url.toURI());
            return f.getAbsolutePath();
        } catch (Throwable t) {
            ExceptionUtils.handleThrowable(t);
            return null;
        }
    }


    /**
     * 获取指定路径的 RequestDispatcher。
     * <p>
     * 此方法在此实现中始终返回 null。
     *
     * @param path 路径
     * @return null
     */
    @Override
    public RequestDispatcher getRequestDispatcher(String path) {
        return null;
    }


    /**
     * 获取指定路径的资源 URL。
     * <p>
     * 首先尝试从资源基础 URL 获取，如果失败则从资源 JAR 中查找。
     *
     * @param path 资源路径，必须以 "/" 开头
     * @return 资源的 URL，如果不存在则返回 null
     * @throws MalformedURLException 如果 URL 格式错误
     */
    @Override
    public URL getResource(String path) throws MalformedURLException {

        if (!path.startsWith("/")) {
            throw new MalformedURLException(Localizer.getMessage("jsp.error.URLMustStartWithSlash", path));
        }

        // 去除开头的 '/'
        path = path.substring(1);

        URL url = null;
        try {
            URI uri = new URI(myResourceBaseURL.toExternalForm() + path);
            url = uri.toURL();
            try (InputStream is = url.openStream()) {
            }
        } catch (Throwable t) {
            ExceptionUtils.handleThrowable(t);
            url = null;
        }

        // 在初始化期间，getResource() 在 resourceJARs 初始化之前被调用
        if (url == null && resourceJARs != null) {
            String jarPath = "META-INF/resources/" + path;
            for (URL jarUrl : resourceJARs) {
                try (Jar jar = JarFactory.newInstance(jarUrl)) {
                    if (jar.exists(jarPath)) {
                        return new URI(jar.getURL(jarPath)).toURL();
                    }
                } catch (IOException | URISyntaxException ioe) {
                    // 忽略
                }
            }
        }
        return url;
    }


    /**
     * 获取指定路径的资源作为输入流。
     *
     * @param path 资源路径
     * @return 资源的输入流，如果不存在则返回 null
     */
    @Override
    public InputStream getResourceAsStream(String path) {
        try {
            URL url = getResource(path);
            if (url == null) {
                return null;
            }
            return url.openStream();
        } catch (Throwable t) {
            ExceptionUtils.handleThrowable(t);
            return null;
        }
    }


    /**
     * 获取指定路径下的所有资源路径集合。
     * <p>
     * 从本地文件系统和资源 JAR 中收集路径信息。
     *
     * @param path 目录路径，必须以 "/" 结尾
     * @return 子路径集合，如果不存在则返回空集合
     */
    @Override
    public Set<String> getResourcePaths(String path) {

        Set<String> thePaths = new HashSet<>();
        if (!path.endsWith("/")) {
            path += "/";
        }
        String basePath = getRealPath(path);
        if (basePath != null) {
            File theBaseDir = new File(basePath);
            if (theBaseDir.isDirectory()) {
                String theFiles[] = theBaseDir.list();
                if (theFiles != null) {
                    for (String theFile : theFiles) {
                        File testFile = new File(basePath + File.separator + theFile);
                        if (testFile.isFile()) {
                            thePaths.add(path + theFile);
                        } else if (testFile.isDirectory()) {
                            thePaths.add(path + theFile + "/");
                        }
                    }
                }
            }
        }

        // 在初始化期间，getResourcePaths() 在 resourceJARs 初始化之前被调用
        if (resourceJARs != null) {
            String jarPath = "META-INF/resources" + path;
            for (URL jarUrl : resourceJARs) {
                try (Jar jar = JarFactory.newInstance(jarUrl)) {
                    jar.nextEntry();
                    for (String entryName = jar.getEntryName();
                            entryName != null;
                            jar.nextEntry(), entryName = jar.getEntryName()) {
                        if (entryName.startsWith(jarPath) &&
                                entryName.length() > jarPath.length()) {
                            // 让 Set 实现处理重复项
                            int sep = entryName.indexOf('/', jarPath.length());
                            if (sep < 0) {
                                // 这是一个文件 - 去除开头的 "META-INF/resources"
                                thePaths.add(entryName.substring(18));
                            } else {
                                // 这是一个目录 - 去除开头的 "META-INF/resources"
                                thePaths.add(entryName.substring(18, sep + 1));
                            }
                        }
                    }
                } catch (IOException e) {
                    log(e.getMessage(), e);
                }
            }
        }

        return thePaths;
    }


    /**
     * 获取服务器信息字符串。
     *
     * @return 服务器信息，格式为 "JspC/ApacheTomcat9"
     */
    @Override
    public String getServerInfo() {
        return "JspC/ApacheTomcat9";
    }


    /**
     * 获取指定名称的 Servlet。
     * <p>
     * 此方法在此实现中始终返回 null。
     *
     * @param name Servlet 名称
     * @return null
     * @throws ServletException 如果发生错误
     * @deprecated 此方法已弃用，没有替代方法
     */
    @Override
    @Deprecated
    public Servlet getServlet(String name) throws ServletException {
        return null;
    }


    /**
     * 获取 Servlet 上下文名称。
     *
     * @return 服务器信息字符串
     */
    @Override
    public String getServletContextName() {
        return getServerInfo();
    }


    /**
     * 获取所有 Servlet 名称的枚举。
     *
     * @return 空的枚举
     * @deprecated 此方法已弃用
     */
    @Override
    @Deprecated
    public Enumeration<String> getServletNames() {
        return new Vector<String>().elements();
    }


    /**
     * 获取所有 Servlet 的枚举。
     *
     * @return 空的枚举
     * @deprecated 此方法已弃用
     */
    @Override
    @Deprecated
    public Enumeration<Servlet> getServlets() {
        return new Vector<Servlet>().elements();
    }


    /**
     * 记录日志消息。
     *
     * @param message 日志消息
     */
    @Override
    public void log(String message) {
        myLogWriter.println(message);
    }


    /**
     * 记录异常和日志消息。
     *
     * @param exception 异常
     * @param message   日志消息
     * @deprecated 建议使用 {@link #log(String, Throwable)}
     */
    @Override
    @Deprecated
    public void log(Exception exception, String message) {
        log(message, exception);
    }


    /**
     * 记录日志消息和异常堆栈跟踪。
     *
     * @param message   日志消息
     * @param exception 异常
     */
    @Override
    public void log(String message, Throwable exception) {
        myLogWriter.println(message);
        exception.printStackTrace(myLogWriter);
    }


    /**
     * 移除指定名称的 Servlet 上下文属性。
     *
     * @param name 属性名称
     */
    @Override
    public void removeAttribute(String name) {
        myAttributes.remove(name);
    }


    /**
     * 设置 Servlet 上下文属性。
     *
     * @param name  属性名称
     * @param value 属性值
     */
    @Override
    public void setAttribute(String name, Object value) {
        myAttributes.put(name, value);
    }


    /**
     * 添加过滤器。
     * <p>
     * 此方法在此实现中始终返回 null。
     *
     * @param filterName 过滤器名称
     * @param className  过滤器类名
     * @return null
     */
    @Override
    public FilterRegistration.Dynamic addFilter(String filterName,
            String className) {
        return null;
    }


    /**
     * 添加 Servlet。
     * <p>
     * 此方法在此实现中始终返回 null。
     *
     * @param servletName Servlet 名称
     * @param className   Servlet 类名
     * @return null
     */
    @Override
    public ServletRegistration.Dynamic addServlet(String servletName,
            String className) {
        return null;
    }


    /**
     * 获取默认的会话跟踪模式。
     *
     * @return 空的会话跟踪模式集合
     */
    @Override
    public Set<SessionTrackingMode> getDefaultSessionTrackingModes() {
        return EnumSet.noneOf(SessionTrackingMode.class);
    }


    /**
     * 获取有效的会话跟踪模式。
     *
     * @return 空的会话跟踪模式集合
     */
    @Override
    public Set<SessionTrackingMode> getEffectiveSessionTrackingModes() {
        return EnumSet.noneOf(SessionTrackingMode.class);
    }


    /**
     * 获取会话 Cookie 配置。
     *
     * @return null
     */
    @Override
    public SessionCookieConfig getSessionCookieConfig() {
        return null;
    }


    /**
     * 设置会话跟踪模式。
     *
     * @param sessionTrackingModes 会话跟踪模式集合
     */
    @Override
    public void setSessionTrackingModes(
            Set<SessionTrackingMode> sessionTrackingModes) {
        // 不执行任何操作
    }


    /**
     * 添加过滤器。
     *
     * @param filterName 过滤器名称
     * @param filter     过滤器实例
     * @return null
     */
    @Override
    public Dynamic addFilter(String filterName, Filter filter) {
        return null;
    }


    /**
     * 添加过滤器。
     *
     * @param filterName   过滤器名称
     * @param filterClass  过滤器类
     * @return null
     */
    @Override
    public Dynamic addFilter(String filterName,
            Class<? extends Filter> filterClass) {
        return null;
    }


    /**
     * 添加 Servlet。
     *
     * @param servletName Servlet 名称
     * @param servlet     Servlet 实例
     * @return null
     */
    @Override
    public ServletRegistration.Dynamic addServlet(String servletName,
            Servlet servlet) {
        return null;
    }


    /**
     * 添加 Servlet。
     *
     * @param servletName   Servlet 名称
     * @param servletClass  Servlet 类
     * @return null
     */
    @Override
    public ServletRegistration.Dynamic addServlet(String servletName,
            Class<? extends Servlet> servletClass) {
        return null;
    }


    /**
     * 添加 JSP 文件作为 Servlet。
     *
     * @param jspName  JSP 名称
     * @param jspFile  JSP 文件路径
     * @return null
     */
    @Override
    public ServletRegistration.Dynamic addJspFile(String jspName, String jspFile) {
        return null;
    }


    /**
     * 创建过滤器实例。
     *
     * @param c 过滤器类
     * @return null
     * @throws ServletException 如果创建失败
     */
    @Override
    public <T extends Filter> T createFilter(Class<T> c)
            throws ServletException {
        return null;
    }


    /**
     * 创建 Servlet 实例。
     *
     * @param c Servlet 类
     * @return null
     * @throws ServletException 如果创建失败
     */
    @Override
    public <T extends Servlet> T createServlet(Class<T> c)
            throws ServletException {
        return null;
    }


    /**
     * 获取过滤器注册信息。
     *
     * @param filterName 过滤器名称
     * @return null
     */
    @Override
    public FilterRegistration getFilterRegistration(String filterName) {
        return null;
    }


    /**
     * 获取 Servlet 注册信息。
     *
     * @param servletName Servlet 名称
     * @return null
     */
    @Override
    public ServletRegistration getServletRegistration(String servletName) {
        return null;
    }


    /**
     * 设置初始化参数。
     *
     * @param name  参数名称
     * @param value 参数值
     * @return 如果参数已存在则返回 false，否则返回 true
     */
    @Override
    public boolean setInitParameter(String name, String value) {
        return myParameters.putIfAbsent(name, value) == null;
    }


    /**
     * 添加监听器类。
     *
     * @param listenerClass 监听器类
     */
    @Override
    public void addListener(Class<? extends EventListener> listenerClass) {
        // 不执行任何操作
    }


    /**
     * 添加监听器类。
     *
     * @param className 监听器类名
     */
    @Override
    public void addListener(String className) {
        // 不执行任何操作
    }


    /**
     * 添加监听器实例。
     *
     * @param t 监听器实例
     */
    @Override
    public <T extends EventListener> void addListener(T t) {
        // 不执行任何操作
    }


    /**
     * 创建监听器实例。
     *
     * @param c 监听器类
     * @return null
     * @throws ServletException 如果创建失败
     */
    @Override
    public <T extends EventListener> T createListener(Class<T> c)
            throws ServletException {
        return null;
    }


    /**
     * 声明安全角色。
     *
     * @param roleNames 角色名称列表
     */
    @Override
    public void declareRoles(String... roleNames) {
        // 不执行任何操作
    }


    /**
     * 获取类加载器。
     *
     * @return Web 应用程序类加载器
     */
    @Override
    public ClassLoader getClassLoader() {
        return loader;
    }


    /**
     * 获取有效的主版本号。
     *
     * @return web.xml 中定义的主版本号
     */
    @Override
    public int getEffectiveMajorVersion() {
        return webXml.getMajorVersion();
    }


    /**
     * 获取有效的次版本号。
     *
     * @return web.xml 中定义的次版本号
     */
    @Override
    public int getEffectiveMinorVersion() {
        return webXml.getMinorVersion();
    }


    /**
     * 获取所有过滤器注册信息。
     *
     * @return null
     */
    @Override
    public Map<String, ? extends FilterRegistration> getFilterRegistrations() {
        return null;
    }


    /**
     * 获取 JSP 配置描述符。
     *
     * @return JSP 配置描述符
     */
    @Override
    public JspConfigDescriptor getJspConfigDescriptor() {
        return jspConfigDescriptor;
    }


    /**
     * 获取所有 Servlet 注册信息。
     *
     * @return null
     */
    @Override
    public Map<String, ? extends ServletRegistration> getServletRegistrations() {
        return null;
    }


    /**
     * 获取虚拟服务器名称。
     *
     * @return null
     */
    @Override
    public String getVirtualServerName() {
        return null;
    }

    /**
     * 获取会话超时时间（分钟）。
     *
     * @return 0
     */
    @Override
    public int getSessionTimeout() {
        return 0;
    }

    /**
     * 设置会话超时时间。
     *
     * @param sessionTimeout 超时时间（分钟）
     */
    @Override
    public void setSessionTimeout(int sessionTimeout) {
        // 不执行任何操作
    }

    /**
     * 获取请求字符编码。
     *
     * @return null
     */
    @Override
    public String getRequestCharacterEncoding() {
        return null;
    }

    /**
     * 设置请求字符编码。
     *
     * @param encoding 字符编码
     */
    @Override
    public void setRequestCharacterEncoding(String encoding) {
        // 不执行任何操作
    }

    /**
     * 获取响应字符编码。
     *
     * @return null
     */
    @Override
    public String getResponseCharacterEncoding() {
        return null;
    }

    /**
     * 设置响应字符编码。
     *
     * @param encoding 字符编码
     */
    @Override
    public void setResponseCharacterEncoding(String encoding) {
        // 不执行任何操作
    }
}
