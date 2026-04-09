package org.apache.jasper.runtime;

import java.io.IOException;
import java.io.Writer;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Set;

import javax.el.ELContext;
import javax.el.ELException;
import javax.el.ExpressionFactory;
import javax.el.ImportHandler;
import javax.el.ValueExpression;
import javax.servlet.RequestDispatcher;
import javax.servlet.Servlet;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspFactory;
import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.PageContext;
import javax.servlet.jsp.tagext.BodyContent;

import org.apache.jasper.Constants;
import org.apache.jasper.compiler.Localizer;
import org.apache.jasper.el.ELContextImpl;
import org.apache.jasper.runtime.JspContextWrapper.ELContextWrapper;

/**
 * PageContext 类的实现类，实现了 JSP 规范中的 PageContext 类。
 * 同时作为 EL（表达式语言）的 VariableResolver。
 *
 * @author Anil K. Vijendran
 * @author Larry Cable
 * @author Hans Bergsten
 * @author Pierre Delisle
 * @author Mark Roth
 * @author Jan Luehe
 * @author Jacob Hookom
 */
public class PageContextImpl extends PageContext {

    // JSP 工厂实例，用于创建 JSP 相关对象
    private static final JspFactory jspf = JspFactory.getDefaultFactory();

    // BodyContent 对象数组，用于管理嵌套的 body 内容（如标签体）
    private BodyContentImpl[] outs;

    // 当前 body 内容嵌套深度，-1 表示没有嵌套
    private int depth;

    // ========== 每个 Servlet 的状态 ==========
    // 当前 JSP 页面对应的 Servlet 实例
    private Servlet servlet;

    // Servlet 配置信息
    private ServletConfig config;

    // Servlet 上下文（应用级别）
    private ServletContext context;

    // JSP 应用上下文，用于 EL 表达式求值等
    private JspApplicationContextImpl applicationContext;

    // 错误页面 URL，用于异常处理时跳转
    private String errorPageURL;

    // ========== 页面作用域属性存储 ==========
    // 页面级别的属性存储（PAGE_SCOPE），使用 HashMap 存储
    private final transient HashMap<String, Object> attributes;

    // ========== 每个请求的状态 ==========
    // 当前请求对象
    private transient ServletRequest request;

    // 当前响应对象
    private transient ServletResponse response;

    // 当前会话对象
    private transient HttpSession session;

    // EL 上下文，用于表达式语言求值
    private transient ELContextImpl elContext;

    // ========== 输出流相关 ==========
    // 当前 JSP 输出流
    private transient JspWriter out;

    // 基础输出流，页面顶层输出
    private transient JspWriterImpl baseOut;

    /**
     * 构造方法。
     */
    PageContextImpl() {
        this.outs = new BodyContentImpl[0];
        this.attributes = new HashMap<>(16);
        this.depth = -1;
    }

    /**
     * 初始化 PageContext。
     * 设置 Servlet、请求、响应、会话等状态，并初始化输出流。
     * 同时按照 JSP 规范注册内置对象（out、request、response、session 等）到页面作用域。
     *
     * @param servlet      当前 JSP 对应的 Servlet
     * @param request      当前请求
     * @param response     当前响应
     * @param errorPageURL 错误页面 URL
     * @param needsSession 是否需要会话
     * @param bufferSize   缓冲区大小
     * @param autoFlush    是否自动刷新
     * @throws IOException 当输出流初始化失败时抛出
     */
    @Override
    public void initialize(Servlet servlet, ServletRequest request,
            ServletResponse response, String errorPageURL,
            boolean needsSession, int bufferSize, boolean autoFlush)
            throws IOException {

        // initialize state
        this.servlet = servlet;
        this.config = servlet.getServletConfig();
        this.context = config.getServletContext();
        this.errorPageURL = errorPageURL;
        this.request = request;
        this.response = response;

        // initialize application context
        this.applicationContext = JspApplicationContextImpl.getInstance(context);

        // Setup session (if required)
        if (request instanceof HttpServletRequest && needsSession) {
            this.session = ((HttpServletRequest) request).getSession();
        }
        if (needsSession && session == null) {
            throw new IllegalStateException(Localizer.getMessage("jsp.error.page.sessionRequired"));
        }

        // initialize the initial out ...
        depth = -1;
        if (bufferSize == JspWriter.DEFAULT_BUFFER) {
            bufferSize = Constants.DEFAULT_BUFFER_SIZE;
        }
        if (this.baseOut == null) {
            this.baseOut = new JspWriterImpl(response, bufferSize, autoFlush);
        } else {
            this.baseOut.init(response, bufferSize, autoFlush);
        }
        this.out = baseOut;

        // register names/values as per spec
        setAttribute(OUT, this.out);
        setAttribute(REQUEST, request);
        setAttribute(RESPONSE, response);

        if (session != null) {
            setAttribute(SESSION, session);
        }

        setAttribute(PAGE, servlet);
        setAttribute(CONFIG, config);
        setAttribute(PAGECONTEXT, this);
        setAttribute(APPLICATION, context);
    }

