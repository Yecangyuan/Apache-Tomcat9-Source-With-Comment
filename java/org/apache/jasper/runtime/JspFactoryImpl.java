package org.apache.jasper.runtime;

import java.io.IOException;
import java.security.AccessController;
import java.security.PrivilegedAction;

import javax.servlet.Servlet;
import javax.servlet.ServletContext;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.jsp.JspApplicationContext;
import javax.servlet.jsp.JspEngineInfo;
import javax.servlet.jsp.JspFactory;
import javax.servlet.jsp.PageContext;

import org.apache.jasper.Constants;

/**
 * JspFactory 的实现类。
 * <p>
 * 该类实现了 JspFactory 抽象类，提供了创建和管理 PageContext 对象的功能。
 * 使用线程本地存储来实现 PageContext 对象池，以提高性能。
 * </p>
 *
 * @author Anil K. Vijendran
 */
public class JspFactoryImpl extends JspFactory {

    // 是否使用 PageContext 对象池，默认开启
    private static final boolean USE_POOL =
        Boolean.parseBoolean(System.getProperty("org.apache.jasper.runtime.JspFactoryImpl.USE_POOL", "true"));
    // 对象池大小，默认为 8
    private static final int POOL_SIZE =
        Integer.parseInt(System.getProperty("org.apache.jasper.runtime.JspFactoryImpl.POOL_SIZE", "8"));

    // 线程本地变量，存储每个线程的 PageContext 对象池
    private final ThreadLocal<PageContextPool> localPool = new ThreadLocal<>();

    /**
     * 获取 PageContext。
     * <p>
     * 根据传入的 Servlet 和请求响应对象创建或从池中获取 PageContext 实例。
     * 如果启用了安全管理器，则在特权模式下执行。
     * </p>
     *
     * @param servlet 当前 Servlet 实例
     * @param request Servlet 请求对象
     * @param response Servlet 响应对象
     * @param errorPageURL 错误页面 URL
     * @param needsSession 是否需要会话
     * @param bufferSize 缓冲区大小
     * @param autoflush 是否自动刷新
     * @return 初始化后的 PageContext 实例
     */
    @Override
    public PageContext getPageContext(Servlet servlet, ServletRequest request,
            ServletResponse response, String errorPageURL, boolean needsSession,
            int bufferSize, boolean autoflush) {

        if( Constants.IS_SECURITY_ENABLED ) {
            PrivilegedGetPageContext dp = new PrivilegedGetPageContext(
                    this, servlet, request, response, errorPageURL,
                    needsSession, bufferSize, autoflush);
            return AccessController.doPrivileged(dp);
        } else {
            return internalGetPageContext(servlet, request, response,
                    errorPageURL, needsSession,
                    bufferSize, autoflush);
        }
    }

    /**
     * 释放 PageContext。
     * <p>
     * 将使用完毕的 PageContext 返回到对象池中以便复用。
     * 如果启用了安全管理器，则在特权模式下执行。
     * </p>
     *
     * @param pc 需要释放的 PageContext 实例
     */
    @Override
    public void releasePageContext(PageContext pc) {
        if( pc == null ) {
            return;
        }
        if( Constants.IS_SECURITY_ENABLED ) {
            PrivilegedReleasePageContext dp = new PrivilegedReleasePageContext(
                    this,pc);
            AccessController.doPrivileged(dp);
        } else {
            internalReleasePageContext(pc);
        }
    }

    @Override
    public JspEngineInfo getEngineInfo() {
        return new JspEngineInfo() {
            @Override
            public String getSpecificationVersion() {
                return Constants.SPEC_VERSION;
            }
        };
    }

    private PageContext internalGetPageContext(Servlet servlet, ServletRequest request,
            ServletResponse response, String errorPageURL, boolean needsSession,
            int bufferSize, boolean autoflush) {

        PageContext pc;
        if (USE_POOL) {
            PageContextPool pool = localPool.get();
            if (pool == null) {
                pool = new PageContextPool();
                localPool.set(pool);
            }
            pc = pool.get();
            if (pc == null) {
                pc = new PageContextImpl();
            }
        } else {
            pc = new PageContextImpl();
        }

        try {
            pc.initialize(servlet, request, response, errorPageURL,
                    needsSession, bufferSize, autoflush);
        } catch (IOException ioe) {
            // Implementation never throws IOE but can't change the signature
            // since it is part of the JSP API
        }

        return pc;
    }

    private void internalReleasePageContext(PageContext pc) {
        pc.release();
        if (USE_POOL && (pc instanceof PageContextImpl)) {
            localPool.get().put(pc);
        }
    }

    private static class PrivilegedGetPageContext
            implements PrivilegedAction<PageContext> {

        private JspFactoryImpl factory;
        private Servlet servlet;
        private ServletRequest request;
        private ServletResponse response;
        private String errorPageURL;
        private boolean needsSession;
        private int bufferSize;
        private boolean autoflush;

        PrivilegedGetPageContext(JspFactoryImpl factory, Servlet servlet,
                ServletRequest request, ServletResponse response, String errorPageURL,
                boolean needsSession, int bufferSize, boolean autoflush) {
            this.factory = factory;
            this.servlet = servlet;
            this.request = request;
            this.response = response;
            this.errorPageURL = errorPageURL;
            this.needsSession = needsSession;
            this.bufferSize = bufferSize;
            this.autoflush = autoflush;
        }

        @Override
        public PageContext run() {
            return factory.internalGetPageContext(servlet, request, response,
                    errorPageURL, needsSession, bufferSize, autoflush);
        }
    }

    private static class PrivilegedReleasePageContext
            implements PrivilegedAction<Void> {

        private JspFactoryImpl factory;
        private PageContext pageContext;

        PrivilegedReleasePageContext(JspFactoryImpl factory,
                PageContext pageContext) {
            this.factory = factory;
            this.pageContext = pageContext;
        }

        @Override
        public Void run() {
            factory.internalReleasePageContext(pageContext);
            return null;
        }
    }

    private static final class PageContextPool  {

        private final PageContext[] pool;

        private int current = -1;

        PageContextPool() {
            this.pool = new PageContext[POOL_SIZE];
        }

        public void put(PageContext o) {
            if (current < (POOL_SIZE - 1)) {
                current++;
                pool[current] = o;
            }
        }

        public PageContext get() {
            PageContext item = null;
            if (current >= 0) {
                item = pool[current];
                current--;
            }
            return item;
        }

    }

    @Override
    public JspApplicationContext getJspApplicationContext(
            final ServletContext context) {
        if (Constants.IS_SECURITY_ENABLED) {
            return AccessController.doPrivileged(
                    (PrivilegedAction<JspApplicationContext>) () -> JspApplicationContextImpl.getInstance(context));
        } else {
            return JspApplicationContextImpl.getInstance(context);
        }
    }
}
