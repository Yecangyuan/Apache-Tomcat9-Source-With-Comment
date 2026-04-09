package org.apache.jasper.servlet;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.RequestDispatcher;
import javax.servlet.Servlet;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.SingleThreadModel;
import javax.servlet.UnavailableException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.jsp.tagext.TagInfo;

import org.apache.jasper.JasperException;
import org.apache.jasper.JspCompilationContext;
import org.apache.jasper.Options;
import org.apache.jasper.compiler.JavacErrorDetail;
import org.apache.jasper.compiler.JspRuntimeContext;
import org.apache.jasper.compiler.Localizer;
import org.apache.jasper.compiler.SmapInput;
import org.apache.jasper.compiler.SmapStratum;
import org.apache.jasper.runtime.ExceptionUtils;
import org.apache.jasper.runtime.InstanceManagerFactory;
import org.apache.jasper.runtime.JspSourceDependent;
import org.apache.jasper.util.FastRemovalDequeue;
import org.apache.juli.logging.Log;
import org.apache.juli.logging.LogFactory;
import org.apache.tomcat.InstanceManager;
import org.apache.tomcat.Jar;

/**
 * JspServletWrapper - JSP 页面和标签文件的核心包装器类。
 * <p>
 * 该类是 Jasper JSP 引擎的核心组件，负责管理 JSP 页面的完整生命周期，包括：
 * - JSP 页面的编译、加载和执行
 * - 标签文件的编译和加载
 * - Servlet 实例的重载管理
 * - 编译异常和运行时错误的处理
 * - 开发模式下的自动重新编译
 * - JSP 页面的卸载策略管理
 * </p>
 * <p>
 * 包装器模式设计使得单个实例可以封装一个 JSP 页面或标签文件的所有运行时状态。
 * 在非开发模式下，编译后的 Servlet 会被缓存以提高性能；在开发模式下，
 * 支持自动检测 JSP 文件变化并重新编译。
 * </p>
 *
 * @author Anil K. Vijendran
 * @author Harish Prabandham
 * @author Remy Maucherat
 * @author Kin-man Chung
 * @author Glenn Nielsen
 * @author Tim Fennell
 */

@SuppressWarnings("deprecation") // 必须支持 SingleThreadModel
public class JspServletWrapper {

    /**
     * 始终被视为过时的依赖项映射。
     * 用于强制重新编译某些关键文件（如 web.xml）发生变化时。
     */
    private static final Map<String,Long> ALWAYS_OUTDATED_DEPENDENCIES =
            new HashMap<>();

    static {
        // 如果 web.xml 缺失，将触发重新编译
        ALWAYS_OUTDATED_DEPENDENCIES.put("/WEB-INF/web.xml", Long.valueOf(-1));
    }

    // 日志记录器 - 不能是静态的，以支持每个实例的日志配置
    private final Log log = LogFactory.getLog(JspServletWrapper.class);

    // 当前 JSP 编译后的 Servlet 实例
    private volatile Servlet theServlet;
    // JSP 文件的 URI 路径
    private final String jspUri;
    // 标签文件处理器类
    private volatile Class<?> tagHandlerClass;
    // JSP 编译上下文
    private final JspCompilationContext ctxt;
    // Servlet 的可用时间戳（用于处理 UnavailableException）
    private long available = 0L;
    // Servlet 配置对象
    private final ServletConfig config;
    // JSP 引擎选项配置
    private final Options options;

    /**
     * 标记 Servlet/标签文件是否需要在首次访问时进行编译检查。
     * 使用独立标志（而不是通过 theServlet == null / tagHandlerClass == null 判断）
     * 可以避免在多个并发请求同时访问该 Servlet/标签时，重复调用开销较大的 isOutDated() 方法。
     */
    private volatile boolean mustCompile = true;

    /**
     * 标记 Servlet/标签文件是否需要在下次访问时重新加载
     */
    private volatile boolean reload = true;