    /**
     * 释放 PageContext 占用的资源。
     * 刷新缓冲区，清空所有引用，回收输出流对象。
     */
    @Override
    public void release() {
        out = baseOut;
        try {
            ((JspWriterImpl) out).flushBuffer();
        } catch (IOException ex) {
            IllegalStateException ise = new IllegalStateException(Localizer.getMessage("jsp.error.flush"), ex);
            throw ise;
        } finally {
            servlet = null;
            config = null;
            context = null;
            applicationContext = null;
            elContext = null;
            errorPageURL = null;
            request = null;
            response = null;
            depth = -1;
            baseOut.recycle();
            session = null;
            attributes.clear();
            for (BodyContentImpl body: outs) {
                body.recycle();
            }
        }
    }

    /**
     * 获取指定名称的属性值，默认在页面作用域中查找。
     *
     * @param name 属性名称
     * @return 属性值，如果不存在则返回 null
     */
    @Override
    public Object getAttribute(final String name) {
        return getAttribute(name, PAGE_SCOPE);
    }

    /**
     * 在指定作用域中获取属性值。
     * 支持的作用域：PAGE_SCOPE、REQUEST_SCOPE、SESSION_SCOPE、APPLICATION_SCOPE。
     *
     * @param name  属性名称
     * @param scope 作用域
     * @return 属性值，如果不存在则返回 null
     * @throws NullPointerException     如果 name 为 null
     * @throws IllegalStateException    如果在 SESSION_SCOPE 中但 session 为 null
     * @throws IllegalArgumentException 如果 scope 无效
     */
    @Override
    public Object getAttribute(final String name, final int scope) {

        if (name == null) {
            throw new NullPointerException(Localizer.getMessage("jsp.error.attribute.null_name"));
        }

        switch (scope) {
        case PAGE_SCOPE:
            return attributes.get(name);

        case REQUEST_SCOPE:
            return request.getAttribute(name);

        case SESSION_SCOPE:
            if (session == null) {
                throw new IllegalStateException(Localizer.getMessage("jsp.error.page.noSession"));
            }
            return session.getAttribute(name);

        case APPLICATION_SCOPE:
            return context.getAttribute(name);

        default:
            throw new IllegalArgumentException(Localizer.getMessage("jsp.error.page.invalid.scope"));
        }
    }

    /**
     * 在页面作用域中设置属性值。
     *
     * @param name      属性名称
     * @param attribute 属性值
     */
    @Override
    public void setAttribute(final String name, final Object attribute) {
        setAttribute(name, attribute, PAGE_SCOPE);
    }

