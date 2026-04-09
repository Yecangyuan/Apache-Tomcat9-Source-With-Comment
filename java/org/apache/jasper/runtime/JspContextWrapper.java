package org.apache.jasper.runtime;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javax.el.ELContext;
import javax.el.ELResolver;
import javax.el.EvaluationListener;
import javax.el.FunctionMapper;
import javax.el.ImportHandler;
import javax.el.VariableMapper;
import javax.servlet.Servlet;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.jsp.JspApplicationContext;
import javax.servlet.jsp.JspContext;
import javax.servlet.jsp.JspFactory;
import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.PageContext;
import javax.servlet.jsp.el.ELException;
import javax.servlet.jsp.el.ExpressionEvaluator;
import javax.servlet.jsp.el.VariableResolver;
import javax.servlet.jsp.tagext.BodyContent;
import javax.servlet.jsp.tagext.JspTag;
import javax.servlet.jsp.tagext.VariableInfo;

import org.apache.jasper.compiler.Localizer;

/**
 * JspContext 包装器的实现。
 *
 * JspContext 包装器是一个由标签处理器实现创建和维护的 JspContext。
 * 它包装了调用者的 JSP 上下文，即调用页面通过 setJspContext() 方法
 * 传递给标签处理器的 JspContext 实例。
 *
 * 此类主要用于支持 JSP 标签文件（tag file）和包含动作（include action），
 * 在标签执行期间提供一个隔离的页面作用域环境，同时能够访问调用页面的
 * 请求、会话和应用作用域。
 *
 * @author Kin-man Chung
 * @author Jan Luehe
 * @author Jacob Hookom
 */
@SuppressWarnings("deprecation") // 必须支持旧的 JSP EL API
public class JspContextWrapper extends PageContext implements VariableResolver {

    /** 当前标签处理器实例 */
    private final JspTag jspTag;

    /** 调用者 JSP 上下文（即调用页面的 PageContext） */
    private final PageContext invokingJspCtxt;

    /** 页面作用域属性存储（本包装器的虚拟页面作用域） */
    private final transient HashMap<String, Object> pageAttributes;

    /** NESTED 作用域的脚本变量列表（标签体内有效） */
    private final ArrayList<String> nestedVars;

    /** AT_BEGIN 作用域的脚本变量列表（从标签开始处有效） */
    private final ArrayList<String> atBeginVars;

    /** AT_END 作用域的脚本变量列表（标签结束时有效） */
    private final ArrayList<String> atEndVars;

    /** 变量别名映射（将变量名映射到别名） */
    private final Map<String,String> aliases;

    /** 保存原始 NESTED 变量值的映射，用于在标签结束后恢复 */
    private final HashMap<String, Object> originalNestedVars;

    /** ServletContext 缓存 */
    private ServletContext servletContext = null;

    /** ELContext 缓存 */
    private ELContext elContext = null;

    /** 根 JSP 上下文（最顶层的 PageContext） */
    private final PageContext rootJspCtxt;

    /**
     * 构造一个新的 JspContextWrapper。
     *
     * @param jspTag 标签处理器实例
     * @param jspContext 调用者的 JspContext
     * @param nestedVars NESTED 作用域变量列表
     * @param atBeginVars AT_BEGIN 作用域变量列表
     * @param atEndVars AT_END 作用域变量列表
     * @param aliases 变量别名映射
     */
    public JspContextWrapper(JspTag jspTag, JspContext jspContext,
            ArrayList<String> nestedVars, ArrayList<String> atBeginVars,
            ArrayList<String> atEndVars, Map<String,String> aliases) {
        this.jspTag = jspTag;
        this.invokingJspCtxt = (PageContext) jspContext;
        if (jspContext instanceof JspContextWrapper) {
            rootJspCtxt = ((JspContextWrapper)jspContext).rootJspCtxt;
        } else {
            rootJspCtxt = invokingJspCtxt;
        }
        this.nestedVars = nestedVars;
        this.atBeginVars = atBeginVars;
        this.atEndVars = atEndVars;
        this.pageAttributes = new HashMap<>(16);
        this.aliases = aliases;

        if (nestedVars != null) {
            this.originalNestedVars = new HashMap<>(nestedVars.size());
        } else {
            this.originalNestedVars = null;
        }
        syncBeginTagFile();
    }

    @Override
    public void initialize(Servlet servlet, ServletRequest request,
            ServletResponse response, String errorPageURL,
            boolean needsSession, int bufferSize, boolean autoFlush)
            throws IOException, IllegalStateException, IllegalArgumentException {
    }