    // 是否为标签文件
    private final boolean isTagFile;
    // 访问计数器（用于统计和调试）
    private int tripCount;
    // 编译异常缓存
    private JasperException compileException;
    // Servlet 类文件的最后修改时间
    private volatile long servletClassLastModifiedTime;
    // 上次修改测试的时间戳
    private long lastModificationTest = 0L;
    // 上次使用时间戳（用于空闲卸载策略）
    private long lastUsageTime = System.currentTimeMillis();
    // 卸载队列句柄
    private FastRemovalDequeue<JspServletWrapper>.Entry unloadHandle;
    // 是否允许卸载
    private final boolean unloadAllowed;
    // 是否按计数卸载
    private final boolean unloadByCount;
    // 是否按空闲时间卸载
    private final boolean unloadByIdle;

    /**
     * 用于 JSP 页面的构造函数。
     *
     * @param config Servlet 配置对象，包含初始化参数和 ServletContext
     * @param options JSP 引擎选项配置
     * @param jspUri JSP 文件的 URI 路径
     * @param rctxt JSP 运行时上下文
     */
    public JspServletWrapper(ServletConfig config, Options options,
            String jspUri, JspRuntimeContext rctxt) {

        this.isTagFile = false;
        this.config = config;
        this.options = options;
        this.jspUri = jspUri;
        // 根据配置决定是否启用计数卸载策略
        unloadByCount = options.getMaxLoadedJsps() > 0 ? true : false;
        // 根据配置决定是否启用空闲时间卸载策略
        unloadByIdle = options.getJspIdleTimeout() > 0 ? true : false;
        // 只要启用了任一卸载策略，就允许卸载
        unloadAllowed = unloadByCount || unloadByIdle ? true : false;
        // 创建 JSP 编译上下文
        ctxt = new JspCompilationContext(jspUri, options,
                                         config.getServletContext(),
                                         this, rctxt);
    }

    /**
     * 用于标签文件的构造函数。
     *
     * @param servletContext Servlet 上下文
     * @param options JSP 引擎选项配置
     * @param tagFilePath 标签文件的路径
     * @param tagInfo 标签信息对象，包含标签的元数据
     * @param rctxt JSP 运行时上下文
     * @param tagJar 包含标签文件的 JAR 包（可能为 null）
     */
    public JspServletWrapper(ServletContext servletContext,
                             Options options,
                             String tagFilePath,
                             TagInfo tagInfo,
                             JspRuntimeContext rctxt,
                             Jar tagJar) {

        this.isTagFile = true;
        this.config = null;        // 标签文件不使用 ServletConfig
        this.options = options;
        this.jspUri = tagFilePath;
        this.tripCount = 0;
        // 根据配置决定是否启用计数卸载策略
        unloadByCount = options.getMaxLoadedJsps() > 0 ? true : false;
        // 根据配置决定是否启用空闲时间卸载策略
        unloadByIdle = options.getJspIdleTimeout() > 0 ? true : false;
        // 只要启用了任一卸载策略，就允许卸载
        unloadAllowed = unloadByCount || unloadByIdle ? true : false;
        // 创建 JSP 编译上下文（针对标签文件）
        ctxt = new JspCompilationContext(jspUri, tagInfo, options,
                                         servletContext, this, rctxt,
                                         tagJar);
    }

    /**
     * 获取 JSP 引擎编译上下文。
     *
     * @return JspCompilationContext 编译上下文对象
     */
    public JspCompilationContext getJspEngineContext() {
        return ctxt;
    }

    /**
     * 设置重新加载标志。
     *
     * @param reload true 表示需要重新加载，false 表示不需要
     */
    public void setReload(boolean reload) {
        this.reload = reload;
    }

    /**
     * 获取当前的重新加载标志状态。
     *
     * @return true 表示需要重新加载，false 表示不需要
     */
    public boolean getReload() {
        return reload;
    }

    /**
     * 内部方法：获取重新加载状态，同时检查是否正在进行编译检查。
     * 如果在编译检查期间，则返回 false 以避免竞争条件。
     *
     * @return true 表示需要重新加载且不在编译检查中
     */
    private boolean getReloadInternal() {
        return reload && !ctxt.getRuntimeContext().isCompileCheckInProgress();
    }

