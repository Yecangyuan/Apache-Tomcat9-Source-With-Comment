package org.apache.jasper.runtime;

import javax.servlet.ServletConfig;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.Tag;

import org.apache.jasper.Constants;
import org.apache.jasper.compiler.Localizer;
import org.apache.juli.logging.LogFactory;
import org.apache.tomcat.InstanceManager;

/**
 * 标签处理器池，用于重用标签处理器实例。
 *
 * @author Jan Luehe
 */
public class TagHandlerPool {

    /** 标签处理器数组，存储可重用的处理器实例 */
    private Tag[] handlers;

    /** 标签池类名配置选项 */
    public static final String OPTION_TAGPOOL = "tagpoolClassName";
    /** 标签池最大大小配置选项 */
    public static final String OPTION_MAXSIZE = "tagpoolMaxSize";

    /** 下一个可用标签处理器的索引 */
    private int current;
    /** 实例管理器，用于创建和管理标签处理器实例 */
    protected InstanceManager instanceManager = null;

    public static TagHandlerPool getTagHandlerPool(ServletConfig config) {
        TagHandlerPool result = null;

        String tpClassName = getOption(config, OPTION_TAGPOOL, null);
        if (tpClassName != null) {
            try {
                Class<?> c = Class.forName(tpClassName);
                result = (TagHandlerPool) c.getConstructor().newInstance();
            } catch (Exception e) {
                LogFactory.getLog(TagHandlerPool.class).info(Localizer.getMessage("jsp.error.tagHandlerPool"), e);
                result = null;
            }
        }
        if (result == null) {
            result = new TagHandlerPool();
        }
        result.init(config);

        return result;
    }

    protected void init(ServletConfig config) {
        int maxSize = -1;
        String maxSizeS = getOption(config, OPTION_MAXSIZE, null);
        if (maxSizeS != null) {
            try {
                maxSize = Integer.parseInt(maxSizeS);
            } catch (Exception ex) {
                maxSize = -1;
            }
        }
        if (maxSize < 0) {
            maxSize = Constants.MAX_POOL_SIZE;
        }
        this.handlers = new Tag[maxSize];
        this.current = -1;
        instanceManager = InstanceManagerFactory.getInstanceManager(config);
    }

    /**
     * Constructs a tag handler pool with the default capacity.
     */
    public TagHandlerPool() {
        // Nothing - jasper generated servlets call the other constructor,
        // this should be used in future + init .
    }

    /**
     * 获取标签处理器。
     * 从池中获取下一个可用的标签处理器，如果池为空则实例化一个新的。
     *
     * @param handlerClass
     *            标签处理器类
     * @return 重用或新实例化的标签处理器
     * @throws JspException
     *             如果标签处理器无法实例化
     */
    public Tag get(Class<? extends Tag> handlerClass) throws JspException {
        Tag handler;
        synchronized (this) {
            if (current >= 0) {
                handler = handlers[current--];
                return handler;
            }
        }

        // Out of sync block - there is no need for other threads to
        // wait for us to construct a tag for this thread.
        try {
            if (Constants.USE_INSTANCE_MANAGER_FOR_TAGS) {
                return (Tag) instanceManager.newInstance(
                        handlerClass.getName(), handlerClass.getClassLoader());
            } else {
                Tag instance = handlerClass.getConstructor().newInstance();
                instanceManager.newInstance(instance);
                return instance;
            }
        } catch (Exception e) {
            Throwable t = ExceptionUtils.unwrapInvocationTargetException(e);
            ExceptionUtils.handleThrowable(t);
            throw new JspException(e.getMessage(), t);
        }
    }

    /**
     * 重用标签处理器。
     * 将指定的标签处理器添加到池中，如果池已满则调用该处理器的 release() 方法。
     *
     * @param handler
     *            要添加到池中的标签处理器
     */
    public void reuse(Tag handler) {
        synchronized (this) {
            if (current < (handlers.length - 1)) {
                handlers[++current] = handler;
                return;
            }
        }
        // There is no need for other threads to wait for us to release
        JspRuntimeLibrary.releaseTag(handler, instanceManager);
    }

    /**
     * 释放池中的所有处理器。
     * 调用池中所有可用标签处理器的 release() 方法。
     */
    public synchronized void release() {
        for (int i = current; i >= 0; i--) {
            JspRuntimeLibrary.releaseTag(handlers[i], instanceManager);
        }
    }


    protected static String getOption(ServletConfig config, String name,
            String defaultV) {
        if (config == null) {
            return defaultV;
        }

        String value = config.getInitParameter(name);
        if (value != null) {
            return value;
        }
        if (config.getServletContext() == null) {
            return defaultV;
        }
        value = config.getServletContext().getInitParameter(name);
        if (value != null) {
            return value;
        }
        return defaultV;
    }

}
