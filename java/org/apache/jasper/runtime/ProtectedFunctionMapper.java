package org.apache.jasper.runtime;

import java.lang.reflect.Method;
import java.util.HashMap;

import javax.servlet.jsp.el.FunctionMapper;

/**
 * 受保护的函数映射器，用于将 EL 函数映射到对应的 Java 方法。
 * 保持实际的 Method 对象受保护，以防止 JSP 页面间接进行反射操作。
 *
 * @author Mark Roth
 * @author Kin-man Chung
 */
@SuppressWarnings("deprecation") // Have to support old JSP EL API
public final class ProtectedFunctionMapper extends javax.el.FunctionMapper
        implements FunctionMapper {

    /**
     * 将 "前缀:名称" 映射到 java.lang.Method 对象的哈希表。
     */
    private HashMap<String,Method> fnmap = null;

    /**
     * 如果映射表中只有一个函数，这是该函数对应的 Method 对象。
     */
    private Method theMethod = null;

    /**
     * 私有构造方法，防止外部实例化。
     */
    private ProtectedFunctionMapper() {
    }

    /**
     * 生成的 Servlet 和标签处理器实现调用此方法获取 ProtectedFunctionMapper 实例。
     *
     * @return 一个新的受保护函数映射器实例
     */
    public static ProtectedFunctionMapper getInstance() {
        ProtectedFunctionMapper funcMapper = new ProtectedFunctionMapper();
        funcMapper.fnmap = new HashMap<>();
        return funcMapper;
    }

    /**
     * 将给定的 EL 函数前缀和名称映射到指定的 Java 方法。
     *
     * @param fnQName
     *            EL 函数的限定名（包含前缀）
     * @param c
     *            包含该 Java 方法的类
     * @param methodName
     *            Java 方法的名称
     * @param args
     *            Java 方法的参数类型数组
     * @throws RuntimeException
     *             如果找不到具有给定签名的方法
     */
    public void mapFunction(String fnQName, final Class<?> c,
            final String methodName, final Class<?>[] args) {
        // 如果传入空值则跳过。它们表示通过 lambda 或 ImportHandler 添加的函数；
        // 这两种情况都不需要放入映射表中。
        if (fnQName == null) {
            return;
        }
        Method method;
        try {
            method = c.getMethod(methodName, args);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(
                    "Invalid function mapping - no such method: "
                            + e.getMessage());
        }

        this.fnmap.put(fnQName, method);
    }

    /**
     * 创建此类的一个实例，并存储给定 EL 函数前缀和名称对应的 Method 对象。
     * 此方法用于 EL 表达式中只有一个函数的情况。
     *
     * @param fnQName
     *            EL 函数的限定名（包含前缀）
     * @param c
     *            包含该 Java 方法的类
     * @param methodName
     *            Java 方法的名称
     * @param args
     *            Java 方法的参数类型数组
     * @throws RuntimeException
     *             如果找不到具有给定签名的方法
     * @return 包含函数映射的 ProtectedFunctionMapper 实例
     */
    public static ProtectedFunctionMapper getMapForFunction(String fnQName,
            final Class<?> c, final String methodName, final Class<?>[] args) {
        Method method = null;
        ProtectedFunctionMapper funcMapper = new ProtectedFunctionMapper();
        // 如果传入空值则跳过。它们表示通过 lambda 或 ImportHandler 添加的函数；
        // 这两种情况都不需要放入映射表中。
        if (fnQName != null) {
            try {
                method = c.getMethod(methodName, args);
            } catch (NoSuchMethodException e) {
                throw new RuntimeException(
                        "Invalid function mapping - no such method: "
                                + e.getMessage());
            }
        }
        funcMapper.theMethod = method;
        return funcMapper;
    }

    /**
     * 将指定的前缀和本地名称解析为 Java.lang.Method 对象。
     * 如果找不到对应的前缀和本地名称，则返回 null。
     *
     * @param prefix
     *            函数的前缀
     * @param localName
     *            函数的短名称
     * @return 方法映射的结果。返回 null 表示未找到对应条目
     */
    @Override
    public Method resolveFunction(String prefix, String localName) {
        if (this.fnmap != null) {
            return this.fnmap.get(prefix + ":" + localName);
        }
        return theMethod;
    }
}
