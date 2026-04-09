package org.apache.jasper.runtime;

import javax.servlet.jsp.JspContext;
import javax.servlet.jsp.PageContext;
import javax.servlet.jsp.tagext.JspFragment;
import javax.servlet.jsp.tagext.JspTag;

/**
 * JspFragment 的帮助类，所有 Jsp Fragment 帮助类都继承自此类。
 * 该类允许在一个类中模拟多个片段，从而减轻类加载器的负担，
 * 因为单个页面中可能存在大量的 JspFragment。
 * <p>
 * 该类还为 JspFragment 的实现提供了各种实用方法。
 *
 * @author Mark Roth
 */
public abstract class JspFragmentHelper extends JspFragment {

    protected final int discriminator;
    protected final JspContext jspContext;
    protected final PageContext _jspx_page_context;
    protected final JspTag parentTag;

    /**
     * 构造方法，初始化 JspFragmentHelper 实例。
     *
     * @param discriminator 区分标识符，用于区分同一类中的不同片段
     * @param jspContext JSP 上下文对象，提供对 JSP 环境的访问
     * @param parentTag 父标签对象，表示包含此片段的父 JspTag
     */
    public JspFragmentHelper( int discriminator, JspContext jspContext,
        JspTag parentTag )
    {
        this.discriminator = discriminator;
        this.jspContext = jspContext;
        if(jspContext instanceof PageContext) {
            _jspx_page_context = (PageContext)jspContext;
        } else {
            _jspx_page_context = null;
        }
        this.parentTag = parentTag;
    }

    /**
     * 获取与此片段关联的 JspContext 对象。
     *
     * @return JSP 上下文对象
     */
    @Override
    public JspContext getJspContext() {
        return this.jspContext;
    }

    /**
     * 获取包含此片段的父 JspTag 对象。
     *
     * @return 父标签对象
     */
    public JspTag getParentTag() {
        return this.parentTag;
    }

}