    /**
     * 获取或加载 Servlet 实例。
     * <p>
     * 使用双重检查锁定（DCL）模式确保线程安全。
     * 如果需要重新加载或尚未加载，将创建新的 Servlet 实例并初始化。
     * </p>
     *
     * @return Servlet 实例
     * @throws ServletException 如果 Servlet 初始化失败
     */
    public Servlet getServlet() throws ServletException {
        /*
         * 对 'reload' 使用 DCL 要求 'reload' 必须是 volatile
         * （这也强制了读内存屏障，确保新 Servlet 对象被一致地读取）。
         *
         * 在非开发模式下使用 checkInterval 时，可能存在竞争条件导致失败
         *（参见 BZ 62603），如果在编译检查运行时重新加载 Servlet 或标签
         */
        if (getReloadInternal() || theServlet == null) {
            synchronized (this) {
                // 在 jsw 上同步允许同时加载不同页面，但同一页面不会
                if (getReloadInternal() || theServlet == null) {
                    // 保持原始协议
                    destroy();

                    final Servlet servlet;

                    try {
                        // 获取实例管理器并创建 Servlet 实例
                        InstanceManager instanceManager = InstanceManagerFactory.getInstanceManager(config);
                        servlet = (Servlet) instanceManager.newInstance(ctxt.getFQCN(), ctxt.getJspLoader());
                    } catch (Exception e) {
                        Throwable t = ExceptionUtils
                                .unwrapInvocationTargetException(e);
                        ExceptionUtils.handleThrowable(t);
                        throw new JasperException(t);
                    }

                    // 初始化 Servlet
                    servlet.init(config);

                    // 如果是重新加载，增加重载计数
                    if (theServlet != null) {
                        ctxt.getRuntimeContext().incrementJspReloadCount();
                    }

                    theServlet = servlet;
                    reload = false;
                    // volatile 'reload' 强制有序写入 'theServlet' 和新 Servlet 对象
                }
            }
        }
        return theServlet;
    }

    /**
     * 获取 Servlet 上下文。
     *
     * @return ServletContext 对象
     */
    public ServletContext getServletContext() {
        return ctxt.getServletContext();
    }

    /**
     * 设置编译异常。
     * 当 JSP 编译失败时，缓存异常以便后续抛出。
     *
     * @param je JasperException 编译异常
     */
    public void setCompilationException(JasperException je) {
        this.compileException = je;
    }

    /**
     * 设置 Servlet 类文件的最后修改时间。
     * <p>
     * 如果新的修改时间大于当前记录的时间，将触发重新加载标志
     * 并清除 JSP 类加载器。
     * </p>
     *
     * @param lastModified Servlet 类的最后修改时间
     */
    public void setServletClassLastModifiedTime(long lastModified) {
        // DCL 要求 servletClassLastModifiedTime 必须是 volatile
        // 以强制访问/设置时的读写屏障（以及获得 long 的原子写入）
        if (this.servletClassLastModifiedTime < lastModified) {
            synchronized (this) {
                if (this.servletClassLastModifiedTime < lastModified) {
                    this.servletClassLastModifiedTime = lastModified;
                    reload = true;
                    // 实际上需要卸载旧类但无法做到。退而求其次，
                    // 丢弃 JspLoader，以便创建新的加载器来加载新类。
                    // TODO: reload 和 isOutDated() 检查之间是否存在低效问题？
                    ctxt.clearJspLoader();
                }
            }
        }
    }

    /**
     * 编译（如需要）并加载标签文件。
     *
     * @return 加载的标签处理器类
     * @throws JasperException 编译或加载标签文件时出错
     */
    public Class<?> loadTagFile() throws JasperException {

        try {
            // 检查文件是否已被删除
            if (ctxt.isRemoved()) {
                throw new FileNotFoundException(jspUri);
            }
            // 在开发模式或首次编译时进行编译
            if (options.getDevelopment() || mustCompile) {
                synchronized (this) {
                    if (options.getDevelopment() || mustCompile) {
                        ctxt.compile();
                        mustCompile = false;
                    }
                }
            } else {
                // 如果有缓存的编译异常，直接抛出
                if (compileException != null) {
                    throw compileException;
                }
            }

            // 重新加载标签处理器类（如需要）
            if (getReloadInternal() || tagHandlerClass == null) {
                synchronized (this) {
                    if (getReloadInternal() || tagHandlerClass == null) {
                        tagHandlerClass = ctxt.load();
                        // volatile 'reload' 强制有序写入 'tagHandlerClass'
                        reload = false;
                    }
                }
            }
        } catch (FileNotFoundException ex) {
            throw new JasperException(ex);
        }

        return tagHandlerClass;
    }

