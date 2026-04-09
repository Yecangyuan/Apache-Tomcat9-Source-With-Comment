package org.apache.jasper.servlet;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.net.MalformedURLException;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.jasper.Constants;
import org.apache.jasper.EmbeddedServletOptions;
import org.apache.jasper.Options;
import org.apache.jasper.compiler.JspRuntimeContext;
import org.apache.jasper.compiler.Localizer;
import org.apache.jasper.runtime.ExceptionUtils;
import org.apache.jasper.security.SecurityUtil;
import org.apache.juli.logging.Log;
import org.apache.juli.logging.LogFactory;
import org.apache.tomcat.PeriodicEventListener;
import org.apache.tomcat.util.security.Escape;

/**
 * Jasper JSP 引擎的主 Servlet 类。
 * <p>
 * 这是 Apache Tomcat JSP 引擎（Jasper）的核心入口类，负责处理所有 JSP 页面的请求。
 * Servlet 容器需要为 Jasper 提供一个 URLClassLoader 用于 Web 应用的类加载。
 * Jasper 会尝试从 Tomcat 的 ServletContext 属性中获取类加载器，如果失败则使用父类加载器。
 * 无论哪种情况，类加载器都必须是 URLClassLoader 类型。
 * </p>
 * <p>
 * 主要职责：
 * <ul>
 *   <li>初始化 JSP 运行时环境</li>
 *   <li>处理 JSP 预编译请求</li>
 *   <li>管理 JSP 页面的编译和加载</li>
 *   <li>提供监控统计信息（JSP 数量、重载次数等）</li>
 * </ul>
 * </p>
 *
 * @author Anil K. Vijendran
 * @author Harish Prabandham
 * @author Remy Maucherat
 * @author Kin-man Chung
 * @author Glenn Nielsen
 */
public class JspServlet extends HttpServlet implements PeriodicEventListener {

    private static final long serialVersionUID = 1L;

    // 日志记录器
    private final transient Log log = LogFactory.getLog(JspServlet.class);

    // Servlet 上下文
    private transient ServletContext context;
    // Servlet 配置
    private ServletConfig config;
    // JSP 引擎配置选项
    private transient Options options;
    // JSP 运行时上下文，管理所有 JSP 页面的编译和加载
    private transient JspRuntimeContext rctxt;
    // 显式配置为 Servlet 的 JSP 文件路径（通过 init-param 传入）
    private String jspFile;


    /**
     * 初始化 JspServlet。
     * <p>
     * 此方法完成以下工作：
     * <ul>
     *   <li>初始化 Servlet 上下文和配置</li>
     *   <li>创建 Options 实例（支持自定义配置类）</li>
     *   <li>创建 JspRuntimeContext 运行时上下文</li>
     *   <li>如果配置了 jspFile 参数，执行预编译</li>
     * </ul>
     * </p>
     *
     * @param config Servlet 配置对象
     * @throws ServletException 当初始化失败时抛出
     */
    @Override
    public void init(ServletConfig config) throws ServletException {

        super.init(config);
        this.config = config;
        this.context = config.getServletContext();

        // 初始化 JSP 运行时上下文
        // 检查是否配置了自定义的 Options 实现类
        String engineOptionsName = config.getInitParameter("engineOptionsClass");
        if (Constants.IS_SECURITY_ENABLED && engineOptionsName != null) {
            log.info(Localizer.getMessage(
                    "jsp.info.ignoreSetting", "engineOptionsClass", engineOptionsName));
            engineOptionsName = null;
        }
        if (engineOptionsName != null) {
            // 实例化指定的 Options 实现类
            try {
                ClassLoader loader = Thread.currentThread().getContextClassLoader();
                Class<?> engineOptionsClass = loader.loadClass(engineOptionsName);
                Class<?>[] ctorSig = { ServletConfig.class, ServletContext.class };
                Constructor<?> ctor = engineOptionsClass.getConstructor(ctorSig);
                Object[] args = { config, context };
                options = (Options) ctor.newInstance(args);
            } catch (Throwable e) {
                e = ExceptionUtils.unwrapInvocationTargetException(e);
                ExceptionUtils.handleThrowable(e);
                // 需要本地化此消息
                log.warn(Localizer.getMessage("jsp.warning.engineOptionsClass", engineOptionsName), e);
                // 使用默认的 Options 实现
                options = new EmbeddedServletOptions(config, context);
            }
        } else {
            // 使用默认的 Options 实现
            options = new EmbeddedServletOptions(config, context);
        }
        rctxt = new JspRuntimeContext(context, options);
        if (config.getInitParameter("jspFile") != null) {
            jspFile = config.getInitParameter("jspFile");
            try {
                if (null == context.getResource(jspFile)) {
                    return;
                }
            } catch (MalformedURLException e) {
                throw new ServletException(Localizer.getMessage("jsp.error.no.jsp", jspFile), e);
            }
            try {
                if (SecurityUtil.isPackageProtectionEnabled()){
                   AccessController.doPrivileged((PrivilegedExceptionAction<Object>) () -> {
                       serviceJspFile(null, null, jspFile, true);
                       return null;
                   });
                } else {
                    serviceJspFile(null, null, jspFile, true);
                }
            } catch (IOException e) {
                throw new ServletException(Localizer.getMessage("jsp.error.precompilation", jspFile), e);
            } catch (PrivilegedActionException e) {
                Throwable t = e.getCause();
                if (t instanceof ServletException) {
                    throw (ServletException)t;
                }
                throw new ServletException(Localizer.getMessage("jsp.error.precompilation", jspFile), e);
            }
        }

        if (log.isDebugEnabled()) {
            log.debug(Localizer.getMessage("jsp.message.scratch.dir.is",
                    options.getScratchDir().toString()));
            log.debug(Localizer.getMessage("jsp.message.dont.modify.servlets"));
        }
    }