    /**
     * 在指定作用域中设置属性值。
     * 如果值为 null，则等效于调用 removeAttribute。
     * 支持的作用域：PAGE_SCOPE、REQUEST_SCOPE、SESSION_SCOPE、APPLICATION_SCOPE。
     *
     * @param name  属性名称
     * @param o     属性值
     * @param scope 作用域
     * @throws NullPointerException     如果 name 为 null
     * @throws IllegalStateException    如果在 SESSION_SCOPE 中但 session 为 null
     * @throws IllegalArgumentException 如果 scope 无效
     */
    @Override
    public void setAttribute(final String name, final Object o, final int scope) {

        if (name == null) {
            throw new NullPointerException(Localizer.getMessage("jsp.error.attribute.null_name"));
        }

        if (o == null) {
            removeAttribute(name, scope);
        } else {
            switch (scope) {
            case PAGE_SCOPE:
                attributes.put(name, o);
                break;

            case REQUEST_SCOPE:
                request.setAttribute(name, o);
                break;

            case SESSION_SCOPE:
                if (session == null) {
                    throw new IllegalStateException(Localizer
                            .getMessage("jsp.error.page.noSession"));
                }
                session.setAttribute(name, o);
                break;

            case APPLICATION_SCOPE:
                context.setAttribute(name, o);
                break;

            default:
                throw new IllegalArgumentException(Localizer.getMessage("jsp.error.page.invalid.scope"));
            }
        }
    }

    /**
     * 从指定作用域中移除属性。
     * 支持的作用域：PAGE_SCOPE、REQUEST_SCOPE、SESSION_SCOPE、APPLICATION_SCOPE。
     *
     * @param name  属性名称
     * @param scope 作用域
     * @throws NullPointerException     如果 name 为 null
     * @throws IllegalStateException    如果在 SESSION_SCOPE 中但 session 为 null
     * @throws IllegalArgumentException 如果 scope 无效
     */
    @Override
    public void removeAttribute(final String name, final int scope) {

        if (name == null) {
            throw new NullPointerException(Localizer.getMessage("jsp.error.attribute.null_name"));
        }

        switch (scope) {
        case PAGE_SCOPE:
            attributes.remove(name);
            break;

        case REQUEST_SCOPE:
            request.removeAttribute(name);
            break;

        case SESSION_SCOPE:
            if (session == null) {
                throw new IllegalStateException(Localizer.getMessage("jsp.error.page.noSession"));
            }
            session.removeAttribute(name);
            break;

        case APPLICATION_SCOPE:
            context.removeAttribute(name);
            break;

        default:
            throw new IllegalArgumentException(Localizer.getMessage("jsp.error.page.invalid.scope"));
        }
    }

    @Override
    public int getAttributesScope(final String name) {

        if (name == null) {
            throw new NullPointerException(Localizer.getMessage("jsp.error.attribute.null_name"));
        }

        if (attributes.get(name) != null) {
            return PAGE_SCOPE;
        }

        if (request.getAttribute(name) != null) {
            return REQUEST_SCOPE;
        }

        if (session != null) {
            try {
                if (session.getAttribute(name) != null) {
                    return SESSION_SCOPE;
                }
            } catch(IllegalStateException ise) {
                // Session has been invalidated.
                // Ignore and fall through to application scope.
            }
        }

        if (context.getAttribute(name) != null) {
            return APPLICATION_SCOPE;
        }

        return 0;
    }

    /**
     * 按优先级顺序在所有作用域中查找属性。
     * 搜索顺序：页面作用域 -> 请求作用域 -> 会话作用域 -> 应用作用域。
     *
     * @param name 属性名称
     * @return 第一个找到的属性值，如果都不存在则返回 null
     * @throws NullPointerException 如果 name 为 null
     */
    @Override
    public Object findAttribute(final String name) {
        if (name == null) {
            throw new NullPointerException(Localizer.getMessage("jsp.error.attribute.null_name"));
        }

        Object o = attributes.get(name);
        if (o != null) {
            return o;
        }

        o = request.getAttribute(name);
        if (o != null) {
            return o;
        }

        if (session != null) {
            try {
                o = session.getAttribute(name);
            } catch(IllegalStateException ise) {
                // Session has been invalidated.
                // Ignore and fall through to application scope.
            }
            if (o != null) {
                return o;
            }
        }

        return context.getAttribute(name);
    }

