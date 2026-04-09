package org.apache.jasper.runtime;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletOutputStream;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;
import javax.servlet.jsp.JspWriter;

/**
 * 用于 JSP 'include' 动作的 ServletResponse 包装器。
 *
 * 此包装器响应对象被传递给 RequestDispatcher.include()，以便被包含资源的
 * 输出被追加到包含页面的输出中。
 *
 * @author Pierre Delisle
 */

public class ServletResponseWrapperInclude extends HttpServletResponseWrapper {

    /**
     * 追加到包含页面 JspWriter 的 PrintWriter。
     */
    private final PrintWriter printWriter;

    private final JspWriter jspWriter;

    /**
     * 构造方法，创建 ServletResponseWrapperInclude 实例。
     *
     * @param response 原始的 ServletResponse 对象
     * @param jspWriter 包含页面的 JspWriter，用于接收被包含资源的输出
     */
    public ServletResponseWrapperInclude(ServletResponse response,
                                         JspWriter jspWriter) {
        super((HttpServletResponse)response);
        this.printWriter = new PrintWriter(jspWriter);
        this.jspWriter = jspWriter;
    }

    /**
     * 获取包装了包含页面 JspWriter 的 PrintWriter。
     *
     * @return 包装在包含页面 JspWriter 周围的 PrintWriter
     * @throws IOException 如果发生 I/O 错误
     */
    @Override
    public PrintWriter getWriter() throws IOException {
        return printWriter;
    }

    /**
     * 获取 Servlet 输出流。
     * 在 JSP include 动作中不支持使用输出流，调用此方法将抛出异常。
     *
     * @return ServletOutputStream（此方法总是抛出异常）
     * @throws IllegalStateException 总是抛出，表示在 include 中不支持此方法
     */
    @Override
    public ServletOutputStream getOutputStream() throws IOException {
        throw new IllegalStateException();
    }

    /**
     * 清空与包含页面关联的 JspWriter 的输出缓冲区。
     */
    @Override
    public void resetBuffer() {
        try {
            jspWriter.clearBuffer();
        } catch (IOException ioe) {
        }
    }
}
