package org.apache.jasper.runtime;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import javax.el.ELContextListener;
import javax.el.ELResolver;
import javax.el.ExpressionFactory;
import javax.servlet.ServletContext;
import javax.servlet.jsp.JspApplicationContext;

import org.apache.jasper.Constants;
import org.apache.jasper.util.ExceptionUtils;
import org.apache.juli.logging.Log;
import org.apache.juli.logging.LogFactory;

/**
 * JspApplicationContext 的实现类。
 * <p>
 * 该类实现了 JspApplicationContext 接口，管理 JSP 应用上下文。
 * 提供了 EL 表达式相关的功能，包括表达式工厂、解析器和上下文监听器。
 * </p>
 *
 * @author Jacob Hookom
 */
public class JspApplicationContextImpl implements JspApplicationContext {

    private static final Log log = LogFactory.getLog(JspApplicationContextImpl.class);

    // 存储每个 ServletContext 对应的 JspApplicationContext 实例
    private static final Map<ServletContext, JspApplicationContextImpl> map =
            new WeakHashMap<>();

    /**
     * 获取指定 ServletContext 的 JspApplicationContextImpl 实例。
     * <p>
     * 如果该 ServletContext 还没有关联的实例，则创建一个新的。
     * </p>
     *
     * @param context ServletContext 实例
     * @return 与该上下文关联的 JspApplicationContextImpl 实例
     */
    public static synchronized JspApplicationContextImpl getInstance(
            ServletContext context) {
        JspApplicationContextImpl impl = map.get(context);
        if (impl == null) {
            impl = new JspApplicationContextImpl();
            map.put(context, impl);
        }
        return impl;
    }

    // EL 表达式工厂，用于创建 EL 表达式
    private ExpressionFactory expressionFactory;
    // EL 解析器列表，用于添加自定义解析器
    private final List<ELResolver> resolvers = new ArrayList<>();
    // EL 上下文监听器列表
    private final List<ELContextListener> listeners = new ArrayList<>();

    /**
     * 私有构造方法，确保只能通过 getInstance 创建实例。
     */
    private JspApplicationContextImpl() {
    }

    /**
     * 添加 EL 解析器。
     * <p>
     * 将自定义的 EL 解析器添加到列表中，用于解析 EL 表达式中的变量和属性。
     * </p>
     *
     * @param resolver 要添加的 EL 解析器
     */
    @Override
    public void addELResolver(ELResolver resolver) {
        this.resolvers.add(resolver);
    }

    /**
     * 获取 EL 表达式工厂。
     * <p>
     * 如果尚未创建，则使用 SPI 机制加载 ExpressionFactory。
     * </p>
     *
     * @return EL 表达式工厂实例
     */
    @Override
    public ExpressionFactory getExpressionFactory() {
        if (this.expressionFactory == null) {
            this.expressionFactory = ExpressionFactory.newInstance();
        }
        return this.expressionFactory;
    }

    /**
     * 添加 EL 上下文监听器。
     * <p>
     * 监听器会在 EL 上下文创建时被通知，可以对其进行修改。
     * </p>
     *
     * @param listener 要添加的 EL 上下文监听器
     */
    @Override
    public void addELContextListener(ELContextListener listener) {
        this.listeners.add(listener);
    }

    /**
     * 获取注册的 EL 解析器列表。
     * <p>
     * 返回所有已添加的自定义 EL 解析器，用于构建完整的 EL 解析器链。
     * </p>
     *
     * @return EL 解析器列表
     */
    public List<ELResolver> getELResolvers() {
        return this.resolvers;
    }

    /**
     * 获取 EL 上下文监听器列表。
     * <p>
     * 返回所有已注册的 EL 上下文监听器，用于在创建 EL 上下文时进行通知。
     * </p>
     *
     * @return EL 上下文监听器列表
     */
    public List<ELContextListener> getELContextListeners() {
        return this.listeners;
    }
}