    /**
     * 编译并加载标签文件的原型。
     * <p>
     * 当编译具有循环依赖关系的标签文件时需要此方法。
     * 生成并编译一个原型（骨架），该原型不依赖于其他标签文件。
     * </p>
     *
     * @return 加载的标签处理器类
     * @throws JasperException 编译或加载标签文件时出错
     */
    public Class<?> loadTagFilePrototype() throws JasperException {

        ctxt.setPrototypeMode(true);
        try {
            return loadTagFile();
        } finally {
            ctxt.setPrototypeMode(false);
        }
    }

    /**
     * 获取当前页面依赖的源文件列表。
     * <p>
     * 用于检测 JSP 页面是否需要重新编译。返回的映射包含依赖文件路径
     * 及其最后修改时间。
     * </p>
     *
     * @return 依赖资源的映射（文件路径 -> 最后修改时间）
     */
    public Map<String,Long> getDependants() {
        try {
            Object target;
            if (isTagFile) {
                // 标签文件：加载处理器类并创建实例
                if (reload) {
                    synchronized (this) {
                        if (reload) {
                            tagHandlerClass = ctxt.load();
                            reload = false;
                        }
                    }
                }
                target = tagHandlerClass.getConstructor().newInstance();
            } else {
                // JSP 页面：获取 Servlet 实例
                target = getServlet();
            }
            // 如果实现了 JspSourceDependent 接口，获取依赖信息
            if (target instanceof JspSourceDependent) {
                return ((JspSourceDependent) target).getDependants();
            }
        } catch (AbstractMethodError ame) {
            // 几乎可以肯定是 Tomcat 7.0.17 之前编译的 JSP，使用了旧版本的接口。
            // 强制重新编译。
            return ALWAYS_OUTDATED_DEPENDENCIES;
        } catch (Throwable ex) {
            ExceptionUtils.handleThrowable(ex);
        }
        return null;
    }

    /**
     * 检查此包装器是否封装了标签文件。
     *
     * @return true 如果是标签文件，false 如果是 JSP 页面
     */
    public boolean isTagFile() {
        return this.isTagFile;
    }

    /**
     * 增加访问计数。
     * 用于统计和调试目的。
     *
     * @return 增加前的计数值
     */
    public int incTripCount() {
        return tripCount++;
    }

    /**
     * 减少访问计数。
     * 用于统计和调试目的。
     *
     * @return 减少后的计数值
     */
    public int decTripCount() {
        return tripCount--;
    }

    /**
     * 获取 JSP URI。
     *
     * @return JSP 文件的 URI 路径
     */
    public String getJspUri() {
        return jspUri;
    }

    /**
     * 获取卸载队列句柄。
     *
     * @return FastRemovalDequeue.Entry 卸载句柄
     */
    public FastRemovalDequeue<JspServletWrapper>.Entry getUnloadHandle() {
        return unloadHandle;
    }