    /**
     * 获取已加载的 JSP 页面数量。
     * <p>
     * 返回当前存在 JspServletWrapper 的 JSP 页面数量，
     * 即已加载到与该 JspServlet 关联的 Web 应用中的 JSP 页面数量。
     * </p>
     * <p>此信息可用于监控目的。</p>
     *
     * @return 已加载的 JSP 页面数量
     */
    public int getJspCount() {
        return this.rctxt.getJspCount();
    }


    /**
     * 重置 JSP 重载计数器。
     *
     * @param count 要将 JSP 重载计数器重置到的值
     */
    public void setJspReloadCount(int count) {
        this.rctxt.setJspReloadCount(count);
    }


    /**
     * 获取已重载的 JSP 页面数量。
     * <p>此信息可用于监控目的。</p>
     *
     * @return 已重载的 JSP 页面数量
     */
    public int getJspReloadCount() {
        return this.rctxt.getJspReloadCount();
    }


    /**
     * 获取 JSP 限制器队列中的 JSP 页面数量。
     * <p>此信息可用于监控目的。</p>
     *
     * @return 在 JSP 限制器队列中的 JSP 页面数量
     */
    public int getJspQueueLength() {
        return this.rctxt.getJspQueueLength();
    }


    /**
     * 获取已卸载的 JSP 页面数量。
     * <p>此信息可用于监控目的。</p>
     *
     * @return 已卸载的 JSP 页面数量
     */
    public int getJspUnloadCount() {
        return this.rctxt.getJspUnloadCount();
    }


    /**
     * <p>检查是否为 JSP 预编译请求（如 JSP 1.2 规范第 8.4.2 节所述）。
     * <strong>警告</strong> - 我们不能使用 <code>request.getParameter()</code> 来检查，
     * 因为那会触发解析所有请求参数，而不会给 Servlet 机会先调用
     * <code>request.setCharacterEncoding()</code>。</p>
     *
     * @param request 正在处理的 Servlet 请求
     * @return 如果是预编译请求则返回 true，否则返回 false
     * @throws ServletException 当为 <code>jsp_precompile</code> 参数指定了无效值时抛出
     */
    boolean preCompile(HttpServletRequest request) throws ServletException {

        String queryString = request.getQueryString();
        if (queryString == null) {
            return false;
        }
        int start = queryString.indexOf(Constants.PRECOMPILE);
        if (start < 0) {
            return false;
        }
        queryString =
            queryString.substring(start + Constants.PRECOMPILE.length());
        if (queryString.length() == 0) {
            return true;             // ?jsp_precompile
        }
        if (queryString.startsWith("&")) {
            return true;             // ?jsp_precompile&foo=bar...
        }
        if (!queryString.startsWith("=")) {
            return false;            // 是其他名称或值的一部分
        }
        int limit = queryString.length();
        int ampersand = queryString.indexOf('&');
        if (ampersand > 0) {
            limit = ampersand;
        }
        String value = queryString.substring(1, limit);
        if (value.equals("true")) {
            return true;             // ?jsp_precompile=true
        } else if (value.equals("false")) {
            // 规范说明如果 jsp_precompile=false，请求不应该传递给 JSP 页面；
            // 最简单的实现方式是将标志设为 true，仍然预编译页面。
            // 这仍然符合规范，因为规范说可以忽略预编译请求。
            return true;             // ?jsp_precompile=false
        } else {
            throw new ServletException(Localizer.getMessage("jsp.error.precompilation.parameter",
                    Constants.PRECOMPILE, value));
        }

    }