    /**
     * 获取指定名称的属性值（从页面作用域中查找）。
     *
     * @param name 属性名称
     * @return 属性值，如果不存在则返回 null
     * @throws NullPointerException 如果 name 为 null
     */
    @Override
    public Object getAttribute(String name) {

        if (name == null) {
            throw new NullPointerException(Localizer
                    .getMessage("jsp.error.attribute.null_name"));
        }

        return pageAttributes.get(name);
    }

    /**
     * 获取指定名称和作用域的属性值。
     *
     * @param name 属性名称
     * @param scope 作用域（PAGE_SCOPE, REQUEST_SCOPE, SESSION_SCOPE, APPLICATION_SCOPE）
     * @return 属性值，如果不存在则返回 null
     * @throws NullPointerException 如果 name 为 null
     */
    @Override
    public Object getAttribute(String name, int scope) {

        if (name == null) {
            throw new NullPointerException(Localizer
                    .getMessage("jsp.error.attribute.null_name"));
        }

        if (scope == PAGE_SCOPE) {
            return pageAttributes.get(name);
        }

        return rootJspCtxt.getAttribute(name, scope);
    }

    /**
     * 设置页面作用域的属性值。
     * 如果 value 为 null，则删除该属性。
     *
     * @param name 属性名称
     * @param value 属性值
     * @throws NullPointerException 如果 name 为 null
     */
    @Override
    public void setAttribute(String name, Object value) {

        if (name == null) {
            throw new NullPointerException(Localizer
                    .getMessage("jsp.error.attribute.null_name"));
        }

        if (value != null) {
            pageAttributes.put(name, value);
        } else {
            removeAttribute(name, PAGE_SCOPE);
        }
    }

    /**
     * 设置指定作用域的属性值。
     * 如果 value 为 null，则删除该属性。
     *
     * @param name 属性名称
     * @param value 属性值
     * @param scope 作用域（PAGE_SCOPE, REQUEST_SCOPE, SESSION_SCOPE, APPLICATION_SCOPE）
     * @throws NullPointerException 如果 name 为 null
     */
    @Override
    public void setAttribute(String name, Object value, int scope) {

        if (name == null) {
            throw new NullPointerException(Localizer
                    .getMessage("jsp.error.attribute.null_name"));
        }

        if (scope == PAGE_SCOPE) {
            if (value != null) {
                pageAttributes.put(name, value);
            } else {
                removeAttribute(name, PAGE_SCOPE);
            }
        } else {
            rootJspCtxt.setAttribute(name, value, scope);
        }
    }

    /**
     * 按顺序在所有作用域中查找属性：页面、请求、会话、应用。
     *
     * @param name 属性名称
     * @return 第一个找到的属性值，如果都不存在则返回 null
     * @throws NullPointerException 如果 name 为 null
     */
    @Override
    public Object findAttribute(String name) {

        if (name == null) {
            throw new NullPointerException(Localizer
                    .getMessage("jsp.error.attribute.null_name"));
        }

        Object o = pageAttributes.get(name);
        if (o == null) {
            o = rootJspCtxt.getAttribute(name, REQUEST_SCOPE);
            if (o == null) {
                if (getSession() != null) {
                    try {
                        o = rootJspCtxt.getAttribute(name, SESSION_SCOPE);
                    } catch (IllegalStateException ise) {
                        // 会话已失效，忽略并继续查找应用作用域
                    }
                }
                if (o == null) {
                    o = rootJspCtxt.getAttribute(name, APPLICATION_SCOPE);
                }
            }
        }

        return o;
    }

    /**
     * 从所有作用域中删除指定名称的属性。
     *
     * @param name 属性名称
     * @throws NullPointerException 如果 name 为 null
     */
    @Override
    public void removeAttribute(String name) {

        if (name == null) {
            throw new NullPointerException(Localizer
                    .getMessage("jsp.error.attribute.null_name"));
        }

        pageAttributes.remove(name);
        rootJspCtxt.removeAttribute(name, REQUEST_SCOPE);
        if (getSession() != null) {
            rootJspCtxt.removeAttribute(name, SESSION_SCOPE);
        }
        rootJspCtxt.removeAttribute(name, APPLICATION_SCOPE);
    }

    /**
     * 从指定作用域中删除指定名称的属性。
     *
     * @param name 属性名称
     * @param scope 作用域
     * @throws NullPointerException 如果 name 为 null
     */
    @Override
    public void removeAttribute(String name, int scope) {

        if (name == null) {
            throw new NullPointerException(Localizer
                    .getMessage("jsp.error.attribute.null_name"));
        }

        if (scope == PAGE_SCOPE) {
            pageAttributes.remove(name);
        } else {
            rootJspCtxt.removeAttribute(name, scope);
        }
    }