    /**
     * 处理 HTTP 请求服务。
     * <p>
     * 这是 JSP 页面请求处理的核心方法，执行以下步骤：
     * 1. 检查文件是否存在
     * 2. 检查 Servlet 是否可用（处理 UnavailableException 后的等待期）
     * 3. 编译 JSP（如需要）
     * 4. （重新）加载 Servlet 类
     * 5. 处理 JSP 加载限制
     * 6. 调用 Servlet 的 service 方法处理请求
     * </p>
     *
     * @param request HTTP 请求对象
     * @param response HTTP 响应对象
     * @param precompile 是否仅预编译
     * @throws ServletException Servlet 处理异常
     * @throws IOException I/O 异常
     * @throws FileNotFoundException JSP 文件未找到
     */
    public void service(HttpServletRequest request,
                        HttpServletResponse response,
                        boolean precompile)
            throws ServletException, IOException, FileNotFoundException {

        Servlet servlet;

        try {

            // 检查文件是否已被删除
            if (ctxt.isRemoved()) {
                throw new FileNotFoundException(jspUri);
            }

            // 检查 Servlet 是否处于不可用状态
            if ((available > 0L) && (available < Long.MAX_VALUE)) {
                if (available > System.currentTimeMillis()) {
                    // 仍在等待期内，返回 503 服务不可用
                    response.setDateHeader("Retry-After", available);
                    response.sendError
                        (HttpServletResponse.SC_SERVICE_UNAVAILABLE,
                         Localizer.getMessage("jsp.error.unavailable"));
                    return;
                }

                // 等待期已过，重置可用时间
                available = 0;
            }

            /*
             * (1) 编译
             */
            if (options.getDevelopment() || mustCompile) {
                synchronized (this) {
                    if (options.getDevelopment() || mustCompile) {
                        // 以下方法会在必要时设置 reload 为 true
                        ctxt.compile();
                        mustCompile = false;
                    }
                }
            } else {
                if (compileException != null) {
                    // 抛出缓存的编译异常
                    throw compileException;
                }
            }

            /*
             * (2) （重新）加载 Servlet 类文件
             */
            servlet = getServlet();

            // 如果仅需预编译，返回
            if (precompile) {
                return;
            }

        } catch (FileNotFoundException fnfe) {
            // 文件已被删除，让调用者处理
            throw fnfe;
        } catch (ServletException | IOException | IllegalStateException ex) {
            if (options.getDevelopment()) {
                throw handleJspException(ex);
            }
            throw ex;
        } catch (Exception ex) {
            if (options.getDevelopment()) {
                throw handleJspException(ex);
            }
            throw new JasperException(ex);
        }

        try {
            /*
             * (3) 处理已加载 JSP 数量的限制
             */
            if (unloadAllowed) {
                synchronized(this) {
                    if (unloadByCount) {
                        // 按计数卸载策略
                        if (unloadHandle == null) {
                            // 首次使用，添加到卸载队列
                            unloadHandle = ctxt.getRuntimeContext().push(this);
                        } else if (lastUsageTime < ctxt.getRuntimeContext().getLastJspQueueUpdate()) {
                            // 队列已更新，将当前项移到最年轻位置
                            ctxt.getRuntimeContext().makeYoungest(unloadHandle);
                            lastUsageTime = System.currentTimeMillis();
                        }
                    } else {
                        // 按空闲时间卸载策略，仅更新时间戳
                        if (lastUsageTime < ctxt.getRuntimeContext().getLastJspQueueUpdate()) {
                            lastUsageTime = System.currentTimeMillis();
                        }
                    }
                }
            }

            /*
             * (4) 服务请求
             */
            if (servlet instanceof SingleThreadModel) {
               // 在包装器上同步，以便在提供服务前确定页面的新鲜度
               synchronized (this) {
                   servlet.service(request, response);
                }
            } else {
                servlet.service(request, response);
            }
        } catch (UnavailableException ex) {
            // 处理 Servlet 不可用异常
            String includeRequestUri = (String)
                request.getAttribute(RequestDispatcher.INCLUDE_REQUEST_URI);
            if (includeRequestUri != null) {
                // 此文件被包含。抛出异常，因为 response.sendError() 将被 Servlet 引擎忽略
                throw ex;
            }

            // 计算下次可用时间
            int unavailableSeconds = ex.getUnavailableSeconds();
            if (unavailableSeconds <= 0) {
                unavailableSeconds = 60;        // 任意默认值
            }
            available = System.currentTimeMillis() +
                (unavailableSeconds * 1000L);
            response.sendError
                (HttpServletResponse.SC_SERVICE_UNAVAILABLE,
                 ex.getMessage());
        } catch (ServletException | IllegalStateException ex) {
            if(options.getDevelopment()) {
                throw handleJspException(ex);
            }
            throw ex;
        } catch (IOException ex) {
            if (options.getDevelopment()) {
                throw new IOException(handleJspException(ex).getMessage(), ex);
            }
            throw ex;
        } catch (Exception ex) {
            if(options.getDevelopment()) {
                throw handleJspException(ex);
            }
            throw new JasperException(ex);
        }
    }

