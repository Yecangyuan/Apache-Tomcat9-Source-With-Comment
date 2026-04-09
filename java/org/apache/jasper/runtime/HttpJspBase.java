package org.apache.jasper.runtime;

import java.io.IOException;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.jsp.HttpJspPage;

import org.apache.jasper.Constants;
import org.apache.jasper.compiler.Localizer;

/**
 * HTTP JSP 页面的基类，所有 JSP 编译后的 Servlet 都继承此类。
 *
 * @author Anil K. Vijendran
 */
public abstract class HttpJspBase extends HttpServlet implements HttpJspPage {

    private static final long serialVersionUID = 1L;

    protected HttpJspBase() {
    }

    /**
     * 初始化
     */
    @Override
    public final void init(ServletConfig config)
        throws ServletException
    {
        super.init(config);
        jspInit();
        _jspInit();
    }

    /**
     * 获取 Servlet 信息
     */
    @Override
    public String getServletInfo() {
        return Localizer.getMessage("jsp.engine.info", Constants.SPEC_VERSION);
    }

    @Override
    public final void destroy() {
        jspDestroy();
        _jspDestroy();
    }

    /**
     * 服务方法
     */
    @Override
    public final void service(HttpServletRequest request, HttpServletResponse response)
        throws ServletException, IOException
    {
        _jspService(request, response);
    }

    @Override
    public void jspInit() {
    }

    /**
     * JSP 初始化方法，由子类实现
     */
    public void _jspInit() {
    }

    @Override
    public void jspDestroy() {
    }

    protected void _jspDestroy() {
    }

    /**
     * JSP 服务方法，由子类实现
     */
    @Override
    public abstract void _jspService(HttpServletRequest request,
                                     HttpServletResponse response)
        throws ServletException, IOException;
}