    @Override
    public Enumeration<String> getAttributeNamesInScope(final int scope) {
        switch (scope) {
        case PAGE_SCOPE:
            return Collections.enumeration(attributes.keySet());

        case REQUEST_SCOPE:
            return request.getAttributeNames();

        case SESSION_SCOPE:
            if (session == null) {
                throw new IllegalStateException(Localizer.getMessage("jsp.error.page.noSession"));
            }
            return session.getAttributeNames();

        case APPLICATION_SCOPE:
            return context.getAttributeNames();

        default:
            throw new IllegalArgumentException(Localizer.getMessage("jsp.error.page.invalid.scope"));
        }
    }

    @Override
    public void removeAttribute(final String name) {

        if (name == null) {
            throw new NullPointerException(Localizer.getMessage("jsp.error.attribute.null_name"));
        }

        removeAttribute(name, PAGE_SCOPE);
        removeAttribute(name, REQUEST_SCOPE);
        if( session != null ) {
            try {
                removeAttribute(name, SESSION_SCOPE);
            } catch(IllegalStateException ise) {
                // Session has been invalidated.
                // Ignore and fall throw to application scope.
            }
        }
        removeAttribute(name, APPLICATION_SCOPE);
    }

    /**
     * 获取当前的 JspWriter 输出流。
     *
     * @return 当前的 JspWriter 对象
     */
    @Override
    public JspWriter getOut() {
        return out;
    }

    /**
     * 获取当前会话对象。
     *
     * @return HttpSession 对象，如果页面不需要会话则可能返回 null
     */
    @Override
    public HttpSession getSession() {
        return session;
    }

    @Override
    public ServletConfig getServletConfig() {
        return config;
    }

    @Override
    public ServletContext getServletContext() {
        return config.getServletContext();
    }

    /**
     * 获取当前请求对象。
     *
     * @return ServletRequest 对象
     */
    @Override
    public ServletRequest getRequest() {
        return request;
    }

    /**
     * 获取当前响应对象。
     *
     * @return ServletResponse 对象
     */
    @Override
    public ServletResponse getResponse() {
        return response;
    }

    /**
     * Returns the exception associated with this page context, if any.
     * <p>
     * Added wrapping for Throwables to avoid ClassCastException: see Bugzilla
     * 31171 for details.
     *
     * @return The Exception associated with this page context, if any.
     */
    @Override
    public Exception getException() {
        Throwable t = JspRuntimeLibrary.getThrowable(request);

        // Only wrap if needed
        if ((t != null) && (!(t instanceof Exception))) {
            t = new JspException(t);
        }

        return (Exception) t;
    }

    @Override
    public Object getPage() {
        return servlet;
    }

    private String getAbsolutePathRelativeToContext(String relativeUrlPath) {
        String path = relativeUrlPath;

        if (!path.startsWith("/")) {
            String uri = (String) request.getAttribute(
                    RequestDispatcher.INCLUDE_SERVLET_PATH);
            if (uri == null) {
                uri = ((HttpServletRequest) request).getServletPath();
            }
            String baseURI = uri.substring(0, uri.lastIndexOf('/'));
            path = baseURI + '/' + path;
        }

        return path;
    }

    /**
     * 包含指定的资源到当前页面。
     * 被包含的资源会共享当前页面的请求和响应对象。
     *
     * @param relativeUrlPath 要包含的资源的相对路径
     * @throws ServletException 如果包含过程中发生 Servlet 异常
     * @throws IOException      如果包含过程中发生 I/O 异常
     */
    @Override
    public void include(String relativeUrlPath) throws ServletException,
            IOException {
        JspRuntimeLibrary
                .include(request, response, relativeUrlPath, out, true);
    }

    /**
     * 包含指定的资源到当前页面，并可指定是否在包含前刷新缓冲区。
     *
     * @param relativeUrlPath 要包含的资源的相对路径
     * @param flush           是否在包含前刷新缓冲区
     * @throws ServletException 如果包含过程中发生 Servlet 异常
     * @throws IOException      如果包含过程中发生 I/O 异常
     */
    @Override
    public void include(final String relativeUrlPath, final boolean flush)
            throws ServletException, IOException {
        JspRuntimeLibrary.include(request, response, relativeUrlPath, out, flush);
    }

    @Override
    @Deprecated
    public javax.servlet.jsp.el.VariableResolver getVariableResolver() {
        return new org.apache.jasper.el.VariableResolverImpl(
                this.getELContext());
    }