    /**
     * 处理 JSP 请求的核心方法。
     * <p>
     * 此方法解析请求中的 JSP 路径，检查是否为预编译请求，
     * 然后调用 serviceJspFile 方法处理具体的 JSP 文件。
     * </p>
     *
     * @param request  HTTP 请求对象
     * @param response HTTP 响应对象
     * @throws ServletException 当处理过程中发生 Servlet 异常时抛出
     * @throws IOException      当发生 I/O 错误时抛出
     */
    @Override
    public void service (HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        // jspFile 可能作为此 Servlet 实例的 init-param 配置
        String jspUri = jspFile;

        if (jspUri == null) {
            /*
             * 检查请求的 JSP 是否是 RequestDispatcher.include() 的目标
             */
            jspUri = (String) request.getAttribute(
                    RequestDispatcher.INCLUDE_SERVLET_PATH);
            if (jspUri != null) {
                /*
                 * 请求的 JSP 是 RequestDispatcher.include() 的目标。
                 * 其路径由相关的 javax.servlet.include.* 请求属性组装而成
                 */
                String pathInfo = (String) request.getAttribute(
                        RequestDispatcher.INCLUDE_PATH_INFO);
                if (pathInfo != null) {
                    jspUri += pathInfo;
                }
            } else {
                /*
                 * 请求的 JSP 不是 RequestDispatcher.include() 的目标。
                 * 从请求的 getServletPath() 和 getPathInfo() 重构其路径
                 */
                jspUri = request.getServletPath();
                String pathInfo = request.getPathInfo();
                if (pathInfo != null) {
                    jspUri += pathInfo;
                }
            }
        }

        if (log.isTraceEnabled()) {
            log.trace("JspEngine --> " + jspUri);
            log.trace("\t     ServletPath: " + request.getServletPath());
            log.trace("\t        PathInfo: " + request.getPathInfo());
            log.trace("\t        RealPath: " + context.getRealPath(jspUri));
            log.trace("\t      RequestURI: " + request.getRequestURI());
            log.trace("\t     QueryString: " + request.getQueryString());
        }

        try {
            boolean precompile = preCompile(request);
            serviceJspFile(request, response, jspUri, precompile);
        } catch (RuntimeException | IOException | ServletException e) {
            throw e;
        } catch (Throwable e) {
            ExceptionUtils.handleThrowable(e);
            throw new ServletException(e);
        }

    }

    /**
     * 销毁 JspServlet，释放所有资源。
     * 调用 JspRuntimeContext 的 destroy 方法进行清理。
     */
    @Override
    public void destroy() {
        if (log.isTraceEnabled()) {
            log.trace("JspServlet.destroy()");
        }

        rctxt.destroy();
    }


    /**
     * 周期性事件处理方法（实现 PeriodicEventListener 接口）。
     * <p>
     * 此方法由容器定期调用，用于：
     * <ul>
     *   <li>检查并卸载长时间未使用的 JSP 页面</li>
     *   <li>检查并编译已修改的 JSP 页面</li>
     * </ul>
     * </p>
     */
    @Override
    public void periodicEvent() {
        rctxt.checkUnload();
        rctxt.checkCompile();
    }

    // -------------------------------------------------------- 私有方法

    /**
     * 处理指定的 JSP 文件请求。
     * <p>
     * 此方法负责获取或创建 JspServletWrapper，
     * 并调用 wrapper 的 service 方法处理请求。
     * </p>
     *
     * @param request    HTTP 请求对象
     * @param response   HTTP 响应对象
     * @param jspUri     JSP 文件的路径
     * @param precompile 是否为预编译请求
     * @throws ServletException 当处理过程中发生 Servlet 异常时抛出
     * @throws IOException      当发生 I/O 错误时抛出
     */
    private void serviceJspFile(HttpServletRequest request,
                                HttpServletResponse response, String jspUri,
                                boolean precompile)
        throws ServletException, IOException {

        JspServletWrapper wrapper = rctxt.getWrapper(jspUri);
        if (wrapper == null) {
            synchronized(this) {
                wrapper = rctxt.getWrapper(jspUri);
                if (wrapper == null) {
                    // 检查请求的 JSP 页面是否存在，以避免创建不必要的目录和文件
                    if (null == context.getResource(jspUri)) {
                        handleMissingResource(request, response, jspUri);
                        return;
                    }
                    wrapper = new JspServletWrapper(config, options, jspUri,
                                                    rctxt);
                    rctxt.addWrapper(jspUri,wrapper);
                }
            }
        }

        try {
            wrapper.service(request, response, precompile);
        } catch (FileNotFoundException fnfe) {
            handleMissingResource(request, response, jspUri);
        }

    }


    /**
     * 处理缺失的 JSP 资源。
     * <p>
     * 如果资源是通过 RequestDispatcher.include() 包含的，则抛出 ServletException；
     * 否则返回 404 错误响应。
     * </p>
     *
     * @param request  HTTP 请求对象
     * @param response HTTP 响应对象
     * @param jspUri   缺失的 JSP 文件路径
     * @throws ServletException 当资源是包含请求时抛出
     * @throws IOException      当发生 I/O 错误时抛出
     */
    private void handleMissingResource(HttpServletRequest request,
            HttpServletResponse response, String jspUri)
            throws ServletException, IOException {

        String includeRequestUri =
            (String)request.getAttribute(RequestDispatcher.INCLUDE_REQUEST_URI);

        String msg = Localizer.getMessage("jsp.error.file.not.found",jspUri);
        if (includeRequestUri != null) {
            // 此文件是被包含的。抛出异常，因为 response.sendError() 会被忽略
            // 严格来说，过滤这是应用层的责任，但以防万一...
            throw new ServletException(Escape.htmlElementContent(msg));
        } else {
            try {
                response.sendError(HttpServletResponse.SC_NOT_FOUND, msg);
            } catch (IllegalStateException ise) {
                log.error(msg);
            }
        }
    }


}