    /**
     * 获取属性所在的作用域。
     *
     * @param name 属性名称
     * @return 作用域常量，如果属性不存在则返回 0
     * @throws NullPointerException 如果 name 为 null
     */
    @Override
    public int getAttributesScope(String name) {

        if (name == null) {
            throw new NullPointerException(Localizer
                    .getMessage("jsp.error.attribute.null_name"));
        }

        if (pageAttributes.get(name) != null) {
            return PAGE_SCOPE;
        } else {
            return rootJspCtxt.getAttributesScope(name);
        }
    }

    /**
     * 获取指定作用域中所有属性的枚举。
     *
     * @param scope 作用域
     * @return 属性名称的枚举
     */
    @Override
    public Enumeration<String> getAttributeNamesInScope(int scope) {
        if (scope == PAGE_SCOPE) {
            return Collections.enumeration(pageAttributes.keySet());
        }

        return rootJspCtxt.getAttributeNamesInScope(scope);
    }

    @Override
    public void release() {
        invokingJspCtxt.release();
    }

    /**
     * 获取当前的 JspWriter 输出流。
     *
     * @return JspWriter 实例
     */
    @Override
    public JspWriter getOut() {
        return rootJspCtxt.getOut();
    }

    /**
     * 获取当前请求的 HttpSession。
     *
     * @return HttpSession 实例，如果没有会话则返回 null
     */
    @Override
    public HttpSession getSession() {
        return rootJspCtxt.getSession();
    }

    /**
     * 获取当前页面的 Servlet 实例。
     *
     * @return Servlet 实例
     */
    @Override
    public Object getPage() {
        return invokingJspCtxt.getPage();
    }

    /**
     * 获取当前请求的 ServletRequest。
     *
     * @return ServletRequest 实例
     */
    @Override
    public ServletRequest getRequest() {
        return invokingJspCtxt.getRequest();
    }

    /**
     * 获取当前响应的 ServletResponse。
     *
     * @return ServletResponse 实例
     */
    @Override
    public ServletResponse getResponse() {
        return rootJspCtxt.getResponse();
    }

    @Override
    public Exception getException() {
        return invokingJspCtxt.getException();
    }

    /**
     * 获取 Servlet 的配置信息。
     *
     * @return ServletConfig 实例
     */
    @Override
    public ServletConfig getServletConfig() {
        return invokingJspCtxt.getServletConfig();
    }

    /**
     * 获取 ServletContext 上下文。
     *
     * @return ServletContext 实例
     */
    @Override
    public ServletContext getServletContext() {
        if (servletContext == null) {
            servletContext = rootJspCtxt.getServletContext();
        }
        return servletContext;
    }

    /**
     * 将请求转发到指定的相对路径。
     *
     * @param relativeUrlPath 相对 URL 路径
     * @throws ServletException 如果转发失败
     * @throws IOException 如果 I/O 错误
     */
    @Override
    public void forward(String relativeUrlPath) throws ServletException,
            IOException {
        invokingJspCtxt.forward(relativeUrlPath);
    }

    /**
     * 包含指定路径的资源输出。
     *
     * @param relativeUrlPath 相对 URL 路径
     * @throws ServletException 如果包含失败
     * @throws IOException 如果 I/O 错误
     */
    @Override
    public void include(String relativeUrlPath) throws ServletException,
            IOException {
        invokingJspCtxt.include(relativeUrlPath);
    }

    /**
     * 包含指定路径的资源输出，并可选择是否在包含前刷新输出缓冲区。
     *
     * @param relativeUrlPath 相对 URL 路径
     * @param flush 是否在包含前刷新缓冲区（此处被忽略，始终为 false）
     * @throws ServletException 如果包含失败
     * @throws IOException 如果 I/O 错误
     */
    @Override
    public void include(String relativeUrlPath, boolean flush)
            throws ServletException, IOException {
        invokingJspCtxt.include(relativeUrlPath, false);
    }

    @Override
    @Deprecated
    public VariableResolver getVariableResolver() {
        return this;
    }

    /**
     * 推送一个新的 BodyContent 缓冲区。
     *
     * @return 新的 BodyContent 实例
     */
    @Override
    public BodyContent pushBody() {
        return invokingJspCtxt.pushBody();
    }