    /**
     * 将请求转发到指定的资源。
     * 转发后，当前页面的输出将被清空，控制权转交给目标资源。
     * 如果缓冲区已被刷新，则抛出 IllegalStateException。
     *
     * @param relativeUrlPath 要转发到的资源的相对路径
     * @throws ServletException 如果转发过程中发生 Servlet 异常
     * @throws IOException      如果转发过程中发生 I/O 异常
     */
    @Override
    public void forward(final String relativeUrlPath) throws ServletException, IOException {
        // JSP.4.5 If the buffer was flushed, throw IllegalStateException
        try {
            out.clear();
            baseOut.clear();
        } catch (IOException ex) {
            throw new IllegalStateException(Localizer.getMessage(
                    "jsp.error.attempt_to_clear_flushed_buffer"), ex);
        }

        // Make sure that the response object is not the wrapper for include
        while (response instanceof ServletResponseWrapperInclude) {
            response = ((ServletResponseWrapperInclude) response).getResponse();
        }

        final String path = getAbsolutePathRelativeToContext(relativeUrlPath);
        String includeUri = (String) request.getAttribute(RequestDispatcher.INCLUDE_SERVLET_PATH);

        if (includeUri != null) {
            request.removeAttribute(RequestDispatcher.INCLUDE_SERVLET_PATH);
        }
        try {
            context.getRequestDispatcher(path).forward(request, response);
        } finally {
            if (includeUri != null) {
                request.setAttribute(RequestDispatcher.INCLUDE_SERVLET_PATH, includeUri);
            }
        }
    }

    @Override
    public BodyContent pushBody() {
        return (BodyContent) pushBody(null);
    }

    @Override
    public JspWriter pushBody(Writer writer) {
        depth++;
        if (depth >= outs.length) {
            BodyContentImpl[] newOuts = Arrays.copyOf(outs, depth + 1);
            newOuts[depth] = new BodyContentImpl(out);
            outs = newOuts;
        }

        outs[depth].setWriter(writer);
        out = outs[depth];

        // Update the value of the "out" attribute in the page scope
        // attribute namespace of this PageContext
        setAttribute(OUT, out);

        return outs[depth];
    }

    @Override
    public JspWriter popBody() {
        depth--;
        if (depth >= 0) {
            out = outs[depth];
        } else {
            out = baseOut;
        }

        // Update the value of the "out" attribute in the page scope
        // attribute namespace of this PageContext
        setAttribute(OUT, out);

        return out;
    }

    /**
     * Provides programmatic access to the ExpressionEvaluator. The JSP
     * Container must return a valid instance of an ExpressionEvaluator that can
     * parse EL expressions.
     */
    @Override
    @Deprecated
    public javax.servlet.jsp.el.ExpressionEvaluator getExpressionEvaluator() {
        return new org.apache.jasper.el.ExpressionEvaluatorImpl(
                this.applicationContext.getExpressionFactory());
    }

    @Override
    public void handlePageException(Exception ex) throws IOException,
            ServletException {
        // Should never be called since handleException() called with a
        // Throwable in the generated servlet.
        handlePageException((Throwable) ex);
    }