    /**
     * 销毁 Servlet 实例。
     * <p>
     * 调用 Servlet 的 destroy 方法，并使用 InstanceManager 销毁实例。
     * 捕获并记录所有异常，避免影响其他操作。
     * </p>
     */
    public void destroy() {
        if (theServlet != null) {
            try {
                theServlet.destroy();
            } catch (Throwable t) {
                ExceptionUtils.handleThrowable(t);
                log.error(Localizer.getMessage("jsp.error.servlet.destroy.failed"), t);
            }
            InstanceManager instanceManager = InstanceManagerFactory.getInstanceManager(config);
            try {
                instanceManager.destroyInstance(theServlet);
            } catch (Exception e) {
                Throwable t = ExceptionUtils.unwrapInvocationTargetException(e);
                ExceptionUtils.handleThrowable(t);
                // 记录任何异常，因为无法传递
                log.error(Localizer.getMessage("jsp.error.file.not.found",
                        e.getMessage()), t);
            }
        }
    }

    /**
     * 获取上次修改测试的时间戳。
     *
     * @return 上次修改测试的时间戳
     */
    public long getLastModificationTest() {
        return lastModificationTest;
    }

    /**
     * 设置上次修改测试的时间戳。
     *
     * @param lastModificationTest 要设置的时间戳
     */
    public void setLastModificationTest(long lastModificationTest) {
        this.lastModificationTest = lastModificationTest;
    }

    /**
     * 获取上次使用时间戳。
     *
     * @return 上次使用时间戳
     */
    public long getLastUsageTime() {
        return lastUsageTime;
    }

    /**
     * 处理 JSP 异常，尝试提供更有帮助的错误信息。
     * <p>
     * 使用 JSP 编译器系统将生成的 Servlet 中的行号转换为 JSP 源文件中的行号。
     * 然后构造包含该信息的异常，并附带 JSP 片段以帮助调试。
     * </p>
     * <p>
     * 参见：https://bz.apache.org/bugzilla/show_bug.cgi?id=37062
     * 和 http://www.tfenne.com/jasper/ 了解更多详情。
     * </p>
     *
     * @param ex 导致问题的异常
     * @return 包含更详细信息的 JasperException
     */
    protected JasperException handleJspException(Exception ex) {
        try {
            Throwable realException = ex;
            if (ex instanceof ServletException) {
                realException = ((ServletException) ex).getRootCause();
            }

            // 查找代表 Jasper 生成代码的第一个堆栈帧
            StackTraceElement[] frames = realException.getStackTrace();
            StackTraceElement jspFrame = null;

            String servletPackageName = ctxt.getBasePackageName();
            for (StackTraceElement frame : frames) {
                if (frame.getClassName().startsWith(servletPackageName)) {
                    jspFrame = frame;
                    break;
                }
            }

            SmapStratum smap = null;

            if (jspFrame != null) {
                smap = ctxt.getCompiler().getSmap(jspFrame.getClassName());
            }

            if (smap == null) {
                // 如果在堆栈跟踪中找不到对应生成 Servlet 类的帧，
                // 或者我们没有 smap 的副本，我们无法添加更多信息
                return new JasperException(ex);
            }

            @SuppressWarnings("null")
            int javaLineNumber = jspFrame.getLineNumber();
            SmapInput source = smap.getInputLineNumber(javaLineNumber);

            // 如果行号小于 1，我们无法确定 JSP 中出错的位置
            if (source.getLineNumber() < 1) {
                throw new JasperException(ex);
            }

            JavacErrorDetail detail = new JavacErrorDetail(jspFrame.getMethodName(), javaLineNumber,
                    source.getFileName(), source.getLineNumber(), null, ctxt);

            if (options.getDisplaySourceFragment()) {
                return new JasperException(Localizer.getMessage
                        ("jsp.exception", detail.getJspFileName(),
                                "" + source.getLineNumber()) + System.lineSeparator() +
                                System.lineSeparator() + detail.getJspExtract() +
                                System.lineSeparator() + System.lineSeparator() +
                                "Stacktrace:", ex);

            }

            return new JasperException(Localizer.getMessage
                    ("jsp.exception", detail.getJspFileName(),
                            "" + source.getLineNumber()), ex);
        } catch (Exception je) {
            // 如果出错，恢复到原始行为
            if (ex instanceof JasperException) {
                return (JasperException) ex;
            }
            return new JasperException(ex);
        }
    }
}