    /**
     * 推送一个使用指定 Writer 的 JspWriter。
     *
     * @param writer 指定的 Writer
     * @return 新的 JspWriter 实例
     */
    @Override
    public JspWriter pushBody(Writer writer) {
        return invokingJspCtxt.pushBody(writer);
    }

    /**
     * 弹出当前的 BodyContent 缓冲区，恢复到上一个输出流。
     *
     * @return 恢复后的 JspWriter 实例
     */
    @Override
    public JspWriter popBody() {
        return invokingJspCtxt.popBody();
    }

    @Override
    @Deprecated
    public ExpressionEvaluator getExpressionEvaluator() {
        return invokingJspCtxt.getExpressionEvaluator();
    }

    @Override
    public void handlePageException(Exception ex) throws IOException,
            ServletException {
        // 此方法不应被调用，因为生成的 servlet 中使用的是带有 Throwable 参数的 handleException()
        handlePageException((Throwable) ex);
    }

    @Override
    public void handlePageException(Throwable t) throws IOException,
            ServletException {
        invokingJspCtxt.handlePageException(t);
    }

    /**
     * VariableResolver 接口实现
     */
    @Override
    @Deprecated
    public Object resolveVariable(String pName) throws ELException {
        ELContext ctx = this.getELContext();
        return ctx.getELResolver().getValue(ctx, null, pName);
    }

    /**
     * 在标签文件开始时同步变量
     */
    public void syncBeginTagFile() {
        saveNestedVariables();
    }

    /**
     * 在片段调用前同步变量
     */
    public void syncBeforeInvoke() {
        copyTagToPageScope(VariableInfo.NESTED);
        copyTagToPageScope(VariableInfo.AT_BEGIN);
    }

    /**
     * 在标签文件结束时同步变量
     */
    public void syncEndTagFile() {
        copyTagToPageScope(VariableInfo.AT_BEGIN);
        copyTagToPageScope(VariableInfo.AT_END);
        restoreNestedVariables();
    }

    /**
     * 将指定作用域的变量从本 JSP 上下文的虚拟页面作用域复制到调用者 JSP 上下文的页面作用域。
     *
     * @param scope 变量作用域（NESTED、AT_BEGIN 或 AT_END 之一）
     */
    private void copyTagToPageScope(int scope) {
        Iterator<String> iter = null;

        switch (scope) {
        case VariableInfo.NESTED:
            if (nestedVars != null) {
                iter = nestedVars.iterator();
            }
            break;
        case VariableInfo.AT_BEGIN:
            if (atBeginVars != null) {
                iter = atBeginVars.iterator();
            }
            break;
        case VariableInfo.AT_END:
            if (atEndVars != null) {
                iter = atEndVars.iterator();
            }
            break;
        }

        while ((iter != null) && iter.hasNext()) {
            String varName = iter.next();
            Object obj = getAttribute(varName);
            varName = findAlias(varName);
            if (obj != null) {
                invokingJspCtxt.setAttribute(varName, obj);
            } else {
                invokingJspCtxt.removeAttribute(varName, PAGE_SCOPE);
            }
        }
    }

    /**
     * 保存调用者 JSP 上下文中存在的所有 NESTED 变量的值，以便后续恢复。
     */
    private void saveNestedVariables() {
        if (nestedVars != null) {
            for (String varName : nestedVars) {
                varName = findAlias(varName);
                Object obj = invokingJspCtxt.getAttribute(varName);
                if (obj != null) {
                    originalNestedVars.put(varName, obj);
                }
            }
        }
    }

    /**
     * 恢复调用者 JSP 上下文中所有 NESTED 变量的值。
     */
    private void restoreNestedVariables() {
        if (nestedVars != null) {
            for (String varName : nestedVars) {
                varName = findAlias(varName);
                Object obj = originalNestedVars.get(varName);
                if (obj != null) {
                    invokingJspCtxt.setAttribute(varName, obj);
                } else {
                    invokingJspCtxt.removeAttribute(varName, PAGE_SCOPE);
                }
            }
        }
    }

    /**
     * 检查给定的变量名是否被用作别名，如果是，则返回它所代表的变量名。
     *
     * @param varName 要检查的变量名
     * @return 如果 varName 被用作别名，则返回对应的变量名；否则返回 varName 本身
     */
    private String findAlias(String varName) {

        if (aliases == null) {
            return varName;
        }

        String alias = aliases.get(varName);
        if (alias == null) {
            return varName;
        }
        return alias;
    }