    @Override
    @SuppressWarnings("deprecation") // Still have to support old JSP EL
    public void handlePageException(final Throwable t) throws IOException, ServletException {
        if (t == null) {
            throw new NullPointerException(Localizer.getMessage("jsp.error.page.nullThrowable"));
        }

        if (errorPageURL != null && !errorPageURL.equals("")) {

            /*
             * Set request attributes. Do not set the
             * javax.servlet.error.exception attribute here (instead, set in the
             * generated servlet code for the error page) in order to prevent
             * the ErrorReportValve, which is invoked as part of forwarding the
             * request to the error page, from throwing it if the response has
             * not been committed (the response will have been committed if the
             * error page is a JSP page).
             */
            request.setAttribute(EXCEPTION, t);
            request.setAttribute(RequestDispatcher.ERROR_STATUS_CODE,
                    Integer.valueOf(HttpServletResponse.SC_INTERNAL_SERVER_ERROR));
            request.setAttribute(RequestDispatcher.ERROR_REQUEST_URI, ((HttpServletRequest) request).getRequestURI());
            request.setAttribute(RequestDispatcher.ERROR_SERVLET_NAME, config.getServletName());
            try {
                forward(errorPageURL);
            } catch (IllegalStateException ise) {
                include(errorPageURL);
            }

            // The error page could be inside an include.

            Object newException = request.getAttribute(RequestDispatcher.ERROR_EXCEPTION);

            // t==null means the attribute was not set.
            if ((newException != null) && (newException == t)) {
                request.removeAttribute(RequestDispatcher.ERROR_EXCEPTION);
            }

            // now clear the error code - to prevent double handling.
            request.removeAttribute(RequestDispatcher.ERROR_STATUS_CODE);
            request.removeAttribute(RequestDispatcher.ERROR_REQUEST_URI);
            request.removeAttribute(RequestDispatcher.ERROR_SERVLET_NAME);
            request.removeAttribute(EXCEPTION);

        } else {
            // Otherwise throw the exception wrapped inside a ServletException.
            // Set the exception as the root cause in the ServletException
            // to get a stack trace for the real problem
            if (t instanceof IOException) {
                throw (IOException) t;
            }
            if (t instanceof ServletException) {
                throw (ServletException) t;
            }
            if (t instanceof RuntimeException) {
                throw (RuntimeException) t;
            }

            Throwable rootCause = null;
            if (t instanceof JspException || t instanceof ELException ||
                    t instanceof javax.servlet.jsp.el.ELException) {
                rootCause = t.getCause();
            }

            if (rootCause != null) {
                throw new ServletException(
                        t.getClass().getName() + ": " + t.getMessage(), rootCause);
            }

            throw new ServletException(t);
        }
    }

    /**
     * Proprietary method to evaluate EL expressions. XXX - This method should
     * go away once the EL interpreter moves out of JSTL and into its own
     * project. For now, this is necessary because the standard machinery is too
     * slow.
     *
     * @param expression
     *            The expression to be evaluated
     * @param expectedType
     *            The expected resulting type
     * @param pageContext
     *            The page context
     * @param functionMap
     *            Maps prefix and name to Method
     * @return The result of the evaluation
     * @throws ELException If an error occurs during the evaluation
     */
    public static Object proprietaryEvaluate(final String expression,
            final Class<?> expectedType, final PageContext pageContext,
            final ProtectedFunctionMapper functionMap)
            throws ELException {
        final ExpressionFactory exprFactory = jspf.getJspApplicationContext(pageContext.getServletContext()).getExpressionFactory();
        ELContext ctx = pageContext.getELContext();
        ELContextImpl ctxImpl;
        if (ctx instanceof ELContextWrapper) {
            ctxImpl = (ELContextImpl) ((ELContextWrapper) ctx).getWrappedELContext();
        } else {
            ctxImpl = (ELContextImpl) ctx;
        }
        ctxImpl.setFunctionMapper(functionMap);
        ValueExpression ve = exprFactory.createValueExpression(ctx, expression, expectedType);
        return ve.getValue(ctx);
    }

    @Override
    public ELContext getELContext() {
        if (elContext == null) {
            elContext = applicationContext.createELContext(this);
            if (servlet instanceof JspSourceImports) {
                ImportHandler ih = elContext.getImportHandler();
                Set<String> packageImports = ((JspSourceImports) servlet).getPackageImports();
                if (packageImports != null) {
                    for (String packageImport : packageImports) {
                        ih.importPackage(packageImport);
                    }
                }
                Set<String> classImports = ((JspSourceImports) servlet).getClassImports();
                if (classImports != null) {
                    for (String classImport : classImports) {
                        if (classImport.startsWith("static ")) {
                            classImport = classImport.substring(7);
                            try {
                                ih.importStatic(classImport);
                            } catch (ELException e) {
                                // Ignore - not all static imports are valid for EL
                            }
                        } else {
                            ih.importClass(classImport);
                        }
                    }
                }
            }
        }
        return this.elContext;
    }
}