    /**
     * 获取与此 JspContext 关联的 ELContext。
     * 如果尚未创建，则创建一个新的 ELContextWrapper。
     *
     * @return ELContext 实例
     */
    @Override
    public ELContext getELContext() {
        if (elContext == null) {
            elContext = new ELContextWrapper(rootJspCtxt.getELContext(), jspTag, this);
            JspFactory factory = JspFactory.getDefaultFactory();
            JspApplicationContext jspAppCtxt = factory.getJspApplicationContext(servletContext);
            if (jspAppCtxt instanceof JspApplicationContextImpl) {
                ((JspApplicationContextImpl) jspAppCtxt).fireListeners(elContext);
            }
        }
        return elContext;
    }


    /**
     * ELContext 包装器类，用于在标签文件中提供隔离的 EL 上下文环境。
     */
    static class ELContextWrapper extends ELContext {

        /** 被包装的 ELContext */
        private final ELContext wrapped;
        
        /** 当前标签处理器 */
        private final JspTag jspTag;
        
        /** 关联的 PageContext */
        private final PageContext pageContext;
        
        /** 导入处理器 */
        private ImportHandler importHandler;

        private ELContextWrapper(ELContext wrapped, JspTag jspTag, PageContext pageContext) {
            this.wrapped = wrapped;
            this.jspTag = jspTag;
            this.pageContext = pageContext;
        }

        ELContext getWrappedELContext() {
            return wrapped;
        }

        @Override
        public void setPropertyResolved(boolean resolved) {
            wrapped.setPropertyResolved(resolved);
        }

        @Override
        public void setPropertyResolved(Object base, Object property) {
            wrapped.setPropertyResolved(base, property);
        }

        @Override
        public boolean isPropertyResolved() {
            return wrapped.isPropertyResolved();
        }

        @Override
        public void putContext(@SuppressWarnings("rawtypes") Class key, Object contextObject) {
            if (key != JspContext.class) {
                wrapped.putContext(key, contextObject);
            }
        }

        @Override
        public Object getContext(@SuppressWarnings("rawtypes") Class key) {
            if (key == JspContext.class) {
                return pageContext;
            }
            return wrapped.getContext(key);
        }

        @Override
        public ImportHandler getImportHandler() {
            if (importHandler == null) {
                importHandler = new ImportHandler();
                if (jspTag instanceof JspSourceImports) {
                    Set<String> packageImports = ((JspSourceImports) jspTag).getPackageImports();
                    if (packageImports != null) {
                        for (String packageImport : packageImports) {
                            importHandler.importPackage(packageImport);
                        }
                    }
                    Set<String> classImports = ((JspSourceImports) jspTag).getClassImports();
                    if (classImports != null) {
                        for (String classImport : classImports) {
                            importHandler.importClass(classImport);
                        }
                    }
                }

            }
            return importHandler;
        }

        @Override
        public Locale getLocale() {
            return wrapped.getLocale();
        }

        @Override
        public void setLocale(Locale locale) {
            wrapped.setLocale(locale);
        }

        @Override
        public void addEvaluationListener(EvaluationListener listener) {
            wrapped.addEvaluationListener(listener);
        }

        @Override
        public List<EvaluationListener> getEvaluationListeners() {
            return wrapped.getEvaluationListeners();
        }

        @Override
        public void notifyBeforeEvaluation(String expression) {
            wrapped.notifyBeforeEvaluation(expression);
        }

        @Override
        public void notifyAfterEvaluation(String expression) {
            wrapped.notifyAfterEvaluation(expression);
        }

        @Override
        public void notifyPropertyResolved(Object base, Object property) {
            wrapped.notifyPropertyResolved(base, property);
        }

        @Override
        public boolean isLambdaArgument(String name) {
            return wrapped.isLambdaArgument(name);
        }

        @Override
        public Object getLambdaArgument(String name) {
            return wrapped.getLambdaArgument(name);
        }

        @Override
        public void enterLambdaScope(Map<String, Object> arguments) {
            wrapped.enterLambdaScope(arguments);
        }

        @Override
        public void exitLambdaScope() {
            wrapped.exitLambdaScope();
        }

        @Override
        public Object convertToType(Object obj, Class<?> type) {
            return wrapped.convertToType(obj, type);
        }

        @Override
        public ELResolver getELResolver() {
            return wrapped.getELResolver();
        }

        @Override
        public FunctionMapper getFunctionMapper() {
            return wrapped.getFunctionMapper();
        }

        @Override
        public VariableMapper getVariableMapper() {
            return wrapped.getVariableMapper();
        }
    }
}
